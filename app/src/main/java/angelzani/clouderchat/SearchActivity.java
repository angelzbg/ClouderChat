package angelzani.clouderchat;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import angelzani.clouderchat.DB.DatabaseHelper;
import angelzani.clouderchat.DB.SendRequest;
import angelzani.clouderchat.DB.UserModel;
import angelzani.clouderchat.Utility.Utility;

public class SearchActivity extends AppCompatActivity {

    // ВНИМАНИЕ! Неизчистен код!

    // UI
    // Display Metrics
    private int width, height;

    ConstraintLayout search_CL_Main;
    //Header
    ConstraintLayout search_CL_Header;
    ImageButton search_IB_Back;
    ConstraintLayout search_CL_Search;
    EditText search_ET_Search;
    ImageButton search_IB_Search;
    //Results
    ScrollView search_SV_Results;
    ConstraintLayout search_CL_People;
    LinearLayout search_LL_Results;
    LinearLayout search_LL_People;
    LinearLayout search_LL_PeopleFriends;
    LinearLayout search_LL_PeopleResults;
    TextView search_TV_People;


    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase fireDB;
    private DatabaseReference dbRef;
    private FirebaseUser user;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    //Local DB
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        if(width > height) {
            int temp = width;
            height = width;
            width = temp;
        }

        db = new DatabaseHelper(getApplicationContext());

        /*if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.gui_gray_normal));
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }*/

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        fireDB = FirebaseDatabase.getInstance();
        dbRef = fireDB.getReference();
        user = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // UI
        search_CL_Main = findViewById(R.id.search_CL_Main);
        //Header
        search_CL_Header = findViewById(R.id.search_CL_Header);
        search_IB_Back = findViewById(R.id.search_IB_Back);
        search_CL_Search = findViewById(R.id.search_CL_Search);
        search_ET_Search = findViewById(R.id.search_ET_Search);
        search_IB_Search = findViewById(R.id.search_IB_Search);
        //Results
        search_SV_Results = findViewById(R.id.search_SV_Results);
        search_CL_People = findViewById(R.id.search_CL_People);
        search_LL_Results = findViewById(R.id.search_LL_Results);
        search_LL_People = findViewById(R.id.search_LL_People);
        search_LL_PeopleFriends = findViewById(R.id.search_LL_PeopleFriends);
        search_LL_PeopleResults = findViewById(R.id.search_LL_PeopleResults);
        search_TV_People = findViewById(R.id.search_TV_People);


