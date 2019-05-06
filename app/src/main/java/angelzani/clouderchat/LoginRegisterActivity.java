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
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
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

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.RunnableScheduledFuture;

import angelzani.clouderchat.UI.LockableScrollView;
import angelzani.clouderchat.UI.OnSwipeTouchListener;
import angelzani.clouderchat.Utility.Utility;

public class LoginRegisterActivity extends AppCompatActivity {

    //Display Metrics
    private int width, height;

    //Ref
    private LockableScrollView logreg_SV_Main;
    private LinearLayout logreg_LL_Main;

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;

    //Loading animation
    private AnimationDrawable animationLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_register);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        // ---------------------------------------- RESIZING THE DESIGN [ START ] ----------------------------------------
        if(width != 480 || height != 800) // 800x480 default design -> трябва да направя константни интегери за всички повтарящи се сметки (height/32, height/10.666 и т.н.), за да зарежда по-бързо, също така може да направя пойнтъри към визуалните обекти, за да не ги викам всеки път с findView
        {
            //First main layouts
            findViewById(R.id.login_IV_Background).getLayoutParams().height = (int) (height/1.176);
            if(width > findViewById(R.id.login_IV_Background).getLayoutParams().height/1.416) {
                findViewById(R.id.login_IV_Background).getLayoutParams().width = (int) (findViewById(R.id.login_IV_Background).getLayoutParams().height/1.416);
            }
            else {
                findViewById(R.id.login_IV_Background).getLayoutParams().width = width;
            }
            findViewById(R.id.register_IV_Background).getLayoutParams().height = findViewById(R.id.login_IV_Background).getLayoutParams().height;
            findViewById(R.id.register_IV_Background).getLayoutParams().width = findViewById(R.id.login_IV_Background).getLayoutParams().width;

            //Login layout Elements
            findViewById(R.id.login_IV_Logo).getLayoutParams().height = (int) (height/6.666);
            findViewById(R.id.login_IV_Logo).getLayoutParams().width = findViewById(R.id.login_IV_Logo).getLayoutParams().height*2;
            findViewById(R.id.login_ET_Email).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.login_IV_Email).getLayoutParams().height = height/32;
            findViewById(R.id.login_IV_Email).getLayoutParams().width = height/32;
            findViewById(R.id.login_ET_Password).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.login_IV_Password).getLayoutParams().height = height/32;
            findViewById(R.id.login_IV_Password).getLayoutParams().width = height/32;
            findViewById(R.id.login_B_Login).getLayoutParams().height = (int) (height/16.666);
            findViewById(R.id.login_B_Login).getLayoutParams().width = findViewById(R.id.login_B_Login).getLayoutParams().height*5;
            findViewById(R.id.login_IV_Arrow).getLayoutParams().height = (int) (height/28.571);
            findViewById(R.id.login_IV_Arrow).getLayoutParams().width = findViewById(R.id.login_IV_Arrow).getLayoutParams().height;

            findViewById(R.id.login_ET_Email).setPadding(0,0,height/32,0);
            findViewById(R.id.login_ET_Password).setPadding(0,0,height/32,0);

            ConstraintSet cs = new ConstraintSet();
            cs.clone((ConstraintLayout) findViewById(R.id.login_CL));
            cs.connect(R.id.login_IV_Logo, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, height/16);
            cs.connect(R.id.login_B_Login, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, (int) (height/22.857));
            cs.connect(R.id.login_B_Login, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, (int) (height/4.705));
            cs.connect(R.id.login_IV_Arrow, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, (int) (height/42.105));
            cs.connect(R.id.login_IV_Arrow, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, (int) (height/34.782));
            cs.connect(R.id.login_CL_WrapInput, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, (int) (height/22.857));
            cs.connect(R.id.login_CL_WrapInput, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, (int) (height/22.857));
            cs.applyTo((ConstraintLayout) findViewById(R.id.login_CL));

            cs.clone((ConstraintLayout) findViewById(R.id.login_CL_WrapInput));
            cs.connect(R.id.login_TV_Forgot, ConstraintSet.TOP, R.id.login_View_Split2, ConstraintSet.BOTTOM, height/32);
            //cs.connect(R.id.login_TV_Forgot, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            cs.applyTo((ConstraintLayout) findViewById(R.id.login_CL_WrapInput));

            ((TextView) findViewById(R.id.login_ET_Email)).setTextSize(TypedValue.COMPLEX_UNIT_PX, height/32);
            ((TextView) findViewById(R.id.login_ET_Password)).setTextSize(TypedValue.COMPLEX_UNIT_PX, height/32);
            ((TextView) findViewById(R.id.login_TV_Forgot)).setTextSize(TypedValue.COMPLEX_UNIT_PX, height/40);
            ((Button) findViewById(R.id.login_B_Login)).setTextSize(TypedValue.COMPLEX_UNIT_PX, height/32);

            //Register Layout Elements
            findViewById(R.id.register_IV_Avatar).getLayoutParams().height = height/8;
            findViewById(R.id.register_IV_Avatar).getLayoutParams().width = height/8;
            findViewById(R.id.register_IB_X).getLayoutParams().width = findViewById(R.id.register_IV_Avatar).getLayoutParams().width/4;
            findViewById(R.id.register_IB_X).getLayoutParams().height = findViewById(R.id.register_IB_X).getLayoutParams().width;
            findViewById(R.id.register_ET_Username).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.register_IV_Username).getLayoutParams().height = height/32;
            findViewById(R.id.register_IV_Username).getLayoutParams().width = height/32;
            findViewById(R.id.register_ET_Email).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.register_IV_Email).getLayoutParams().height = height/32;
            findViewById(R.id.register_IV_Email).getLayoutParams().width = height/32;
            findViewById(R.id.register_ET_Password).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.register_IV_Password).getLayoutParams().height = height/32;
            findViewById(R.id.register_IV_Password).getLayoutParams().width = height/32;
            findViewById(R.id.register_ET_Confirm).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.register_IV_Confirm).getLayoutParams().height = height/32;
            findViewById(R.id.register_IV_Confirm).getLayoutParams().width = height/32;
            findViewById(R.id.register_ET_Email).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.register_IV_Email).getLayoutParams().height = height/32;
            findViewById(R.id.register_IV_Email).getLayoutParams().width = height/32;
            findViewById(R.id.register_B_Signup).getLayoutParams().height = (int) (height/16.666);
            findViewById(R.id.register_B_Signup).getLayoutParams().width = findViewById(R.id.register_B_Signup).getLayoutParams().height*5;
            findViewById(R.id.register_IV_Arrow).getLayoutParams().height = (int) (height/28.571);
            findViewById(R.id.register_IV_Arrow).getLayoutParams().width = findViewById(R.id.register_IV_Arrow).getLayoutParams().height;

            findViewById(R.id.register_ET_Username).setPadding(0,0,height/32,0);
            findViewById(R.id.register_ET_Email).setPadding(0,0,height/32,0);
            findViewById(R.id.register_ET_Password).setPadding(0,0,height/32,0);
            findViewById(R.id.register_ET_Confirm).setPadding(0,0,height/32,0);

            cs.clone((ConstraintLayout) findViewById(R.id.register_CL));
            cs.connect(R.id.register_IV_Avatar, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) (height/5.161));
            cs.connect(R.id.register_IV_Arrow, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, (int) (height/42.105));
            cs.connect(R.id.register_IV_Arrow, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, (int) (height/34.782));
            cs.connect(R.id.register_CL_WrapInput, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, (int) (height/22.857));
            cs.connect(R.id.register_CL_WrapInput, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, (int) (height/22.857));
            cs.connect(R.id.register_B_Signup, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, (int) (height/22.857));
            cs.connect(R.id.register_B_Signup, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, (int) (height/22.857));
            cs.applyTo((ConstraintLayout) findViewById(R.id.register_CL));

            ((TextView) findViewById(R.id.register_ET_Username)).setTextSize(TypedValue.COMPLEX_UNIT_PX, height/32);
            ((TextView) findViewById(R.id.register_ET_Email)).setTextSize(TypedValue.COMPLEX_UNIT_PX, height/32);
            ((TextView) findViewById(R.id.register_ET_Password)).setTextSize(TypedValue.COMPLEX_UNIT_PX, height/32);
            ((TextView) findViewById(R.id.register_ET_Confirm)).setTextSize(TypedValue.COMPLEX_UNIT_PX, height/32);
            ((Button) findViewById(R.id.register_B_Signup)).setTextSize(TypedValue.COMPLEX_UNIT_PX, height/32);

            //Auth Layout
            //findViewById(R.id.auth_CL).getLayoutParams().height = height/2;
            findViewById(R.id.auth_IV_Google1).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.auth_IV_Google1).getLayoutParams().width = (int) (height/10.666);
            findViewById(R.id.auth_IV_Facebook1).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.auth_IV_Facebook1).getLayoutParams().width = (int) (height/10.666);
            findViewById(R.id.auth_IV_Twitter1).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.auth_IV_Twitter1).getLayoutParams().width = (int) (height/10.666);
            findViewById(R.id.auth_IV_Google2).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.auth_IV_Google2).getLayoutParams().width = (int) (height/10.666);
            findViewById(R.id.auth_IV_Facebook2).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.auth_IV_Facebook2).getLayoutParams().width = (int) (height/10.666);
            findViewById(R.id.auth_IV_Twitter2).getLayoutParams().height = (int) (height/10.666);
            findViewById(R.id.auth_IV_Twitter2).getLayoutParams().width = (int) (height/10.666);

            cs.clone((ConstraintLayout) findViewById(R.id.auth_CL));
            cs.connect(R.id.auth_IV_Facebook1, ConstraintSet.START, R.id.auth_IV_Google1, ConstraintSet.END, height/32);
            cs.connect(R.id.auth_IV_Facebook1, ConstraintSet.TOP, R.id.auth_IV_Google1, ConstraintSet.TOP, (int) (height/17.777));
            cs.connect(R.id.auth_IV_Twitter1, ConstraintSet.START, R.id.auth_IV_Facebook1, ConstraintSet.END, height/32);
            cs.connect(R.id.auth_IV_Twitter1, ConstraintSet.TOP, R.id.auth_IV_Facebook1, ConstraintSet.TOP, (int) (height/17.777));
            cs.connect(R.id.auth_IV_Twitter1, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, height/8);
            cs.connect(R.id.auth_IV_Twitter1, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, (int) (height/17.777));

            cs.connect(R.id.auth_IV_Facebook2, ConstraintSet.END, R.id.auth_IV_Twitter2, ConstraintSet.START, height/32);
            cs.connect(R.id.auth_IV_Facebook2, ConstraintSet.BOTTOM, R.id.auth_IV_Twitter2, ConstraintSet.BOTTOM, (int) (height/17.777));
            cs.connect(R.id.auth_IV_Google2, ConstraintSet.END, R.id.auth_IV_Facebook2, ConstraintSet.START, height/32);
            cs.connect(R.id.auth_IV_Google2, ConstraintSet.BOTTOM, R.id.auth_IV_Facebook2, ConstraintSet.BOTTOM, (int) (height/17.777));
            cs.applyTo((ConstraintLayout) findViewById(R.id.auth_CL));

            //Loading Layout
            findViewById(R.id.logreg_IV_Loading).getLayoutParams().width = width;
            findViewById(R.id.logreg_IV_Loading).getLayoutParams().height = width;

            //Info Layout
            findViewById(R.id.logreg_IB_InfoClose).getLayoutParams().width = height/20;
            findViewById(R.id.logreg_IB_InfoClose).getLayoutParams().height = height/20;
            ((TextView)findViewById(R.id.logreg_TV_Info)).setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) (height/44.44));

            findViewById(R.id.logreg_CL_Info).setPadding(height/40, height/40, height/40, height/40);
            findViewById(R.id.logreg_TV_Info).setPadding(height/20, height/20, height/20, height/20);

        }
        // ---------------------------------------- RESIZING THE DESIGN [  END  ] ----------------------------------------

        // Ref design ----------------------------------------
        logreg_SV_Main = findViewById(R.id.logreg_SV_Main);
        logreg_LL_Main = findViewById(R.id.logreg_LL_Main);

        // On Touch Listeners design ----------------------------------------
        logreg_LL_Main.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeTop() {
                //Toast.makeText(MyActivity.this, "top", Toast.LENGTH_SHORT).show();
                scrollDown();
            }
            public void onSwipeRight() {
                //Toast.makeText(MyActivity.this, "right", Toast.LENGTH_SHORT).show();
            }
            public void onSwipeLeft() {
                //Toast.makeText(MyActivity.this, "left", Toast.LENGTH_SHORT).show();
            }
            public void onSwipeBottom() {
                //Toast.makeText(MyActivity.this, "bottom", Toast.LENGTH_SHORT).show();
                scrollUp();
            }

        });

        // On Click Listeners design ----------------------------------------
        findViewById(R.id.login_IV_Arrow).setOnClickListener(scrollDown);
        findViewById(R.id.register_IV_Arrow).setOnClickListener(scrollDown);

        // Little Fix on the design
        findViewById(R.id.login_CL_WrapInput).setVisibility(View.VISIBLE);
        findViewById(R.id.register_CL_WrapInput).setVisibility(View.INVISIBLE);


        // *********************************** APPLICATION ***********************************

        //On Click Listeners
        findViewById(R.id.login_B_Login).setOnClickListener(loginClick);
        findViewById(R.id.register_B_Signup).setOnClickListener(registerClick);

        findViewById(R.id.register_IV_Avatar).setOnClickListener(goForAvatar);

        findViewById(R.id.register_IB_X).setOnClickListener(removeAvatar);

        //Animations
        animationLoading = (AnimationDrawable) findViewById(R.id.logreg_IV_Loading).getBackground();

        //Info Layout
        GradientDrawable gdInfoBoxBack = new GradientDrawable();
        gdInfoBoxBack.setColor(ContextCompat.getColor(this, R.color.white));
        gdInfoBoxBack.setShape(GradientDrawable.RECTANGLE);
        gdInfoBoxBack.setCornerRadius(height/10);
        gdInfoBoxBack.setStroke(height/800, ContextCompat.getColor(this, R.color.gui_text_whitish));
        gdInfoBoxBack.setAlpha(229); // 90%
        findViewById(R.id.logreg_CL_InfoWrap).setBackground(gdInfoBoxBack);
        View.OnClickListener closeInfoClicker = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideInfo();
            }
        };
        findViewById(R.id.logreg_IB_InfoClose).setOnClickListener(closeInfoClicker);
        findViewById(R.id.logreg_CL_InfoWrap).setOnClickListener(closeInfoClicker);

        //Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        dbRef = database.getReference();

    } // End of OnCreate()

    private static final int PICK_PHOTO_FOR_AVATAR = 0;
    private Bitmap avatarBitmap;
    private boolean uploadAvatar = false;
    private View.OnClickListener goForAvatar = new View.OnClickListener() {
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
                Toast.makeText(LoginRegisterActivity.this, "data = null", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                //Now you can do whatever you want with your inpustream, save it as file, upload to a server, decode a bitmap...
                avatarBitmap = BitmapFactory.decodeStream(inputStream);
                avatarBitmap = Utility.CropBitmapCenterCircle(avatarBitmap);
                ((ImageView)findViewById(R.id.register_IV_Avatar)).setImageBitmap(avatarBitmap);
                findViewById(R.id.register_IB_X).setVisibility(View.VISIBLE);
                uploadAvatar = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else { //Cancelled
        }
    }
    private View.OnClickListener removeAvatar = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            findViewById(R.id.register_IB_X).setVisibility(View.INVISIBLE);
            uploadAvatar = false;
            ((ImageView)findViewById(R.id.register_IV_Avatar)).setImageResource(R.drawable.icon_upload_circle);
        }
    };

    private View.OnClickListener loginClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showLoading();
            login(((EditText)findViewById(R.id.login_ET_Email)).getText().toString().toLowerCase().trim(), ((EditText)findViewById(R.id.login_ET_Password)).getText().toString().trim());
        }
    };
    private void login(final String email, final String password) {
        if(email.isEmpty() || password.isEmpty()) {
            hideLoading();
            showInfo("All fields must be filled.");
            //Toast.makeText(LoginRegisterActivity.this, "All fields must be filled.", Toast.LENGTH_LONG).show();
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            hideLoading();
            showInfo("Email address is invalid.");
            //Toast.makeText(LoginRegisterActivity.this, "Email address is invalid.", Toast.LENGTH_LONG).show();
        }
        else {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    hideLoading();
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        //FirebaseUser user = mAuth.getCurrentUser();
                        //Toast.makeText(LoginRegisterActivity.this, "Login success", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent( LoginRegisterActivity.this, AppActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        showInfo("Login failed: " + task.getException().getMessage());
                        //Toast.makeText(LoginRegisterActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
    private View.OnClickListener registerClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showLoading();
            disableRegisterButton();
            register(((EditText) findViewById(R.id.register_ET_Username)).getText().toString().trim(),
                    ((EditText) findViewById(R.id.register_ET_Email)).getText().toString().toLowerCase().trim(),
                    ((EditText) findViewById(R.id.register_ET_Password)).getText().toString().trim(),
                    ((EditText) findViewById(R.id.register_ET_Confirm)).getText().toString().trim());
        }
    };
    private void register(final String username, final String email, final String password, final String confirm) {
        if(username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            hideLoading();
            enableRegisterButton();
            showInfo("All fields must be filled.");
            //Toast.makeText(LoginRegisterActivity.this, "All fields must be filled.", Toast.LENGTH_LONG).show();
        }
        else if(username.length() < 2 || username.length() > 16) {
            hideLoading();
            enableRegisterButton();
            showInfo("Username must contain from 2 to 16 characters.");
            //Toast.makeText(LoginRegisterActivity.this, "Username must contain from 2 to 16 characters.", Toast.LENGTH_LONG).show();
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            hideLoading();
            enableRegisterButton();
            showInfo("Email address is invalid.");
            //Toast.makeText(LoginRegisterActivity.this, "Email address is invalid.", Toast.LENGTH_LONG).show();
        }
        else if(password.length() < 6) {
            hideLoading();
            enableRegisterButton();
            showInfo("Password cant contain less than 6 characters.");
            //Toast.makeText(LoginRegisterActivity.this, "Password cant contain less than 6 characters.", Toast.LENGTH_LONG).show();
        }
        else if(!password.equals(confirm)) {
            hideLoading();
            enableRegisterButton();
            showInfo("Passwords don't match.");
            //Toast.makeText(LoginRegisterActivity.this, "Passwords don't match.", Toast.LENGTH_LONG).show();
        }
        else {
            if(!isInternetAvailable()) {
                hideLoading();
                enableRegisterButton();
                showInfo("No internet connection.");
                //Toast.makeText(LoginRegisterActivity.this, "No internet connection.", Toast.LENGTH_LONG).show();
            }
            else {
                DatabaseReference dbRef1 = dbRef.child("nicknames").child(username.toLowerCase());

                dbRef1.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            hideLoading();
                            enableRegisterButton();
                            showInfo("This Username is already taken.");
                            //Toast.makeText(LoginRegisterActivity.this, "This Username is already taken.", Toast.LENGTH_LONG).show();
                        }
                        else {
                            registerFinalize(username, email, password);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        hideLoading();
                        enableRegisterButton();
                        showInfo("Error: " + databaseError.getMessage());
                        //Toast.makeText(LoginRegisterActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void registerFinalize(final String username, final String email, final String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) { // ако регистрацията е успешна искаме да вкараме username и avatar в датабазата и сториджа, за това чакаме тръгъра да се изпълн ии да въведе данните за потребителя -> userInfo.UID.nick трябва да се появи
                    final FirebaseUser user = mAuth.getCurrentUser();
                    final DatabaseReference dbRef2 = dbRef.child("userInfo").child(user.getUid()).child("nick");
                    ValueEventListener valueEventListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {

                                if (dataSnapshot.getValue(String.class).equals(username)) {
                                    dbRef2.removeEventListener(this);
                                    return;
                                }

                                dbRef2.setValue(username, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                        if (uploadAvatar) {
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
                                                    if (!task.isSuccessful()) { throw task.getException(); }
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

                                                        final DatabaseReference dbRef3 = dbRef.child("userInfo").child(user.getUid()).child("avatar");
                                                        dbRef3.addValueEventListener(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                if (dataSnapshot.exists()) {

                                                                    if (dataSnapshot.getValue(String.class).equals(real_url[0])) return;

                                                                    dbRef3.setValue(real_url[0], new DatabaseReference.CompletionListener() {
                                                                        @Override
                                                                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                                            goToMainFromRegSuccessOrFail();
                                                                        }
                                                                    });
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) { }
                                                        });
                                                    } else {
                                                        // Handle failures
                                                        hideLoading();
                                                        showInfo("Error: " + task.getException().getMessage());
                                                        //Toast.makeText(LoginRegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            });

                                        } else {
                                            goToMainFromRegSuccessOrFail();
                                        }
                                    }
                                });
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    };

                    dbRef2.addValueEventListener(valueEventListener);
                } else {
                    // If sign in fails, display a message to the user.
                    //Log.w(TAG, "createUserWithEmail:failure", task.getException());
                    hideLoading();
                    enableRegisterButton();
                    showInfo("Authentication failed: " + task.getException().getMessage());
                    //Toast.makeText(LoginRegisterActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void goToMainFromRegSuccessOrFail() {
        hideLoading();
        Intent intent = new Intent(LoginRegisterActivity.this, AppActivity.class);
        startActivity(intent);
        finish();
    }
    private void disableRegisterButton() {
        findViewById(R.id.register_B_Signup).setOnClickListener(null);
        findViewById(R.id.register_B_Signup).setBackgroundResource(R.drawable.icon_button_signup_clicked);
    }
    private void enableRegisterButton() {
        findViewById(R.id.register_B_Signup).setOnClickListener(registerClick);
        findViewById(R.id.register_B_Signup).setBackgroundResource(R.drawable.icon_button_signup);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            Intent intent = new Intent( LoginRegisterActivity.this, AppActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /* -------------------- DESIGN [ START ] -------------------- */
    // ---------------------------------------- On Click Listeners design
    private View.OnClickListener scrollDown = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            scrollDown();
        }
    };
    private View.OnClickListener scrollUp = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            scrollUp();
        }
    };

    // Scroll Methods design
    private MediaPlayer mediaPlayer;
    private void scrollDown() {
        findViewById(R.id.register_CL_WrapInput).setVisibility(View.VISIBLE);
        findViewById(R.id.login_CL_WrapInput).setVisibility(View.INVISIBLE);
        logreg_SV_Main.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.login_flip1);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.reset();
                        mp.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();
                logreg_SV_Main.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
        handleChangesOnView(true);
        findViewById(R.id.register_ET_Username).requestFocus();
    }
    private void scrollUp() {
        findViewById(R.id.login_CL_WrapInput).setVisibility(View.VISIBLE);
        findViewById(R.id.register_CL_WrapInput).setVisibility(View.INVISIBLE);
        logreg_SV_Main.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.login_flip2);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.reset();
                        mp.release();
                        mediaPlayer = null;
                    }
                });
                mediaPlayer.start();
                logreg_SV_Main.fullScroll(ScrollView.FOCUS_UP);
                findViewById(R.id.login_ET_Email).requestFocus();
            }
        });
        handleChangesOnView(false);
        findViewById(R.id.login_ET_Email).requestFocus();
    }
    private void handleChangesOnView(boolean bool) {
        if(bool) {
            findViewById(R.id.login_IV_Arrow).setOnClickListener(scrollUp);
            findViewById(R.id.register_IV_Arrow).setOnClickListener(scrollUp);
            ((ImageView) findViewById(R.id.login_IV_Arrow)).setImageResource(R.drawable.icon_arrow_down);
            ((ImageView) findViewById(R.id.register_IV_Arrow)).setImageResource(R.drawable.icon_arrow_down);
            showHideAuth(View.INVISIBLE, View.VISIBLE);
        } else {
            findViewById(R.id.login_IV_Arrow).setOnClickListener(scrollDown);
            findViewById(R.id.register_IV_Arrow).setOnClickListener(scrollDown);
            ((ImageView) findViewById(R.id.login_IV_Arrow)).setImageResource(R.drawable.icon_arrow_up);
            ((ImageView) findViewById(R.id.register_IV_Arrow)).setImageResource(R.drawable.icon_arrow_up);
            showHideAuth(View.VISIBLE, View.INVISIBLE);
        }
    }
    private void showHideAuth(int vis1, int vis2) {
        findViewById(R.id.auth_IV_Google1).setVisibility(vis1);
        findViewById(R.id.auth_IV_Facebook1).setVisibility(vis1);
        findViewById(R.id.auth_IV_Twitter1).setVisibility(vis1);
        findViewById(R.id.auth_IV_Google2).setVisibility(vis2);
        findViewById(R.id.auth_IV_Facebook2).setVisibility(vis2);
        findViewById(R.id.auth_IV_Twitter2).setVisibility(vis2);
    }
    //Loading anim
    private void showLoading(){
        animationLoading.setOneShot(false);
        animationLoading.start();
        findViewById(R.id.logreg_CL_Loading).setVisibility(View.VISIBLE);
    }
    private void hideLoading(){
        animationLoading.stop();
        findViewById(R.id.logreg_CL_Loading).setVisibility(View.INVISIBLE);
    }
    //Info Box
    private void showInfo(String stringInfo){
        ((TextView)findViewById(R.id.logreg_TV_Info)).setText(stringInfo);
        findViewById(R.id.logreg_CL_Info).setBackground(new BitmapDrawable(getResources(), Utility.createBlurBitmapFromScreen(findViewById(R.id.logreg_CL_Main), getApplicationContext(), width, height)));
        findViewById(R.id.logreg_IB_InfoClose).setVisibility(View.INVISIBLE);
        findViewById(R.id.logreg_CL_Info).setVisibility(View.VISIBLE);
        final Animation expandOutSlow = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.expand_out_slow);
        findViewById(R.id.logreg_CL_InfoWrap).startAnimation(expandOutSlow);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final Animation dropToNormal = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.drop_to_normal);
                findViewById(R.id.logreg_IB_InfoClose).setVisibility(View.VISIBLE);
                findViewById(R.id.logreg_IB_InfoClose).startAnimation(dropToNormal);
            }
        }, 125);
    }
    private void hideInfo(){
        final Animation dropToBottom = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.drop_to_bottom);
        findViewById(R.id.logreg_IB_InfoClose).setVisibility(View.VISIBLE);
        findViewById(R.id.logreg_IB_InfoClose).startAnimation(dropToBottom);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final Animation shrink = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shrink);
                findViewById(R.id.logreg_CL_InfoWrap).startAnimation(shrink);
            }
        }, 125);

        final Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.logreg_CL_Info).setVisibility(View.INVISIBLE);
            }
        }, 249);
    }
    /* -------------------- DESIGN [  END  ] -------------------- */

    // Utility
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

} //End of LoginRegisterActivity
