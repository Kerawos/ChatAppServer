package pl.mareksowa.SerwerChat.models.sockets;

import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pl.mareksowa.SerwerChat.models.MessageFactory;
import pl.mareksowa.SerwerChat.models.UserModel;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;


/**
 * Class responsible for chat logic
 */
@EnableWebSocket
@Configuration
public class ChatSocket extends TextWebSocketHandler implements WebSocketConfigurer {

    Map<String, UserModel> userList = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    private ChatManager chatManager;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(this, "/chat")
                .setAllowedOrigins("*");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        userList.put(session.getId(), new UserModel(session)); //add new User by ID
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String ipAd = null;
        String currentUserNick = null;
        UserModel userModel = userList.get(session.getId());
        Type factory = new TypeToken<MessageFactory>() {}.getType();
        //pack message to Json format
        MessageFactory factoryCreated = MessageFactory.GSON.fromJson(message.getPayload(), factory);
        MessageFactory factoryNewMessage;

        switch (factoryCreated.getMessageType()) {
            case SEND_MESSAGE: {
                factoryNewMessage = new MessageFactory();

                //check if user sent empty message
                if (factoryCreated.getMessage().length() == 0){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage("SERVER: EMPTY MESSAGE CANNOT BE SENT");
                    chatManager.sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                //check if user sent spam
                if (factoryCreated.getMessage().length() > 140){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage("SERVER: MESSAGE CANNOT BY LONGER THAN 140 LETTERS");
                    chatManager.sendMessageToUser(userModel, factoryNewMessage);
                    break;
                }

                //check VULGARISMS
                if (chatManager.isVulgarityAbsent(factoryCreated.getMessage())){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage("SERVER: MESSAGE SUSPEND, REASON = VULGARISM DETECTED.. ");
                    chatManager.sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                //1st type of command
                if (factoryCreated.getMessage().equals("/users")){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage(chatManager.showOnlineUsers(userList));
                    chatManager.sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                //get history of user
                if (factoryCreated.getMessage().length()>8){
                    if (factoryCreated.getMessage().substring(0,9).equals("/history ")){
                        currentUserNick = factoryCreated.getMessage().substring(9, factoryCreated.getMessage().length());
                        if (chatManager.isUserPresent(currentUserNick, userList)){
                            factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                            factoryNewMessage.setMessage("SERVER: User-> " + currentUserNick + " history: \n"
                                    + chatManager.showHistory(chatManager.getUserModelAfterNick(currentUserNick,
                                        userList), userList).toString());
                            chatManager.sendMessageToUser(userModel, factoryNewMessage);
                        } else {
                            factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                            factoryNewMessage.setMessage("SERVER: given user (" + currentUserNick + ") not exist..-> ");
                            chatManager.sendMessageToUser(userModel, factoryNewMessage);
                        }
                        return;
                    }
                }

                //reset block list
                if (factoryCreated.getMessage().length()>11){
                    if (factoryCreated.getMessage().substring(0,12).equals("/block reset")){
                        userModel.resetBlockedList();
                        factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                        factoryNewMessage.setMessage("SERVER: block list has been reset");
                        chatManager.sendMessageToUser(userModel, factoryNewMessage);
                        return;
                    }
                }

                //add user to block list
                if (factoryCreated.getMessage().length()>7){
                    if (factoryCreated.getMessage().substring(0,7).equals("/block ")){
                        currentUserNick = factoryCreated.getMessage().substring(7, factoryCreated.getMessage().length());
                        if (currentUserNick.equals(userModel.getNick())){
                            factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                            factoryNewMessage.setMessage("SERVER: you cannot block yourself.. ");
                            chatManager.sendMessageToUser(userModel, factoryNewMessage);
                        } else if (chatManager.isUserPresent(currentUserNick, userList)) {
                            factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                            userModel.addBlockedUser(chatManager.getUserModelAfterNick(currentUserNick, userList));
                            factoryNewMessage.setMessage("SERVER: user (" + currentUserNick + ") has been blocked");
                            chatManager.sendMessageToUser(userModel, factoryNewMessage);
                        } else {
                            factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                            factoryNewMessage.setMessage("SERVER: given user (" + currentUserNick + ") not exist..-> ");
                            chatManager.sendMessageToUser(userModel, factoryNewMessage);
                        }
                        return;
                    }
                }

                //show block list
                if (factoryCreated.getMessage().equals("/blockedList")){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage("SERVER: blocked list :\n" + chatManager.showBlockedUserNicks(userModel, userList));
                    chatManager.sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                //send message and set nick before message
                userModel.addMessageToHistory(factoryCreated.getMessage());
                factoryNewMessage.setMessage(userModel.getNick() + ": " + factoryCreated.getMessage());
                factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                chatManager.sendMessageThruFilter(userModel,factoryNewMessage, userList);
                break;
            }
            case USER_JOIN:
                break;
            case USER_LEFT:
                break;
            case SET_NICK:{
                factoryNewMessage = new MessageFactory();
                if (chatManager.isVulgarityAbsent(factoryCreated.getMessage())){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.NICK_NOT_FREE);
                    factoryNewMessage.setMessage("SERVER: NICK CONTAIN VULGARISM, IT COULDN'T BE... ");
                    chatManager.sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                factoryNewMessage = new MessageFactory();
                if (factoryCreated.getMessage().equals("SERVER")){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.NICK_NOT_FREE);
                    factoryNewMessage.setMessage("SERVER: NICK BELONG TO THE SERVER, IT COULDN'T BE... ");
                    chatManager.sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                if (!chatManager.isNickFree(factoryCreated.getMessage(), userList)){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.NICK_NOT_FREE);
                    factoryNewMessage.setMessage("SERVER: NICK IS ALREADY TAKEN MY FRIEND...");
                    chatManager.sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                try {
                    ipAd = String.valueOf(InetAddress.getLocalHost());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                sendPacket(factoryCreated.getMessage() + "(" + ipAd + ")", MessageFactory.MessageType.USER_JOIN);
                userModel.setNick(factoryCreated.getMessage());
                factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                factoryNewMessage.setMessage(
                        "SERVER: NICK HAS BEEN SET " +
                        "\nSERVER: TO SEND MESSAGE PRESS 'ENTER' " +
                        "\nSERVER: MESSAGES CANNOT BE LONGER THAN 140 LETTERS! " +
                        "\nSERVER: TO VIEW ALL ACTIVE USERS ON CHAT TYPE '/users' ");
                chatManager.sendMessageToUser(userModel, factoryNewMessage);
                break;
            }
            case NICK_NOT_FREE:
                break;
        }
    }

    public void sendPacket(String nick, MessageFactory.MessageType type){
        MessageFactory factory = new MessageFactory();
        factory.setMessageType(type);
        factory.setMessage(nick);
        chatManager.sendMessageToAll(factory, userList);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UserModel userModel = userList.get(session.getId());
        sendPacket(userModel.getNick(), MessageFactory.MessageType.USER_LEFT);
        userList.remove(session.getId());
    }
}
