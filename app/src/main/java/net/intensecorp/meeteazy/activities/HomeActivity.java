package net.intensecorp.meeteazy.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.adapters.ContactsAdapter;
import net.intensecorp.meeteazy.listener.ActionListener;
import net.intensecorp.meeteazy.models.Contact;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.Firestore;
import net.intensecorp.meeteazy.utils.FormatterUtility;
import net.intensecorp.meeteazy.utils.NetworkInfoUtility;
import net.intensecorp.meeteazy.utils.Patterns;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;
import net.intensecorp.meeteazy.utils.Snackbars;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity implements ActionListener {

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
    private List<Contact> mContacts;
    private ContactsAdapter mContactsAdapter;
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
    private AlertDialog mCreateContactDialog;
    private TextInputLayout mContactEmailLayout;
    private TextInputEditText mContactEmailField;
    private AlertDialog mDeleteContactDialog;
    private AlertDialog mCreateRoomDialog;
    private AlertDialog mJoinRoomDialog;
    private TextInputLayout mRoomIdLayout;
    private TextInputEditText mRoomIdField;

    @RequiresApi(api = Build.VERSION_CODES.O)
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
        mContactsAdapter = new ContactsAdapter(mContacts, this);
        contactsRecyclerView.setAdapter(mContactsAdapter);

        getContacts();

        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.colorSwipeRefreshLayoutProgressSpinnerBackground, getTheme()));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorSwipeRefreshLayoutProgressSpinner, getTheme()));

        mMaterialToolbar.setNavigationOnClickListener(v -> Toast.makeText(HomeActivity.this, "Search", Toast.LENGTH_SHORT).show());

        mToolbar.setNavigationOnClickListener(v -> {
            ContactsAdapter.sSelectedContacts.clear();
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

                                            Contact contact = new Contact();
                                            contact.firstName = documentSnapshot.getString(Firestore.FIELD_FIRST_NAME);
                                            contact.lastName = documentSnapshot.getString(Firestore.FIELD_LAST_NAME);
                                            contact.email = documentSnapshot.getString(Firestore.FIELD_EMAIL);
                                            contact.about = documentSnapshot.getString(Firestore.FIELD_ABOUT);
                                            contact.profilePictureUrl = documentSnapshot.getString(Firestore.FIELD_PROFILE_PICTURE_URL);
                                            contact.fcmToken = documentSnapshot.getString(Firestore.FIELD_FCM_TOKEN);
                                            contact.uid = documentSnapshot.getString(Firestore.FIELD_UID);

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
    public void initiatePersonalCall(Contact contact) {
        if (new NetworkInfoUtility(HomeActivity.this).isConnectedToInternet()) {
            if (contact.fcmToken == null || contact.fcmToken.trim().isEmpty()) {
                Toast.makeText(this, contact.firstName + " " + contact.lastName + " is not available right now", Toast.LENGTH_SHORT).show();
            } else {
                startOutgoingCallActivity(contact);
            }
        } else {
            new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection, mNewFloatingActionButton);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handleSelection(List<Contact> contacts) {
        if (new NetworkInfoUtility(HomeActivity.this).isConnectedToInternet()) {
            if (contacts.size() > 0) {
                mMaterialToolbar.setVisibility(View.INVISIBLE);
                mToolbar.setVisibility(View.VISIBLE);
                mToolbar.setTitle(contacts.size() + " " + getResources().getString(R.string.toolbar_title_selected));
                mToolbar.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.item_call:
                            if (contacts.size() > 0) {
                                if (contacts.size() == 1) {
                                    startOutgoingCallActivity(contacts.get(0));
                                } else {
                                    startOutgoingCallActivity(contacts);
                                }

                                mMaterialToolbar.setVisibility(View.VISIBLE);
                                mToolbar.setVisibility(View.INVISIBLE);

                                ContactsAdapter.sSelectedContacts.clear();
                                mContactsAdapter.notifyDataSetChanged();
                            }
                            break;

                        case R.id.item_delete:
                            if (contacts.size() > 0) {
                                showDeleteContactDialog(contacts);
                            }
                            break;

                        default:
                            break;
                    }

                    return true;
                });
            } else {
                ContactsAdapter.sSelectedContacts.clear();
                mMaterialToolbar.setVisibility(View.VISIBLE);
                mToolbar.setVisibility(View.INVISIBLE);
                mToolbar.setTitle(null);
            }

            mContactsAdapter.notifyDataSetChanged();
        } else {
            new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection, mNewFloatingActionButton);
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

    private String getContactEmail() {

        String contactEmail = Objects.requireNonNull(mContactEmailLayout.getEditText()).getText().toString().trim();

        if (contactEmail.isEmpty()) {
            mContactEmailLayout.setError(getString(R.string.error_empty_contact_email));
            putFocusOn(mContactEmailField);
        } else {
            mContactEmailLayout.setErrorEnabled(false);
            return contactEmail;
        }

        return null;
    }

    private boolean isContactEmailValid() {
        if (getContactEmail() != null) {
            Matcher emailMatcher = Patterns.EMAIL_PATTERN.matcher(getContactEmail());

            if (emailMatcher.matches()) {
                mContactEmailLayout.setErrorEnabled(false);
                return true;
            } else {
                mContactEmailLayout.setError(getString(R.string.error_invalid_email));
                putFocusOn(mContactEmailField);
                return false;
            }
        } else {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void isUserExists() {
        if (isContactEmailValid() && getContactEmail() != null) {
            showProgressDialog();

            mStore.collection(Firestore.COLLECTION_USERS)
                    .whereEqualTo(Firestore.FIELD_EMAIL, getContactEmail())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            dismissProgressDialog();

                            new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_no_user_exists_with_provided_email_address, mNewFloatingActionButton);
                        } else {
                            String contactUid = queryDocumentSnapshots.getDocuments().get(0).getId();
                            isContactAlreadyExists(contactUid);
                        }
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();

                        new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_error_occurred, mNewFloatingActionButton);
                    });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void isContactAlreadyExists(String contactUid) {
        mStore.collection(Firestore.COLLECTION_USERS)
                .document(mUid)
                .collection(Firestore.COLLECTION_CONTACTS)
                .document(contactUid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.getId().equals(mUid)) {
                        new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_cannot_add_yourself, mNewFloatingActionButton);
                    } else if (documentSnapshot.exists()) {
                        dismissProgressDialog();

                        new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_contact_already_exists, mNewFloatingActionButton);
                    } else {
                        saveContact(contactUid);
                    }
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();

                    new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_error_occurred, mNewFloatingActionButton);
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveContact(String contactUid) {
        Map<String, Object> contact = new HashMap<>();
        contact.put(Firestore.FIELD_UID, contactUid);

        mStore.collection(Firestore.COLLECTION_USERS)
                .document(mUid)
                .collection(Firestore.COLLECTION_CONTACTS)
                .document(contactUid)
                .set(contact)
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();

                    new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_contact_saved_successfully, mNewFloatingActionButton);
                    getContacts();
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();

                    new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_error_occurred, mNewFloatingActionButton);
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void deleteContact(List<Contact> contacts) {
        showProgressDialog();

        for (int i = 0; i < contacts.size(); i++) {
            mStore.collection(Firestore.COLLECTION_USERS)
                    .document(mUid)
                    .collection(Firestore.COLLECTION_CONTACTS)
                    .document(contacts.get(i).uid)
                    .delete();
        }

        dismissProgressDialog();

        new Snackbars(HomeActivity.this).snackbar(R.string.snackbar_text_contacts_deleted_successfully, mNewFloatingActionButton);

        mMaterialToolbar.setVisibility(View.VISIBLE);
        mToolbar.setVisibility(View.INVISIBLE);

        ContactsAdapter.sSelectedContacts.clear();
        mContactsAdapter.notifyDataSetChanged();
        getContacts();
    }

    private void initiateMeeting(String roomId) {
        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(HomeActivity.this, SharedPrefsManager.PREF_USER_DATA);

        HashMap<String, String> userData = sharedPrefsManager.getUserDataPrefs();
        String firstName = userData.get(SharedPrefsManager.PREF_FIRST_NAME);
        String lastName = userData.get(SharedPrefsManager.PREF_LAST_NAME);
        String email = userData.get(SharedPrefsManager.PREF_EMAIL);
        String profilePictureLink = userData.get(SharedPrefsManager.PREF_PROFILE_PICTURE_URL);

        String fullName = FormatterUtility.getFullName(firstName, lastName);
        URL profilePictureUrl;

        JitsiMeetUserInfo jitsiMeetUserInfo = new JitsiMeetUserInfo();
        jitsiMeetUserInfo.setDisplayName(fullName);
        jitsiMeetUserInfo.setEmail(email);

        try {
            profilePictureUrl = new URL(profilePictureLink);
            jitsiMeetUserInfo.setAvatar(profilePictureUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        JitsiMeetConferenceOptions.Builder conferenceOptionsBuilder = new JitsiMeetConferenceOptions.Builder()
                .setServerURL(ApiUtility.getJitsiMeetServerUrl())
                .setWelcomePageEnabled(false)
                .setRoom(roomId)
                .setUserInfo(jitsiMeetUserInfo)
                .setVideoMuted(true)
                .setAudioMuted(false);

        JitsiMeetActivity.launch(HomeActivity.this, conferenceOptionsBuilder.build());
    }

    private String getRoomId() {
        String roomId = Objects.requireNonNull(mRoomIdLayout.getEditText()).getText().toString().toUpperCase().trim();

        if (roomId.isEmpty()) {
            mRoomIdLayout.setError(getString(R.string.error_empty_room_id));
            putFocusOn(mRoomIdField);
        } else {
            mRoomIdLayout.setErrorEnabled(false);
            return roomId;
        }

        return null;
    }

    private boolean isRoomIdValid() {
        if (getRoomId() != null) {
            Matcher roomIdMatcher = Patterns.ROOM_ID_PATTERN.matcher(getRoomId());

            if (roomIdMatcher.matches()) {
                mRoomIdLayout.setErrorEnabled(false);
                return true;
            } else {
                mRoomIdLayout.setError(getString(R.string.error_invalid_room_id));
                putFocusOn(mRoomIdField);
                return false;
            }
        } else {
            return false;
        }
    }

    private void putFocusOn(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void hideSoftInput() {
        View view = this.getCurrentFocus();

        if (view != null) {
            view.clearFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNewBottomSheetDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(HomeActivity.this, R.style.StyleBottomSheetDialog);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_dialog_new, findViewById(R.id.linearLayout_bottom_sheet_dialog_container));

        bottomSheetView.findViewById(R.id.linearLayout_create_new_contact).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showCreateContactDialog();
        });

        bottomSheetView.findViewById(R.id.linearLayout_create_new_room).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showCreateRoomDialog();
        });

        bottomSheetView.findViewById(R.id.linearLayout_join_a_room).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showJoinRoomDialog();
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showCreateContactDialog() {
        if (mCreateContactDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_contact, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mCreateContactDialog = builder.create();

            if (mCreateContactDialog.getWindow() != null) {
                mCreateContactDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            ImageView closeButton = view.findViewById(R.id.imageView_close);
            mContactEmailLayout = view.findViewById(R.id.textInputLayout_contact_email);
            mContactEmailField = view.findViewById(R.id.textInputEditText_contact_email);
            MaterialButton saveButton = view.findViewById(R.id.button_save);

            mContactEmailField.addTextChangedListener(new ValidationWatcher(mContactEmailField));

            closeButton.setOnClickListener(v -> {
                hideSoftInput();
                dismissCreateContactDialog();
            });

            saveButton.setOnClickListener(v -> {
                hideSoftInput();
                dismissCreateContactDialog();
                isUserExists();
            });

        }

        mCreateContactDialog.show();
    }

    private void dismissCreateContactDialog() {
        if (mCreateContactDialog != null) {
            mCreateContactDialog.dismiss();
            mCreateContactDialog = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showDeleteContactDialog(List<Contact> contacts) {
        if (mDeleteContactDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_delete_contact, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mDeleteContactDialog = builder.create();

            if (mDeleteContactDialog.getWindow() != null) {
                mDeleteContactDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.button_no).setOnClickListener(v -> dismissDeleteContactDialog());

            view.findViewById(R.id.button_yes).setOnClickListener(v -> {
                dismissDeleteContactDialog();
                deleteContact(contacts);
            });

        }

        mDeleteContactDialog.show();
    }

    private void dismissDeleteContactDialog() {
        if (mDeleteContactDialog != null) {
            mDeleteContactDialog.dismiss();
            mDeleteContactDialog = null;
        }
    }

    private void showCreateRoomDialog() {
        if (mCreateRoomDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_room, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mCreateRoomDialog = builder.create();

            if (mCreateRoomDialog.getWindow() != null) {
                mCreateRoomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            String generatedRoomId = Patterns.generateRoomId();

            ImageView closeButton = view.findViewById(R.id.imageView_close);
            TextInputLayout roomIdLayout = view.findViewById(R.id.textInputLayout_room_id);
            TextInputEditText roomIdField = view.findViewById(R.id.textInputEditText_room_id);
            MaterialButton createButton = view.findViewById(R.id.button_create);

            roomIdField.setText(generatedRoomId);

            closeButton.setOnClickListener(v -> dismissCreateRoomDialog());

            roomIdLayout.setEndIconOnClickListener(v -> {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("Room ID", generatedRoomId);
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(this, R.string.toast_text_room_id_copied_to_clipboard, Toast.LENGTH_SHORT).show();
            });

            createButton.setOnClickListener(v -> {
                hideSoftInput();
                dismissCreateRoomDialog();
                initiateMeeting(generatedRoomId);
            });
        }

        mCreateRoomDialog.show();
    }

    private void dismissCreateRoomDialog() {
        if (mCreateRoomDialog != null) {
            mCreateRoomDialog.dismiss();
            mCreateRoomDialog = null;
        }
    }

    private void showJoinRoomDialog() {
        if (mJoinRoomDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_join_room, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mJoinRoomDialog = builder.create();

            if (mJoinRoomDialog.getWindow() != null) {
                mJoinRoomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            ImageView closeButton = view.findViewById(R.id.imageView_close);
            mRoomIdLayout = view.findViewById(R.id.textInputLayout_room_id);
            mRoomIdField = view.findViewById(R.id.textInputEditText_room_id);
            MaterialButton joinButton = view.findViewById(R.id.button_join);

            closeButton.setOnClickListener(v -> {
                hideSoftInput();
                dismissJoinRoomDialog();
            });

            mRoomIdField.addTextChangedListener(new ValidationWatcher(mRoomIdField));

            mRoomIdLayout.setEndIconOnClickListener(v -> {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = clipboardManager.getPrimaryClip();
                ClipData.Item item = clipData.getItemAt(0);
                String pasteData = item.getText().toString().trim().toUpperCase();
                mRoomIdField.setText(pasteData);
            });

            joinButton.setOnClickListener(v -> {
                if (isRoomIdValid()) {
                    hideSoftInput();
                    dismissJoinRoomDialog();
                    initiateMeeting(getRoomId());
                }
            });
        }

        mJoinRoomDialog.show();
    }

    private void dismissJoinRoomDialog() {
        if (mJoinRoomDialog != null) {
            mJoinRoomDialog.dismiss();
            mJoinRoomDialog = null;
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

            view.findViewById(R.id.imageView_close).setOnClickListener(v -> dismissProfileDialog());

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

    private void startOutgoingCallActivity(Contact callee) {
        Intent outgoingCallIntent = new Intent(HomeActivity.this, OutgoingCallActivity.class);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALL_TYPE, ApiUtility.CALL_TYPE_PERSONAL);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALLEE, callee);
        startActivity(outgoingCallIntent);
    }

    private void startOutgoingCallActivity(List<Contact> callees) {
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

    public class ValidationWatcher implements TextWatcher {

        private final View view;

        private ValidationWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        public void afterTextChanged(Editable editable) {
            if (view.getId() == R.id.textInputEditText_contact_email) {
                getContactEmail();
            }

            if (view.getId() == R.id.textInputEditText_room_id) {
                getRoomId();
            }
        }
    }
}
