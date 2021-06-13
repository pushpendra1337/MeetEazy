package net.intensecorp.meeteazy.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.adapters.UsersAdapter;
import net.intensecorp.meeteazy.listener.UsersListener;
import net.intensecorp.meeteazy.models.User;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.Firestore;
import net.intensecorp.meeteazy.utils.NetworkInfoUtility;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;
import net.intensecorp.meeteazy.utils.Snackbars;

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
    private List<User> mContacts;
    private UsersAdapter mContactsAdapter;
    private MaterialToolbar mMaterialToolbar;
    private Toolbar mToolbar;
    private ProgressBar mLoadingProgressbar;
    private LinearLayout mNoInternetErrorLayout;
    private LinearLayout mNoUserErrorLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private FloatingActionButton mNewFloatingActionButton;
    private AlertDialog mBatteryOptimizationDialog;
    private AlertDialog mNoInternetDialog;
    private AlertDialog mProfileDialog;
    private AlertDialog mProgressDialog;
    private AlertDialog mSignOutDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mMaterialToolbar = findViewById(R.id.materialToolbar);
        mToolbar = findViewById(R.id.toolbar);
        RecyclerView contactsRecyclerView = findViewById(R.id.recyclerView_users);
        mLoadingProgressbar = findViewById(R.id.progressBar_loading);
        mNoInternetErrorLayout = findViewById(R.id.linearLayout_error_no_internet);
        mNoUserErrorLayout = findViewById(R.id.linearLayout_error_no_user);
        MaterialButton tryAgainButton = findViewById(R.id.button_try_again);
        MaterialButton refreshButton = findViewById(R.id.button_refresh);
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mNewFloatingActionButton = findViewById(R.id.floatingActionButton_new);

        mMaterialToolbar.setTitle(R.string.toolbar_title_search);
        setSupportActionBar(mMaterialToolbar);

        mAuth = FirebaseAuth.getInstance();
        mMessaging = FirebaseMessaging.getInstance();

        mStore = FirebaseFirestore.getInstance();
        mUid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        mUserReference = mStore.collection(Firestore.COLLECTION_USERS).document(mUid);

        if (isUserValid()) {
            getFcmToken();
        }

        mContacts = new ArrayList<>();
        mContactsAdapter = new UsersAdapter(mContacts, this);
        contactsRecyclerView.setAdapter(mContactsAdapter);

        getContacts();

        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorSwipeRefreshLayoutProgressSpinnerBackground, getTheme()));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorSwipeRefreshLayoutProgressSpinner, getTheme()));

        mMaterialToolbar.setNavigationOnClickListener(v -> Toast.makeText(HomeActivity.this, "Search", Toast.LENGTH_SHORT).show());

        mToolbar.setNavigationOnClickListener(v -> {
            UsersAdapter.mSelectedUsers.clear();
            mToolbar.setVisibility(View.INVISIBLE);
            mMaterialToolbar.setVisibility(View.VISIBLE);
            mContactsAdapter.notifyDataSetChanged();
        });

        mMaterialToolbar.setOnClickListener(v -> {
            // TODO: Search
        });

        tryAgainButton.setOnClickListener(v -> {
            if (mNoInternetErrorLayout.getVisibility() == View.VISIBLE) {
                mNoInternetErrorLayout.setVisibility(View.GONE);
            }

            getContacts();
        });

        refreshButton.setOnClickListener(v -> {
            if (mNoUserErrorLayout.getVisibility() == View.VISIBLE) {
                mNoUserErrorLayout.setVisibility(View.GONE);
            }

            getContacts();
        });

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            getContacts();
        });

        mNewFloatingActionButton.setOnClickListener(v -> showNewBottomSheetDialog());

        isBatteryOptimizationEnabled();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_material_toolbar, menu);

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
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "FCM token: " + token);

                    setFcmToken(token);
                    SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(HomeActivity.this, SharedPrefsManager.PREF_USER_DATA);
                    sharedPrefsManager.setFcmTokenPref(token);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get token: " + e.getMessage()));
    }

    private void setFcmToken(String fcmToken) {
        mUserReference.update(Firestore.FIELD_FCM_TOKEN, fcmToken)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully updated: " + fcmToken))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update token: " + e.getMessage()));
    }

    private void getContacts() {
        if (mSwipeRefreshLayout.isRefreshing()) {
            mLoadingProgressbar.setVisibility(View.GONE);
        } else {
            mLoadingProgressbar.setVisibility(View.VISIBLE);
        }

        mContacts.clear();

        if (new NetworkInfoUtility(HomeActivity.this).isConnectedToInternet()) {

            mStore.collection(Firestore.COLLECTION_USERS)
                    .document(mUid)
                    .collection(Firestore.COLLECTION_CONTACTS)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {

                        ArrayList<String> contactsUidList = new ArrayList<>();
                        contactsUidList.clear();

                        for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                            contactsUidList.add(queryDocumentSnapshot.getId());
                        }

                        if (contactsUidList.size() > 0) {

                            for (int i = 0; i < contactsUidList.size(); i++) {

                                mStore.collection(Firestore.COLLECTION_USERS)
                                        .document(contactsUidList.get(i))
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {

                                            if (mSwipeRefreshLayout.isRefreshing()) {
                                                mSwipeRefreshLayout.setRefreshing(false);
                                            }

                                            if (mLoadingProgressbar.getVisibility() == View.VISIBLE) {
                                                mLoadingProgressbar.setVisibility(View.GONE);
                                            }

                                            User contact = new User();
                                            contact.mFirstName = documentSnapshot.getString(Firestore.FIELD_FIRST_NAME);
                                            contact.mLastName = documentSnapshot.getString(Firestore.FIELD_LAST_NAME);
                                            contact.mEmail = documentSnapshot.getString(Firestore.FIELD_EMAIL);
                                            contact.mAbout = documentSnapshot.getString(Firestore.FIELD_ABOUT);
                                            contact.mProfilePictureUrl = documentSnapshot.getString(Firestore.FIELD_PROFILE_PICTURE_URL);
                                            contact.mFcmToken = documentSnapshot.getString(Firestore.FIELD_FCM_TOKEN);

                                            mContacts.add(contact);

                                            if (mContacts.size() > 0) {
                                                mContactsAdapter.notifyDataSetChanged();
                                            } else {
                                                mNoUserErrorLayout.setVisibility(View.VISIBLE);
                                            }

                                            if (mNewFloatingActionButton.getVisibility() == View.GONE) {
                                                mNewFloatingActionButton.setVisibility(View.VISIBLE);
                                            }

                                            if (mNoInternetErrorLayout.getVisibility() == View.VISIBLE) {
                                                mNoInternetErrorLayout.setVisibility(View.GONE);
                                            }

                                            if (mNoUserErrorLayout.getVisibility() == View.VISIBLE) {
                                                mNoUserErrorLayout.setVisibility(View.GONE);
                                            }
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

                                                mNewFloatingActionButton.setVisibility(View.GONE);
                                            } else {
                                                mNoUserErrorLayout.setVisibility(View.VISIBLE);
                                            }

                                            Log.e(TAG, "Failed to load users: " + e.getMessage());
                                        });
                            }
                        }

                        if (mContacts.size() > 0) {
                            mContactsAdapter.notifyDataSetChanged();
                        } else {
                            mNoUserErrorLayout.setVisibility(View.VISIBLE);
                        }

                        if (mNoInternetErrorLayout.getVisibility() == View.VISIBLE) {
                            mNoInternetErrorLayout.setVisibility(View.GONE);

                            mNewFloatingActionButton.setVisibility(View.VISIBLE);
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

                            mNewFloatingActionButton.setVisibility(View.GONE);
                        } else {
                            mNoUserErrorLayout.setVisibility(View.VISIBLE);
                        }

                        Log.e(TAG, "Failed to load users: " + e.getMessage());
                    });
        } else {
            mContactsAdapter.notifyDataSetChanged();

            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }

            if (mLoadingProgressbar.getVisibility() == View.VISIBLE) {
                mLoadingProgressbar.setVisibility(View.GONE);
            }

            mNoInternetErrorLayout.setVisibility(View.VISIBLE);

            mNewFloatingActionButton.setVisibility(View.GONE);
        }
    }

    private void isBatteryOptimizationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                showBatteryOptimizationDialog();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void initiatePersonalCall(User callee) {
        if (new NetworkInfoUtility(HomeActivity.this).isConnectedToInternet()) {
            if (callee.mFcmToken == null || callee.mFcmToken.trim().isEmpty()) {
                Toast.makeText(HomeActivity.this, callee.mFirstName + " " + callee.mLastName + " is not available", Toast.LENGTH_SHORT).show();
            } else {
                startOutgoingCallActivity(callee);
            }
        } else {
            new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_check_your_internet_connection);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void initiateGroupCall(List<User> callees) {
        if (new NetworkInfoUtility(HomeActivity.this).isConnectedToInternet()) {
            if (callees.size() > 0) {
                mMaterialToolbar.setVisibility(View.INVISIBLE);
                mToolbar.setVisibility(View.VISIBLE);
                mToolbar.setTitle(callees.size() + " " + getResources().getString(R.string.toolbar_title_selected));
                mToolbar.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.item_call) {
                        if (callees.size() > 0) {
                            if (callees.size() == 1) {
                                startOutgoingCallActivity(callees.get(0));
                            } else {
                                startOutgoingCallActivity(callees);
                            }

                            mMaterialToolbar.setVisibility(View.VISIBLE);
                            mToolbar.setVisibility(View.INVISIBLE);

                            UsersAdapter.mSelectedUsers.clear();
                            mContactsAdapter.notifyDataSetChanged();
                        }
                    }

                    return true;
                });
            } else {
                UsersAdapter.mSelectedUsers.clear();
                mMaterialToolbar.setVisibility(View.VISIBLE);
                mToolbar.setVisibility(View.INVISIBLE);
                mToolbar.setTitle(null);
            }

            mContactsAdapter.notifyDataSetChanged();
        } else {
            new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_check_your_internet_connection);
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

    private void showNewBottomSheetDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(HomeActivity.this, R.style.StyleBottomSheetDialog);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_dialog_new, findViewById(R.id.linearLayout_bottom_sheet_dialog_container));

        LinearLayout createNewContact = bottomSheetView.findViewById(R.id.linearLayout_create_new_contact);
        LinearLayout createRoomLayout = bottomSheetView.findViewById(R.id.linearLayout_create_new_room);
        LinearLayout joinRoomLayout = bottomSheetView.findViewById(R.id.linearLayout_join_a_room);

        createNewContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        createRoomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        joinRoomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void showBatteryOptimizationDialog() {
        if (mBatteryOptimizationDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_battery_optimization, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mBatteryOptimizationDialog = builder.create();

            if (mBatteryOptimizationDialog.getWindow() != null) {
                mBatteryOptimizationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.button_settings).setOnClickListener(v -> startBatteryOptimizationSettingsActivity());

            view.findViewById(R.id.button_cancel).setOnClickListener(v -> dismissBatteryOptimizationDialog());
        }

        mBatteryOptimizationDialog.show();
    }

    private void dismissBatteryOptimizationDialog() {
        if (mBatteryOptimizationDialog != null) {
            mBatteryOptimizationDialog.dismiss();
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

    private void startBatteryOptimizationSettingsActivity() {
        Intent batteryOptimizationSettingsIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        batteryOptimizationSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(batteryOptimizationSettingsIntent);
    }

    private void startComposeEmailActivity() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, FEEDBACK_RECIPIENTS_EMAIL_ADDRESS);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, FEEDBACK_EMAIL_SUBJECT);
        emailIntent.setType(FEEDBACK_EMAIL_TYPE);
        emailIntent.setPackage(FEEDBACK_EMAIL_HANDLER_PACKAGE);
        startActivity(emailIntent);
    }

    private void startOutgoingCallActivity(User callee) {
        Intent outgoingCallIntent = new Intent(HomeActivity.this, OutgoingCallActivity.class);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALL_TYPE, ApiUtility.CALL_TYPE_PERSONAL);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALLEE, callee);
        startActivity(outgoingCallIntent);
    }

    private void startOutgoingCallActivity(List<User> callees) {
        Intent outgoingCallIntent = new Intent(HomeActivity.this, OutgoingCallActivity.class);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALL_TYPE, ApiUtility.CALL_TYPE_GROUP);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALLEES, new Gson().toJson(callees));
        outgoingCallIntent.putExtra(Extras.EXTRA_CALLEE, callees.get(0));
        outgoingCallIntent.putExtra(Extras.EXTRA_OTHER_CALLEES_COUNT, String.valueOf(callees.size() - 1));
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
