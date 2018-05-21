package pl.mareksowa.SerwerChat.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class responsible for managing messages
 */
public class MessageFactory {

    public static transient Gson GSON = new GsonBuilder().create();
    public enum MessageType{
        SEND_MESSAGE, NICK_NOT_FREE, USER_JOIN, USER_LEFT, SET_NICK,
    }

    private String message;
    private String author;
    private MessageType messageType;

    public MessageFactory(){
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
