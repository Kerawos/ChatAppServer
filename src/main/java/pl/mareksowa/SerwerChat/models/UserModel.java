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

    public UserModel(WebSocketSession session) {
        this.session = session;
        nick = null;
        chatHistory = new ArrayList<>();
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
}
