package angelzani.clouderchat.DB;

import java.util.Map;

public class SendRequest {
    private String message;
    private java.util.Map date;

    public SendRequest(String message, Map date) {
        this.message = message;
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public Map getDate() {
        return date;
    }
}