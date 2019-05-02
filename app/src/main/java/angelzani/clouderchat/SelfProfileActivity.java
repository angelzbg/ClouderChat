package angelzani.clouderchat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import angelzani.clouderchat.DB.DatabaseHelper;
import angelzani.clouderchat.DB.UserModel;
import angelzani.clouderchat.Utility.Utility;

public class SelfProfileActivity extends AppCompatActivity {

    // UI
    // Display Metrics
    private int width, height;

    ConstraintLayout self_CL_Main;

    ConstraintLayout self_CL_Header;
    ImageView self_IV_Back;
    TextView self_TV_Me;

    ConstraintLayout self_CL_inLL;

    ImageView self_IV_CircleBack;
    ImageView self_IV_Avatar;
    ImageView self_IV_Accept;
    ImageView self_IV_Decline;
    TextView self_TV_Nickname;

    ConstraintLayout self_CL_WrapPointsAge;
    LinearLayout self_LL_MiddleWrap;
    ImageView self_IV_Droplets;
    TextView self_TV_DropletsCount;
    TextView self_TV_Droplets;
    ImageView self_IV_Age;
    TextView self_TV_AgeCount;
    TextView self_TV_Age;

    LinearLayout self_LL_Settings_Wrap;
    ConstraintLayout self_CL_Setting_Switch;
    Switch self_Switch;
    ConstraintLayout self_CL_Setting_Logout;
    TextView self_TV_Logout;
    ImageView self_IV_Logout;


    private UserModel local_user;
    private DatabaseHelper db;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_profile);

        /*if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }*/

        //Design
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

        // ----- Resizing
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

        //Header
        self_CL_Header.getLayoutParams().height = (int) (height/12.5);
        self_IV_Back.getLayoutParams().height = height/16;
        self_IV_Back.getLayoutParams().width = height/16;
        self_TV_Me.setTextSize(TypedValue.COMPLEX_UNIT_PX, height/32);

        ConstraintSet cs = new ConstraintSet();
        cs.clone(self_CL_Header);
        cs.connect(R.id.self_IV_Back, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, (int) (height/28.5714));
        cs.connect(R.id.self_TV_Me, ConstraintSet.START, R.id.self_IV_Back, ConstraintSet.END, (int) (height/28.5714));
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
        self_TV_Nickname.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int)(height/26.666));

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
        cs.connect(R.id.self_LL_Settings_Wrap, ConstraintSet.TOP, R.id.self_CL_WrapPointsAge, ConstraintSet.BOTTOM, height/20);
        cs.applyTo(self_CL_Main);

        self_CL_Setting_Switch.getLayoutParams().height = height/16;
        self_Switch.setTextSize(TypedValue.COMPLEX_UNIT_PX, height/40);
        self_CL_Setting_Logout.getLayoutParams().height = height/16;
        self_TV_Logout.setTextSize(TypedValue.COMPLEX_UNIT_PX, height/40);
        self_IV_Logout.getLayoutParams().width = (int) (height/26.666);
        self_IV_Logout.getLayoutParams().height = (int) (height/26.666);

        // Resizing -----



        db = new DatabaseHelper(getApplicationContext());
        user = FirebaseAuth.getInstance().getCurrentUser();
        local_user = db.getUser(user.getUid());

        LoadProfileInfo();

        //OnClickListeners
        self_IV_Back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                finish();
            }
        });

        self_CL_Setting_Logout.setOnClickListener(new View.OnClickListener() { // логаут и излизане от приложението -> пращане в хоум скрийн
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                finishAffinity();
                System.exit(0);
            }
        });

        self_IV_Avatar.setOnClickListener(pickPhoto);
        self_IV_Decline.setOnClickListener(declinePhoto);
        self_IV_Accept.setOnClickListener(acceptPhoto);

    }//end of onCreate()



    private void LoadProfileInfo() {
        self_TV_Nickname.setText(local_user.getNick());
        if(!local_user.getAvatar().equals("default")) {
            self_IV_Avatar.setImageBitmap(Utility.StringToBitMap(local_user.getAvatarString()));
        }
        else {
            self_IV_Avatar.setImageResource(R.drawable.clouder_avatar_default);
        }
        self_TV_DropletsCount.setText(local_user.getPoints()+"");

        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        long calculateDate = (currentDate.getTime() - local_user.getDateCr()) / 1000 / 60; //времето в минути

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

    //OnClickListeners
    private static final int PICK_PHOTO_FOR_AVATAR = 0;
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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO_FOR_AVATAR && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                Toast.makeText(SelfProfileActivity.this, "data = null", Toast.LENGTH_LONG).show();
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
        else { //Cancelled
        }
    }

    View.OnClickListener declinePhoto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            self_IV_Avatar.setImageBitmap(Utility.StringToBitMap(local_user.getAvatarString()));
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
                        Toast.makeText(SelfProfileActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    };

    //Utility
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        Toast.makeText(getApplicationContext(), "No Internet Connection!", Toast.LENGTH_LONG).show();
        return isConnected;
    }
}
