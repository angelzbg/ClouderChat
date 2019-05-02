package angelzani.clouderchat.UI;

import android.content.Context;
import android.support.constraint.ConstraintLayout;

public class ConstraintLayout_UID_Date extends ConstraintLayout {

    public ConstraintLayout_UID_Date(Context context, String UID, Long DATE) {
        super(context);

        this.uid = UID;
        this.date = DATE;
    }

    private String uid;
    private Long date;
    public String getUid() { return uid; }
    public Long getDate() { return date; }
}