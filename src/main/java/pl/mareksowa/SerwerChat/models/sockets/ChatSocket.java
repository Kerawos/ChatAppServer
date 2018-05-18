package pl.mareksowa.SerwerChat.models.sockets;

import com.google.gson.reflect.TypeToken;
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
import java.util.*;


/**
 * Class responsible for chat logic
 */
@EnableWebSocket
@Configuration
public class ChatSocket extends TextWebSocketHandler implements WebSocketConfigurer {

    Map<String, UserModel> userList = Collections.synchronizedMap(new HashMap<>());

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
                    sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                //check if user sent spam
                if (factoryCreated.getMessage().length() > 256){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage("SERVER: MESSAGE CANNOT BY LONGER THAN 256 LETTERS");
                    sendMessageToUser(userModel, factoryNewMessage);
                    break;
                }

                //check VULGARISMS
                if (isVulgarityAbsent(factoryCreated.getMessage())){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage("SERVER: MESSAGE SUSPEND, REASON = VULGARISM DETECTED.. ");
                    sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                //1st type of command
                if (factoryCreated.getMessage().equals("/users")){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage(showOnlineUsers());
                    sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                //set nick before message
                factoryNewMessage.setMessage(userModel.getNick() + ": " + factoryCreated.getMessage());
                factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                sendMessageToAll(factoryNewMessage);
                break;
            }

            case SET_NICK:{
                factoryNewMessage = new MessageFactory();
                if (isVulgarityAbsent(factoryCreated.getMessage())){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.NICK_NOT_FREE);
                    factoryNewMessage.setMessage("SERVER: NICK CONTAIN VULGARISM, IT COULDN'T BE.. ");
                    sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                factoryNewMessage = new MessageFactory();
                if (factoryCreated.getMessage().equals("SERVER")){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.NICK_NOT_FREE);
                    factoryNewMessage.setMessage("SERVER: NICK BELONG TO THE SERVER, IT COULDN'T BE.. ");
                    sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                if (!isNickFree(factoryCreated.getMessage())){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.NICK_NOT_FREE);
                    factoryNewMessage.setMessage("SERVER: NICK IS ALREADY TAKEN MY FRIEND..");
                    sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }
                sendPacket(factoryCreated.getMessage(), MessageFactory.MessageType.USER_JOIN);
                userModel.setNick(factoryCreated.getMessage());
                factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                factoryNewMessage.setMessage(
                        "SERVER: NICK HAS BEEN SET " +
                        "\nSERVER: TO SEND MESSAGE PRESS 'ENTER' " +
                        "\nSERVER: MESSAGES CANNOT BE LONGER THAN 256 LETTERS! " +
                        "\nSERVER: TO VIEW ALL ACTIVE USERS ON CHAT TYPE '/users' ");
                sendMessageToUser(userModel, factoryNewMessage);
                break;
            }
        }
    }

    private boolean isVulgarityAbsent(String text){
        List<String> vulgarity = Arrays.asList("kurwa", "cipa", "chuj");
        for (String s : vulgarity) {
            if (text.toLowerCase().contains(s)){
                return true;
            }
        }
        return false;
    }

    private String showOnlineUsers(){
        StringBuilder builder = new StringBuilder();
        builder.append("SERVER: ON-LINE USERS: ");
        userList.values().forEach(s-> builder.append(s.getNick() + "; "));
        return builder.toString();
    }

    private boolean isNickFree(String nick){
        for(UserModel userModel : userList.values()){
            if (userModel.getNick() != null & nick.equals(userModel.getNick())){
                return false;
            }
        }
        return true;
    }

    private String convertFactoryToString(MessageFactory factory){
        return MessageFactory.GSON.toJson(factory);
    }

    public void sendMessageToAllWithoutMe(UserModel userModel, MessageFactory factory){
        for(UserModel user : userList.values()){
            try {
                if (user.getSession().getId().equals(userModel.getSession().getId())) {
                    continue;
                }
                user.getSession().sendMessage(new TextMessage(convertFactoryToString(factory)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPacket(String nick, MessageFactory.MessageType type){
        MessageFactory factory = new MessageFactory();
        factory.setMessageType(type);
        factory.setMessage(nick);
        sendMessageToAll(factory);
    }


    public void sendMessageToUser(UserModel user, MessageFactory factory){
        try {
            user.getSession().sendMessage(new TextMessage(convertFactoryToString(factory)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToAll(MessageFactory factory){
        for(UserModel user : userList.values()){
            try {
                user.getSession().sendMessage(new TextMessage(convertFactoryToString(factory)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UserModel userModel = userList.get(session.getId());
        sendPacket(userModel.getNick(), MessageFactory.MessageType.USER_LEFT);
        userList.remove(session.getId());
    }
}
