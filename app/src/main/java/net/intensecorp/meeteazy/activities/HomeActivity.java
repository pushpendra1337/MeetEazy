package net.intensecorp.meeteazy.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.Firestore;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String FEEDBACK_EMAIL_TYPE = "text/html";
    private static final String FEEDBACK_EMAIL_HANDLER_PACKAGE = "com.google.android.gm";
    private static final String[] FEEDBACK_RECIPIENTS_EMAIL_ADDRESS = {"pushpendray1337@gmail.com"};
    private static final String FEEDBACK_EMAIL_SUBJECT = "Feedback on MeetEazy app";
    private AlertDialog mProfileDialog;
    private AlertDialog mSignOutDialog;
    private AlertDialog mProgressDialog;
    private FirebaseAuth mAuth;
    private FirebaseMessaging mMessaging;
    private DocumentReference mUserReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setTitle(R.string.toolbar_title_search_in_rooms);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        mMessaging = FirebaseMessaging.getInstance();

        FirebaseFirestore store = FirebaseFirestore.getInstance();
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        mUserReference = store.collection(Firestore.COLLECTION_USERS).document(uid);

        if (isUserValid()) {
            getFcmToken();
        }

        toolbar.setNavigationOnClickListener(v -> Toast.makeText(HomeActivity.this, "Search", Toast.LENGTH_SHORT).show());

        toolbar.setOnClickListener(v -> Toast.makeText(HomeActivity.this, "Search", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);

        MenuItem menuItem = menu.findItem(R.id.item_profile);
        View view = menuItem.getActionView();

        CircleImageView profilePicture = view.findViewById(R.id.circleImageView_profile_picture);

        profilePicture.setOnClickListener(v -> showProfileDialog());

        return super.onCreateOptionsMenu(menu);
    }

    private boolean isUserValid() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    private void getFcmToken() {
        mMessaging.getToken()
                .addOnSuccessListener(s -> {
                    Log.d(TAG, "FCM token: " + s);

                    setFcmToken(s);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get token: " + e.getMessage()));
    }

    private void setFcmToken(String token) {

        mUserReference.update(Firestore.FIELD_FCM_TOKEN, token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully updated: " + token))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update token: " + e.getMessage()));
    }

    private void showProfileDialog() {
        if (mProfileDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_profile, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mProfileDialog = builder.create();

            if (mProfileDialog.getWindow() != null) {
                mProfileDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.linearLayout_profile).setOnClickListener(v -> {
                dismissProfileDialog();

                startProfileActivity();
            });

            view.findViewById(R.id.linearLayout_settings).setOnClickListener(v -> {
                dismissProfileDialog();

                startSettingsActivity();
            });

            view.findViewById(R.id.linearLayout_about).setOnClickListener(v -> {
                dismissProfileDialog();

                startAboutActivity();
            });

            view.findViewById(R.id.linearLayout_feedback).setOnClickListener(v -> {
                dismissProfileDialog();

                sendEmail();
            });

            view.findViewById(R.id.button_sign_out).setOnClickListener(v -> {
                dismissProfileDialog();

                showSignOutDialog();
            });

            view.findViewById(R.id.imageView_close).setOnClickListener(v -> dismissProfileDialog());
        }

        mProfileDialog.show();
    }

    private void dismissProfileDialog() {
        if (mProfileDialog != null) {
            mProfileDialog.dismiss();
        }
    }

    private void showSignOutDialog() {
        if (mSignOutDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_sign_out, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mSignOutDialog = builder.create();

            if (mSignOutDialog.getWindow() != null) {
                mSignOutDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.button_no).setOnClickListener(v -> dismissSignOutDialog());

            view.findViewById(R.id.button_yes).setOnClickListener(v -> {
                dismissSignOutDialog();

                signOut();
            });
        }

        mSignOutDialog.show();
    }

    private void dismissSignOutDialog() {
        if (mSignOutDialog != null) {
            mSignOutDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, findViewById(R.id.constraintLayout_progress_dialog_container));
            builder.setView(view).setCancelable(false);
            mProgressDialog = builder.create();

            if (mProgressDialog.getWindow() != null) {
                mProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
        }
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void signOut() {
        showProgressDialog();

        mUserReference
                .update(Firestore.FIELD_FCM_TOKEN, FieldValue.delete())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully deleted from database"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete token from database: " + e.getMessage()));

        mMessaging.deleteToken()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully deleted from device"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete token from device: " + e.getMessage()));

        mAuth.signOut();

        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(HomeActivity.this, SharedPrefsManager.PREF_USER_DATA);
        sharedPrefsManager.invalidateSession();

        dismissProgressDialog();

        startSignInActivity();
    }

    private void startProfileActivity() {
        Intent profileIntent = new Intent(getApplicationContext(), ProfileActivity.class);
        startActivity(profileIntent);
    }

    private void startSettingsActivity() {
        Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void startAboutActivity() {
        Intent aboutIntent = new Intent(getApplicationContext(), AboutActivity.class);
        startActivity(aboutIntent);
    }

    private void sendEmail() {
        startComposeEmailActivity();
    }

    private void startComposeEmailActivity() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, FEEDBACK_RECIPIENTS_EMAIL_ADDRESS);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, FEEDBACK_EMAIL_SUBJECT);
        emailIntent.setType(FEEDBACK_EMAIL_TYPE);
        emailIntent.setPackage(FEEDBACK_EMAIL_HANDLER_PACKAGE);
        startActivity(emailIntent);
    }

    private void startSignInActivity() {
        Intent signInIntent = new Intent(HomeActivity.this, SignInActivity.class);
        signInIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(signInIntent);
        finish();
    }
}