package angelzani.clouderchat.UI;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ConstraintLayoutPrivChat extends ConstraintLayout {
    public ConstraintLayoutPrivChat(Context context) {
        super(context);

        IV_Avatar = new ImageView(context);
        TV_Nick = new TextView(context);
        TV_Message = new TextView(context);
        TV_Date = new TextView(context);

        this.setId(View.generateViewId());

        IV_Avatar.setId(View.generateViewId());
        TV_Nick.setId(View.generateViewId());
        TV_Message.setId(View.generateViewId());
        TV_Date.setId(View.generateViewId());

        this.addView(IV_Avatar);
        this.addView(TV_Nick);
        TV_Message.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ViewGroup.LayoutParams.WRAP_CONTENT));
        this.addView(TV_Message);
        this.addView(TV_Date);
    }

    private String friendUID;
    public String getFriendUID() { return friendUID; }
    public void setFriendUID(String friendUID) { this.friendUID = friendUID; }

    public ImageView IV_Avatar;
    public TextView TV_Nick, TV_Message, TV_Date;

}//end ConstraintLayoutPrivChat{}