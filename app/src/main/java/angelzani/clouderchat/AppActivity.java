package angelzani.clouderchat;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import angelzani.clouderchat.DB.DatabaseHelper;
import angelzani.clouderchat.DB.OnlineFriendsCounter;
import angelzani.clouderchat.DB.PrivChatModel;
import angelzani.clouderchat.DB.RequestModel;
import angelzani.clouderchat.DB.SendPrivChatModel;
import angelzani.clouderchat.DB.SendRequest;
import angelzani.clouderchat.DB.UserModel;
import angelzani.clouderchat.UI.ConstraintLayoutFrUID;
import angelzani.clouderchat.UI.ConstraintLayoutPrivChat;
import angelzani.clouderchat.UI.ConstraintLayout_UID_Date;
import angelzani.clouderchat.UI.ZoomableImageView;
import angelzani.clouderchat.Utility.StorageHelper;
import angelzani.clouderchat.Utility.Utility;

public class AppActivity extends AppCompatActivity {

    @Override
    public void onBackPressed() {
        if(app_CL_BigImage.getVisibility() == View.VISIBLE){ // ако е отворена картинка
            app_IB_BigImageClose.performClick();
        }
        else if(app_CL_PrivChat.getVisibility() == View.VISIBLE){ // ако чата е отворен искаме да го затвори
            app_IV_PrivChat_X.performClick();
        }
        else if(search_CL_Main.getVisibility() == View.VISIBLE){ // ако търсачката е отворена
            search_IB_Back.performClick();
        }
        else if(self_CL_Main.getVisibility() == View.VISIBLE){ // ако профила е отворен
            self_IV_Back.performClick();
        }
        else{
            this.moveTaskToBack(true); // при бек бутона да отива в хоум скрийна вместо да приключва активитито и да се връща в loginregactivity
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO_FOR_AVATAR && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                Toast.makeText(AppActivity.this, "data = null", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                //Now you can do whatever you want with your inpustream, save it as file, upload to a server, decode a bitmap...
                avatarBitmap = BitmapFactory.decodeStream(inputStream);
                avatarBitmap = Utility.CropBitmapCenterCircle(avatarBitmap);
                ((ImageView)findViewById(R.id.self_IV_Avatar)).setImageBitmap(avatarBitmap);
                uploadAvatar = true;
                self_IV_Accept.setVisibility(View.VISIBLE);
                self_IV_Decline.setVisibility(View.VISIBLE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == PICK_PHOTO_FOR_CHAT && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                Toast.makeText(AppActivity.this, "data = null", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = Utility.resizeBitmapTo1024pxMax(BitmapFactory.decodeStream(inputStream));
                if(bitmap==null) return; //едва ли, но все пак

                final SendPrivChatModel messageToSend = new SendPrivChatModel(user.getUid(), Utility.BitMapToString(bitmap), 2);
                final String friendUID = private_chat_friend;
                dbRef.child("privateChats").child(friendUID).push().setValue(messageToSend, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if(databaseError!=null){
                            Toast.makeText(getApplicationContext(), "You are not authorized to message this user.", Toast.LENGTH_LONG).show();
                        } else {
                            final PrivChatModel messageToSave = new PrivChatModel(friendUID, messageToSend.getSender(), messageToSend.getMessage(), messageToSend.getType(), System.currentTimeMillis());
                            db.addMessage(messageToSave);
                            if(friendUID.equals(private_chat_friend)){ // искаме да се уверим, че след като е изпратено съобщението успешно, юзъра вече не е затворил прозореца и да е отворил друг чат за да не показваме съобщението там
                                showMessageInThePrivateChat(friendUID, messageToSave, "bottom");
                            }
                            //Да заредим съобщението в листа с чатове (т'ва нещо за да се изчисти и да се вика по 2/3 различни начина от един метод ще отнеме много време -> не си струва занимавката)
                            int childrenCount = app_LL_Chats.getChildCount();
                            boolean isShown = false;
                            int index = 0;
                            for(int i=0; i<childrenCount; i++){
                                if(((ConstraintLayoutPrivChat)app_LL_Chats.getChildAt(i)).getFriendUID().equals(messageToSave.getTargetUID())){
                                    isShown = true;
                                    index = i;
                                    break;
                                }
                            }
                            if(isShown){ // вече е показан -> нуждае се само от обновление
                                UserModel user_friend = db.getUser(messageToSave.getTargetUID());

                                ConstraintLayoutPrivChat layout = ((ConstraintLayoutPrivChat)app_LL_Chats.getChildAt(index));

                                app_LL_Chats.removeView(layout);
                                app_LL_Chats.addView(layout, 0); // отива най-горе

                                if(messageToSave.getType() == 1){
                                    layout.TV_Message.setText("\u21AA " + messageToSave.getMessage());
                                } else if (messageToSave.getType() == 2) {
                                    layout.TV_Message.setText("\u21AA Sent photo");
                                }

                                if(!user_friend.getAvatar().equals("default")){
                                    layout.IV_Avatar.setImageBitmap(Utility.StringToBitMap(user_friend.getAvatarString()));
                                } else {
                                    layout.IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                }

                                SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");
                                layout.TV_Date.setText(SDF.format(new Date(messageToSave.getDate())));

                                layout.setBackground(gdChatsBackRead);

                            } else { // не е показан -> трябва да го добавим

                                final ConstraintLayoutPrivChat layoutPrivChat = new ConstraintLayoutPrivChat(getApplicationContext());
                                layoutPrivChat.setFriendUID(messageToSave.getTargetUID());
                                app_LL_Chats.addView(layoutPrivChat, 0); // най-горе
                                Utility.setMargins(layoutPrivChat, layout_margin_start_end/2, layout_margin_start_end/4, layout_margin_start_end/2, 0);
                                layoutPrivChat.setBackground(gdChatsBackRead);

                                ImageView IV_Avatar = layoutPrivChat.IV_Avatar;
                                TextView TV_Nick = layoutPrivChat.TV_Nick, TV_Date = layoutPrivChat.TV_Date, TV_Message = layoutPrivChat.TV_Message;

                                IV_Avatar.getLayoutParams().width = avatar;
                                IV_Avatar.getLayoutParams().height = avatar;
                                IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                IV_Avatar.setScaleType(ImageView.ScaleType.FIT_XY);

                                TV_Nick.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                                TV_Nick.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_white));

                                TV_Date.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_60);
                                TV_Date.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.text_lightgray1));
                                SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");
                                TV_Date.setText(SDF.format(new Date(messageToSave.getDate())));

                                TV_Message.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_50);
                                TV_Message.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_whitish));
                                TV_Message.setMaxLines(1);
                                if(messageToSave.getType() == 1){
                                    TV_Message.setText("\u21A9 " + messageToSave.getMessage());
                                } else if (messageToSave.getType() == 2) {
                                    TV_Message.setText("Sent photo");
                                }

                                if(db.getUser(messageToSave.getTargetUID()) == null){ // нямаме юзъра -> това би станало в много редки случаи и заради това просто оставяме ника и аватара празни (при добра връзка при следващото съобщение ще се обнови)
                                    TV_Nick.setText("");
                                    IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                } else { // имаме юзъра
                                    TV_Nick.setText(db.getUser(messageToSave.getTargetUID()).getNick());
                                    if(!db.getUser(messageToSave.getTargetUID()).getAvatar().equals("default")){
                                        IV_Avatar.setImageBitmap(Utility.StringToBitMap(db.getUser(messageToSave.getTargetUID()).getAvatarString()));
                                    } else {
                                        IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                    }
                                }

                                ConstraintSet cs = new ConstraintSet();
                                cs.clone(layoutPrivChat);
                                cs.connect(IV_Avatar.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, layout_margin_start_end/2);
                                cs.connect(IV_Avatar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layout_margin_start_end/2);
                                cs.connect(IV_Avatar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, layout_margin_start_end/2);
                                cs.connect(TV_Nick.getId(), ConstraintSet.START, IV_Avatar.getId(), ConstraintSet.END, layout_margin_start_end/2);
                                cs.connect(TV_Nick.getId(), ConstraintSet.TOP, IV_Avatar.getId(), ConstraintSet.TOP);
                                cs.connect(TV_Date.getId(), ConstraintSet.TOP, TV_Nick.getId(), ConstraintSet.TOP);
                                cs.connect(TV_Date.getId(), ConstraintSet.START, TV_Nick.getId(), ConstraintSet.END, layout_margin_start_end/4);
                                cs.connect(TV_Message.getId(), ConstraintSet.TOP, TV_Nick.getId(), ConstraintSet.BOTTOM);
                                cs.connect(TV_Message.getId(), ConstraintSet.START, TV_Nick.getId(), ConstraintSet.START);
                                cs.connect(TV_Message.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, layout_margin_start_end/2);
                                cs.applyTo(layoutPrivChat);

                                layoutPrivChat.setBackground(gdChatsBackRead);

