package angelzani.clouderchat.DB;

public class PrivChatModel {

    private String targetUID, senderUID, message;
    private int type;
    private long date;

    public PrivChatModel(String targetUID, String senderUID, String message, int type, long date) {
        this.targetUID = targetUID;
        this.senderUID = senderUID;
        this.message = message;
        this.type = type;
        this.date = date;
    }

    public String getTargetUID() {
        return targetUID;
    }

    public String getSenderUID() {
        return senderUID;
    }

    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }

    public long getDate() {
        return date;
    }

    public void setTargetUID(String targetUID) {
        this.targetUID = targetUID;
    }

    public void setSenderUID(String senderUID) {
        this.senderUID = senderUID;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
