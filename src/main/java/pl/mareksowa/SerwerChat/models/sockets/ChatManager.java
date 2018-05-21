package pl.mareksowa.SerwerChat.models.sockets;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import pl.mareksowa.SerwerChat.models.MessageFactory;
import pl.mareksowa.SerwerChat.models.UserModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Service help managing messages
 */
@Service
public class ChatManager {

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
        builder.append("SERVER: ON-LINE USERS: ");
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

    public List<String> showBlockedUserNicks(UserModel userModel, Map<String, UserModel> userList){
        List<String> blockedUsers = new ArrayList<>();
        for(Map.Entry<String, UserModel> element : userList.entrySet()){
            if (element.getValue().getNick().equals(userModel.getNick())){
                for (UserModel model : element.getValue().getBlockedList()) {
                    blockedUsers.add(model.getNick());
                }
            }
        }
        return blockedUsers;
    }

    public UserModel getUserModelAfterNick(String userNick, Map<String, UserModel> userList){
        UserModel userModel;
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

    public void sendMessageToUser(UserModel user, MessageFactory factory){
        try {
            user.getSession().sendMessage(new TextMessage(convertFactoryToString(factory)));
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

}
