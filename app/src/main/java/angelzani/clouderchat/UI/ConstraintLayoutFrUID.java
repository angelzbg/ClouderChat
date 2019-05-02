package angelzani.clouderchat.UI;

import android.content.Context;
import android.support.constraint.ConstraintLayout;

public class ConstraintLayoutFrUID extends ConstraintLayout {
    public ConstraintLayoutFrUID(Context context) {
        super(context);
    }
    private String friendUID;

    public String getFriendUID() {
        return friendUID;
    }

    public void setFriendUID(String friendUID) {
        this.friendUID = friendUID;
    }
}