        // Resizing
        search_CL_Header.getLayoutParams().height = (int) (height / 12.5);
        search_IB_Back.getLayoutParams().width = height / 16;
        search_IB_Back.getLayoutParams().height = height / 16;
        search_CL_Search.getLayoutParams().height = height / 16;
        search_ET_Search.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 40);
        search_IB_Search.getLayoutParams().width = (int) (height / 26.667);
        search_IB_Search.getLayoutParams().height = search_IB_Search.getLayoutParams().width;
        search_IB_Search.setColorFilter(ContextCompat.getColor(this, R.color.gui_text_hint)); // Grey Tint

        search_TV_People.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 57);

        GradientDrawable gdSearchBack = new GradientDrawable();
        gdSearchBack.setColor(ContextCompat.getColor(this, R.color.gui_gray_normal));
        //gdSearchBack.setStroke(app_CL_SearchHeader.getLayoutParams().height/48, ContextCompat.getColor(this, R.color.gui_gray_dark));
        gdSearchBack.setShape(GradientDrawable.RECTANGLE);
        gdSearchBack.setCornerRadius(search_CL_Search.getLayoutParams().height / 8);
        search_CL_Search.setBackground(gdSearchBack);

        ConstraintSet cs = new ConstraintSet();
        cs.clone(search_CL_Header);
        cs.connect(R.id.search_IB_Back, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, (int) (height / 28.5714));
        cs.connect(R.id.search_CL_Search, ConstraintSet.START, R.id.search_IB_Back, ConstraintSet.END, (int) (height / 28.5714));
        cs.connect(R.id.search_CL_Search, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, (int) (height / 28.5714));
        cs.applyTo(search_CL_Header);

        cs.clone(search_CL_Search);
        cs.connect(R.id.search_ET_Search, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, height / 40);
        cs.connect(R.id.search_ET_Search, ConstraintSet.END, R.id.search_IB_Search, ConstraintSet.START, height / 80);
        cs.connect(R.id.search_IB_Search, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, height / 40);
        cs.applyTo(search_CL_Search);

        // OnClickListeners
        search_IB_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        search_IB_Search.setOnClickListener(search);

        //TextWatchers
        search_ET_Search.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!search_ET_Search.getText().toString().trim().isEmpty()) {
                    search_IB_Search.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white)); // Blue Tint

                    search_LL_PeopleFriends.removeAllViews();

                    final String searchString = search_ET_Search.getText().toString().toLowerCase().trim();
                    ArrayList<String> friendsUIDs = db.getAllFriendsUIDs(user.getUid());
                    final ArrayList<UserModel> friends = new ArrayList<>();
                    if (friendsUIDs != null && friendsUIDs.size() > 0) {
                        for (String friendUID : friendsUIDs) {
                            UserModel user = db.getUser(friendUID);
                            if (user != null && user.getNick().toLowerCase().contains(searchString)) friends.add(user);
                        }
                    }

                    final int avatar = height / 14, presence = height/14/4, layout_margin_start_end = (int)((height / 28.5714)-(height/14-height/16)/2);

                    final GradientDrawable gdPresenceOnline = new GradientDrawable();
                    gdPresenceOnline.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_online_color));
                    gdPresenceOnline.setStroke(presence/6, ContextCompat.getColor(getApplicationContext(), R.color.gui_friends_back));
                    gdPresenceOnline.setShape(GradientDrawable.OVAL);

                    final GradientDrawable gdPresenceOffline = new GradientDrawable();
                    gdPresenceOffline.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_normal));
                    gdPresenceOffline.setStroke(presence/6, ContextCompat.getColor(getApplicationContext(), R.color.gui_friends_back));
                    gdPresenceOffline.setShape(GradientDrawable.OVAL);

                    final GradientDrawable gdFriendsBack = new GradientDrawable();
                    gdFriendsBack.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_friends_back));
                    gdFriendsBack.setStroke(layout_margin_start_end/48, ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_dark));
                    gdFriendsBack.setShape(GradientDrawable.RECTANGLE);
                    gdFriendsBack.setCornerRadius(layout_margin_start_end/4);

                    final GradientDrawable gdButtons = new GradientDrawable();
                    gdButtons.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_mid));
                    //gdSearchBack.setStroke(app_CL_SearchHeader.getLayoutParams().height/48, ContextCompat.getColor(this, R.color.gui_gray_dark));
                    gdButtons.setShape(GradientDrawable.RECTANGLE);
                    gdButtons.setCornerRadius(layout_margin_start_end/4);

                    for (final UserModel friend : friends) {
                        final ConstraintLayout CL_User = new ConstraintLayout(getApplicationContext());
                        CL_User.setId(View.generateViewId());
                        CL_User.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        search_LL_PeopleFriends.addView(CL_User);
                        Utility.setMargins(CL_User, layout_margin_start_end/2, layout_margin_start_end/4, layout_margin_start_end/2, 0);
                        CL_User.setBackground(gdFriendsBack);

                        TextView TV_Nickname = new TextView(getApplicationContext());
                        TV_Nickname.setId(View.generateViewId());
                        CL_User.addView(TV_Nickname);
                        TV_Nickname.setTypeface(null, Typeface.BOLD);
                        TV_Nickname.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 40);
                        TV_Nickname.setTextColor(Color.WHITE);
                        //TV_Nickname.setShadowLayer(1, 0, 0, Color.BLACK);
                        TV_Nickname.setText(friend.getNick());
                        Utility.setMargins(TV_Nickname, layout_margin_start_end/2, 0, 0, 0);

                        ImageView IV_Avatar = new ImageView(getApplicationContext());
                        IV_Avatar.setId(View.generateViewId());
                        CL_User.addView(IV_Avatar);
                        IV_Avatar.getLayoutParams().height = avatar;
                        IV_Avatar.getLayoutParams().width = avatar;
                        IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                        if(!friend.getAvatar().equals("default")){
                            try {
                                IV_Avatar.setImageBitmap(Utility.StringToBitMap(friend.getAvatarString()));
                            } catch (Exception e) {
                                IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                            }
                        }

                        TextView TV_Connected = new TextView(getApplicationContext());
                        TV_Connected.setId(View.generateViewId());
                        CL_User.addView(TV_Connected);
                        TV_Connected.setTypeface(null, Typeface.ITALIC);
                        TV_Connected.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 60);
                        TV_Connected.setTextColor(Color.LTGRAY);
                        TV_Connected.setText("Connected");

                        final ImageView IV_Remove = new ImageView(getApplicationContext());
                        IV_Remove.setId(View.generateViewId());
                        CL_User.addView(IV_Remove);
                        IV_Remove.getLayoutParams().width = (int)(IV_Avatar.getLayoutParams().height*0.8);
                        IV_Remove.getLayoutParams().height = IV_Remove.getLayoutParams().width;
                        IV_Remove.setImageResource(R.drawable.icon_friend_remove);
                        IV_Remove.setBackground(gdButtons);

                        IV_Remove.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final Dialog dialog = new Dialog(SearchActivity.this);
                                dialog.setContentView(R.layout.layout_cancel_friend_request);
                                dialog.show();

                                dialog.findViewById(R.id.cancel_CL_Main).getLayoutParams().width = width / 8 * 6;
                                TextView cancel_TV_Title = dialog.findViewById(R.id.cancel_TV_Title);
                                cancel_TV_Title.setText("How would you like to proceed with " + friend.getNick() + "?");
                                ConstraintLayout calcel_CL_ButtonsWrap = dialog.findViewById(R.id.calcel_CL_ButtonsWrap);
                                final Button cancel_B_Block = dialog.findViewById(R.id.cancel_B_Block);
                                final Button cancel_B_Dismiss = dialog.findViewById(R.id.cancel_B_Dismiss);
                                cancel_B_Dismiss.setText("UNFRIEND");

                                Utility.setMargins(cancel_TV_Title, height / 40, height / 60, height / 40, 0);
                                Utility.setMargins(calcel_CL_ButtonsWrap, height / 40, height / 40, height / 40, height / 80);
                                cancel_TV_Title.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 36);
                                cancel_B_Block.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 40);
                                cancel_B_Dismiss.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 40);

                                View.OnClickListener removeFriend = new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if(v.getId() == cancel_B_Dismiss.getId()){
                                            dbRef.child("friendList").child(user.getUid()).child(friend.getUid()).setValue(false, new DatabaseReference.CompletionListener() {
                                                @Override
                                                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                    Toast.makeText(getApplicationContext(), friend.getNick() + " unfriended.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        else if(v.getId() == cancel_B_Block.getId()){
                                            dbRef.child("blockList").child(user.getUid()).child(friend.getUid()).setValue(true, new DatabaseReference.CompletionListener() {
                                                @Override
                                                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                    Toast.makeText(getApplicationContext(), friend.getNick() + " blocked.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                        dialog.dismiss();
                                        IV_Remove.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_normal));
                                        cancel_B_Dismiss.setOnClickListener(null);
                                        cancel_B_Block.setOnClickListener(null);
                                    }
                                };
                                cancel_B_Dismiss.setOnClickListener(removeFriend);
                                cancel_B_Block.setOnClickListener(removeFriend);
                            }
                        });

                        ImageView IV_Presence = new ImageView(getApplicationContext());
                        IV_Presence.setId(View.generateViewId());
                        CL_User.addView(IV_Presence);
                        IV_Presence.getLayoutParams().width = presence;
                        IV_Presence.getLayoutParams().height = presence;
                        if(friend.isOnline()){
                            IV_Presence.setBackground(gdPresenceOnline);
                        } else {
                            IV_Presence.setBackground(gdPresenceOffline);
                        }

                        ConstraintSet cs = new ConstraintSet();
                        cs.clone(CL_User);
                        cs.connect(IV_Avatar.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, layout_margin_start_end/2);
                        cs.connect(IV_Avatar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layout_margin_start_end/2);
                        cs.connect(IV_Avatar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, layout_margin_start_end/2);
                        cs.connect(TV_Nickname.getId(), ConstraintSet.START, IV_Avatar.getId(), ConstraintSet.END);
                        cs.connect(TV_Nickname.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layout_margin_start_end/2);
                        cs.connect(TV_Connected.getId(), ConstraintSet.TOP, TV_Nickname.getId(), ConstraintSet.BOTTOM);
                        cs.connect(TV_Connected.getId(), ConstraintSet.START, TV_Nickname.getId(), ConstraintSet.START);
                        cs.connect(IV_Presence.getId(), ConstraintSet.END, IV_Avatar.getId(),ConstraintSet.END);
                        cs.connect(IV_Presence.getId(), ConstraintSet.BOTTOM, IV_Avatar.getId(),ConstraintSet.BOTTOM);
                        cs.connect(IV_Remove.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID,ConstraintSet.END, layout_margin_start_end/2);
                        cs.connect(IV_Remove.getId(), ConstraintSet.BOTTOM, IV_Avatar.getId(),ConstraintSet.BOTTOM);
                        cs.connect(IV_Remove.getId(), ConstraintSet.TOP, IV_Avatar.getId(),ConstraintSet.TOP);
                        cs.applyTo(CL_User);
                    }
                } else {
                    search_IB_Search.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_hint)); // Grey Tint
                }
            }
        });

        search_ET_Search.requestFocus();
    }// end of onCreate()

    private String last_search = "";
    View.OnClickListener search = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final String searchString = search_ET_Search.getText().toString().toLowerCase().trim();

            if (!searchString.isEmpty()) {
                Utility.hideKeyboard(SearchActivity.this);
                search_IB_Search.setColorFilter(Color.parseColor("#C1C1C1")); // Grey Tint

                if (last_search.equals(searchString))
                    return; // няма смисъл от две повторни търсения (също така предотвратява няколкократни натискания на бутона от потребителя и изпълнението на търсенето многократно без причина)
                else last_search = searchString;

                search_LL_PeopleResults.removeAllViews();
                dbRef.child("nicknames").orderByKey().startAt(searchString).endAt(searchString + "\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot DATA) {
                        if (DATA.getChildrenCount() == 0 && search_LL_PeopleFriends.getChildCount() == 0) {
                            ((TextView) findViewById(R.id.search_TV_People)).setText("No people found");
                        } else {
                            ((TextView) findViewById(R.id.search_TV_People)).setText("People found");
                        }
                        findViewById(R.id.search_TV_People).setVisibility(View.VISIBLE);
                        for (DataSnapshot dataSnapshot : DATA.getChildren()) {
                            final String UID = dataSnapshot.getValue(String.class);

                            if (UID.equals(user.getUid())) {
                                if (DATA.getChildrenCount() == 1 && search_LL_PeopleFriends.getChildCount() == 0) { // намери сме себе си и само себе си
                                    ((TextView) findViewById(R.id.search_TV_People)).setText("No people found");
                                    return;
                                }
                                continue; // не искаме да показваме себе си
                            }

                            final int avatar = height / 14, layout_margin_start_end = (int)((height / 28.5714)-(height/14-height/16)/2);
                            final GradientDrawable gdFriendsBack = new GradientDrawable();
                            gdFriendsBack.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_friends_back));
                            gdFriendsBack.setStroke(layout_margin_start_end/48, ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_dark));
                            gdFriendsBack.setShape(GradientDrawable.RECTANGLE);
                            gdFriendsBack.setCornerRadius(layout_margin_start_end/4);
                            final GradientDrawable gdButtons = new GradientDrawable();
                            gdButtons.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_mid));
                            //gdSearchBack.setStroke(app_CL_SearchHeader.getLayoutParams().height/48, ContextCompat.getColor(this, R.color.gui_gray_dark));
                            gdButtons.setShape(GradientDrawable.RECTANGLE);
                            gdButtons.setCornerRadius(layout_margin_start_end/4);

                            if (!db.isFriend(user.getUid(), UID)) { // Ще теглим информация за потребителя от датабазата само ако не ни е в листа с приятели (вече е изтеглен и показан)
                                //--
                                dbRef.child("userInfo").child(UID).child("nick").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        final String nickname = dataSnapshot.getValue(String.class);

                                        final ConstraintLayout CL_User = new ConstraintLayout(getApplicationContext());
                                        CL_User.setId(View.generateViewId());
                                        CL_User.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                                        search_LL_PeopleResults.addView(CL_User);
                                        Utility.setMargins(CL_User, layout_margin_start_end/2, layout_margin_start_end/4, layout_margin_start_end/2, 0);
                                        CL_User.setBackground(gdFriendsBack);

                                        final TextView TV_Nickname = new TextView(getApplicationContext());
                                        TV_Nickname.setId(View.generateViewId());
                                        CL_User.addView(TV_Nickname);
                                        TV_Nickname.setTypeface(null, Typeface.BOLD);
                                        TV_Nickname.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 40);
                                        TV_Nickname.setTextColor(Color.WHITE);
                                        //TV_Nickname.setShadowLayer(1, 1, 1, Color.BLACK);
                                        TV_Nickname.setText(nickname);
                                        Utility.setMargins(TV_Nickname, height / 80, 0, 0, 0);

                                        final ImageView V_Avatar = new ImageView(getApplicationContext());
                                        V_Avatar.setId(View.generateViewId());
                                        CL_User.addView(V_Avatar);
                                        V_Avatar.getLayoutParams().height = avatar;
                                        V_Avatar.getLayoutParams().width = avatar;
                                        V_Avatar.setImageResource(R.drawable.clouder_avatar_default);

                                        final ImageView IV_AddFriend = new ImageView(getApplicationContext());
                                        IV_AddFriend.setId(View.generateViewId());
                                        CL_User.addView(IV_AddFriend);
                                        IV_AddFriend.getLayoutParams().width = (int)(V_Avatar.getLayoutParams().height*0.8);
                                        IV_AddFriend.getLayoutParams().height = IV_AddFriend.getLayoutParams().width;
                                        IV_AddFriend.setImageResource(R.drawable.icon_add_friend);
                                        IV_AddFriend.setBackground(gdButtons);

                                        ConstraintSet cs = new ConstraintSet();
                                        cs.clone(CL_User);
                                        cs.connect(V_Avatar.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, layout_margin_start_end/2);
                                        cs.connect(V_Avatar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layout_margin_start_end/2);
                                        cs.connect(V_Avatar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, layout_margin_start_end/2);
                                        cs.connect(TV_Nickname.getId(), ConstraintSet.START, V_Avatar.getId(), ConstraintSet.END, layout_margin_start_end/2);
                                        cs.connect(TV_Nickname.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                                        cs.connect(TV_Nickname.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                                        cs.connect(IV_AddFriend.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                                        cs.connect(IV_AddFriend.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                                        cs.connect(IV_AddFriend.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, layout_margin_start_end/2);
                                        cs.applyTo(CL_User);

                                        final View.OnClickListener addFriend = new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                // Popup
                                                final Dialog dialog = new Dialog(SearchActivity.this);
                                                dialog.setContentView(R.layout.layout_search_add_friend);
                                                dialog.show();

                                                dialog.findViewById(R.id.addF_CL_Main).getLayoutParams().width = width / 8 * 6;
                                                TextView addF_TV_Title = dialog.findViewById(R.id.addF_TV_Title);
                                                final EditText addF_ET_Message = dialog.findViewById(R.id.addF_ET_Message);
                                                Button addF_B_Send = dialog.findViewById(R.id.addF_B_Send);
                                                Button addF_B_Cancel = dialog.findViewById(R.id.addF_B_Cancel);

                                                addF_TV_Title.setText("Friend request to " + nickname);
                                                Utility.setMargins(addF_TV_Title, height / 40, height / 60, 0, 0);
                                                Utility.setMargins(addF_ET_Message, height / 44, 0, height / 44, 0);
                                                addF_TV_Title.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 36);
                                                addF_ET_Message.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 40);
                                                addF_ET_Message.setText("Hey " + nickname + "! Lets be friends!");
                                                addF_B_Send.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 40);
                                                Utility.setMargins(addF_B_Send, 0,0,height / 64,0);
                                                addF_B_Cancel.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 40);

                                                addF_B_Send.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        final String message = addF_ET_Message.getText().toString().trim();
                                                        if(message.isEmpty()){
                                                            Toast.makeText(getApplicationContext(), "Please write a greeting message to the user first.", Toast.LENGTH_LONG).show();
                                                            return;
                                                        }
                                                        SendRequest friendRequest = new SendRequest(message, ServerValue.TIMESTAMP);
                                                        dbRef.child("friendRequest").child(UID).child(user.getUid()).setValue(friendRequest, new DatabaseReference.CompletionListener(){
                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                                if(databaseError==null) { // всичко е минало добре
                                                                    Toast.makeText(getApplicationContext(), "Friend request sent to " + nickname, Toast.LENGTH_SHORT).show();
                                                                } else { // имаме грешка -> или сме блокирани или вече сме изпратили заявка
                                                                }

                                                                IV_AddFriend.setOnClickListener(null);
                                                                IV_AddFriend.setColorFilter(Color.parseColor("#333333")); // Gray Tint
                                                                dialog.dismiss();
                                                            }
                                                        });
                                                    }
                                                });

                                                addF_B_Cancel.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        dialog.dismiss();
                                                    }
                                                });
                                            }
                                        };
                                        IV_AddFriend.setOnClickListener(addFriend);


                                        final UserModel searched_user = db.getUser(UID);
                                        if (searched_user != null) { // ако юзъра вече е бил теглен
                                            // Обновяваме никнейма (не знаем дали никнейма не е бил сменян)
                                            searched_user.setNick(nickname);
                                            db.updateUser(searched_user);

                                            // Да сложим вече изтегления аватар
                                            if (!searched_user.getAvatar().equals("default")) {
                                                try {
                                                    V_Avatar.setImageBitmap(Utility.StringToBitMap(searched_user.getAvatarString()));
                                                } catch (Exception e) {
                                                    V_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                                }
                                            }

                                            // Да обновим аватара ако има промяна
                                            dbRef.child("userInfo").child(UID).child("avatarCh").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    final Long avatarCh = dataSnapshot.getValue(Long.class);
                                                    if (searched_user.getAvatarCh() != avatarCh) {
                                                        searched_user.setAvatarCh(avatarCh);
                                                        dbRef.child("userInfo").child(UID).child("avatar").addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                final String avatar = dataSnapshot.getValue(String.class);
                                                                searched_user.setAvatar(avatar);
                                                                db.updateUser(searched_user);
                                                                if (!avatar.equals("default")) {
                                                                    StorageReference avatarImagesRef = storage.getReferenceFromUrl(avatar);
                                                                    final long ONE_MEGABYTE = 1024 * 1024;
                                                                    avatarImagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                                                        @Override
                                                                        public void onSuccess(byte[] bytes) {
                                                                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                                            bitmap = Utility.CropBitmapCenterCircle(bitmap);
                                                                            String base64String = Utility.BitMapToString(bitmap);
                                                                            searched_user.setAvatarString(base64String);

                                                                            db.updateUser(searched_user);

                                                                            try {
                                                                                V_Avatar.setImageBitmap(Utility.StringToBitMap(searched_user.getAvatarString()));
                                                                            } catch (Exception e) {
                                                                                V_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                                                            }

                                                                        }
                                                                    }).addOnFailureListener(new OnFailureListener() {
                                                                        @Override
                                                                        public void onFailure(@NonNull Exception exception) {
                                                                            Toast.makeText(SearchActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                                                                        }
                                                                    });
                                                                } else {
                                                                    V_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                            }
                                                        });
                                                    } else {
                                                        if (!searched_user.getAvatar().equals("default")) {
                                                            try {
                                                                V_Avatar.setImageBitmap(Utility.StringToBitMap(searched_user.getAvatarString()));
                                                            } catch (Exception e) {
                                                                V_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                                            }
                                                        } else {
                                                            V_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                                        }
                                                    }
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) { }
                                            });

                                        } else { // ако юзъра не е бил теглен до момента
                                            final UserModel new_user = new UserModel();
                                            new_user.setUid(UID);
                                            new_user.setNick(nickname);
                                            db.addUser(new_user);

                                            // Трябваше да направя privateUserInfo, friendUserInfo и publicUserInfo, обаче понеже съм мързел и сега ей така ще се тегли 1 по 1

                                            dbRef.child("userInfo").child(UID).child("avatarCh").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    final Long avatarCh = dataSnapshot.getValue(Long.class);
                                                    new_user.setAvatarCh(avatarCh);
                                                    db.updateUser(new_user);
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) { }
                                            });

                                            dbRef.child("userInfo").child(UID).child("avatar").addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    final String avatar = dataSnapshot.getValue(String.class);
                                                    new_user.setAvatar(avatar);
                                                    db.updateUser(new_user);
                                                    if (!avatar.equals("default")) {
                                                        StorageReference avatarImagesRef = storage.getReferenceFromUrl(avatar);
                                                        final long ONE_MEGABYTE = 1024 * 1024;
                                                        avatarImagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                                            @Override
                                                            public void onSuccess(byte[] bytes) {
                                                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                                bitmap = Utility.CropBitmapCenterCircle(bitmap);
                                                                String base64String = Utility.BitMapToString(bitmap);
                                                                new_user.setAvatarString(base64String);

                                                                db.updateUser(new_user);

                                                                try {
                                                                    V_Avatar.setImageBitmap(Utility.StringToBitMap(new_user.getAvatarString()));
                                                                } catch (Exception e) {
                                                                    V_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                                                }

                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception exception) {
                                                                Toast.makeText(SearchActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                                                            }
                                                        });
                                                    } else {
                                                        V_Avatar.setBackgroundResource(R.drawable.clouder_avatar_default);
                                                    }
                                                }
                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) { }
                                            });
                                        }

                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                                });
                                //--
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { }
                });
            }
        }
    };

} // end of SearchActivity{}