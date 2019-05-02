package angelzani.clouderchat.DB;

import com.google.firebase.database.ServerValue;

import java.util.Map;

public class SendPrivChatModel {

    private String sender;
    private String message;
    private int type;
    private java.util.Map date;

    public SendPrivChatModel(String senderUID, String message, int type) {
        this.sender = senderUID;
        this.date = ServerValue.TIMESTAMP;
        this.type = type;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public Map getDate() {
        return date;
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

}
