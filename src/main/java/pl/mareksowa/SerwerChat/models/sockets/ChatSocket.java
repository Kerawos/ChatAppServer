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

@EnableWebSocket
@Configuration
public class ChatSocket extends TextWebSocketHandler implements WebSocketConfigurer {

    //mapa dla przechowywania id oraz usera
    Map<String, UserModel> userList = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(this, "/chat")
                .setAllowedOrigins("*");
    }

    //ustanowiono polaczenie
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        session.sendMessage(new TextMessage("Hello!"));
//        session.sendMessage(new TextMessage("Your first message will be your nick"));
        //dodajemy id i nowego usera
        userList.put(session.getId(), new UserModel(session));
    }

    //przychodzi wiadomosc
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UserModel userModel = userList.get(session.getId()); // pobieramy ID
        Type factory = new TypeToken<MessageFactory>() {
        }.getType(); // uruchamiamy fabryke
        //konwertujmy jsona na nasz obiekt
        MessageFactory factoryCreated = MessageFactory.GSON.fromJson(message.getPayload(), factory);
        MessageFactory factoryNewMessage;

        //sprawdzamy jaki typ wiadomosci zawiera nasz obiekt
        switch (factoryCreated.getMessageType()) {
            case SEND_MESSAGE: {
                //pod message znajdzie sie normalna wiadomosc
                factoryNewMessage = new MessageFactory();

                //check if user sent empty message
                if (factoryCreated.getMessage().length() == 0){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage("@@@ EMPTY MESSAGE CANNOT BE SENT");
                    sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                //check if user sent spam
                if (factoryCreated.getMessage().length() > 256){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage("@@@ MESSAGE CANNOT BY LONGER THAN 256 LETTERS");
                    sendMessageToUser(userModel, factoryNewMessage);
                    break;
                }

                if (isVulgarityAbsent(factoryCreated.getMessage())){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                    factoryNewMessage.setMessage("@@@ MESSAGE SUSPEND, REASON = VULGARISM DETECTED.. ");
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

                factoryNewMessage.setMessage(userModel.getNick() + ": " + factoryCreated.getMessage()); // umieszczamy przed wiadomoscia nick wysylajacego
                factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                sendMessageToAll(factoryNewMessage);
                break;
            }

            case SET_NICK:{
                // pod message znajdzie sie nick
                factoryNewMessage = new MessageFactory();
                if (isVulgarityAbsent(factoryCreated.getMessage())){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.NICK_NOT_FREE);
                    factoryNewMessage.setMessage("@@@ NICK CONTAIN VULGARISM, IT COULDN'T BE.. ");
                    sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }

                if (!isNickFree(factoryCreated.getMessage())){
                    factoryNewMessage.setMessageType(MessageFactory.MessageType.NICK_NOT_FREE);
                    factoryNewMessage.setMessage("@@@ NICK IS ALREADY TAKEN MY FRIEND..");
                    sendMessageToUser(userModel, factoryNewMessage);
                    return;
                }
                sendPacket(factoryCreated.getMessage(), MessageFactory.MessageType.USER_JOIN);
                userModel.setNick(factoryCreated.getMessage());
                factoryNewMessage.setMessageType(MessageFactory.MessageType.SEND_MESSAGE);
                factoryNewMessage.setMessage("@@@ NICK HAS BEEN SET \n@@@ TO SEND MESSAGE PRESS 'ENTER' " +
                        "\n@@@ MESSAGES CANNOT BE LONGER THAN 256 LETTERS! " +
                        "\n@@@ TO VIEW ALL ACTIVE USERS ON CHAT TYPE '/users' ");
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
        builder.append("@@@ ON-LINE USERS: ");
        userList.values().forEach(s-> builder.append(s.getNick() + "; "));
        return builder.toString();
    }

    private boolean isNickFree(String nick){
        for(UserModel userModel : userList.values()){
            if (userModel.getNick() != null & nick.equals(userModel.getNick())){ // uzytkownik uruchamiajac aplikacje bedziue mial nick null dopoki nie ustali innego
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




//        if (userModel.getNick() == null){ // sprawdza czy user nie ma nicku
//
//            //wykluczamy nicki dluzsze niz 15 znakow
//            if (message.getPayload().length() > 15){
//                userModel.getSession().sendMessage(new TextMessage("Nick should have maximum 15 characters, try again.."));
//                return;
//            }
//
//            //sprawdzamy czy nick juz nie jest zajety w mapie naszych nickow
//            for(UserModel localNick : userList.values()){
//                if (localNick.getNick() != null && localNick.getNick().equals(message.getPayload()) ){
//                    userModel.getSession().sendMessage(new TextMessage("Nick already used try again.."));
//                    return;
//                }
//            }
//
//            userModel.setNick(message.getPayload());
//
//            System.out.println("nick zapisany " + userModel.getNick());
//
//            userModel.getSession().sendMessage(new TextMessage("Nick has been seted!"));
//            return;
//        }

//        userList.values().forEach(s -> { //i do kazdego wysylamy wiadomosc
//                    try {
//                        TextMessage newMessage =
//                                new TextMessage(userModel.getNick() + ": " + message.getPayload());
//                        s.getSession().sendMessage(newMessage);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                });
//    }

    //uzytkownik opuszcza
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UserModel userModel = userList.get(session.getId()); //zbieramy id usera
        sendPacket(userModel.getNick(), MessageFactory.MessageType.USER_LEFT);
        userList.remove(session.getId());
    }
}