                                layoutPrivChat.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        layoutPrivChat.setBackground(gdChatsBackRead);
                                        openPrivateChat(messageToSave.getTargetUID());
                                    }
                                });
                            }

                            if (app_LL_PrivChatBody.getChildCount() == 1) { // искаме да вземем датата най-старото съобщение в чата, в случай, че потребителят до сега не си е писал с този човек ще се прецака много жестоко цялата система и ще тегли едни и същи съобщения -> за това се подсигуряваме
                                private_chat_oldest_date_loaded = messageToSave.getDate();
                            }
                        }
                    }
                });


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else { //Cancelled
        }
    }

    // UI
    // Display Metrics
    private int width, height;
    // Малко променливи за да оптимизираме работата с изчисленията и оразмеряването:
    private int h_div_14, h_div_16, h_div_12_5, h_div_26_666, h_div_40, h_div_44, h_div_28_5714, h_div_80, h_div_32, h_div_100, h_div_120, h_div_60, h_div_160, h_div_320, h_div_23, h_div_72, h_div_20, h_div_44_44, h_div_66_66, h_div_36, h_div_50, h_div_57;
    private int avatar, layout_margin_start_end, presence;

    // Малко drawables по същата причина:
    private GradientDrawable gdButtons, gdPresenceOnline, gdPresenceOffline, gdFriendsBack, gdChatsBackRead, gdChatsBackUnread, gdRequestBack;

    // APP MAIN <-----
    private ConstraintLayout app_CL_Main;

    // Header
    private ConstraintLayout app_CL_Header;
    private ImageView app_IV_Avatar;
    private ConstraintLayout app_CL_SearchHeader;
    private ImageView app_IV_IconSearch;
    private TextView app_TV_Search;
    // Bottom menu
    private ConstraintLayout app_CL_BottomMenu, app_CL_BottomMenuWrap;
    private ImageButton app_IB_Chat_Menu, app_IB_Friends_Menu, app_IB_Requests_Menu;
    private TextView app_TV_Requests, app_TV_OnlineFriends, app_TV_ChatNew;
    // Content layouts
    private ConstraintLayout app_CL_Friends, app_CL_Requests, app_CL_Chats;
    private ScrollView app_SV_Friends, app_SV_Requests, app_SV_Chats;
    private LinearLayout app_LL_Friends, app_LL_Requests, app_LL_Chats;
    // Private Chat
    private ConstraintLayout app_CL_PrivChat;
    private ConstraintLayout app_CL_PrivChatHeader;
    private ImageView app_IV_PrivChat_Avatar;
    private View app_V_PrivChat_Presence;
    private TextView app_TV_PrivChat_Nick;
    private ImageView app_IV_PrivChat_X;
    private ConstraintLayout app_CL_PrivChatBottom;
    private ImageView app_IV_PrivChat_Gallery;
    private ImageButton app_IB_PrivChat_Send;
    private EditText app_ET_PrivChat_TextBox;
    private ScrollView app_SV_PrivChatBody;
    private LinearLayout app_LL_PrivChatBody;

    private String private_chat_friend = "";
    private Long private_chat_oldest_date_loaded = 0L;

    // SEARCH <-----
    private ConstraintLayout search_CL_Main;
    //Header
    private ConstraintLayout search_CL_Header;
    private ImageButton search_IB_Back;
    private ConstraintLayout search_CL_Search;
    private EditText search_ET_Search;
    private ImageButton search_IB_Search;
    //Results
    private ScrollView search_SV_Results;
    private ConstraintLayout search_CL_People;
    private LinearLayout search_LL_Results;
    private LinearLayout search_LL_People;
    private LinearLayout search_LL_PeopleFriends;
    private LinearLayout search_LL_PeopleResults;
    private TextView search_TV_People;

    // PROFILE <-----
    private ConstraintLayout self_CL_Main;

    private ConstraintLayout self_CL_Header;
    private ImageView self_IV_Back;
    private TextView self_TV_Me;

    private ConstraintLayout self_CL_inLL;

    private ImageView self_IV_CircleBack;
    private ImageView self_IV_Avatar;
    private ImageView self_IV_Accept;
    private ImageView self_IV_Decline;
    private TextView self_TV_Nickname;

    private ConstraintLayout self_CL_WrapPointsAge;
    private LinearLayout self_LL_MiddleWrap;
    private ImageView self_IV_Droplets;
    private TextView self_TV_DropletsCount;
    private TextView self_TV_Droplets;
    private ImageView self_IV_Age;
    private TextView self_TV_AgeCount;
    private TextView self_TV_Age;

    private LinearLayout self_LL_Settings_Wrap;
    private ConstraintLayout self_CL_Setting_Switch;
    private Switch self_Switch;
    private ConstraintLayout self_CL_Setting_Logout;
    private TextView self_TV_Logout;
    private ImageView self_IV_Logout;

    // РАЗГЛЕЖДАНЕ НА КАРТИНКА <-----
    private ConstraintLayout app_CL_BigImage;
    private ZoomableImageView app_IV_BigImage;
    private ConstraintLayout app_CL_BigImageMenu;
    private ImageButton app_IB_BigImageClose;
    private ImageButton app_IB_BigImageSave;

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_app);

        db = new DatabaseHelper(getApplicationContext());

        // APP MAIN <-----
        app_CL_Main = findViewById(R.id.app_CL_Main);

        app_CL_Header = findViewById(R.id.app_CL_Header);
        app_IV_Avatar = findViewById(R.id.app_IV_Avatar);
        app_CL_SearchHeader = findViewById(R.id.app_CL_SearchHeader);
        app_IV_IconSearch = findViewById(R.id.app_IV_IconSearch);
        app_TV_Search = findViewById(R.id.app_TV_Search);

        app_CL_BottomMenu = findViewById(R.id.app_CL_BottomMenu);
        app_CL_BottomMenuWrap = findViewById(R.id.app_CL_BottomMenuWrap);
        app_IB_Chat_Menu = findViewById(R.id.app_IB_Chat_Menu);
        app_TV_ChatNew = findViewById(R.id.app_TV_ChatNew);
        app_IB_Friends_Menu = findViewById(R.id.app_IB_Friends_Menu);
        app_TV_OnlineFriends = findViewById(R.id.app_TV_OnlineFriends);
        app_IB_Requests_Menu = findViewById(R.id.app_IB_Requests_Menu);
        app_TV_Requests = findViewById(R.id.app_TV_Requests);

        app_CL_Chats = findViewById(R.id.app_CL_Chats);
        app_SV_Chats = findViewById(R.id.app_SV_Chats);
        app_LL_Chats = findViewById(R.id.app_LL_Chats);
        app_CL_Friends = findViewById(R.id.app_CL_Friends);
        app_SV_Friends = findViewById(R.id.app_SV_Friends);
        app_LL_Friends = findViewById(R.id.app_LL_Friends);
        app_CL_Requests = findViewById(R.id.app_CL_Requests);
        app_SV_Requests = findViewById(R.id.app_SV_Requests);
        app_LL_Requests = findViewById(R.id.app_LL_Requests);

        app_CL_PrivChat = findViewById(R.id.app_CL_PrivChat);
        app_CL_PrivChatHeader = findViewById(R.id.app_CL_PrivChatHeader);
        app_IV_PrivChat_Avatar = findViewById(R.id.app_IV_PrivChat_Avatar);
        app_V_PrivChat_Presence = findViewById(R.id.app_V_PrivChat_Presence);
        app_TV_PrivChat_Nick = findViewById(R.id.app_TV_PrivChat_Nick);
        app_IV_PrivChat_X = findViewById(R.id.app_IV_PrivChat_X);
        app_CL_PrivChatBottom = findViewById(R.id.app_CL_PrivChatBottom);
        app_IV_PrivChat_Gallery = findViewById(R.id.app_IV_PrivChat_Gallery);
        app_IB_PrivChat_Send = findViewById(R.id.app_IB_PrivChat_Send);
        app_ET_PrivChat_TextBox = findViewById(R.id.app_ET_PrivChat_TextBox);
        app_SV_PrivChatBody = findViewById(R.id.app_SV_PrivChatBody);
        app_LL_PrivChatBody = findViewById(R.id.app_LL_PrivChatBody);

        // SEARCH <-----
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

        // PROFILE <-----
        self_CL_Main = findViewById(R.id.self_CL_Main);

        self_CL_Header = findViewById(R.id.self_CL_Header);
        self_IV_Back = findViewById(R.id.self_IV_Back);
        self_TV_Me = findViewById(R.id.self_TV_Me);

        self_CL_inLL = findViewById(R.id.self_CL_inLL);

        self_IV_CircleBack = findViewById(R.id.self_IV_CircleBack);
        self_IV_Avatar = findViewById(R.id.self_IV_Avatar);
        self_IV_Accept = findViewById(R.id.self_IV_Accept);
        self_IV_Decline = findViewById(R.id.self_IV_Decline);
        self_TV_Nickname = findViewById(R.id.self_TV_Nickname);

        self_CL_WrapPointsAge = findViewById(R.id.self_CL_WrapPointsAge);
        self_LL_MiddleWrap = findViewById(R.id.self_LL_MiddleWrap);
        self_IV_Droplets = findViewById(R.id.self_IV_Droplets);
        self_TV_DropletsCount = findViewById(R.id.self_TV_DropletsCount);
        self_TV_Droplets = findViewById(R.id.self_TV_Droplets);
        self_IV_Age = findViewById(R.id.self_IV_Age);
        self_TV_AgeCount = findViewById(R.id.self_TV_AgeCount);
        self_TV_Age = findViewById(R.id.self_TV_Age);

        self_LL_Settings_Wrap = findViewById(R.id.self_LL_Settings_Wrap);
        self_CL_Setting_Switch = findViewById(R.id.self_CL_Setting_Switch);
        self_Switch = findViewById(R.id.self_Switch);
        self_CL_Setting_Logout = findViewById(R.id.self_CL_Setting_Logout);
        self_TV_Logout = findViewById(R.id.self_TV_Logout);
        self_IV_Logout = findViewById(R.id.self_IV_Logout);

        // РАЗГЛЕЖДАНЕ НА КАРТИНКА <-----
        app_CL_BigImage = findViewById(R.id.app_CL_BigImage);
        app_IV_BigImage = findViewById(R.id.app_IV_BigImage);
        app_CL_BigImageMenu = findViewById(R.id.app_CL_BigImageMenu);
        app_IB_BigImageClose = findViewById(R.id.app_IB_BigImageClose);
        app_IB_BigImageSave = findViewById(R.id.app_IB_BigImageSave);


        // ----- Resizing [ НАЧАЛО ]
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        // Малко променливи за да оптимизираме работата с изчисленията и оразмеряването:
        h_div_14 = height/14;
        h_div_16 = height/16;
        h_div_12_5 = (int)(height/12.5);
        h_div_26_666 = (int)(height/26.666);
        h_div_40 = height/40;
        h_div_44 = height / 44;
        h_div_28_5714 = (int)(height/28.5714);
        h_div_80 = height / 80;
        h_div_32 = height/32;
        h_div_100 = height/100;
        h_div_120 = height/120;
        h_div_60 = height/60;
        h_div_160 = height/160;
        h_div_320 = height/320;
        h_div_23 = height/23;
        h_div_72 = height/72;
        h_div_20 = height/20;
        h_div_44_44 = (int)(height/44.44);
        h_div_66_66 = (int) (height/66.66);
        h_div_36 = height / 36;
        h_div_50 = height/50;
        h_div_57 = height / 57;
        avatar = height / 14;
        presence = height/14/4;
        layout_margin_start_end = (int)((height / 28.5714)-(height/14-height/16)/2);

        // GradientDrawables
        gdButtons = new GradientDrawable();
        gdButtons.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_mid));
        //gdSearchBack.setStroke(app_CL_SearchHeader.getLayoutParams().height/48, ContextCompat.getColor(this, R.color.gui_gray_dark));
        gdButtons.setShape(GradientDrawable.RECTANGLE);
        gdButtons.setCornerRadius(layout_margin_start_end/4);

        gdPresenceOnline = new GradientDrawable();
        gdPresenceOnline.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_online_color));
        gdPresenceOnline.setStroke(presence/6, ContextCompat.getColor(getApplicationContext(), R.color.gui_friends_back));
        gdPresenceOnline.setShape(GradientDrawable.OVAL);

        gdPresenceOffline = new GradientDrawable();
        gdPresenceOffline.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_normal));
        gdPresenceOffline.setStroke(presence/6, ContextCompat.getColor(getApplicationContext(), R.color.gui_friends_back));
        gdPresenceOffline.setShape(GradientDrawable.OVAL);

        gdFriendsBack = new GradientDrawable();
        gdFriendsBack.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_friends_back));
        gdFriendsBack.setStroke(layout_margin_start_end/48, ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_dark));
        gdFriendsBack.setShape(GradientDrawable.RECTANGLE);
        gdFriendsBack.setCornerRadius(layout_margin_start_end/4);

        gdChatsBackRead = new GradientDrawable();
        gdChatsBackRead.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_friends_back));
        gdChatsBackRead.setStroke(layout_margin_start_end / 48, ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_dark));
        gdChatsBackRead.setShape(GradientDrawable.RECTANGLE);
        gdChatsBackRead.setCornerRadius(layout_margin_start_end / 4);

        gdChatsBackUnread = new GradientDrawable();
        gdChatsBackUnread.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_request_new));
        gdChatsBackUnread.setStroke(layout_margin_start_end / 48, ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_dark));
        gdChatsBackUnread.setShape(GradientDrawable.RECTANGLE);
        gdChatsBackUnread.setCornerRadius(layout_margin_start_end / 4);

        gdRequestBack = new GradientDrawable();
        gdRequestBack.setColor(ContextCompat.getColor(this, R.color.gui_request_old));
        gdRequestBack.setShape(GradientDrawable.RECTANGLE);
        gdRequestBack.setCornerRadius(layout_margin_start_end/4);

        /*if(width > height) { // при завъртане всички визуални елементи се преизчисляват, заради това се налага този хак
            int temp = width;
            height = width;
            width = temp;
        }*/

        // APP MAIN <-----
        /* Header */
        app_CL_Header.getLayoutParams().height = h_div_12_5;
        app_IV_Avatar.getLayoutParams().height = h_div_16;
        app_IV_Avatar.getLayoutParams().width = h_div_16;
        app_CL_SearchHeader.getLayoutParams().height = h_div_16;
        app_IV_IconSearch.getLayoutParams().height = h_div_26_666;
        app_IV_IconSearch.getLayoutParams().width = app_IV_IconSearch.getLayoutParams().height;
        app_TV_Search.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);

        ConstraintSet cs = new ConstraintSet();
        cs.clone(app_CL_Header);
        cs.connect(R.id.app_IV_Avatar, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, h_div_28_5714);
        cs.connect(R.id.app_CL_SearchHeader, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, h_div_28_5714);
        cs.connect(R.id.app_CL_SearchHeader, ConstraintSet.START, R.id.app_IV_Avatar, ConstraintSet.END, h_div_28_5714);
        cs.applyTo(app_CL_Header);

        GradientDrawable gdSearchBack = new GradientDrawable();
        gdSearchBack.setColor(ContextCompat.getColor(this, R.color.gui_gray_normal));
        //gdSearchBack.setStroke(app_CL_SearchHeader.getLayoutParams().height/48, ContextCompat.getColor(this, R.color.gui_gray_dark));
        gdSearchBack.setShape(GradientDrawable.RECTANGLE);
        gdSearchBack.setCornerRadius(app_CL_SearchHeader.getLayoutParams().height / 8);
        app_CL_SearchHeader.setBackground(gdSearchBack);

        cs.clone(app_CL_SearchHeader);
        cs.connect(R.id.app_TV_Search, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, h_div_40);
        cs.connect(R.id.app_TV_Search, ConstraintSet.END, R.id.app_IV_IconSearch, ConstraintSet.START, h_div_80);
        cs.connect(R.id.app_IV_IconSearch, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, h_div_40);
        cs.applyTo(app_CL_SearchHeader);
        /* Header */

        /* Bottom Menu */
        app_CL_BottomMenu.getLayoutParams().height = h_div_12_5;
        app_IB_Chat_Menu.getLayoutParams().height = h_div_16;
        app_IB_Chat_Menu.getLayoutParams().width = h_div_16;
        app_IB_Friends_Menu.getLayoutParams().height = h_div_16;
        app_IB_Friends_Menu.getLayoutParams().width = h_div_16;
        app_IB_Requests_Menu.getLayoutParams().height = h_div_16;
        app_IB_Requests_Menu.getLayoutParams().width = h_div_16;

        app_TV_Requests.getLayoutParams().height = h_div_32;
        app_TV_Requests.setPadding(h_div_100, 0, h_div_100, 0);
        app_TV_Requests.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_60);
        app_TV_Requests.setTextColor(Color.WHITE);
        app_TV_Requests.setShadowLayer(1, 1, 1, Color.BLACK);
        app_TV_Requests.setVisibility(View.INVISIBLE);
        GradientDrawable gdRequestTextBack = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0xFFB161E2,0xFF6080E0});
        gdRequestTextBack.setShape(GradientDrawable.RECTANGLE);
        gdRequestTextBack.setCornerRadius(h_div_160);
        gdRequestTextBack.setStroke(h_div_320, ContextCompat.getColor(this, R.color.gui_gray_dark));
        app_TV_Requests.setBackground(gdRequestTextBack);

        app_TV_OnlineFriends.getLayoutParams().height = h_div_32;
        app_TV_OnlineFriends.setPadding(h_div_100, 0, h_div_100, 0);
        app_TV_OnlineFriends.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_60);
        app_TV_OnlineFriends.setTextColor(Color.WHITE);
        app_TV_OnlineFriends.setShadowLayer(1, 1, 1, Color.BLACK);
        app_TV_OnlineFriends.setVisibility(View.INVISIBLE);
        GradientDrawable gdFriendsTextBack = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0xFFD2F952,0xFF33E851});
        gdFriendsTextBack.setShape(GradientDrawable.RECTANGLE);
        gdFriendsTextBack.setCornerRadius(h_div_160);
        gdFriendsTextBack.setStroke(h_div_320, ContextCompat.getColor(this, R.color.gui_gray_dark));
        app_TV_OnlineFriends.setBackground(gdFriendsTextBack);

        app_TV_ChatNew.getLayoutParams().height = h_div_32;
        app_TV_ChatNew.getLayoutParams().width = h_div_23;
        app_TV_ChatNew.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_72);
        app_TV_ChatNew.setTextColor(Color.WHITE);
        app_TV_ChatNew.setShadowLayer(1, 1, 1, Color.BLACK);
        app_TV_ChatNew.setVisibility(View.INVISIBLE);
        GradientDrawable gdChatNewTextBack = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0xFF42C5F4,0xFF41A9F4});
        gdChatNewTextBack.setShape(GradientDrawable.RECTANGLE);
        gdChatNewTextBack.setCornerRadius(h_div_160);
        gdChatNewTextBack.setStroke(h_div_320, ContextCompat.getColor(this, R.color.gui_gray_dark));
        app_TV_ChatNew.setBackground(gdChatNewTextBack);


        cs.clone(app_CL_BottomMenuWrap);
        cs.connect(R.id.app_IB_Friends_Menu, ConstraintSet.START, R.id.app_IB_Chat_Menu, ConstraintSet.END, h_div_32);
        cs.connect(R.id.app_IB_Requests_Menu, ConstraintSet.START, R.id.app_IB_Friends_Menu, ConstraintSet.END, h_div_32);
        cs.applyTo(app_CL_BottomMenuWrap);
        /* Bottom Menu */

        /* Private Chat */
        app_CL_PrivChatHeader.getLayoutParams().height = h_div_12_5;
        app_IV_PrivChat_Avatar.getLayoutParams().height = h_div_16;
        app_IV_PrivChat_Avatar.getLayoutParams().width = app_IV_PrivChat_Avatar.getLayoutParams().height;
        app_V_PrivChat_Presence.getLayoutParams().height = app_IV_PrivChat_Avatar.getLayoutParams().height/4;
        app_V_PrivChat_Presence.getLayoutParams().width = app_V_PrivChat_Presence.getLayoutParams().height;
        app_TV_PrivChat_Nick.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
        app_IV_PrivChat_X.getLayoutParams().height = h_div_20;
        app_IV_PrivChat_X.getLayoutParams().width = app_IV_PrivChat_X.getLayoutParams().height;
        app_IV_PrivChat_Gallery.getLayoutParams().height = h_div_20;
        app_IV_PrivChat_Gallery.getLayoutParams().width = app_IV_PrivChat_Gallery.getLayoutParams().height;
        app_IB_PrivChat_Send.getLayoutParams().height = h_div_20;
        app_IB_PrivChat_Send.getLayoutParams().width = app_IB_PrivChat_Send.getLayoutParams().height;
        app_ET_PrivChat_TextBox.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_44_44);
        app_ET_PrivChat_TextBox.setPadding(h_div_66_66,0,h_div_66_66,0);

        cs.clone(app_CL_PrivChatHeader);
        cs.connect(R.id.app_IV_PrivChat_Avatar, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, h_div_28_5714);
        cs.connect(R.id.app_TV_PrivChat_Nick, ConstraintSet.START, R.id.app_IV_PrivChat_Avatar, ConstraintSet.END, h_div_66_66);
        cs.connect(R.id.app_IV_PrivChat_X, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, h_div_66_66);
        cs.applyTo(app_CL_PrivChatHeader);

        cs.clone(app_CL_PrivChatBottom);
        cs.connect(R.id.app_IV_PrivChat_Gallery, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, h_div_66_66);
        cs.connect(R.id.app_IV_PrivChat_Gallery, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, h_div_66_66);
        cs.connect(R.id.app_IV_PrivChat_Gallery, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, h_div_66_66);
        cs.connect(R.id.app_IB_PrivChat_Send, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, h_div_66_66);
        cs.connect(R.id.app_IB_PrivChat_Send, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, h_div_66_66);
        cs.connect(R.id.app_IB_PrivChat_Send, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, h_div_66_66);
        cs.connect(R.id.app_ET_PrivChat_TextBox, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, h_div_66_66);
        cs.connect(R.id.app_ET_PrivChat_TextBox, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, h_div_66_66);
        cs.connect(R.id.app_ET_PrivChat_TextBox, ConstraintSet.START, R.id.app_IV_PrivChat_Gallery, ConstraintSet.END, h_div_66_66);
        cs.connect(R.id.app_ET_PrivChat_TextBox, ConstraintSet.END, R.id.app_IB_PrivChat_Send, ConstraintSet.START, h_div_66_66);
        cs.applyTo(app_CL_PrivChatBottom);
        /* Private Chat */

        // SEARCH RESIZING <-----
        search_CL_Header.getLayoutParams().height = h_div_12_5;
        search_IB_Back.getLayoutParams().width = h_div_16;
        search_IB_Back.getLayoutParams().height = h_div_16;
        search_CL_Search.getLayoutParams().height = h_div_16;
        search_ET_Search.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
        search_IB_Search.getLayoutParams().width = h_div_26_666;
        search_IB_Search.getLayoutParams().height = search_IB_Search.getLayoutParams().width;
        search_IB_Search.setColorFilter(ContextCompat.getColor(this, R.color.gui_text_hint)); // Grey Tint

        search_TV_People.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_57);

        search_CL_Search.setBackground(gdSearchBack);

        cs.clone(search_CL_Header);
        cs.connect(R.id.search_IB_Back, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, h_div_28_5714);
        cs.connect(R.id.search_CL_Search, ConstraintSet.START, R.id.search_IB_Back, ConstraintSet.END, h_div_28_5714);
        cs.connect(R.id.search_CL_Search, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, h_div_28_5714);
        cs.applyTo(search_CL_Header);

        cs.clone(search_CL_Search);
        cs.connect(R.id.search_ET_Search, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, h_div_40);
        cs.connect(R.id.search_ET_Search, ConstraintSet.END, R.id.search_IB_Search, ConstraintSet.START, h_div_80);
        cs.connect(R.id.search_IB_Search, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, h_div_40);
        cs.applyTo(search_CL_Search);

        // PROFILE RESIZING <-----
        //Header
        self_CL_Header.getLayoutParams().height = h_div_12_5;
        self_IV_Back.getLayoutParams().height = h_div_16;
        self_IV_Back.getLayoutParams().width = h_div_16;
        self_TV_Me.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_32);

        cs.clone(self_CL_Header);
        cs.connect(R.id.self_IV_Back, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, h_div_28_5714);
        cs.connect(R.id.self_TV_Me, ConstraintSet.START, R.id.self_IV_Back, ConstraintSet.END, h_div_28_5714);
        cs.applyTo(self_CL_Header);

        //Profile
        self_IV_CircleBack.getLayoutParams().width = (int)(width*0.66);
        self_IV_CircleBack.getLayoutParams().height = (int)(width*0.66);
        self_IV_Avatar.getLayoutParams().width = (int)((width*0.66)*0.7);
        self_IV_Avatar.getLayoutParams().height = (int)((width*0.66)*0.7);
        self_IV_Accept.getLayoutParams().width = (int) (height/13.333);
        self_IV_Accept.getLayoutParams().height = (int) (height/13.333);
        self_IV_Decline.getLayoutParams().width = (int) (height/13.333);
        self_IV_Decline.getLayoutParams().height = (int) (height/13.333);
        self_TV_Nickname.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_26_666);

        //Wrap Points Age
        self_CL_WrapPointsAge.getLayoutParams().width = (int)(width/1.371);
        self_CL_WrapPointsAge.getLayoutParams().height = width/8;
        self_LL_MiddleWrap.getLayoutParams().height = (int)(width/10.666);
        self_IV_Droplets.getLayoutParams().width = width/12;
        self_IV_Droplets.getLayoutParams().height = width/12;
        self_TV_DropletsCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, width/24);
        self_TV_Droplets.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int)(width/34.285));
        self_IV_Age.getLayoutParams().width = width/12;
        self_IV_Age.getLayoutParams().height = width/12;
        self_TV_AgeCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, width/24);
        self_TV_Age.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int)(width/34.285));

        cs.clone(self_CL_inLL);
        cs.connect(R.id.self_CL_WrapPointsAge, ConstraintSet.TOP, R.id.self_TV_Nickname, ConstraintSet.BOTTOM, width/24);
        cs.applyTo(self_CL_inLL);

        //Wrap Settings
        cs.clone(self_CL_Main);
        cs.connect(R.id.self_LL_Settings_Wrap, ConstraintSet.TOP, R.id.self_CL_WrapPointsAge, ConstraintSet.BOTTOM, h_div_20);
        cs.applyTo(self_CL_Main);

        self_CL_Setting_Switch.getLayoutParams().height = h_div_16;
        self_Switch.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
        self_CL_Setting_Logout.getLayoutParams().height = h_div_16;
        self_TV_Logout.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
        self_IV_Logout.getLayoutParams().width = h_div_26_666;
        self_IV_Logout.getLayoutParams().height = h_div_26_666;

        // РАЗГЛЕЖДАНЕ НА КАРТИНКА RESIZING <-----
        app_CL_BigImageMenu.getLayoutParams().height = h_div_12_5;
        Utility.setMargins(app_IB_BigImageClose, 0,h_div_80,h_div_80,h_div_80);
        Utility.setMargins(app_IB_BigImageSave, h_div_80,h_div_80,0,h_div_80);

        // Resizing ----- [  КРАЙ  ]

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        fireDB = FirebaseDatabase.getInstance();
        dbRef = fireDB.getReference();
        user = mAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        UserModel localUser = db.getUser(user.getUid());
        if( localUser == null ) { // трябва да изтеглим информацията за юзъра и да я вкараме в локалната дб
            dbRef.child("userInfo").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    final UserModel um = new UserModel();
                    um.setUid(user.getUid());
                    um.setAvatar(dataSnapshot.child("avatar").getValue(String.class));
                    um.setAvatarCh(dataSnapshot.child("avatarCh").getValue(Long.class));
                    um.setAvatarString(null);
                    um.setDateCr(dataSnapshot.child("dateCr").getValue(Long.class));
                    um.setNick(dataSnapshot.child("nick").getValue(String.class));
                    um.setNickCh(dataSnapshot.child("nickCh").getValue(Long.class));
                    um.setPoints(dataSnapshot.child("points").getValue(Long.class));
                    db.addUser(um);

                    if(!um.getAvatar().equals("default")) {
                        StorageReference avatarImagesRef = storage.getReferenceFromUrl(um.getAvatar());
                        final long ONE_MEGABYTE = 1024 * 1024;
                        avatarImagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                bitmap = Utility.CropBitmapCenterCircle(bitmap);
                                String base64String = Utility.BitMapToString(bitmap);
                                um.setAvatarString(base64String);

                                db.updateUser(um);
                                LoadSelfProfile();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Toast.makeText(AppActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        LoadSelfProfile();
                    }

                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(AppActivity.this, "DB Error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else { //трябва просто да покажем и обновим
            LoadSelfProfile();
        }

        prepareLoadFriendList();
        loadFriendRequests();

        //OnClick Listeners
        app_IV_Avatar.setOnClickListener(goToSelfProfile);
        app_IB_Chat_Menu.setOnClickListener(menuSwitcher);
        app_IB_Friends_Menu.setOnClickListener(menuSwitcher);
        app_IB_Requests_Menu.setOnClickListener(menuSwitcher);
        app_CL_SearchHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search_CL_Main.setVisibility(View.VISIBLE);
                Animation expandOutHorizontal = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.expand_out_horizontal);
                search_CL_Main.startAnimation(expandOutHorizontal);
                search_ET_Search.requestFocus();
            }
        });
        app_IV_PrivChat_X.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                private_chat_friend = "";
                private_chat_oldest_date_loaded = 0L;
                Utility.hideKeyboard(AppActivity.this); // преди да се скрие чата трябва да се скрие клавиатурата, понеже стане ли INVISIBLE метода hideKeyboard няма да може да засече фокусираното View... кво става с тоя андроид въобще не знам
                Animation minimizeAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.minimize_fast_vertical);
                app_CL_PrivChat.startAnimation(minimizeAnim);
                app_CL_PrivChat.setVisibility(View.INVISIBLE);
                app_ET_PrivChat_TextBox.setText("");
                app_LL_PrivChatBody.removeAllViews();
            }
        });
        app_IB_PrivChat_Send.setOnClickListener(sendMessage);
        app_ET_PrivChat_TextBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app_SV_PrivChatBody.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        app_SV_PrivChatBody.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                }, 500);
            }
        });
        app_IV_PrivChat_Gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_PHOTO_FOR_CHAT);
            }
        });
        // Search OnClickListeners
        search_IB_Back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.hideKeyboard(AppActivity.this);
                Animation minimize = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.minimize_fast_horizontal);
                search_CL_Main.startAnimation(minimize);
                search_CL_Main.setVisibility(View.INVISIBLE);
            }
        });
        search_IB_Search.setOnClickListener(search);
        // Profile OnClickListeners
        self_IV_Back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Animation minimize = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.minimize_fast_to_bottom);
                self_CL_Main.startAnimation(minimize);
                self_CL_Main.setVisibility(View.INVISIBLE);
            }
        });
        self_CL_Setting_Logout.setOnClickListener(new View.OnClickListener() { // логаут и излизане от приложението -> пращане в хоум скрийн
            @Override
            public void onClick(View v) {
                // директно излизаме от приложението про логаут, иначе се налага да чистим всички firebase listeners (като преди това трябва да ги направим като пропъртита на класа) и става дълга и широка и чак след това да се върнем в login_register_activity с finish()
                FirebaseAuth.getInstance().signOut();
                finishAffinity();
                System.exit(0);
            }
        });
        self_IV_Avatar.setOnClickListener(pickPhoto);
        self_IV_Decline.setOnClickListener(declinePhoto);
        self_IV_Accept.setOnClickListener(acceptPhoto);
        // Разглеждане на картинка OnClickListeners
        app_IB_BigImageClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app_IV_BigImage.setImageBitmap(null); // garbage collectora да си свърши работата
                app_IB_BigImageSave.setOnClickListener(null); // същото
                app_CL_BigImage.setVisibility(View.INVISIBLE);
            }
        });

        // Text Watchers
        app_ET_PrivChat_TextBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                String text = app_ET_PrivChat_TextBox.getText().toString().trim();
                if(text.length() > 0) {
                    app_IB_PrivChat_Send.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_whitish));
                } else {
                    app_IB_PrivChat_Send.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_normal));
                }
            }
        });

        // TextWatcher Search
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
                        TV_Nickname.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
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
                        TV_Connected.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_60);
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
                                final Dialog dialog = new Dialog(AppActivity.this);
                                dialog.setContentView(R.layout.layout_cancel_friend_request);
                                dialog.show();

                                dialog.findViewById(R.id.cancel_CL_Main).getLayoutParams().width = width / 8 * 6;
                                TextView cancel_TV_Title = dialog.findViewById(R.id.cancel_TV_Title);
                                cancel_TV_Title.setText("How would you like to proceed with " + friend.getNick() + "?");
                                ConstraintLayout calcel_CL_ButtonsWrap = dialog.findViewById(R.id.calcel_CL_ButtonsWrap);
                                final Button cancel_B_Block = dialog.findViewById(R.id.cancel_B_Block);
                                final Button cancel_B_Dismiss = dialog.findViewById(R.id.cancel_B_Dismiss);
                                cancel_B_Dismiss.setText("UNFRIEND");

                                Utility.setMargins(cancel_TV_Title, h_div_40, h_div_60, h_div_40, 0);
                                Utility.setMargins(calcel_CL_ButtonsWrap, h_div_40, h_div_40, h_div_40, h_div_80);
                                cancel_TV_Title.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_36);
                                cancel_B_Block.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                                cancel_B_Dismiss.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);

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

                        CL_User.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                search_IB_Back.performClick();
                                openPrivateChat(friend.getUid());
                            }
                        });
                    }
                } else {
                    search_IB_Search.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_hint)); // Grey Tint
                }
            }
        });

        //ScrollChangeListeners
        app_SV_PrivChatBody.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                if (app_SV_PrivChatBody.getScrollY() == 0) { // ако сме стигнали най-горе
                    if(app_CL_PrivChat.getVisibility() == View.VISIBLE){ // когато затваряме чат лейаута ние го чистим и Y скролера пак отива на 0 и метода loadOldMessagesInPrivChat() се вика -> така се справяме с това
                        loadOldMessagesInPrivChat();
                    }
                } else {
                    // не ми трябва за момента
                }
            }
        });

        // eXTRAS
        Bundle extras = getIntent().getExtras();
        if(getIntent().hasExtra("action_code")){

            String action_code = extras.getString("action_code");

            if(action_code.equals("new_message")) {
                self_IV_Back.performClick();
                search_IB_Back.performClick();
                app_IV_PrivChat_X.performClick();
                app_IB_Chat_Menu.performClick();
                openPrivateChat(extras.getString("chat_user_key"));
            }
            else if(action_code.equals("friend_request")) {
                self_IV_Back.performClick();
                search_IB_Back.performClick();
                app_IV_PrivChat_X.performClick();
                app_IB_Requests_Menu.performClick();
            }

        }

    }//end of OnCreate

    /* OnClickListeners [ START ] */
    // Profile clickers
    private View.OnClickListener goToSelfProfile = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Animation expandOutTop = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.expand_out_to_top);
            self_CL_Main.startAnimation(expandOutTop);
            self_CL_Main.setVisibility(View.VISIBLE);
            LoadProfileInfo();
        }
    };
    private void LoadProfileInfo() {
        self_TV_Nickname.setText(currentLocalUser.getNick());
        if(!currentLocalUser.getAvatar().equals("default")) {
            self_IV_Avatar.setImageBitmap(Utility.StringToBitMap(currentLocalUser.getAvatarString()));
        }
        else {
            self_IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
        }
        self_TV_DropletsCount.setText(currentLocalUser.getPoints()+"");

        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        long calculateDate = (currentDate.getTime() - currentLocalUser.getDateCr()) / 1000 / 60; //времето в минути

        if(calculateDate<60) // по-малко от час
        {
            ((TextView)findViewById(R.id.self_TV_AgeCount)).setText(calculateDate + "mins");
        }
        else if(calculateDate<=1440) // по-малко от едно денонощие
        {
            ((TextView)findViewById(R.id.self_TV_AgeCount)).setText(calculateDate/60 + "h " + calculateDate%60 + "m");
        }
        else if(calculateDate<=43200) //минутите са по-малко от един месец
        {
            ((TextView)findViewById(R.id.self_TV_AgeCount)).setText(calculateDate/1440 + "d "+ calculateDate%1440/60 + "h");
        }
        else if(calculateDate<=525948) //минутите са по-малко от една година
        {
            ((TextView)findViewById(R.id.self_TV_AgeCount)).setText(calculateDate/43200 + "m "+ calculateDate%43200/1440 + "d");
        }
        else
        {
            ((TextView)findViewById(R.id.self_TV_AgeCount)).setText(calculateDate/525948 + "y " + calculateDate%525948/43200 + "m "+ calculateDate%43200/1440 + "d");
        }
    }
    private static final int PICK_PHOTO_FOR_AVATAR = 0, PICK_PHOTO_FOR_CHAT = 1;
    private Bitmap avatarBitmap;
    private boolean uploadAvatar = false;
    View.OnClickListener pickPhoto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_PHOTO_FOR_AVATAR);
        }
    };
    View.OnClickListener declinePhoto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            self_IV_Avatar.setImageBitmap(Utility.StringToBitMap(currentLocalUser.getAvatarString()));
            self_IV_Accept.setVisibility(View.INVISIBLE);
            self_IV_Decline.setVisibility(View.INVISIBLE);
        }
    };

    View.OnClickListener acceptPhoto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            final StorageReference avatarImagesRef = storageRef.child("avatars/" + user.getUid() + ".jpg");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            avatarBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();

            UploadTask uploadTask = avatarImagesRef.putBytes(data);
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Upload error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        //throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return avatarImagesRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        final String download_url = downloadUri.toString();
                        final String[] real_url = download_url.split("\\?");

                        //Toast.makeText(LoginRegisterActivity.this, "Download Uri: " + real_url, Toast.LENGTH_LONG).show();

                        final DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("userInfo").child(user.getUid()).child("avatar");
                        // Нито с onUpdate, нито с onWrite trigger стават работите, заради това се налага да напиша нещо в avatar и след това да го сменя със същия стринг -> мястото никога няма да се смени -> добре ще е за напред да внимавам и да не правя излишни работи
                        dbRef.setValue("default", new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                dbRef.setValue(real_url[0], new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                        self_IV_Accept.setVisibility(View.INVISIBLE);
                                        self_IV_Decline.setVisibility(View.INVISIBLE);
                                    }
                                });
                            }
                        });
                    } else {
                        // Handle failures
                        Toast.makeText(AppActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };
    // Bottom Menu Switcher
    private View.OnClickListener menuSwitcher = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Animation expandInBox = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.expand_in_slow);
            //Animation expandOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.expand_out);
            Animation expandOutHorizontal = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.expand_out_horizontal);
            switch (v.getId()){
                case R.id.app_IB_Chat_Menu:
                    app_CL_Friends.setVisibility(View.INVISIBLE);
                    app_CL_Requests.setVisibility(View.INVISIBLE);
                    app_CL_Chats.setVisibility(View.VISIBLE);
                    app_TV_ChatNew.setVisibility(View.INVISIBLE);
                    app_IB_Chat_Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_whitish));
                    app_IB_Friends_Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_light));
                    app_IB_Requests_Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_light));
                    app_CL_Chats.startAnimation(expandOutHorizontal);
                    break;
                case R.id.app_IB_Friends_Menu:
                    app_CL_Chats.setVisibility(View.INVISIBLE);
                    app_CL_Requests.setVisibility(View.INVISIBLE);
                    app_CL_Friends.setVisibility(View.VISIBLE);
                    app_IB_Friends_Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_whitish));
                    app_IB_Chat_Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_light));
                    app_IB_Requests_Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_light));
                    app_CL_Friends.startAnimation(expandOutHorizontal);
                    break;
                case R.id.app_IB_Requests_Menu:
                    app_CL_Chats.setVisibility(View.INVISIBLE);
                    app_CL_Friends.setVisibility(View.INVISIBLE);
                    app_CL_Requests.setVisibility(View.VISIBLE);
                    app_IB_Requests_Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_whitish));
                    app_IB_Friends_Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_light));
                    app_IB_Chat_Menu.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_light));
                    app_CL_Requests.startAnimation(expandOutHorizontal);
                    break;
            }
        }
    };
    private View.OnClickListener sendMessage = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String text = app_ET_PrivChat_TextBox.getText().toString().trim();
            if(text.isEmpty()) return;

            final SendPrivChatModel messageToSend = new SendPrivChatModel(user.getUid(), text, 1);
            final String friendUID = private_chat_friend;
            app_ET_PrivChat_TextBox.setText("");
            dbRef.child("privateChats").child(friendUID).push().setValue(messageToSend, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                    if(databaseError!=null){
                        Toast.makeText(getApplicationContext(), "You are not authorized to message this user.", Toast.LENGTH_LONG).show();
                    } else {
                        final PrivChatModel messageToSave = new PrivChatModel(friendUID, messageToSend.getSender(), messageToSend.getMessage(), messageToSend.getType(), System.currentTimeMillis());
                        db.addMessage(messageToSave);
                        if(friendUID.equals(private_chat_friend)){ // искаме да се уверим, че след като е изпратено съобщението успешно, юзъра вече не е затворил прозореца и да е отворил друг чат за да не показваме съобщението там
                            showMessageInThePrivateChat(friendUID, messageToSave, "bottom");
                        }
                        //Да заредим съобщението в листа с чатове (т'ва нещо за да се изчисти и да се вика по 2/3 различни начина от един метод ще отнеме много време -> не си струва занимавката)
                        int childrenCount = app_LL_Chats.getChildCount();
                        boolean isShown = false;
                        int index = 0;
                        for(int i=0; i<childrenCount; i++){
                            if(((ConstraintLayoutPrivChat)app_LL_Chats.getChildAt(i)).getFriendUID().equals(messageToSave.getTargetUID())){
                                isShown = true;
                                index = i;
                                break;
                            }
                        }
                        if(isShown){ // вече е показан -> нуждае се само от обновление
                            UserModel user_friend = db.getUser(messageToSave.getTargetUID());

                            ConstraintLayoutPrivChat layout = ((ConstraintLayoutPrivChat)app_LL_Chats.getChildAt(index));

                            app_LL_Chats.removeView(layout);
                            app_LL_Chats.addView(layout, 0); // отива най-горе

                            if(messageToSave.getType() == 1){
                                layout.TV_Message.setText("\u21AA " + messageToSave.getMessage());
                            } else if (messageToSave.getType() == 2) {
                                layout.TV_Message.setText("\u21AA Sent photo");
                            }

                            if(!user_friend.getAvatar().equals("default")){
                                layout.IV_Avatar.setImageBitmap(Utility.StringToBitMap(user_friend.getAvatarString()));
                            } else {
                                layout.IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                            }

                            SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");
                            layout.TV_Date.setText(SDF.format(new Date(messageToSave.getDate())));

                            layout.setBackground(gdChatsBackRead);

                        } else { // не е показан -> трябва да го добавим

                            final ConstraintLayoutPrivChat layoutPrivChat = new ConstraintLayoutPrivChat(getApplicationContext());
                            layoutPrivChat.setFriendUID(messageToSave.getTargetUID());
                            app_LL_Chats.addView(layoutPrivChat, 0); // най-горе
                            Utility.setMargins(layoutPrivChat, layout_margin_start_end/2, layout_margin_start_end/4, layout_margin_start_end/2, 0);
                            layoutPrivChat.setBackground(gdChatsBackRead);

                            ImageView IV_Avatar = layoutPrivChat.IV_Avatar;
                            TextView TV_Nick = layoutPrivChat.TV_Nick, TV_Date = layoutPrivChat.TV_Date, TV_Message = layoutPrivChat.TV_Message;

                            IV_Avatar.getLayoutParams().width = avatar;
                            IV_Avatar.getLayoutParams().height = avatar;
                            IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                            IV_Avatar.setScaleType(ImageView.ScaleType.FIT_XY);

                            TV_Nick.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                            TV_Nick.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_white));

                            TV_Date.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_60);
                            TV_Date.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.text_lightgray1));
                            SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");
                            TV_Date.setText(SDF.format(new Date(messageToSave.getDate())));

                            TV_Message.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_50);
                            TV_Message.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_whitish));
                            TV_Message.setMaxLines(1);
                            if(messageToSave.getType() == 1){
                                TV_Message.setText("\u21A9 " + messageToSave.getMessage());
                            } else if (messageToSave.getType() == 2) {
                                TV_Message.setText("Sent photo");
                            }

                            if(db.getUser(messageToSave.getTargetUID()) == null){ // нямаме юзъра -> това би станало в много редки случаи и заради това просто оставяме ника и аватара празни (при добра връзка при следващото съобщение ще се обнови)
                                TV_Nick.setText("");
                                IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                            } else { // имаме юзъра
                                TV_Nick.setText(db.getUser(messageToSave.getTargetUID()).getNick());
                                if(!db.getUser(messageToSave.getTargetUID()).getAvatar().equals("default")){
                                    IV_Avatar.setImageBitmap(Utility.StringToBitMap(db.getUser(messageToSave.getTargetUID()).getAvatarString()));
                                } else {
                                    IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                }
                            }

                            ConstraintSet cs = new ConstraintSet();
                            cs.clone(layoutPrivChat);
                            cs.clone(layoutPrivChat);
                            cs.connect(IV_Avatar.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, layout_margin_start_end/2);
                            cs.connect(IV_Avatar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layout_margin_start_end/2);
                            cs.connect(IV_Avatar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, layout_margin_start_end/2);
                            cs.connect(TV_Nick.getId(), ConstraintSet.START, IV_Avatar.getId(), ConstraintSet.END, layout_margin_start_end/2);
                            cs.connect(TV_Nick.getId(), ConstraintSet.TOP, IV_Avatar.getId(), ConstraintSet.TOP);
                            cs.connect(TV_Date.getId(), ConstraintSet.TOP, TV_Nick.getId(), ConstraintSet.TOP);
                            cs.connect(TV_Date.getId(), ConstraintSet.START, TV_Nick.getId(), ConstraintSet.END, layout_margin_start_end/4);
                            cs.connect(TV_Message.getId(), ConstraintSet.TOP, TV_Nick.getId(), ConstraintSet.BOTTOM);
                            cs.connect(TV_Message.getId(), ConstraintSet.START, TV_Nick.getId(), ConstraintSet.START);
                            cs.connect(TV_Message.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, layout_margin_start_end/2);
                            cs.applyTo(layoutPrivChat);

                            layoutPrivChat.setBackground(gdChatsBackRead);

                            layoutPrivChat.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    layoutPrivChat.setBackground(gdChatsBackRead);
                                    openPrivateChat(messageToSave.getTargetUID());
                                }
                            });
                        }

                        if (app_LL_PrivChatBody.getChildCount() == 1) { // искаме да вземем датата най-старото съобщение в чата, в случай, че потребителят до сега не си е писал с този човек ще се прецака много жестоко цялата система и ще тегли едни и същи съобщения -> за това се подсигуряваме
                            private_chat_oldest_date_loaded = messageToSave.getDate();
                        }
                    }
                }
            });
        }
    };

    // Search click
    private String last_search = "";
    View.OnClickListener search = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final String searchString = search_ET_Search.getText().toString().toLowerCase().trim();

            if (!searchString.isEmpty()) {
                Utility.hideKeyboard(AppActivity.this);
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
                                        TV_Nickname.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                                        TV_Nickname.setTextColor(Color.WHITE);
                                        //TV_Nickname.setShadowLayer(1, 1, 1, Color.BLACK);
                                        TV_Nickname.setText(nickname);
                                        Utility.setMargins(TV_Nickname, h_div_80, 0, 0, 0);

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
                                                final Dialog dialog = new Dialog(AppActivity.this);
                                                dialog.setContentView(R.layout.layout_search_add_friend);
                                                dialog.show();

                                                dialog.findViewById(R.id.addF_CL_Main).getLayoutParams().width = width / 8 * 6;
                                                TextView addF_TV_Title = dialog.findViewById(R.id.addF_TV_Title);
                                                final EditText addF_ET_Message = dialog.findViewById(R.id.addF_ET_Message);
                                                Button addF_B_Send = dialog.findViewById(R.id.addF_B_Send);
                                                Button addF_B_Cancel = dialog.findViewById(R.id.addF_B_Cancel);

                                                addF_TV_Title.setText("Friend request to " + nickname);
                                                Utility.setMargins(addF_TV_Title, h_div_40, h_div_60, 0, 0);
                                                Utility.setMargins(addF_ET_Message, h_div_44, 0, h_div_44, 0);
                                                addF_TV_Title.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_36);
                                                addF_ET_Message.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                                                addF_ET_Message.setText("Hey " + nickname + "! Lets be friends!");
                                                addF_B_Send.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                                                Utility.setMargins(addF_B_Send, 0,0,height / 64,0);
                                                addF_B_Cancel.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);

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
                                                                            Toast.makeText(AppActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
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
                                                                Toast.makeText(AppActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
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
    /* OnClickListeners [  END  ] */

    private UserModel currentLocalUser;
    private void LoadSelfProfile() {
        currentLocalUser = db.getUser(user.getUid());
        // Update online state
        dbRef.child("userInfo").child(user.getUid()).child("online").setValue(true);
        dbRef.child("userInfo").child(user.getUid()).child("online").onDisconnect().setValue(false);

        if(!currentLocalUser.getAvatar().equals("default")) { // ако не е default
            Bitmap avatar = Utility.StringToBitMap(currentLocalUser.getAvatarString());
            if(avatar != null) {
                app_IV_Avatar.setImageBitmap( avatar );
            } else {
                app_IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
            }
        } else {
            app_IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
        }

        // Avatar change
        dbRef.child("userInfo").child(user.getUid()).child("avatarCh").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.getValue(Long.class) != currentLocalUser.getAvatarCh()) {

                    currentLocalUser.setAvatarCh(dataSnapshot.getValue(Long.class));
                    db.updateUser(currentLocalUser);

                    dbRef.child("userInfo").child(user.getUid()).child("avatar").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            currentLocalUser.setAvatar(dataSnapshot.getValue(String.class));
                            db.updateUser(currentLocalUser);

                            if(!currentLocalUser.getAvatar().equals("default")) {
                                StorageReference avatarImagesRef = storage.getReferenceFromUrl(currentLocalUser.getAvatar());
                                final long ONE_MEGABYTE = 1024 * 1024;
                                avatarImagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                    @Override
                                    public void onSuccess(byte[] bytes) {
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        bitmap = Utility.CropBitmapCenterCircle(bitmap);
                                        String base64String = Utility.BitMapToString(bitmap);
                                        currentLocalUser.setAvatarString(base64String);

                                        db.updateUser(currentLocalUser);

                                        Bitmap avatar = Utility.StringToBitMap(currentLocalUser.getAvatarString());
                                        if(avatar != null) {
                                            app_IV_Avatar.setImageBitmap( avatar );
                                        } else {
                                            app_IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                        }

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Toast.makeText(AppActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                            else {
                                app_IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
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

        //Nick change
        dbRef.child("userInfo").child(user.getUid()).child("nickCh").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.getValue(Long.class) != currentLocalUser.getNickCh()) {

                    currentLocalUser.setNickCh(dataSnapshot.getValue(Long.class));
                    db.updateUser(currentLocalUser);

                    dbRef.child("userInfo").child(user.getUid()).child("nick").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            currentLocalUser.setNick(dataSnapshot.getValue(String.class));
                            db.updateUser(currentLocalUser);

                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });

                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        //Points change
        dbRef.child("userInfo").child(user.getUid()).child("points").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                currentLocalUser.setPoints(dataSnapshot.getValue(Long.class));
                db.updateUser(currentLocalUser);

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        //Create date забравих xD
        if(currentLocalUser.getDateCr() == 0){ // ако е 0 значи не сме я теглили до момента
            dbRef.child("userInfo").child(user.getUid()).child("dateCr").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    currentLocalUser.setDateCr(dataSnapshot.getValue(Long.class));
                    db.updateUser(currentLocalUser);

                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }

    }//end of LoadSelfProfile()

    private void prepareLoadFriendList(){ // при всеки логин се налага да се прави това, понеже не знам потребителя от колко устройства се логва и не знам какви промени може да е направил.
        // Ще стане двойно теглене, понеже искаме да сме сигурни, че ако потребителя е прамахнал приятел от друго устройство, ние ще го изтрием от тук.
        dbRef.child("friendList").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                ArrayList<String> friendsUIDs = db.getAllFriendsUIDs(user.getUid());

                if(dataSnapshot.exists()){
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        if(!db.isFriend(user.getUid(), ds.getKey())){ // ако е приятел, но не е добавен в локалната датабаза
                            db.addFriend(user.getUid(), ds.getKey());
                        }
                    }
                    // А сега да изтрием хора, които вече не са ни приятели, но не са изтрити от локалната датабаза като такива
                    if(friendsUIDs!=null){
                        for(String listUID : friendsUIDs) { // не се нуждаем от новите приятели, добавени в датабазата, понеже ни интересуват само тези, които ги няма, а точно в момента тях на 100% ги има (ако има премахнати/блокиране разбира се)
                            boolean found = false;
                            for(DataSnapshot ds : dataSnapshot.getChildren()){
                                if(listUID.equals(ds.getKey())){
                                    found = true;
                                    break; // това би трябвало да брайква само нестнатия лууп, надявам се не греша :D
                                }
                            }
                            if(!found) db.removeFriend(user.getUid(), listUID);
                        }
                    }
                } else { // ако от датабазата няма никакви приятели -> затриваме всички -> или са били изтрити от друго устройство или блокирани и т.н.
                    if(friendsUIDs!=null){
                        for(String friendUID : friendsUIDs){
                            db.removeFriend(user.getUid(), friendUID);
                        }
                    }
                }

                // След като свършихме досадната работа можем да започнем начисто със зареждането на информацията от датабазите:
                loadFriendList();
                loadPrivateChats();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }// end of loadFriendList()

    private OnlineFriendsCounter online_friends_counter = new OnlineFriendsCounter();

    private void loadFriendList() {

        dbRef.child("friendList").child(user.getUid()).addChildEventListener(new ChildEventListener() { // следим за текущите и за добавяне в листа с приятели
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                final String friendUID = dataSnapshot.getKey();
                if (!db.isFriend(user.getUid(), friendUID)) { // ако след зареждането на така или иначе вече изтеглените приятелски айдита по-горе сме добавили нов приятел, трябва да отразим промените
                    db.addFriend(user.getUid(), friendUID);
                }

                final ConstraintLayoutFrUID CL_Friend = new ConstraintLayoutFrUID(getApplicationContext());
                CL_Friend.setId(View.generateViewId());
                CL_Friend.setFriendUID(friendUID);
                CL_Friend.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                app_LL_Friends.addView(CL_Friend);
                Utility.setMargins(CL_Friend, layout_margin_start_end/2, layout_margin_start_end/4, layout_margin_start_end/2, 0);

                CL_Friend.setBackground(gdFriendsBack);

                final ImageView IV_Avatar = new ImageView(getApplicationContext());
                IV_Avatar.setId(View.generateViewId());
                CL_Friend.addView(IV_Avatar);
                IV_Avatar.getLayoutParams().width = avatar;
                IV_Avatar.getLayoutParams().height = avatar;
                IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);

                final TextView TV_Nick = new TextView(getApplicationContext());
                TV_Nick.setId(View.generateViewId());
                CL_Friend.addView(TV_Nick);
                TV_Nick.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                TV_Nick.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_white));

                final ImageView IV_Presence = new ImageView(getApplicationContext());
                IV_Presence.setId(View.generateViewId());
                CL_Friend.addView(IV_Presence);
                IV_Presence.getLayoutParams().width = presence;
                IV_Presence.getLayoutParams().height = presence;
                // IV_Presence.setBackground(gdPresenceOffline);

                final ImageView IV_Remove = new ImageView(getApplicationContext());
                IV_Remove.setId(View.generateViewId());
                CL_Friend.addView(IV_Remove);
                IV_Remove.getLayoutParams().width = (int)(IV_Avatar.getLayoutParams().height*0.8);
                IV_Remove.getLayoutParams().height = IV_Remove.getLayoutParams().width;
                IV_Remove.setImageResource(R.drawable.icon_friend_remove);
                IV_Remove.setBackground(gdButtons);

                IV_Remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Dialog dialog = new Dialog(AppActivity.this);
                        dialog.setContentView(R.layout.layout_cancel_friend_request);
                        dialog.show();

                        dialog.findViewById(R.id.cancel_CL_Main).getLayoutParams().width = width / 8 * 6;
                        TextView cancel_TV_Title = dialog.findViewById(R.id.cancel_TV_Title);
                        cancel_TV_Title.setText("How would you like to proceed with " + db.getUser(friendUID).getNick() + "?"); // в най-лошия случай ще изкара null
                        ConstraintLayout calcel_CL_ButtonsWrap = dialog.findViewById(R.id.calcel_CL_ButtonsWrap);
                        final Button cancel_B_Block = dialog.findViewById(R.id.cancel_B_Block);
                        final Button cancel_B_Dismiss = dialog.findViewById(R.id.cancel_B_Dismiss);
                        cancel_B_Dismiss.setText("UNFRIEND");

                        Utility.setMargins(cancel_TV_Title, h_div_40, h_div_60, h_div_40, 0);
                        Utility.setMargins(calcel_CL_ButtonsWrap, h_div_40, h_div_40, h_div_40, h_div_80);
                        cancel_TV_Title.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_36);
                        cancel_B_Block.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                        cancel_B_Dismiss.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);

                        View.OnClickListener removeFriend = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if(v.getId() == cancel_B_Dismiss.getId()){
                                    dbRef.child("friendList").child(user.getUid()).child(friendUID).setValue(false, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                            Toast.makeText(getApplicationContext(), db.getUser(friendUID).getNick() + " unfriended.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                else if(v.getId() == cancel_B_Block.getId()){
                                    dbRef.child("blockList").child(user.getUid()).child(friendUID).setValue(true, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                            Toast.makeText(getApplicationContext(), db.getUser(friendUID).getNick() + " blocked.", Toast.LENGTH_SHORT).show();
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

                ConstraintSet cs = new ConstraintSet();
                cs.clone(CL_Friend);
                cs.connect(IV_Avatar.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, layout_margin_start_end/2);
                cs.connect(IV_Avatar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layout_margin_start_end/2);
                cs.connect(IV_Avatar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, layout_margin_start_end/2);
                cs.connect(TV_Nick.getId(), ConstraintSet.START, IV_Avatar.getId(), ConstraintSet.END, layout_margin_start_end/2);
                cs.connect(TV_Nick.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                cs.connect(TV_Nick.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                cs.connect(IV_Presence.getId(), ConstraintSet.END, IV_Avatar.getId(),ConstraintSet.END);
                cs.connect(IV_Presence.getId(), ConstraintSet.BOTTOM, IV_Avatar.getId(),ConstraintSet.BOTTOM);
                cs.connect(IV_Remove.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID,ConstraintSet.END, layout_margin_start_end/2);
                cs.connect(IV_Remove.getId(), ConstraintSet.BOTTOM, IV_Avatar.getId(),ConstraintSet.BOTTOM);
                cs.connect(IV_Remove.getId(), ConstraintSet.TOP, IV_Avatar.getId(),ConstraintSet.TOP);
                cs.applyTo(CL_Friend);

                if (db.getUser(friendUID) != null) { // потребителят вече е бил зареждан преди на това устройство

                    final UserModel existing_user = db.getUser(friendUID);

                    // Теглим никнейма и го запазваме (не знам дали не е бил променян)
                    dbRef.child("userInfo").child(friendUID).child("nick").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            existing_user.setNick(dataSnapshot.getValue(String.class));
                            db.updateUser(existing_user);
                            TV_Nick.setText(existing_user.getNick());
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });

                    // Слагаме вече изтегления аватар
                    if(!existing_user.getAvatar().equals("default")){
                        try {
                            IV_Avatar.setImageBitmap(Utility.StringToBitMap(existing_user.getAvatarString()));
                        } catch (Exception e) {
                            IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                        }
                    } else {
                        IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                    }

                    // Обновяваме аватара ако е бил сменен
                    dbRef.child("userInfo").child(friendUID).child("avatarCh").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getValue(Long.class) != existing_user.getAvatarCh()) { // нуждаем се от обновяване
                                existing_user.setAvatarCh(dataSnapshot.getValue(Long.class));
                                db.updateUser(existing_user);

                                dbRef.child("userInfo").child(friendUID).child("avatar").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        existing_user.setAvatar(dataSnapshot.getValue(String.class));
                                        db.updateUser(existing_user);

                                        if(!existing_user.getAvatar().equals("default")){
                                            StorageReference avatarImagesRef = storage.getReferenceFromUrl(existing_user.getAvatar());
                                            final long ONE_MEGABYTE = 1024 * 1024;
                                            avatarImagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                                @Override
                                                public void onSuccess(byte[] bytes) {
                                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                    bitmap = Utility.CropBitmapCenterCircle(bitmap);
                                                    String base64String = Utility.BitMapToString(bitmap);

                                                    existing_user.setAvatarString(base64String);
                                                    db.updateUser(existing_user);

                                                    Bitmap avatar = Utility.StringToBitMap(existing_user.getAvatarString());
                                                    if(avatar != null) {
                                                        IV_Avatar.setImageBitmap( avatar );
                                                    } else {
                                                        IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                                    }

                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception exception) {
                                                    Toast.makeText(AppActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                        } else {
                                            IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
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

                    dbRef.child("userInfo").child(friendUID).child("online").addValueEventListener(new ValueEventListener() { // Следим кога приятелят е онлайн и кога излиза
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            existing_user.setOnline(dataSnapshot.getValue(Boolean.class));
                            db.updateUser(existing_user);
                            if(existing_user.isOnline()){
                                IV_Presence.setBackground(gdPresenceOnline);
                                app_LL_Friends.removeView(CL_Friend);
                                app_LL_Friends.addView(CL_Friend, 0); // най-отгоре
                                if(private_chat_friend.equals(existing_user.getUid())) {
                                    app_V_PrivChat_Presence.setBackground(gdPresenceOnline);
                                }
                            } else {
                                IV_Presence.setBackground(gdPresenceOffline);
                                app_LL_Friends.removeView(CL_Friend);
                                app_LL_Friends.addView(CL_Friend); // най-отдолу
                                if(private_chat_friend.equals(existing_user.getUid())) {
                                    app_V_PrivChat_Presence.setBackground(gdPresenceOffline);
                                }
                            }
                            int counter = online_friends_counter.getOnlineCount(existing_user.getUid(), existing_user.isOnline());
                            if(counter > 0) {
                                app_TV_OnlineFriends.setText(counter+"");
                                app_TV_OnlineFriends.setVisibility(View.VISIBLE);
                            } else {
                                app_TV_OnlineFriends.setVisibility(View.INVISIBLE);
                            };
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });

                } else { // потребителят не е бил зареждан преди ба това устройство -> ще се нуждае от създаване на юзър в датабазата и зареждане на всичко

                    final UserModel new_user = new UserModel();
                    new_user.setUid(friendUID);
                    db.addUser(new_user);

                    dbRef.child("userInfo").child(friendUID).child("nick").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            new_user.setNick(dataSnapshot.getValue(String.class));
                            db.updateUser(new_user);
                            TV_Nick.setText(new_user.getNick());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });

                    dbRef.child("userInfo").child(friendUID).child("avatarCh").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            new_user.setAvatarCh(dataSnapshot.getValue(Long.class));
                            db.updateUser(new_user);
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });

                    dbRef.child("userInfo").child(friendUID).child("avatar").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            new_user.setAvatar(dataSnapshot.getValue(String.class));
                            db.updateUser(new_user);

                            if (!new_user.getAvatar().equals("default")) {
                                StorageReference avatarImagesRef = storage.getReferenceFromUrl(new_user.getAvatar());
                                final long ONE_MEGABYTE = 1024 * 1024;
                                avatarImagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                    @Override
                                    public void onSuccess(byte[] bytes) {
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        bitmap = Utility.CropBitmapCenterCircle(bitmap);
                                        String base64String = Utility.BitMapToString(bitmap);

                                        new_user.setAvatarString(base64String);
                                        db.updateUser(new_user);

                                        Bitmap avatar = Utility.StringToBitMap(new_user.getAvatarString());
                                        if(avatar != null) {
                                            IV_Avatar.setImageBitmap(avatar);
                                        } else {
                                            IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                        }

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Toast.makeText(AppActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                            }

                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });

                    dbRef.child("userInfo").child(friendUID).child("online").addValueEventListener(new ValueEventListener() { // Следим кога приятелят е онлайн и кога излиза
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            new_user.setOnline(dataSnapshot.getValue(Boolean.class));
                            db.updateUser(new_user);
                            if(new_user.isOnline()){
                                IV_Presence.setBackground(gdPresenceOnline);
                                app_LL_Friends.removeView(CL_Friend);
                                app_LL_Friends.addView(CL_Friend, 0); // най-отгоре
                                if(private_chat_friend.equals(new_user.getUid())) {
                                    app_V_PrivChat_Presence.setBackground(gdPresenceOnline);
                                }
                            } else {
                                IV_Presence.setBackground(gdPresenceOffline);
                                app_LL_Friends.removeView(CL_Friend);
                                app_LL_Friends.addView(CL_Friend); // най-отдолу
                                if(private_chat_friend.equals(new_user.getUid())) {
                                    app_V_PrivChat_Presence.setBackground(gdPresenceOffline);
                                }
                            }
                            int counter = online_friends_counter.getOnlineCount(new_user.getUid(), new_user.isOnline());
                            if(counter > 0) {
                                app_TV_OnlineFriends.setText(counter+"");
                                app_TV_OnlineFriends.setVisibility(View.VISIBLE);
                            } else {
                                app_TV_OnlineFriends.setVisibility(View.INVISIBLE);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });

                }

                // Кликъри за отваряне на чат с потребителя от лайаута с приятел:
                CL_Friend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openPrivateChat(friendUID);
                    }
                });

            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                // Ако премахнем приятел трябва да го махнем от листа с приятели в локалната датабаза и след това да го зтрием от линейния лейаут с приятели
                for (int i = 0; i < app_LL_Friends.getChildCount(); i++) {
                    ConstraintLayoutFrUID child = (ConstraintLayoutFrUID) app_LL_Friends.getChildAt(i);
                    if (child.getFriendUID().equals(dataSnapshot.getKey())) {
                        db.removeFriend(user.getUid(), child.getFriendUID());
                        int counter = online_friends_counter.getOnlineCount(child.getFriendUID(), false);
                        if(counter > 0) {
                            app_TV_OnlineFriends.setText(counter+"");
                            app_TV_OnlineFriends.setVisibility(View.VISIBLE);
                        } else {
                            app_TV_OnlineFriends.setVisibility(View.INVISIBLE);
                        }
                        app_LL_Friends.removeView(child);
                        break; // няма да махаме чатовете, само премахнатия приятел
                    }
                }
            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }//end of loadFriendList()

    private void loadFriendRequests(){

        ChildEventListener friendRequestListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                RequestModel request = new RequestModel();
                request.setUserUID(user.getUid());
                request.setSenderUID(dataSnapshot.getKey());
                request.setMessage(dataSnapshot.child("message").getValue(String.class));
                request.setDate(dataSnapshot.child("date").getValue(Long.class));
                db.addRequest(request);
                addFriendRequestInTheLayout(request, true);
                ActivityManager.RunningAppProcessInfo myProcess = new ActivityManager.RunningAppProcessInfo();
                ActivityManager.getMyMemoryState(myProcess);
                if(myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) { // приложението в в бекграунд -> можем да изпратим известие
                    createNotificationOnFriendRequest(getApplicationContext(), request);
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        };

        ArrayList<RequestModel> allRequests = db.getAllRequests(user.getUid());
        if(allRequests.size() > 0){
            //показват се всички рикуести за приятел в лейаута 1 по 1 -> идват по низкодящ ред -> по-нови към по-стари -> т.е. зареждаме ги по ред; след това търсим от датата на последния получен рикуест и следим (най-новите заявки най-отгоре)
            final int requests_count = allRequests.size();
            for(int i=0; i<requests_count; i++){
                if(db.isFriend(user.getUid(), allRequests.get(i).getSenderUID())) { // приели сме поканата от друго устройство -> трябва да бъде изтрита, а не показана
                    db.removeRequest(user.getUid(), allRequests.get(i).getSenderUID());
                } else {
                    addFriendRequestInTheLayout(allRequests.get(i), false);
                }
                if(i==requests_count-1){ // след като сме заредили и последния рикуест е време да видим какво ново има и да следим за нови добавяния
                    dbRef.child("friendRequest").child(user.getUid()).orderByChild("date").startAt(allRequests.get(0).getDate()+1).addChildEventListener(friendRequestListener);
                }
            }
        } else {
            //теглим абсолютно всички рикуести и следим за нови
            dbRef.child("friendRequest").child(user.getUid()).orderByChild("date").startAt(0).addChildEventListener(friendRequestListener);
        }
        int notifications_count = app_LL_Requests.getChildCount();
        if(notifications_count > 0){
            app_TV_Requests.setText(notifications_count+"");
            app_TV_Requests.setVisibility(View.VISIBLE);
        } else {
            app_TV_Requests.setVisibility(View.INVISIBLE);
        }
    }// end of loadFriendRequests()
    private void addFriendRequestInTheLayout(final RequestModel request, boolean isNew){
        final ConstraintLayout CL_Request = new ConstraintLayout(getApplicationContext());
        CL_Request.setId(View.generateViewId());
        CL_Request.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        app_LL_Requests.addView(CL_Request);
        Utility.setMargins(CL_Request, layout_margin_start_end/2, layout_margin_start_end/4, layout_margin_start_end/2, 0);

        final ImageView IV_Avatar = new ImageView(getApplicationContext());
        IV_Avatar.setId(View.generateViewId());
        CL_Request.addView(IV_Avatar);
        IV_Avatar.getLayoutParams().width = avatar;
        IV_Avatar.getLayoutParams().height = avatar;
        IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
        IV_Avatar.setScaleType(ImageView.ScaleType.FIT_XY);

        final TextView TV_Nick = new TextView(getApplicationContext());
        TV_Nick.setId(View.generateViewId());
        TV_Nick.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ViewGroup.LayoutParams.WRAP_CONTENT));
        CL_Request.addView(TV_Nick);
        TV_Nick.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
        TV_Nick.setTextColor(ContextCompat.getColor(this, R.color.gui_text_white));

        final TextView TV_Date = new TextView(getApplicationContext());
        TV_Date.setId(View.generateViewId());
        CL_Request.addView(TV_Date);
        TV_Date.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_60);
        TV_Date.setTextColor(ContextCompat.getColor(this, R.color.text_lightgray1));
        SimpleDateFormat SDF;
        long calculateDate = (Calendar.getInstance().getTime().getTime() - request.getDate()) / 1000 / 60; //времето в минути
        if(calculateDate > 525948) { // поканата е по-стара от една година
            SDF = new SimpleDateFormat("dd MMM yy");
        } else {
            SDF = new SimpleDateFormat("dd MMM");
        }
        TV_Date.setText(SDF.format(new Date(request.getDate())));

        final TextView TV_Message = new TextView(getApplicationContext());
        TV_Message.setId(View.generateViewId());
        TV_Message.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_CONSTRAINT, ViewGroup.LayoutParams.WRAP_CONTENT));
        CL_Request.addView(TV_Message);
        TV_Message.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_50);
        TV_Message.setTextColor(ContextCompat.getColor(this, R.color.gui_text_whitish));
        TV_Message.setText(request.getMessage());

        final ImageView IV_Decline = new ImageView(getApplicationContext());
        IV_Decline.setId(View.generateViewId());
        CL_Request.addView(IV_Decline);
        IV_Decline.getLayoutParams().width = (int)(IV_Avatar.getLayoutParams().height*0.8);
        IV_Decline.getLayoutParams().height = IV_Decline.getLayoutParams().width;
        IV_Decline.setImageResource(R.drawable.icon_friend_decline);
        IV_Decline.setScaleType(ImageView.ScaleType.FIT_XY);
        IV_Decline.setBackground(gdButtons);

        final ImageView IV_Accept = new ImageView(getApplicationContext());
        IV_Accept.setId(View.generateViewId());
        CL_Request.addView(IV_Accept);
        IV_Accept.getLayoutParams().width = IV_Decline.getLayoutParams().width;
        IV_Accept.getLayoutParams().height = IV_Decline.getLayoutParams().width;
        IV_Accept.setImageResource(R.drawable.icon_friend_accept);
        IV_Accept.setScaleType(ImageView.ScaleType.FIT_XY);
        IV_Accept.setBackground(gdButtons);

        ConstraintSet cs = new ConstraintSet();
        cs.clone(CL_Request);
        cs.connect(IV_Avatar.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, layout_margin_start_end/2);
        cs.connect(IV_Avatar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layout_margin_start_end/2);
        cs.connect(IV_Decline.getId(), ConstraintSet.TOP, IV_Avatar.getId(), ConstraintSet.TOP );
        cs.connect(IV_Decline.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, layout_margin_start_end/2);
        cs.connect(IV_Decline.getId(), ConstraintSet.BOTTOM, IV_Avatar.getId(), ConstraintSet.BOTTOM);
        cs.connect(IV_Accept.getId(), ConstraintSet.TOP, IV_Decline.getId(), ConstraintSet.TOP);
        cs.connect(IV_Accept.getId(), ConstraintSet.END, IV_Decline.getId(), ConstraintSet.START, layout_margin_start_end/2);
        cs.connect(TV_Nick.getId(), ConstraintSet.START, IV_Avatar.getId(), ConstraintSet.END, layout_margin_start_end/2);
        cs.connect(TV_Nick.getId(), ConstraintSet.END, IV_Accept.getId(), ConstraintSet.START);
        cs.connect(TV_Nick.getId(), ConstraintSet.TOP, IV_Avatar.getId(), ConstraintSet.TOP);
        cs.connect(TV_Date.getId(), ConstraintSet.TOP, TV_Nick.getId(), ConstraintSet.BOTTOM);
        cs.connect(TV_Date.getId(), ConstraintSet.START, TV_Nick.getId(), ConstraintSet.START);
        cs.connect(TV_Message.getId(), ConstraintSet.TOP, TV_Date.getId(), ConstraintSet.BOTTOM);
        cs.connect(TV_Message.getId(), ConstraintSet.START, TV_Nick.getId(), ConstraintSet.START);
        cs.connect(TV_Message.getId(), ConstraintSet.END, IV_Accept.getId(), ConstraintSet.START, layout_margin_start_end/2);
        cs.connect(TV_Message.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,layout_margin_start_end/2);
        cs.applyTo(CL_Request);

        if(isNew) gdRequestBack.setColor(ContextCompat.getColor(this, R.color.gui_request_new));
        else gdRequestBack.setColor(ContextCompat.getColor(this, R.color.gui_request_old));
        CL_Request.setBackground(gdRequestBack);

        if(isNew) {
            app_LL_Requests.removeView(CL_Request);
            app_LL_Requests.addView(CL_Request, 0);
        }

        int notifications_count = app_LL_Requests.getChildCount();
        if(notifications_count > 0){
            app_TV_Requests.setText(notifications_count+"");
            app_TV_Requests.setVisibility(View.VISIBLE);
        } else {
            app_TV_Requests.setVisibility(View.INVISIBLE);
        }

        if(db.getUser(request.getSenderUID())!=null){ // теглили сме този потребител преди
            final UserModel existing_user = db.getUser(request.getSenderUID());

            // Показваме текущите данни (може и да са остарели)
            TV_Nick.setText(existing_user.getNick());
            if(!existing_user.getAvatar().equals("default")){
                try {
                    IV_Avatar.setImageBitmap(Utility.StringToBitMap(existing_user.getAvatarString()));
                } catch (Exception e) {
                    IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                }
            }

            // Сега да проверим дали няма някаква промяна и да я отразим
            dbRef.child("userInfo").child(existing_user.getUid()).child("nick").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!existing_user.getNick().equals(dataSnapshot.getValue(String.class))){
                        existing_user.setNick(dataSnapshot.getValue(String.class));
                        TV_Nick.setText(existing_user.getNick());
                        db.updateUser(existing_user);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

            dbRef.child("userInfo").child(existing_user.getUid()).child("avatarCh").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(existing_user.getAvatarCh() != dataSnapshot.getValue(Long.class)){
                        existing_user.setAvatarCh(dataSnapshot.getValue(Long.class));
                        db.updateUser(existing_user);
                        StorageReference avatarImagesRef = storage.getReferenceFromUrl(existing_user.getAvatar());
                        final long ONE_MEGABYTE = 1024 * 1024;
                        avatarImagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                bitmap = Utility.CropBitmapCenterCircle(bitmap);
                                String base64String = Utility.BitMapToString(bitmap);
                                existing_user.setAvatarString(base64String);

                                db.updateUser(existing_user);

                                Bitmap avatar = Utility.StringToBitMap(existing_user.getAvatarString());
                                if(avatar != null) {
                                    IV_Avatar.setImageBitmap(avatar);
                                } else {
                                    IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                }

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Toast.makeText(AppActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

        } else { // не сме теглили този потребител преди
            final UserModel new_user = new UserModel();
            new_user.setUid(request.getSenderUID());
            db.addUser(new_user);

            dbRef.child("userInfo").child(new_user.getUid()).child("nick").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    new_user.setNick(dataSnapshot.getValue(String.class));
                    db.updateUser(new_user);
                    TV_Nick.setText(new_user.getNick());
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

            dbRef.child("userInfo").child(new_user.getUid()).child("avatarCh").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    new_user.setAvatarCh(dataSnapshot.getValue(Long.class));
                    db.updateUser(new_user);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

            dbRef.child("userInfo").child(new_user.getUid()).child("avatar").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    new_user.setAvatar(dataSnapshot.getValue(String.class));
                    db.updateUser(new_user);
                    if(!new_user.getAvatar().equals("default")){
                        StorageReference avatarImagesRef = storage.getReferenceFromUrl(new_user.getAvatar());
                        final long ONE_MEGABYTE = 1024 * 1024;
                        avatarImagesRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                bitmap = Utility.CropBitmapCenterCircle(bitmap);
                                String base64String = Utility.BitMapToString(bitmap);
                                new_user.setAvatarString(base64String);

                                db.updateUser(new_user);

                                Bitmap avatar = Utility.StringToBitMap(new_user.getAvatarString());
                                if(avatar != null) {
                                    IV_Avatar.setImageBitmap(avatar);
                                } else {
                                    IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                                }

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Toast.makeText(AppActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }

        IV_Accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbRef.child("friendRequest").child(user.getUid()).child(request.getSenderUID()).child("accepted").setValue(true, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        if(databaseError == null) {
                            IV_Accept.setOnClickListener(null);
                            IV_Decline.setOnClickListener(null);
                            app_LL_Requests.removeView(CL_Request);
                            db.removeRequest(user.getUid(), request.getSenderUID());
                            if(app_LL_Requests.getChildCount() > 0) {
                                app_TV_Requests.setText(app_LL_Requests.getChildCount()+"");
                            } else {
                                app_TV_Requests.setVisibility(View.INVISIBLE);
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Ooops! Something went wrong :(", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        IV_Decline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(AppActivity.this);
                dialog.setContentView(R.layout.layout_cancel_friend_request);
                dialog.show();

                dialog.findViewById(R.id.cancel_CL_Main).getLayoutParams().width = width / 8 * 6;
                TextView cancel_TV_Title = dialog.findViewById(R.id.cancel_TV_Title);
                ConstraintLayout calcel_CL_ButtonsWrap = dialog.findViewById(R.id.calcel_CL_ButtonsWrap);
                final Button cancel_B_Block = dialog.findViewById(R.id.cancel_B_Block);
                Button cancel_B_Dismiss = dialog.findViewById(R.id.cancel_B_Dismiss);

                Utility.setMargins(cancel_TV_Title, h_div_40, h_div_60, h_div_40, 0);
                Utility.setMargins(calcel_CL_ButtonsWrap, h_div_40, h_div_40, h_div_40, h_div_80);
                cancel_TV_Title.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_36);
                cancel_B_Block.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                cancel_B_Dismiss.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);

                View.OnClickListener cancelRequest = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dbRef.child("friendRequest").child(user.getUid()).child(request.getSenderUID()).child("accepted").setValue(false, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                app_LL_Requests.removeView(CL_Request);
                                db.removeRequest(user.getUid(), request.getSenderUID());
                                if(app_LL_Requests.getChildCount() > 0) {
                                    app_TV_Requests.setText(app_LL_Requests.getChildCount());
                                } else {
                                    app_TV_Requests.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                        if(v.getId() == cancel_B_Block.getId()){
                            dbRef.child("blockList").child(user.getUid()).child(request.getSenderUID()).setValue(true);
                        }
                        dialog.dismiss();
                    }
                };
                cancel_B_Dismiss.setOnClickListener(cancelRequest);
                cancel_B_Block.setOnClickListener(cancelRequest);
            }
        });

    }//end of addFriendRequestInTheLayout()

    private void loadPrivateChats() {
        // Първо да заредим старите чатове
        ArrayList<String> friendsUIDs = db.getAllFriendsUIDs(user.getUid());
        if (friendsUIDs != null && friendsUIDs.size() > 0) {

            int count = friendsUIDs.size();
            ArrayList<PrivChatModel> messages = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                PrivChatModel message = db.getLastMessageFromUser(user.getUid(), friendsUIDs.get(i));
                if (message != null) messages.add(message);
            }

            // Сортираме по дата, за да изглежда така, както преди да напуснем приложението
            count = messages.size();
            for (int i = 0; i < count; i++) {
                for (int j = 1; j < (count - i); j++) {
                    if (messages.get(j-1).getDate() < messages.get(j).getDate()) {
                        Collections.swap(messages, j-1, j);
                    }
                }
            }

            for (int i = 0; i < count; i++) {

                String friendUID = "";
                if(messages.get(i).getSenderUID().equals(user.getUid())){
                    friendUID = messages.get(i).getTargetUID();
                } else {
                    friendUID = messages.get(i).getSenderUID();
                }

                PrivChatModel message = messages.get(i);

                final ConstraintLayoutPrivChat layoutPrivChat = new ConstraintLayoutPrivChat(getApplicationContext());
                layoutPrivChat.setFriendUID(friendUID);
                app_LL_Chats.addView(layoutPrivChat);
                Utility.setMargins(layoutPrivChat, layout_margin_start_end / 2, layout_margin_start_end / 4, layout_margin_start_end / 2, 0);
                layoutPrivChat.setBackground(gdChatsBackRead);

                ImageView IV_Avatar = layoutPrivChat.IV_Avatar;
                TextView TV_Nick = layoutPrivChat.TV_Nick, TV_Date = layoutPrivChat.TV_Date, TV_Message = layoutPrivChat.TV_Message;

                UserModel user_friend = db.getUser(friendUID);

                IV_Avatar.getLayoutParams().width = avatar;
                IV_Avatar.getLayoutParams().height = avatar;
                IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                IV_Avatar.setScaleType(ImageView.ScaleType.FIT_XY);
                if (!user_friend.getAvatar().equals("default")) {
                    IV_Avatar.setImageBitmap(Utility.StringToBitMap(user_friend.getAvatarString()));
                } else {
                    IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                }

                TV_Nick.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                TV_Nick.setTextColor(ContextCompat.getColor(this, R.color.gui_text_white));
                TV_Nick.setText(user_friend.getNick());

                TV_Date.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_60);
                TV_Date.setTextColor(ContextCompat.getColor(this, R.color.text_lightgray1));
                SimpleDateFormat SDF;
                long calculateDate = (Calendar.getInstance().getTime().getTime() - message.getDate()) / 1000 / 60; //времето в минути
                if (calculateDate > 525948) { // // съобщението е по-старо от 1 година
                    SDF = new SimpleDateFormat("dd MMM yy");
                } else if (calculateDate > 1440) { // съобщението е по-старо от 24 часа
                    SDF = new SimpleDateFormat("dd MMM HH:mm");
                } else {
                    SDF = new SimpleDateFormat("HH:mm");
                }
                TV_Date.setText(SDF.format(new Date(message.getDate())));

                TV_Message.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_50);
                TV_Message.setTextColor(ContextCompat.getColor(this, R.color.gui_text_whitish));
                TV_Message.setMaxLines(1);
                if (message.getType() == 1) {
                    TV_Message.setText(message.getMessage());
                } else if (message.getType() == 2) {
                    TV_Message.setText("Sent photo");
                }
                if (message.getSenderUID().equals(friendUID)) {
                    TV_Message.setText("\u21A9 " + TV_Message.getText().toString()); // лява
                } else {
                    TV_Message.setText("\u21AA " + TV_Message.getText().toString()); // дясна
                }

                ConstraintSet cs = new ConstraintSet();
                cs.clone(layoutPrivChat);
                cs.connect(IV_Avatar.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, layout_margin_start_end / 2);
                cs.connect(IV_Avatar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layout_margin_start_end / 2);
                cs.connect(IV_Avatar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, layout_margin_start_end / 2);
                cs.connect(TV_Nick.getId(), ConstraintSet.START, IV_Avatar.getId(), ConstraintSet.END, layout_margin_start_end / 2);
                cs.connect(TV_Nick.getId(), ConstraintSet.TOP, IV_Avatar.getId(), ConstraintSet.TOP);
                cs.connect(TV_Date.getId(), ConstraintSet.TOP, TV_Message.getId(), ConstraintSet.TOP);
                cs.connect(TV_Date.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, layout_margin_start_end / 2);
                cs.connect(TV_Message.getId(), ConstraintSet.TOP, TV_Nick.getId(), ConstraintSet.BOTTOM);
                cs.connect(TV_Message.getId(), ConstraintSet.START, TV_Nick.getId(), ConstraintSet.START);
                cs.connect(TV_Message.getId(), ConstraintSet.END, TV_Date.getId(), ConstraintSet.START, layout_margin_start_end / 2);
                cs.applyTo(layoutPrivChat);

                final String finalFriendUID = friendUID;
                layoutPrivChat.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        layoutPrivChat.setBackground(gdChatsBackRead);
                        openPrivateChat(finalFriendUID);
                    }
                });

                /*if (i == count - 1) {
                    listenForPrivChats();
                }*/
            }

        } /*else {
            listenForPrivChats();
        }*/
        app_IB_Chat_Menu.performClick();
        listenForPrivChats();
    }// end of loadPrivateChats()

    private void listenForPrivChats(){
        dbRef.child("privateChats").child(user.getUid()).orderByChild("date").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (dataSnapshot.exists()) {
                    final PrivChatModel message = new PrivChatModel(user.getUid(), dataSnapshot.child("sender").getValue(String.class), dataSnapshot.child("message").getValue(String.class), dataSnapshot.child("type").getValue(Integer.class), dataSnapshot.child("date").getValue(Long.class));
                    if(message.getType() == 2) {
                        Bitmap bitmap = Utility.StringToBitMap(message.getMessage());
                        if(bitmap==null) return; // стринга, който е изпратен не е изображение

                        // Оразмеряване преди запаметяваме -> ако някой изпрати по-голяма картинка в датабазата (а това може да стане без проблем, няма валидация на стринга) потребителят, на когото е изпратена, ще има проблеми с паметта на заявките или просто ще крашне с OOME заради големината на bitmapa, който се опитваме да възпроизведем
                        bitmap = Utility.resizeBitmapTo1024pxMax(bitmap);

                        // Преправяне на base64 stringa за да се запише в датабазата (преправяне само ако не е бил наред)
                        message.setMessage(Utility.BitMapToString(bitmap));
                    }
                    db.addMessage(message);
                    dbRef.child("privateChats").child(user.getUid()).child(dataSnapshot.getKey()).removeValue(); // не държим изтеглени съобщения в датабазата -> липса на синхронизация (все тая)

                    if(private_chat_friend.equals(message.getSenderUID())) showMessageInThePrivateChat(message.getSenderUID(), message, "bottom");

                    if (app_CL_Chats.getVisibility() == View.INVISIBLE && !private_chat_friend.equals(message.getSenderUID())) {
                        app_TV_ChatNew.setVisibility(View.VISIBLE);
                    }

                    int childrenCount = app_LL_Chats.getChildCount();
                    boolean isShown = false;
                    int index = 0;
                    for (int i = 0; i < childrenCount; i++) { // тук щеше да е много по-добре ако бях използвал HashMap...
                        if (((ConstraintLayoutPrivChat) app_LL_Chats.getChildAt(i)).getFriendUID().equals(message.getSenderUID())) {
                            isShown = true;
                            index = i;
                            break;
                        }
                    }
                    if (isShown) { // вече е показан -> нуждае се само от обновление
                        UserModel user_friend = db.getUser(message.getSenderUID());

                        ConstraintLayoutPrivChat layout = ((ConstraintLayoutPrivChat) app_LL_Chats.getChildAt(index));

                        app_LL_Chats.removeView(layout);
                        app_LL_Chats.addView(layout, 0); // отива най-горе

                        if (message.getType() == 1) {
                            layout.TV_Message.setText("\u21A9 " + message.getMessage());
                        } else if (message.getType() == 2) {
                            layout.TV_Message.setText("\u21A9 Sent photo");
                        }

                        if (!user_friend.getAvatar().equals("default")) {
                            layout.IV_Avatar.setImageBitmap(Utility.StringToBitMap(user_friend.getAvatarString()));
                        } else {
                            layout.IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                        }

                        SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");
                        layout.TV_Date.setText(SDF.format(new Date(message.getDate())));

                        if (!private_chat_friend.equals(message.getSenderUID())) {
                            layout.setBackground(gdChatsBackUnread);
                        }

                    } else { // не е показан -> трябва да го добавим

                        final ConstraintLayoutPrivChat layoutPrivChat = new ConstraintLayoutPrivChat(getApplicationContext());
                        layoutPrivChat.setFriendUID(message.getSenderUID());
                        app_LL_Chats.addView(layoutPrivChat, 0); // най-горе
                        Utility.setMargins(layoutPrivChat, layout_margin_start_end / 2, layout_margin_start_end / 4, layout_margin_start_end / 2, 0);
                        layoutPrivChat.setBackground(gdChatsBackRead);

                        ImageView IV_Avatar = layoutPrivChat.IV_Avatar;
                        TextView TV_Nick = layoutPrivChat.TV_Nick, TV_Date = layoutPrivChat.TV_Date, TV_Message = layoutPrivChat.TV_Message;

                        IV_Avatar.getLayoutParams().width = avatar;
                        IV_Avatar.getLayoutParams().height = avatar;
                        IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                        IV_Avatar.setScaleType(ImageView.ScaleType.FIT_XY);

                        TV_Nick.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
                        TV_Nick.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_white));

                        TV_Date.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_60);
                        TV_Date.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.text_lightgray1));
                        SimpleDateFormat SDF = new SimpleDateFormat("HH:mm");
                        TV_Date.setText(SDF.format(new Date(message.getDate())));

                        TV_Message.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_50);
                        TV_Message.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_whitish));
                        TV_Message.setMaxLines(1);
                        if (message.getType() == 1) {
                            TV_Message.setText("\u21A9 " + message.getMessage());
                        } else if (message.getType() == 2) {
                            TV_Message.setText("\u21A9 Sent photo");
                        }

                        if (db.getUser(message.getSenderUID()) == null) { // нямаме юзъра -> това би станало в много редки случаи и заради това просто оставяме ника и аватара празни
                            TV_Nick.setText("");
                            IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                        } else { // имаме юзъра
                            TV_Nick.setText(db.getUser(message.getSenderUID()).getNick());
                            if (!db.getUser(message.getSenderUID()).getAvatar().equals("default")) {
                                IV_Avatar.setImageBitmap(Utility.StringToBitMap(db.getUser(message.getSenderUID()).getAvatarString()));
                            } else {
                                IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
                            }
                        }

                        ConstraintSet cs = new ConstraintSet();
                        cs.clone(layoutPrivChat);
                        cs.connect(IV_Avatar.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, layout_margin_start_end / 2);
                        cs.connect(IV_Avatar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, layout_margin_start_end / 2);
                        cs.connect(IV_Avatar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, layout_margin_start_end / 2);
                        cs.connect(TV_Nick.getId(), ConstraintSet.START, IV_Avatar.getId(), ConstraintSet.END, layout_margin_start_end / 2);
                        cs.connect(TV_Nick.getId(), ConstraintSet.TOP, IV_Avatar.getId(), ConstraintSet.TOP);
                        cs.connect(TV_Date.getId(), ConstraintSet.TOP, TV_Message.getId(), ConstraintSet.TOP);
                        cs.connect(TV_Date.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, layout_margin_start_end / 2);
                        cs.connect(TV_Message.getId(), ConstraintSet.TOP, TV_Nick.getId(), ConstraintSet.BOTTOM);
                        cs.connect(TV_Message.getId(), ConstraintSet.START, TV_Nick.getId(), ConstraintSet.START);
                        cs.connect(TV_Message.getId(), ConstraintSet.END, TV_Date.getId(), ConstraintSet.START, layout_margin_start_end / 2);
                        cs.applyTo(layoutPrivChat);

                        if (!private_chat_friend.equals(message.getSenderUID())) {
                            layoutPrivChat.setBackground(gdChatsBackUnread);
                        }

                        layoutPrivChat.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                layoutPrivChat.setBackground(gdChatsBackRead);
                                openPrivateChat(message.getSenderUID());
                            }
                        });
                    }

                    if (app_LL_PrivChatBody.getChildCount() == 1) { // искаме да вземем датата най-старото съобщение в чата, в случай, че потребителят до сега не си е писал с този човек ще се прецака много жестоко цялата система и ще тегли едни и същи съобщения -> за това се подсигуряваме
                        private_chat_oldest_date_loaded = message.getDate();
                    }

                    ActivityManager.RunningAppProcessInfo myProcess = new ActivityManager.RunningAppProcessInfo();
                    ActivityManager.getMyMemoryState(myProcess);
                    if(myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) { // приложението в в бекграунд -> можем да изпратим известие
                        createNotification(getApplicationContext(), message);
                    }
                }
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }//end of listenForPrivChats()

    // ------------------------------------------------------------ Private Chat [ START ]
    private void openPrivateChat(final String friendUID) {
        private_chat_friend = friendUID;

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(db.getUserIdForNotificationId(friendUID));

        final UserModel user_there = db.getUser(friendUID);

        if (user_there.getAvatar().equals("default")) {
            app_IV_PrivChat_Avatar.setImageResource(R.drawable.clouder_avatar_default);
        } else {
            Bitmap avatar = Utility.StringToBitMap(user_there.getAvatarString());
            if (avatar != null) {
                app_IV_PrivChat_Avatar.setImageBitmap(avatar);
            } else {
                app_IV_PrivChat_Avatar.setImageResource(R.drawable.clouder_avatar_default);
            }
        }

        app_TV_PrivChat_Nick.setText(user_there.getNick());

        if (user_there.isOnline()) {
            app_V_PrivChat_Presence.setBackground(gdPresenceOnline);
        } else {
            app_V_PrivChat_Presence.setBackground(gdPresenceOffline);
        }

        app_ET_PrivChat_TextBox.setHint("Write to " + user_there.getNick());

        app_CL_PrivChat.setVisibility(View.VISIBLE);
        Animation expandOutVertical = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.expand_out_vertical);
        app_CL_PrivChat.startAnimation(expandOutVertical);

        final ArrayList<PrivChatModel> messages = db.getLast20MessagesFromOrToUser(user.getUid(), friendUID);
        if (messages != null && messages.size() > 0) {
            int lastIndex = messages.size() - 1;
            for (int i = lastIndex; i > -1; i--) {
                final PrivChatModel message = messages.get(i);
                showMessageInThePrivateChat(friendUID, message, "bottom");
            }//end of for loop
            private_chat_oldest_date_loaded = messages.get(messages.size()-1).getDate();
        }

        /*app_SV_PrivChatBody.postDelayed(new Runnable() {
            @Override
            public void run() {
                app_SV_PrivChatBody.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 1000);*/

    }// end of openPrivateChat()

    private void showMessageInThePrivateChat(String friendUID, final PrivChatModel message, String place) {
        if (message.getType() == 1) { // имаме просто текст

            ConstraintLayout_UID_Date CL_MessageWrap = new ConstraintLayout_UID_Date(getApplicationContext(), message.getSenderUID(), message.getDate());
            CL_MessageWrap.setId(View.generateViewId());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (message.getSenderUID().equals(friendUID)) {
                params.gravity = Gravity.LEFT;
            } else {
                params.gravity = Gravity.RIGHT;
            }
            if(place.equals("bottom")) app_LL_PrivChatBody.addView(CL_MessageWrap, params); // долу
            else  app_LL_PrivChatBody.addView(CL_MessageWrap, 0, params); // горе

            TextView TV_Message = new TextView(getApplicationContext());
            TV_Message.setId(View.generateViewId());
            CL_MessageWrap.addView(TV_Message);
            TV_Message.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_40);
            TV_Message.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_white));
            TV_Message.setText(message.getMessage());
            TV_Message.setShadowLayer(1,0,0, Color.BLACK);
            Typeface _font = Typeface.create("roboto_regular", Typeface.NORMAL);
            TV_Message.setTypeface(_font);

            boolean showDate = true;
            long calculateDate = (Calendar.getInstance().getTime().getTime() - message.getDate()) / 1000 / 60; //времето в минути
            if(app_LL_PrivChatBody.getChildCount() > 1) { // имаме съобщение преди текущото
                ConstraintLayout_UID_Date oldMessageLayout = ((ConstraintLayout_UID_Date)app_LL_PrivChatBody.getChildAt(app_LL_PrivChatBody.getChildCount()-2));
                if( oldMessageLayout.getUid().equals(message.getSenderUID()) && ( (Calendar.getInstance().getTime().getTime() - oldMessageLayout.getDate()) / 1000 / 60 ) == calculateDate ){
                    showDate = false;
                }
            }

            TextView TV_Date = null;
            if(showDate){
                TV_Date = new TextView(getApplicationContext());
                TV_Date.setId(View.generateViewId());
                CL_MessageWrap.addView(TV_Date);
                TV_Date.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_80);
                SimpleDateFormat SDF;

                if (calculateDate > 525948) { // // съобщението е по-старо от 1 година
                    SDF = new SimpleDateFormat("dd MMM yy");
                } else if (calculateDate > 1440) { // съобщението е по-старо от 24 часа
                    SDF = new SimpleDateFormat("dd MMM HH:mm");
                } else {
                    SDF = new SimpleDateFormat("HH:mm");
                }
                TV_Date.setText(SDF.format(message.getDate()));
                TV_Date.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_hint));
            }

            // Тези двата не могат да се оптимизират, понеже всеки текст е с различна големина и при каквото и да било действие, което променя изгледа (например показване на клавиатурата) те започват да изглеждат по един и същи начин (например бекграунда става малко кръгче, а текства излиза извън него -> падинга не спасява)
            GradientDrawable gdMessageSent = new GradientDrawable();
            gdMessageSent.setColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_gray_normal));
            gdMessageSent.setShape(GradientDrawable.RECTANGLE);
            gdMessageSent.setCornerRadius(h_div_40);

            GradientDrawable gdMessageReceived = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0xFF745EFF,0xFF4EB0E9});
            gdMessageReceived.setShape(GradientDrawable.RECTANGLE);
            gdMessageReceived.setCornerRadius(h_div_40);

            ConstraintSet cs = new ConstraintSet();
            cs.clone(CL_MessageWrap);
            if (message.getSenderUID().equals(friendUID)) {
                Utility.setMargins(CL_MessageWrap, h_div_160,0,h_div_14,h_div_160);
                TV_Message.setBackground(gdMessageReceived);
                if(showDate){
                    cs.connect(TV_Date.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                    cs.connect(TV_Date.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                    cs.connect(TV_Message.getId(), ConstraintSet.TOP, TV_Date.getId(), ConstraintSet.BOTTOM);
                }
                else {
                    cs.connect(TV_Message.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                }
                cs.connect(TV_Message.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                cs.connect(TV_Message.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                //cs.connect(TV_Message.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            } else {
                Utility.setMargins(CL_MessageWrap, h_div_14,0,h_div_160,h_div_160);
                TV_Message.setBackground(gdMessageSent);
                if(showDate){
                    cs.connect(TV_Date.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                    cs.connect(TV_Date.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                    cs.connect(TV_Message.getId(), ConstraintSet.TOP, TV_Date.getId(), ConstraintSet.BOTTOM);
                }
                else {
                    cs.connect(TV_Message.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                }
                cs.connect(TV_Message.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                cs.connect(TV_Message.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                //cs.connect(TV_Message.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            }
            cs.applyTo(CL_MessageWrap);

            if(showDate) TV_Date.setPadding(h_div_60,0,h_div_60,0);
            TV_Message.setPadding(h_div_60,h_div_120,h_div_60,h_div_120);

            Animation expandOutSlow = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.expand_out_slow);
            TV_Message.startAnimation(expandOutSlow);

        } else if (message.getType() == 2) { // имаме изображение

            ConstraintLayout_UID_Date CL_MessageWrap = new ConstraintLayout_UID_Date(getApplicationContext(), message.getSenderUID(), message.getDate());
            CL_MessageWrap.setId(View.generateViewId());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (message.getSenderUID().equals(friendUID)) {
                params.gravity = Gravity.LEFT;
            } else {
                params.gravity = Gravity.RIGHT;
            }
            if(place.equals("bottom")) app_LL_PrivChatBody.addView(CL_MessageWrap, params); // долу
            else  app_LL_PrivChatBody.addView(CL_MessageWrap, 0, params); // горе

            boolean showDate = true;
            long calculateDate = (Calendar.getInstance().getTime().getTime() - message.getDate()) / 1000 / 60; //времето в минути
            if(app_LL_PrivChatBody.getChildCount() > 1) { // имаме съобщение преди текущото
                ConstraintLayout_UID_Date oldMessageLayout = ((ConstraintLayout_UID_Date)app_LL_PrivChatBody.getChildAt(app_LL_PrivChatBody.getChildCount()-2));
                if( oldMessageLayout.getUid().equals(message.getSenderUID()) && ( (Calendar.getInstance().getTime().getTime() - oldMessageLayout.getDate()) / 1000 / 60 ) == calculateDate ){
                    showDate = false;
                }
            }

            TextView TV_Date = null;
            if(showDate){
                TV_Date = new TextView(getApplicationContext());
                TV_Date.setId(View.generateViewId());
                CL_MessageWrap.addView(TV_Date);
                TV_Date.setTextSize(TypedValue.COMPLEX_UNIT_PX, h_div_80);
                SimpleDateFormat SDF;

                if (calculateDate > 525948) { // // съобщението е по-старо от 1 година
                    SDF = new SimpleDateFormat("dd MMM yy");
                } else if (calculateDate > 1440) { // съобщението е по-старо от 24 часа
                    SDF = new SimpleDateFormat("dd MMM HH:mm");
                } else {
                    SDF = new SimpleDateFormat("HH:mm");
                }
                TV_Date.setText(SDF.format(message.getDate()));
                TV_Date.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gui_text_hint));
            }

            final ImageView IV_Message = new ImageView(getApplicationContext());
            IV_Message.setId(View.generateViewId());
            CL_MessageWrap.addView(IV_Message);

            IV_Message.getLayoutParams().width = width/2;
            final Bitmap bitmap = Utility.StringToBitMap(message.getMessage());

            // изчисляване на височината на ImageView-а в лейаута спрямо ширината на оригинала
            int bitmapWidth = bitmap.getWidth(), bitmapHeight = bitmap.getHeight();
            if(bitmapWidth > bitmapHeight) {
                double scale = (bitmapWidth*1.00)/(bitmapHeight*1.00);
                IV_Message.getLayoutParams().height = (int) ((width/2.00)/scale);
                //Toast.makeText(getApplicationContext(), "1. Width>Height |Width = " + IV_Message.getLayoutParams().width + " <-> Height = " + IV_Message.getLayoutParams().height + "|", Toast.LENGTH_LONG).show();
            } else if (bitmapHeight > bitmapWidth) {
                double scale = (bitmapHeight*1.00)/(bitmapWidth*1.00);
                IV_Message.getLayoutParams().height = (int) ((width/2.00)*scale);
                //Toast.makeText(getApplicationContext(), "2. Height>Width |Width = " + IV_Message.getLayoutParams().width + " <-> Height = " + IV_Message.getLayoutParams().height + "|", Toast.LENGTH_LONG).show();
            } else if (bitmapHeight == bitmapWidth) {
                IV_Message.getLayoutParams().height = IV_Message.getLayoutParams().width;
                //Toast.makeText(getApplicationContext(), "3. Width=Height |Width = " + IV_Message.getLayoutParams().width + " <-> Height = " + IV_Message.getLayoutParams().height + "|", Toast.LENGTH_LONG).show();
            }

            //final Bitmap bitmapMini = Bitmap.createScaledBitmap(bitmap, IV_Message.getLayoutParams().width, IV_Message.getLayoutParams().height, true);

            ConstraintSet cs = new ConstraintSet();
            cs.clone(CL_MessageWrap);
            if (message.getSenderUID().equals(friendUID)) {
                Utility.setMargins(CL_MessageWrap, h_div_160,0,h_div_14,h_div_160);
                if(showDate){
                    cs.connect(TV_Date.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                    cs.connect(TV_Date.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                    cs.connect(IV_Message.getId(), ConstraintSet.TOP, TV_Date.getId(), ConstraintSet.BOTTOM);
                }
                else {
                    cs.connect(IV_Message.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                }
                cs.connect(IV_Message.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                cs.connect(IV_Message.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                //cs.connect(TV_Message.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            } else {
                Utility.setMargins(CL_MessageWrap, h_div_14,0,h_div_160,h_div_160);
                if(showDate){
                    cs.connect(TV_Date.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                    cs.connect(TV_Date.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                    cs.connect(IV_Message.getId(), ConstraintSet.TOP, TV_Date.getId(), ConstraintSet.BOTTOM);
                }
                else {
                    cs.connect(IV_Message.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                }
                cs.connect(IV_Message.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                cs.connect(IV_Message.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                //cs.connect(TV_Message.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            }
            cs.applyTo(CL_MessageWrap);

            if(showDate) TV_Date.setPadding(h_div_60,0,h_div_60,0);

            //final BitmapDrawable ob = new BitmapDrawable(getResources(), bitmap);
            AppActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    IV_Message.setImageBitmap(bitmap);
                    //IV_Message.setImageBitmap(bitmapMini);
                    //IV_Message.setBackground(ob);
                }
            });

            Animation expandOutSlow = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.expand_out_slow);
            IV_Message.startAnimation(expandOutSlow);

            IV_Message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showBigImage(message.getSenderUID(), message.getTargetUID(), message.getDate());
                }
            });

        }

        app_SV_PrivChatBody.post(new Runnable() {
            @Override
            public void run() {
                app_SV_PrivChatBody.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }//end of showMessageInThePrivateChat()
    private void showBigImage(final String senderUID, final String targetUID, final long date) {

        String imageString = db.getImageFromMessage(senderUID, targetUID, date);
        if(imageString!=null && !imageString.isEmpty()){

            final Bitmap image = Utility.StringToBitMap(imageString);
            app_IV_BigImage.setImageBitmap(image);

            app_IB_BigImageSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(isStoragePermissionGranted()) {
                        if(StorageHelper.isExternalStorageWritable()){
                            String root = Environment.getExternalStorageDirectory().toString();
                            File myDir = new File(root + "/ClouderChat");
                            if (!myDir.exists()) {
                                myDir.mkdir();
                                //myDir.mkdirs();
                            }
                            String fname = date + "_" + senderUID + "_to_" + targetUID + ".jpg";
                            File file = new File(myDir.getAbsolutePath(), fname);
                            if (file.exists()) file.delete(); // не мисля, че ще се стигне до тук, но за всеки случай
                            try {
                                FileOutputStream out = new FileOutputStream(file);
                                image.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                out.flush();
                                out.close();
                                Toast.makeText(getApplicationContext(), "Image saved successfully", Toast.LENGTH_SHORT).show();
                                StorageHelper.addImageТоGallery(file, getApplicationContext());
                            } catch (Exception e) {
                                //Toast.makeText(getApplicationContext(), "Image couldn't be saved", Toast.LENGTH_SHORT).show();
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                //e.printStackTrace();
                            }
                        } else {
                            /*String root = Environment.getDataDirectory().toString();
                            File myDir = new File(root + "/ClouderChat");
                            if (!myDir.exists()) {
                                myDir.mkdir();
                                //myDir.mkdirs();
                            }
                            String fname = date + "_" + senderUID + "_to_" + targetUID + ".jpg";
                            File file = new File(myDir.getAbsolutePath(), fname);
                            if (file.exists()) file.delete(); // не мисля, че ще се стигне до тук, но за всеки случай
                            try {
                                FileOutputStream out = new FileOutputStream(file);
                                image.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                out.flush();
                                out.close();
                                Toast.makeText(getApplicationContext(), "Image saved successfully", Toast.LENGTH_SHORT).show();
                                StorageHelper.addImageТоGallery(file, getApplicationContext());
                            } catch (Exception e) {
                                //Toast.makeText(getApplicationContext(), "Image couldn't be saved", Toast.LENGTH_SHORT).show();
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                //e.printStackTrace();
                            }*/
                        }
                    }

                }
            });

            app_CL_BigImage.setVisibility(View.VISIBLE);

        }

    }//end of showBigImage()
    // ---

    private void loadOldMessagesInPrivChat(){
        ArrayList<PrivChatModel> messages = db.getLast20MessagesFromOrToUserOlder(user.getUid(), private_chat_friend, private_chat_oldest_date_loaded);
        if (messages != null && messages.size() > 0) {
            for (int i = 0; i < messages.size(); i++) {
                PrivChatModel message = messages.get(i);
                showMessageInThePrivateChat(private_chat_friend, message, "top");
            }//end of for loop
            private_chat_oldest_date_loaded = messages.get(messages.size()-1).getDate();
        }
        app_SV_PrivChatBody.post(new Runnable() {
            @Override
            public void run() {
                app_SV_PrivChatBody.scrollTo(0, 1); // искаме винаги да има поне 1 пиксел от топа, за да можем да отчетем следващия скрол нагоре на потребителя (не че не може да скролне надолу и после пак нагоре, но ще е неудобно за самия потребител)
            }
        });
    }
    // ------------------------------------------------------------ Private Chat [  END  ]

    // -------------------- Notifications
    public void createNotification(Context context, PrivChatModel message) {
        String CHANNEL_ID = "some_channel_id";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
        if (db.getUser(message.getSenderUID()).getAvatar().equals("default")) {
            notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.clouder_avatar_default));
        } else {
            if (Utility.StringToBitMap(db.getUser(message.getSenderUID()).getAvatarString()) != null) {
                notificationBuilder.setLargeIcon(Utility.StringToBitMap(db.getUser(message.getSenderUID()).getAvatarString()));
            } else {
                notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.clouder_avatar_default));
            }
        }
        notificationBuilder.setContentTitle(db.getUser(message.getSenderUID()).getNick());
        if (message.getType() == 1) {
            notificationBuilder.setContentText(message.getMessage());
        } else if (message.getType() == 2) {
            notificationBuilder.setContentText("Sent photo.");
        }
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        //notificationBuilder.setContentIntent(pendingIntent);
        //notificationBuilder.setColor(0xFF5194FF);
        //notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT);
        notificationBuilder.setVibrate(new long[]{0, 500, 250, 500});
        notificationBuilder.setLights(Color.CYAN, 500, 250);

        Intent intent = new Intent(context, AppActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("action_code", "new_message");
        intent.putExtra("chat_user_key", message.getSenderUID());
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(contentIntent);
        //From the doc for PendingIntent.FLAG_UPDATE_CURRENT:
        //If the described PendingIntent already exists, then keep it but replace its extra data with what is in this new Intent. This can be used if you are creating intents where only the extras change, and don't care that any entities that received your previous PendingIntent will be able to launch it with your new extras even if they are not explicitly given to it.

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {       // For Oreo and greater than it, we required Notification Channel.
            CharSequence name = "Some Channel";                   // The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance); //Create Notification Channel
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(db.getUserIdForNotificationId(message.getSenderUID()), notificationBuilder.build()); // за да показваме само последното съобщение на конкретния потребител
    }

    @Override
    public void onNewIntent(Intent intent){
        Bundle extras = intent.getExtras();
        if(intent.hasExtra("action_code")){

            String action_code = extras.getString("action_code");

            if(action_code.equals("new_message")) {
                self_IV_Back.performClick();
                search_IB_Back.performClick();
                app_IV_PrivChat_X.performClick();
                app_IB_Chat_Menu.performClick();
                openPrivateChat(extras.getString("chat_user_key"));
            }
            else if(action_code.equals("friend_request")) {
                self_IV_Back.performClick();
                search_IB_Back.performClick();
                app_IV_PrivChat_X.performClick();
                app_IB_Requests_Menu.performClick();
            }

        }
    }

    public void createNotificationOnFriendRequest(Context context, RequestModel request) {
        String CHANNEL_ID = "some_channel_id";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher_foreground);
        notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.clouder_avatar_default));
        notificationBuilder.setContentTitle("New friend request");
        notificationBuilder.setContentText("You have a new friend request. (" +app_LL_Requests.getChildCount() + ")");
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationBuilder.setVibrate(new long[]{0, 500, 250, 500});
        notificationBuilder.setLights(Color.CYAN, 500, 250);

        Intent intent = new Intent(context, AppActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("action_code", "friend_request");
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(contentIntent);
        //From the doc for PendingIntent.FLAG_UPDATE_CURRENT:
        //If the described PendingIntent already exists, then keep it but replace its extra data with what is in this new Intent. This can be used if you are creating intents where only the extras change, and don't care that any entities that received your previous PendingIntent will be able to launch it with your new extras even if they are not explicitly given to it.

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {       // For Oreo and greater than it, we required Notification Channel.
            CharSequence name = "Some Channel";                   // The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance); //Create Notification Channel
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(467159, notificationBuilder.build());
    }

    // Bonus
    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                //Log.v(TAG,"Permission is granted");
                return true;
            } else {
                //Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            //Log.v(TAG,"Permission is granted");
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            //Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

}//end of AppActivity{}