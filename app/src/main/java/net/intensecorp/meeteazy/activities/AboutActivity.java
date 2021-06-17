package net.intensecorp.meeteazy.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.FirebaseFirestore;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.Firestore;

import java.util.Objects;

public class AboutActivity extends AppCompatActivity {

    private static final String TAG = AboutActivity.class.getSimpleName();
    private static final String[] CREATOR_EMAIL = {"pushpendray1337@gmail.com"};
    private static final String EMAIL_TYPE = "text/html";
    private static final String EMAIL_HANDLER_PACKAGE = "com.google.android.gm";
    private MaterialTextView mCreatorView;
    private MaterialTextView mEmailView;
    private MaterialTextView mWebsiteView;
    private MaterialTextView mLinkedInView;
    private MaterialTextView mGitHubView;
    private MaterialTextView mInstagramView;
    private MaterialTextView mTwitterView;
    private MaterialTextView mFacebookView;
    private FirebaseFirestore mStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar materialToolbar = findViewById(R.id.materialToolbar_about);
        mCreatorView = findViewById(R.id.textView_creator_holder);
        mEmailView = findViewById(R.id.textView_email_holder);
        mWebsiteView = findViewById(R.id.textView_website_holder);
        mLinkedInView = findViewById(R.id.textView_linkedin_holder);
        mGitHubView = findViewById(R.id.textView_github_holder);
        mInstagramView = findViewById(R.id.textView_instagram_holder);
        mTwitterView = findViewById(R.id.textView_twitter_holder);
        mFacebookView = findViewById(R.id.textView_facebook_holder);

        ImageView goToEmailButton = findViewById(R.id.imageView_go_to_email);
        ImageView goToWebsiteButton = findViewById(R.id.imageView_go_to_website);
        ImageView goToLinkedInButton = findViewById(R.id.imageView_go_to_linkedin);
        ImageView goToGitHubButton = findViewById(R.id.imageView_go_to_github);
        ImageView goToInstagramButton = findViewById(R.id.imageView_go_to_instagram);
        ImageView goToTwitterButton = findViewById(R.id.imageView_go_to_twitter);
        ImageView goToFacebookButton = findViewById(R.id.imageView_go_to_facebook);

        setSupportActionBar(materialToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        materialToolbar.setNavigationOnClickListener(v -> onBackPressed());

        mStore = FirebaseFirestore.getInstance();

        geCreatorData();

        goToEmailButton.setOnClickListener(v -> startComposeEmailActivity());

        goToWebsiteButton.setOnClickListener(v -> startBrowserActivity(mWebsiteView.getText().toString()));

        goToLinkedInButton.setOnClickListener(v -> startBrowserActivity("https://www.linkedin.com/in/" + mLinkedInView.getText().toString()));

        goToGitHubButton.setOnClickListener(v -> startBrowserActivity("https://github.com/" + mGitHubView.getText().toString()));

        goToInstagramButton.setOnClickListener(v -> startBrowserActivity("https://www.instagram.com/" + mInstagramView.getText().toString()));

        goToTwitterButton.setOnClickListener(v -> startBrowserActivity("https://twitter.com/" + mTwitterView.getText().toString()));

        goToFacebookButton.setOnClickListener(v -> startBrowserActivity("https://facebook.com/" + mFacebookView.getText().toString()));
    }

    private void geCreatorData() {
        mStore.collection(Firestore.COLLECTION_DEVELOPERS)
                .document(Firestore.DOCUMENT_CREATOR)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    mCreatorView.setText(documentSnapshot.getString(Firestore.FIELD_NAME));
                    mEmailView.setText(documentSnapshot.getString(Firestore.FIELD_EMAIL));
                    mWebsiteView.setText(documentSnapshot.getString(Firestore.FIELD_WEBSITE));
                    mLinkedInView.setText(documentSnapshot.getString(Firestore.FIELD_LINKEDIN));
                    mGitHubView.setText(documentSnapshot.getString(Firestore.FIELD_GITHUB));
                    mInstagramView.setText(documentSnapshot.getString(Firestore.FIELD_INSTAGRAM));
                    mTwitterView.setText(documentSnapshot.getString(Firestore.FIELD_TWITTER));
                    mFacebookView.setText(documentSnapshot.getString(Firestore.FIELD_FACEBOOK));

                    Log.d(TAG, "Data loaded successfully");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load data: " + e.getMessage()));
    }

    private void startBrowserActivity(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void startComposeEmailActivity() {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, CREATOR_EMAIL);
        emailIntent.setType(EMAIL_TYPE);
        emailIntent.setPackage(EMAIL_HANDLER_PACKAGE);
        startActivity(emailIntent);
    }
}
