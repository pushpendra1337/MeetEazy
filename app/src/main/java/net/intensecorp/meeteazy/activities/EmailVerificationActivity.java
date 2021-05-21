package net.intensecorp.meeteazy.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.NetworkInfoUtility;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;
import net.intensecorp.meeteazy.utils.SnackbarUtility;

public class EmailVerificationActivity extends AppCompatActivity {

    private static final String TAG = EmailVerificationActivity.class.getSimpleName();
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private String mEmail;
    private SharedPrefsManager mSharedPrefsManager;
    private AlertDialog mProgressDialog;
    private AlertDialog mNoInternetDialog;
    private AlertDialog mEmailVerificationDialog;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        MaterialTextView firstNameView = findViewById(R.id.textView_first_name);
        MaterialTextView emailView = findViewById(R.id.textView_email);
        MaterialButton verifyButton = findViewById(R.id.button_verify);
        MaterialTextView signInActivityLink = findViewById(R.id.textView_sign_in);

        mAuth = FirebaseAuth.getInstance();
        mSharedPrefsManager = new SharedPrefsManager(EmailVerificationActivity.this, SharedPrefsManager.PREF_USER_DATA);

        Intent emailVerificationIntent = getIntent();
        String firstName = emailVerificationIntent.getStringExtra(Extras.EXTRA_FIRST_NAME);
        mEmail = emailVerificationIntent.getStringExtra(Extras.EXTRA_EMAIL);

        firstNameView.setText(firstName);
        emailView.setText(mEmail);

        verifyButton.setOnClickListener(v -> sendEmailVerificationLink());

        signInActivityLink.setOnClickListener(v -> {
            mSharedPrefsManager.invalidateSession();
            mAuth.signOut();

            startSignInActivity();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mUser = mAuth.getCurrentUser();

        if (mUser != null) {
            mUser.reload()
                    .addOnSuccessListener(aVoid -> {
                        if (mUser.isEmailVerified()) {
                            Log.d(TAG, "Email address verified: " + mUser.getEmail());

                            dismissEmailVerificationDialog();
                            startHomeActivity();
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "User reload failed: " + e.getMessage()));
        } else {
            dismissEmailVerificationDialog();

            mAuth.signOut();
            mSharedPrefsManager.invalidateSession();
            startSignInActivity();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendEmailVerificationLink() {

        showProgressDialog();

        if (new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {

            mUser = mAuth.getCurrentUser();

            if (mUser != null && !mUser.isEmailVerified())

                mUser.sendEmailVerification()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Email sent to: " + mUser.getEmail());

                            dismissProgressDialog();
                            showEmailVerificationDialog();
                        })
                        .addOnFailureListener(e -> {

                            if (!new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {
                                dismissProgressDialog();
                                showNoInternetDialog();
                            } else {
                                dismissProgressDialog();
                                new SnackbarUtility(EmailVerificationActivity.this).snackbar(R.string.snackbar_text_error_occurred);
                            }

                            Log.e(TAG, "Email not sent: " + e.getMessage());
                        });
        } else {
            dismissProgressDialog();
            showNoInternetDialog();
        }
    }

    private void startHomeActivity() {
        Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
        finish();
    }

    private void startSignInActivity() {
        Intent signInIntent = new Intent(getApplicationContext(), SignInActivity.class);
        signInIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(signInIntent);
        finish();
    }

    private void startWirelessSettingsActivity() {
        Intent wirelessSettingsIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        wirelessSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(wirelessSettingsIntent);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EmailVerificationActivity.this);
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

    private void showNoInternetDialog() {
        if (mNoInternetDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EmailVerificationActivity.this);
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

    private void showEmailVerificationDialog() {
        if (mEmailVerificationDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EmailVerificationActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_email_verification, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mEmailVerificationDialog = builder.create();
            mEmailVerificationDialog.setCancelable(false);

            if (mEmailVerificationDialog.getWindow() != null) {
                mEmailVerificationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            MaterialTextView emailView = view.findViewById(R.id.textView_email);
            emailView.setText(mEmail);

            view.findViewById(R.id.button_ok).setOnClickListener(v -> dismissEmailVerificationDialog());
        }

        mEmailVerificationDialog.show();
    }

    private void dismissEmailVerificationDialog() {
        if (mEmailVerificationDialog != null) {
            mEmailVerificationDialog.dismiss();
        }
    }
}