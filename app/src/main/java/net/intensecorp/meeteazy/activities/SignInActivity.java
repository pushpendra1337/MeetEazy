package net.intensecorp.meeteazy.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.Firestore;
import net.intensecorp.meeteazy.utils.NetworkInfoUtility;
import net.intensecorp.meeteazy.utils.RegExUtility;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;
import net.intensecorp.meeteazy.utils.SnackbarUtility;

import java.util.Objects;
import java.util.regex.Matcher;


public class SignInActivity extends AppCompatActivity {

    private static final String TAG = SignInActivity.class.getSimpleName();
    private TextInputLayout mEmailLayout;
    private TextInputEditText mEmailField;
    private TextInputLayout mPasswordLayout;
    private TextInputEditText mPasswordField;
    private AlertDialog mProgressDialog;
    private AlertDialog mNoInternetDialog;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mStore;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mEmailLayout = findViewById(R.id.textInputLayout_email);
        mPasswordLayout = findViewById(R.id.textInputLayout_password);
        mEmailField = findViewById(R.id.textInputEditText_email);
        mPasswordField = findViewById(R.id.textInputEditText_password);
        MaterialButton signInButton = findViewById(R.id.button_sign_in);
        MaterialTextView signUpLink = findViewById(R.id.textView_sign_up);
        MaterialTextView resetPasswordLink = findViewById(R.id.textView_forgot_password);

        mEmailField.addTextChangedListener(new ValidationWatcher(mEmailField));
        mPasswordField.addTextChangedListener(new ValidationWatcher(mPasswordField));

        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();

        signInButton.setOnClickListener(v -> initSignIn());

        signUpLink.setOnClickListener(v -> startSignUpActivity());

        resetPasswordLink.setOnClickListener(v -> startResetPasswordActivity());
    }

    private String getEmail() {
        String email = Objects.requireNonNull(mEmailLayout.getEditText()).getText().toString().trim();

        if (email.isEmpty()) {
            mEmailLayout.setError(getString(R.string.error_empty_email));
            putFocusOn(mEmailField);
        } else {
            mEmailLayout.setErrorEnabled(false);
            return email;
        }
        return null;
    }

    private boolean isEmailValid() {
        if (getEmail() != null) {
            Matcher emailMatcher = RegExUtility.EMAIL_PATTERN.matcher(getEmail());

            if (emailMatcher.matches()) {
                mEmailLayout.setErrorEnabled(false);
                return true;
            } else {
                mEmailLayout.setError(getString(R.string.error_invalid_email));
                putFocusOn(mEmailField);
                return false;
            }
        } else {
            return false;
        }
    }

    private String getPassword() {
        String password = Objects.requireNonNull(mPasswordLayout.getEditText()).getText().toString().trim();

        if (password.isEmpty()) {
            mPasswordLayout.setError(getResources().getString(R.string.error_empty_password));
            putFocusOn(mPasswordField);
        } else {
            mPasswordLayout.setErrorEnabled(false);
            return password;
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initSignIn() {
        if (isEmailValid() && getEmail() != null && getPassword() != null) {

            hideSoftInput();

            showProgressDialog();

            if (new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {

                mAuth.signInWithEmailAndPassword(getEmail(), getPassword())
                        .addOnSuccessListener(authResult -> {
                            Log.d(TAG, "Signed in to: " + Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
                            getUserData();
                        })
                        .addOnFailureListener(e -> {

                            if (e instanceof FirebaseAuthInvalidUserException) {
                                dismissProgressDialog();
                                new SnackbarUtility(SignInActivity.this).snackbar(R.string.snackbar_text_email_not_registered);
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                dismissProgressDialog();
                                new SnackbarUtility(SignInActivity.this).snackbar(R.string.snackbar_text_login_failed_wrong_credentials);
                            } else if (!new NetworkInfoUtility(getApplicationContext()).isConnectedToInternet()) {
                                dismissProgressDialog();
                                showNoInternetDialog();
                            } else {
                                dismissProgressDialog();
                                new SnackbarUtility(SignInActivity.this).snackbar(R.string.snackbar_text_error_occurred);
                            }

                            Log.e(TAG, "Sign in failed: " + e.getMessage());
                        });
            } else {
                dismissProgressDialog();
                showNoInternetDialog();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void getUserData() {

        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(SignInActivity.this, SharedPrefsManager.PREF_USER_DATA);

        mStore.collection(Firestore.COLLECTION_USERS)
                .document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid())
                .get(Source.SERVER)
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Fetched user data: " + Firestore.COLLECTION_USERS + "/" + mAuth.getCurrentUser().getUid());

                    String firstName = documentSnapshot.getString(Firestore.FIELD_FIRST_NAME);
                    String lastName = documentSnapshot.getString(Firestore.FIELD_LAST_NAME);
                    String email = documentSnapshot.getString(Firestore.FIELD_EMAIL);
                    String about = documentSnapshot.getString(Firestore.FIELD_ABOUT);
                    String profileImageUrl = documentSnapshot.getString(Firestore.FIELD_PROFILE_PICTURE_URL);

                    sharedPrefsManager.setUserDataPrefs(firstName, lastName, email, about, profileImageUrl);

                    if (mAuth.getCurrentUser().isEmailVerified()) {
                        dismissProgressDialog();
                        startHomeActivity();
                    } else {
                        dismissProgressDialog();
                        startEmailVerificationActivity(firstName, email);
                    }
                })
                .addOnFailureListener(e -> {

                    mAuth.signOut();
                    sharedPrefsManager.invalidateSession();

                    dismissProgressDialog();
                    new SnackbarUtility(SignInActivity.this).snackbar(R.string.snackbar_text_error_occurred);

                    Log.e(TAG, "Failed to get user data: " + e.getMessage());
                });
    }

    private void startEmailVerificationActivity(String firstName, String email) {
        Intent emailVerificationIntent = new Intent(getApplicationContext(), EmailVerificationActivity.class);
        emailVerificationIntent.putExtra(Extras.EXTRA_FIRST_NAME, firstName);
        emailVerificationIntent.putExtra(Extras.EXTRA_EMAIL, email);
        emailVerificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(emailVerificationIntent);
        finish();
    }

    private void startHomeActivity() {
        Intent homeIntent = new Intent(SignInActivity.this, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
        finish();
    }

    private void startResetPasswordActivity() {
        Intent resetPasswordIntent = new Intent(getApplicationContext(), ResetPasswordActivity.class);
        startActivity(resetPasswordIntent);
    }

    private void startSignUpActivity() {
        Intent signUpIntent = new Intent(getApplicationContext(), SignUpActivity.class);
        startActivity(signUpIntent);
    }

    private void startWirelessSettingsActivity() {
        Intent wirelessSettingsIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        wirelessSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(wirelessSettingsIntent);
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

    private void showProgressDialog() {
        if (mProgressDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(SignInActivity.this);
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

            AlertDialog.Builder builder = new AlertDialog.Builder(SignInActivity.this);
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

    public class ValidationWatcher implements TextWatcher {

        private final View view;

        private ValidationWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @SuppressLint("NonConstantResourceId")
        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.textInputEditText_email:
                    getEmail();
                    break;
                case R.id.textInputEditText_password:
                    getPassword();
                    break;
            }
        }
    }
}