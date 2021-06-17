package net.intensecorp.meeteazy.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.models.Contact;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.Firestore;
import net.intensecorp.meeteazy.utils.FormatterUtility;
import net.intensecorp.meeteazy.utils.NetworkInfoUtility;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;
import net.intensecorp.meeteazy.utils.Snackbars;

import java.util.HashMap;
import java.util.Objects;

public class ViewProfileActivity extends AppCompatActivity {

    private static final String TAG = ViewProfileActivity.class.getSimpleName();
    private AppCompatImageView mProfilePictureView;
    private MaterialTextView mFirstNameView;
    private MaterialTextView mLastNameView;
    private MaterialTextView mEmailView;
    private MaterialTextView mAboutView;
    private FirebaseFirestore mStore;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        MaterialToolbar materialToolbar = findViewById(R.id.materialToolbar_view_profile);
        mProfilePictureView = findViewById(R.id.appCompatImageView_profile_picture);
        mFirstNameView = findViewById(R.id.textView_first_name_holder);
        mLastNameView = findViewById(R.id.textView_last_name_holder);
        mEmailView = findViewById(R.id.textView_email_holder);
        mAboutView = findViewById(R.id.textView_about_holder);
        FloatingActionButton editButton = findViewById(R.id.floatingActionButton_edit);
        FloatingActionButton callButton = findViewById(R.id.floatingActionButton_call);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();

        Intent profileIntent = getIntent();
        boolean isSelf = profileIntent.getBooleanExtra(Extras.EXTRA_IS_SELF, false);
        Contact contact = (Contact) profileIntent.getSerializableExtra(Extras.EXTRA_CONTACT);

        String firstName;
        String lastName;
        String email;
        String about;
        String profilePictureUrl;

        if (isSelf) {
            SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(ViewProfileActivity.this, SharedPrefsManager.PREF_USER_DATA);
            HashMap<String, String> userData = sharedPrefsManager.getUserDataPrefs();
            firstName = userData.get(SharedPrefsManager.PREF_FIRST_NAME);
            lastName = userData.get(SharedPrefsManager.PREF_LAST_NAME);
            email = userData.get(SharedPrefsManager.PREF_EMAIL);
            about = userData.get(SharedPrefsManager.PREF_ABOUT);
            profilePictureUrl = userData.get(SharedPrefsManager.PREF_PROFILE_PICTURE_URL);

            editButton.setVisibility(View.VISIBLE);
            callButton.setVisibility(View.GONE);
        } else {
            firstName = contact.firstName;
            lastName = contact.lastName;
            email = contact.email;
            about = contact.about;
            profilePictureUrl = contact.profilePictureUrl;

            editButton.setVisibility(View.GONE);
            callButton.setVisibility(View.VISIBLE);
        }

        materialToolbar.setTitle(FormatterUtility.getFullName(firstName, lastName));
        setSupportActionBar(materialToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        materialToolbar.setNavigationOnClickListener(v -> onBackPressed());

        mFirstNameView.setText(firstName);
        mLastNameView.setText(lastName);
        mEmailView.setText(email);
        mAboutView.setText(about);

        loadProfilePicture(profilePictureUrl);

        if (isSelf) {
            getFreshData(Objects.requireNonNull(auth.getCurrentUser()).getUid());
        } else {
            getFreshData(contact.uid);
        }

        editButton.setOnClickListener(v -> startEditProfileActivity());

        callButton.setOnClickListener(v -> initiatePersonalCall(contact));
    }

    private void getFreshData(String uid) {
        mStore.collection(Firestore.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    mFirstNameView.setText(documentSnapshot.getString(Firestore.FIELD_FIRST_NAME));
                    mLastNameView.setText(documentSnapshot.getString(Firestore.FIELD_LAST_NAME));
                    mEmailView.setText(documentSnapshot.getString(Firestore.FIELD_EMAIL));
                    mAboutView.setText(documentSnapshot.getString(Firestore.FIELD_ABOUT));
                    loadProfilePicture(documentSnapshot.getString(Firestore.FIELD_PROFILE_PICTURE_URL));

                    Log.d(TAG, "Data loaded successfully");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load data: " + e.getMessage()));
    }

    private void loadProfilePicture(String profilePictureUrl) {
        Glide.with(ViewProfileActivity.this)
                .load(profilePictureUrl)
                .centerCrop()
                .placeholder(R.drawable.img_profile_picture)
                .into(mProfilePictureView);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void initiatePersonalCall(Contact contact) {
        if (new NetworkInfoUtility(ViewProfileActivity.this).isConnectedToInternet()) {
            if (contact.fcmToken == null || contact.fcmToken.trim().isEmpty()) {
                Toast.makeText(this, contact.firstName + " " + contact.lastName + " is not available right now", Toast.LENGTH_SHORT).show();
            } else {
                startOutgoingCallActivity(contact);
            }
        } else {
            new Snackbars(ViewProfileActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection);
        }
    }

    private void startEditProfileActivity() {
        Intent editProfileIntent = new Intent(ViewProfileActivity.this, EditProfileActivity.class);
        startActivity(editProfileIntent);
    }

    private void startOutgoingCallActivity(Contact callee) {
        Intent outgoingCallIntent = new Intent(ViewProfileActivity.this, OutgoingCallActivity.class);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALL_TYPE, ApiUtility.CALL_TYPE_PERSONAL);
        outgoingCallIntent.putExtra(Extras.EXTRA_CALLEE, callee);
        startActivity(outgoingCallIntent);
    }
}
