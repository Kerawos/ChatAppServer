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
    private ChatManager chatManager; //--> some bug from IntelliJ indicate it as error that is not

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
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String clientIP = null;
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
                    chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                            "SERVER: EMPTY MESSAGE CANNOT BE SENT");
                    return;
                }

                //check if user sent spam
                if (factoryCreated.getMessage().length() > 140){
                    chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                            "SERVER: MESSAGE CANNOT BY LONGER THAN 140 LETTERS");
                    break;
                }

                //check VULGARISMS
                if (chatManager.isVulgarityAbsent(factoryCreated.getMessage())){
                    chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                            "SERVER: MESSAGE SUSPEND, REASON = VULGARISM DETECTED.. ");
                    return;
                }

                //1st type of command
                if (factoryCreated.getMessage().equals("/users")){
                    chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                            chatManager.showOnlineUsers(userList));
                    return;
                }

                //get history of user
                if (factoryCreated.getMessage().length()>8){
                    if (factoryCreated.getMessage().substring(0,9).equals("/history ")){
                        currentUserNick = factoryCreated.getMessage().substring(9, factoryCreated.getMessage().length());
                        if (chatManager.isUserPresent(currentUserNick, userList)){
                            chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                                    "SERVER: User-> " + currentUserNick + " history: \n"
                                            + chatManager.showHistory(chatManager.getUserModelAfterNick(currentUserNick,
                                            userList), userList).toString());
                        } else {
                            chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                                    "SERVER: given user (" + currentUserNick + ") not exist..-> ");
                        }
                        return;
                    }
                }

                //reset block list
                if (factoryCreated.getMessage().length()>11){
                    if (factoryCreated.getMessage().substring(0,12).equals("/block reset")){
                        userModel.resetBlockedList();
                        chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                                "SERVER: block list has been reset");
                        return;
                    }
                }

                //show block list
                if (factoryCreated.getMessage().equals("/block list")){
                    chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                            "SERVER: blocked list :\n" + chatManager.showBlockedUserNicks(userModel, userList));
                    return;
                }

                //add user to block list
                if (factoryCreated.getMessage().length()>7){
                    if (factoryCreated.getMessage().substring(0,7).equals("/block ")){
                        currentUserNick = factoryCreated.getMessage().substring(7, factoryCreated.getMessage().length());
                        if (currentUserNick.equals(userModel.getNick())){
                            chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                                    "SERVER: you cannot block yourself.. ");
                        } else if (chatManager.isUserPresent(currentUserNick, userList)) {
                            userModel.blockUser(chatManager.getUserModelAfterNick(currentUserNick, userList));
                            chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                                    "SERVER: user (" + currentUserNick + ") has been blocked");
                        } else {
                            chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                                    "SERVER: given user (" + currentUserNick + ") not exist..-> ");
                        }
                        return;
                    }
                }

                //show menu
                if (factoryCreated.getMessage().equals("/help")){
                    chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                            chatManager.getHelpMenu(userModel.getNick()));
                    break;
                }

                //send message and set nick before message
                userModel.addMessageToHistory(factoryCreated.getMessage());
                chatManager.sendPacketThruFilter(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                        userModel.getNick() + ": " + factoryCreated.getMessage(), userList);
                break;
            }
            case USER_JOIN:
                break;
            case USER_LEFT:
                break;
            case SET_NICK:{
                factoryNewMessage = new MessageFactory();
                if (chatManager.isVulgarityAbsent(factoryCreated.getMessage())){
                    chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.NICK_NOT_FREE,
                            "SERVER: NICK CONTAIN VULGARISM, IT COULDN'T BE... ");
                    return;
                }

                if (!chatManager.isNickFree(factoryCreated.getMessage(), userList) ||
                        factoryCreated.getMessage().toLowerCase().equals("server") ||
                        factoryCreated.getMessage().toLowerCase().equals("list") ||
                        factoryCreated.getMessage().toLowerCase().equals("reset")){
                    chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.NICK_NOT_FREE,
                            "SERVER: INVALID NICK OR ITS HAS BEEN ALREADY TAKEN..");
                    return;
                }

                //get IP
                try {
                    clientIP = String.valueOf(InetAddress.getLocalHost());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                chatManager.sendPacketToAll(MessageFactory.MessageType.USER_JOIN, factoryCreated.getMessage()
                        + "(" + clientIP + ")", userList);
                userModel.setNick(factoryCreated.getMessage());
                chatManager.sendPacketToUser(userModel, MessageFactory.MessageType.SEND_MESSAGE,
                        chatManager.getHelpMenu(userModel.getNick()));
                break;
            }
            case NICK_NOT_FREE:
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        String leftUserNick;
        UserModel userModel = userList.get(session.getId());
        leftUserNick = userModel.getNick();
        //chatManager.removeUserModelFromUsers(userModel, userList); //-> not working properly when two users have same IP
        userList.remove(session.getId());
        chatManager.sendPacketToAll(MessageFactory.MessageType.USER_LEFT, leftUserNick, userList);
    }
}
