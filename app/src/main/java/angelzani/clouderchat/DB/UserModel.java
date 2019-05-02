package angelzani.clouderchat.DB;

public class UserModel {
    private String uid;
    private String avatar;
    private long avatarCh;
    private String avatarString;
    private long dateCr;
    private String nick;
    private long nickCh;
    private long points;
    private boolean online;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public long getAvatarCh() {
        return avatarCh;
    }

    public void setAvatarCh(long avatarCh) {
        this.avatarCh = avatarCh;
    }

    public String getAvatarString() {
        return avatarString;
    }

    public void setAvatarString(String avatarString) {
        this.avatarString = avatarString;
    }

    public long getDateCr() {
        return dateCr;
    }

    public void setDateCr(long dateCr) {
        this.dateCr = dateCr;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public long getNickCh() {
        return nickCh;
    }

    public void setNickCh(long nickCh) {
        this.nickCh = nickCh;
    }

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public boolean isOnline() { return online; }

    public void setOnline(boolean online) { this.online = online; }
}
