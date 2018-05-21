package pl.mareksowa.SerwerChat.models;

import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for holding User Data
 */
public class UserModel {

    private String nick;
    private List<String> chatHistory;
    private WebSocketSession session;
    private List<UserModel> blockedList;

    public UserModel(WebSocketSession session) {
        this.session = session;
        nick = null;
        chatHistory = new ArrayList<>();
        blockedList = new ArrayList<>();
    }

    public List<String> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<String> chatHistory) {
        this.chatHistory = chatHistory;
    }
    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    public List<UserModel> getBlockedList() {
        return blockedList;
    }

    public void setBlockedList(List<UserModel> blockedList) {
        this.blockedList = blockedList;
    }

    public void addMessageToHistory(String message){
        getChatHistory().add(message);
    }

    public List<String> getChatHistoryReverseOrder(){
        List<String> reverseHistory = new ArrayList<>();
        for (int i = 0; i < getChatHistory().size(); i++) {
            reverseHistory.add(getChatHistory().get(getChatHistory().size()-i-1));
        }
        return reverseHistory;
    }

    public void addBlockedUser(UserModel userModel){
        getBlockedList().add(userModel);
    }

    public void resetBlockedList(){
        setBlockedList(new ArrayList<>());
    }
}
