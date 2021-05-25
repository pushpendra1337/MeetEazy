package net.intensecorp.meeteazy.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.adapters.UsersAdapter;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.listener.UsersListener;
import net.intensecorp.meeteazy.models.User;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.Firestore;
import net.intensecorp.meeteazy.utils.NetworkInfoUtility;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity implements UsersListener {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final String FEEDBACK_EMAIL_TYPE = "text/html";
    private static final String FEEDBACK_EMAIL_HANDLER_PACKAGE = "com.google.android.gm";
    private static final String[] FEEDBACK_RECIPIENTS_EMAIL_ADDRESS = {"pushpendray1337@gmail.com"};
    private static final String FEEDBACK_EMAIL_SUBJECT = "Feedback on MeetEazy app";
    private FirebaseAuth mAuth;
    private FirebaseMessaging mMessaging;
    private FirebaseFirestore mStore;
    private DocumentReference mUserReference;
    private String mUid;
    private List<User> mUsers;
    private UsersAdapter mUsersAdapter;
    private ProgressBar mLoadingProgressbar;
    private LinearLayout mNoInternetErrorLayout;
    private LinearLayout mNoUserErrorLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AlertDialog mNoInternetDialog;
    private AlertDialog mProfileDialog;
    private AlertDialog mProgressDialog;
    private AlertDialog mSignOutDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        RecyclerView usersRecyclerView = findViewById(R.id.recyclerView_users);
        mLoadingProgressbar = findViewById(R.id.progressBar_loading);
        mNoInternetErrorLayout = findViewById(R.id.linearLayout_error_no_internet);
        mNoUserErrorLayout = findViewById(R.id.linearLayout_error_no_user);
        MaterialButton tryAgainButton = findViewById(R.id.button_try_again);
        MaterialButton refreshButton = findViewById(R.id.button_refresh);
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        toolbar.setTitle(R.string.toolbar_title_search);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        mMessaging = FirebaseMessaging.getInstance();

        mStore = FirebaseFirestore.getInstance();
        mUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        mUserReference = mStore.collection(Firestore.COLLECTION_USERS).document(mUid);

        if (isUserValid()) {
            getFcmToken();
        }

        mUsers = new ArrayList<>();
        mUsersAdapter = new UsersAdapter(mUsers, this);
        usersRecyclerView.setAdapter(mUsersAdapter);

        getUsers();

        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorSwipeRefreshLayoutProgressSpinnerBackground, getTheme()));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorSwipeRefreshLayoutProgressSpinner, getTheme()));

        toolbar.setNavigationOnClickListener(v -> Toast.makeText(HomeActivity.this, "Search", Toast.LENGTH_SHORT).show());

        toolbar.setOnClickListener(v -> Toast.makeText(HomeActivity.this, "Search", Toast.LENGTH_SHORT).show());

        tryAgainButton.setOnClickListener(v -> {
            if (mNoInternetErrorLayout.getVisibility() == View.VISIBLE) {
                mNoInternetErrorLayout.setVisibility(View.GONE);
            }

            getUsers();
        });

        refreshButton.setOnClickListener(v -> {
            if (mNoUserErrorLayout.getVisibility() == View.VISIBLE) {
                mNoUserErrorLayout.setVisibility(View.GONE);
            }

            getUsers();
        });

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            getUsers();
        });
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
                    SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(HomeActivity.this, SharedPrefsManager.PREF_USER_DATA);
                    sharedPrefsManager.setFcmTokenPref(s);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get token: " + e.getMessage()));
    }

    private void setFcmToken(String token) {
        mUserReference.update(Firestore.FIELD_FCM_TOKEN, token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully updated: " + token))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update token: " + e.getMessage()));
    }

    private void getUsers() {
        if (mSwipeRefreshLayout.isRefreshing()) {
            mLoadingProgressbar.setVisibility(View.GONE);
        } else {
            mLoadingProgressbar.setVisibility(View.VISIBLE);
        }

        if (new NetworkInfoUtility(HomeActivity.this).isConnectedToInternet()) {

            mUsers.clear();

            mStore.collection(Firestore.COLLECTION_USERS)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {

                        mUsers.clear();

                        if (mSwipeRefreshLayout.isRefreshing()) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }

                        if (mLoadingProgressbar.getVisibility() == View.VISIBLE) {
                            mLoadingProgressbar.setVisibility(View.GONE);
                        }

                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {

                            if (mUid.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }

                            User user = new User();
                            user.mFirstName = queryDocumentSnapshot.getString(Firestore.FIELD_FIRST_NAME);
                            user.mLastName = queryDocumentSnapshot.getString(Firestore.FIELD_LAST_NAME);
                            user.mEmail = queryDocumentSnapshot.getString(Firestore.FIELD_EMAIL);
                            user.mAbout = queryDocumentSnapshot.getString(Firestore.FIELD_ABOUT);
                            user.mProfilePictureUrl = queryDocumentSnapshot.getString(Firestore.FIELD_PROFILE_PICTURE_URL);
                            user.mFcmToken = queryDocumentSnapshot.getString(Firestore.FIELD_FCM_TOKEN);

                            mUsers.add(user);
                        }

                        if (mUsers.size() > 0) {
                            mUsersAdapter.notifyDataSetChanged();
                        } else {
                            mNoUserErrorLayout.setVisibility(View.VISIBLE);
                        }

                        if (mNoInternetErrorLayout.getVisibility() == View.VISIBLE) {
                            mNoInternetErrorLayout.setVisibility(View.GONE);
                        }

                        if (mNoUserErrorLayout.getVisibility() == View.VISIBLE) {
                            mNoUserErrorLayout.setVisibility(View.GONE);
                        }

                        Log.d(TAG, "Users loaded successfully");
                    })
                    .addOnFailureListener(e -> {

                        if (mSwipeRefreshLayout.isRefreshing()) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }

                        if (mLoadingProgressbar.getVisibility() == View.VISIBLE) {
                            mLoadingProgressbar.setVisibility(View.GONE);
                        }

                        if (!new NetworkInfoUtility(HomeActivity.this).isConnectedToInternet()) {
                            mNoInternetErrorLayout.setVisibility(View.VISIBLE);
                        } else {
                            mNoUserErrorLayout.setVisibility(View.VISIBLE);
                        }

                        Log.e(TAG, "Failed to load users: " + e.getMessage());
                    });
        } else {

            mUsers.clear();
            mUsersAdapter.notifyDataSetChanged();

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (mLoadingProgressbar.getVisibility() == View.VISIBLE) {
                mLoadingProgressbar.setVisibility(View.GONE);
            }

            mNoInternetErrorLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void initiateVoiceCall(User user) {
        if (user.mFcmToken == null || user.mFcmToken.trim().isEmpty()) {
            Toast.makeText(HomeActivity.this, user.mFirstName + " " + user.mLastName + " is not available", Toast.LENGTH_SHORT).show();
        } else {
            startOutgoingCallActivity(ApiUtility.TYPE_VOICE_CALL, user);
        }
    }

    @Override
    public void initiateVideoCall(User user) {
        if (user.mFcmToken == null || user.mFcmToken.trim().isEmpty()) {
            Toast.makeText(HomeActivity.this, user.mFirstName + " " + user.mLastName + " is not available", Toast.LENGTH_SHORT).show();
        } else {
            startOutgoingCallActivity(ApiUtility.TYPE_VIDEO_CALL, user);
        }
    }

    private void signOut() {

        if (new NetworkInfoUtility(HomeActivity.this).isConnectedToInternet()) {

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

        } else {
            dismissProgressDialog();

            showNoInternetDialog();
        }
    }

    private void showNoInternetDialog() {
        if (mNoInternetDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_no_internet, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mNoInternetDialog = builder.create();

            if (mNoInternetDialog.getWindow() != null) {
                mNoInternetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.button_ok).setOnClickListener(v -> dismissNoInternetDialog());

            view.findViewById(R.id.button_settings).setOnClickListener(v -> {
                dismissNoInternetDialog();

                startWirelessSettingsActivity();
            });
        }

        mNoInternetDialog.show();
    }

    private void dismissNoInternetDialog() {
        if (mNoInternetDialog != null) {
            mNoInternetDialog.dismiss();
        }
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

                startComposeEmailActivity();
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

    private void startAboutActivity() {
        Intent aboutIntent = new Intent(HomeActivity.this, AboutActivity.class);
        startActivity(aboutIntent);
    }

    private void startComposeEmailActivity() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, FEEDBACK_RECIPIENTS_EMAIL_ADDRESS);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, FEEDBACK_EMAIL_SUBJECT);
        emailIntent.setType(FEEDBACK_EMAIL_TYPE);
        emailIntent.setPackage(FEEDBACK_EMAIL_HANDLER_PACKAGE);
        startActivity(emailIntent);
    }

    private void startOutgoingCallActivity(String callType, User user) {
        Intent outgoingCallIntent = new Intent(HomeActivity.this, OutgoingCallActivity.class);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALL_TYPE, callType);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALLEE, user);
        startActivity(outgoingCallIntent);
    }

    private void startProfileActivity() {
        Intent profileIntent = new Intent(HomeActivity.this, ProfileActivity.class);
        startActivity(profileIntent);
    }

    private void startSettingsActivity() {
        Intent settingsIntent = new Intent(HomeActivity.this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void startSignInActivity() {
        Intent signInIntent = new Intent(HomeActivity.this, SignInActivity.class);
        signInIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(signInIntent);
        finish();
    }

    private void startWirelessSettingsActivity() {
        Intent wirelessSettingsIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        wirelessSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(wirelessSettingsIntent);
    }
}
