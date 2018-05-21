package pl.mareksowa.SerwerChat.models.sockets;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import pl.mareksowa.SerwerChat.models.MessageFactory;
import pl.mareksowa.SerwerChat.models.UserModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Service help managing messages
 */
@Service
public class ChatManager {

    public void sendPacketToAll(MessageFactory.MessageType messageType, String message, Map<String, UserModel> userList){
        MessageFactory messageFactory = new MessageFactory();
        messageFactory.setMessageType(messageType);
        messageFactory.setMessage(message);
        sendMessageToAll(messageFactory, userList);
    }

    public void sendPacketToUser(UserModel userModel, MessageFactory.MessageType messageType, String message){
        MessageFactory messageFactory = new MessageFactory();
        messageFactory.setMessageType(messageType);
        messageFactory.setMessage(message);
        sendMessageToUser(userModel, messageFactory);
    }

    public void sendPacketThruFilter(UserModel userModel, MessageFactory.MessageType messageType, String message,  Map<String, UserModel> userList){
        MessageFactory messageFactory = new MessageFactory();
        messageFactory.setMessageType(messageType);
        messageFactory.setMessage(message);
        sendMessageThruFilter(userModel, messageFactory, userList);
    }

    public boolean isVulgarityAbsent(String text){
        List<String> vulgarity = Arrays.asList("kurwa", "cipa", "chuj");
        for (String s : vulgarity) {
            if (text.toLowerCase().contains(s)){
                return true;
            }
        }
        return false;
    }

    public String showOnlineUsers(Map<String, UserModel> userList){
        StringBuilder builder = new StringBuilder();
        builder.append("SERVER: ON-LINE USERS: \n");
        userList.values().forEach(s-> builder.append(s.getNick() + "; "));
        return builder.toString();
    }

    public boolean isUserPresent(String userNick, Map<String, UserModel> userList){
        for(Map.Entry<String, UserModel> element : userList.entrySet()){
            if (element.getValue().getNick().equals(userNick)){
                return true;
            }
        }
        return false;
    }

    public List<String> showHistory(UserModel userModel, Map<String, UserModel> userList){
        for(Map.Entry<String, UserModel> element : userList.entrySet()){
            if (element.getValue().getNick().equals(userModel.getNick())){
                return userModel.getChatHistoryReverseOrder();
            }
        }
        throw new IllegalArgumentException("invalid user");
    }

    public String showBlockedUserNicks(UserModel userModel, Map<String, UserModel> userList){
        StringBuilder builder = new StringBuilder();
        for(Map.Entry<String, UserModel> element : userList.entrySet()){
            if (element.getValue().getNick().equals(userModel.getNick())){
                element.getValue().getBlockedList().forEach(um->builder.append(um.getNick() + ";"));
            }
        }
        return builder.toString();
    }

    public UserModel getUserModelAfterNick(String userNick, Map<String, UserModel> userList){
        for(Map.Entry<String, UserModel> element : userList.entrySet()){
            if (element.getValue().getNick().equals(userNick)){
                return element.getValue();
            }
        }
        throw new IllegalArgumentException("no user found");
    }

    public boolean isNickFree(String nick, Map<String, UserModel> userList){
        for(UserModel userModel : userList.values()){
            if (userModel.getNick() != null & nick.equals(userModel.getNick())){
                return false;
            }
        }
        return true;
    }

    public String convertFactoryToString(MessageFactory factory){
        return MessageFactory.GSON.toJson(factory);
    }

    public void sendMessageToAllWithoutMe(UserModel userModel, MessageFactory factory, Map<String, UserModel> userList){
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

    public void sendMessageToUser(UserModel userModel, MessageFactory factory){
        try {
            userModel.getSession().sendMessage(new TextMessage(convertFactoryToString(factory)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToAll(MessageFactory factory, Map<String, UserModel> userList){
        for(UserModel user : userList.values()){
            try {
                user.getSession().sendMessage(new TextMessage(convertFactoryToString(factory)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessageThruFilter(UserModel userModelSender, MessageFactory factory, Map<String, UserModel> userList){
        for(UserModel user : userList.values()){
            if (!isBlockedUser(user, userModelSender)){ // will be faster to have following list instead of blocked list
                try {
                    user.getSession().sendMessage(new TextMessage(convertFactoryToString(factory)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isBlockedUser(UserModel userModel, UserModel potentialBlockedUser){
        for (UserModel model : userModel.getBlockedList()) {
            if (model.equals(potentialBlockedUser)){
                return true;
            }
        }
        return false;
    }

    public void removeUserModelFromUsers(UserModel userToDelete, Map<String, UserModel> users){
        for(UserModel user: users.values()){
            if (user.equals(userToDelete)){
                users.remove(user);
                return;
            }
        }
        throw new IllegalArgumentException("User not found");
    }

    public String getHelpMenu(String nick){
        return "*****\n" +
                "SERVER: NICK HAS BEEN SET (" + nick + ")\n" +
                "SERVER: TO SEND MESSAGE PRESS 'ENTER' \n" +
                "SERVER: MESSAGES CANNOT BE LONGER THAN 140 LETTERS! \n" +
                "SERVER: TO VIEW ALL ACTIVE USERS ON CHAT TYPE '/users' \n" +
                "SERVER: TO VIEW HISTORY OF USER: '/history (user nick)' \n" +
                "SERVER: TO BLOCK USER: '/block (user nick)' \n" +
                "SERVER: TO VIEW YOUR BLOCK LIST: '/block list' \n" +
                "SERVER: TO RESET YOUR BLOCK LIST: '/block reset' \n" +
                "SERVER: TO DISPLAY THIS MENU: '/help' \n" +
                "*****";
    }

}
