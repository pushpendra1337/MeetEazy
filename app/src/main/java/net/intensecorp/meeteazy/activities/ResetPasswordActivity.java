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
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.NetworkInfoUtility;
import net.intensecorp.meeteazy.utils.Patterns;
import net.intensecorp.meeteazy.utils.Snackbars;

import java.util.Objects;
import java.util.regex.Matcher;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = ResetPasswordActivity.class.getSimpleName();
    private TextInputLayout mEmailLayout;
    private TextInputEditText mEmailField;
    private FirebaseAuth mAuth;
    private AlertDialog mNoInternetDialog;
    private AlertDialog mProgressDialog;
    private AlertDialog mResetPasswordDialog;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mEmailLayout = findViewById(R.id.textInputLayout_registered_email);
        mEmailField = findViewById(R.id.textInputEditText_registered_email);
        MaterialButton sendLinkButton = findViewById(R.id.button_send_link);
        MaterialTextView signInLink = findViewById(R.id.textView_sign_in);

        mEmailField.addTextChangedListener(new ValidationWatcher(mEmailField));

        mAuth = FirebaseAuth.getInstance();

        sendLinkButton.setOnClickListener(v -> {
            hideSoftInput();
            sendPasswordResetLink();
        });

        signInLink.setOnClickListener(v -> startSignInActivity());
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
            Matcher emailMatcher = Patterns.EMAIL_PATTERN.matcher(getEmail());

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendPasswordResetLink() {
        if (isEmailValid() && getEmail() != null) {

            showProgressDialog();

            if (new NetworkInfoUtility(ResetPasswordActivity.this).isConnectedToInternet()) {

                mAuth.sendPasswordResetEmail(getEmail())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Email sent to: " + getEmail());

                            dismissProgressDialog();
                            showResetPasswordDialog();
                        })
                        .addOnFailureListener(e -> {
                            dismissProgressDialog();

                            if (e instanceof FirebaseAuthInvalidUserException) {
                                new Snackbars(ResetPasswordActivity.this).snackbar(R.string.snackbar_text_email_not_registered);
                            } else if (e instanceof FirebaseAuthEmailException) {
                                new Snackbars(ResetPasswordActivity.this).snackbar(R.string.snackbar_text_failed_to_send_email);
                            } else if (!new NetworkInfoUtility(ResetPasswordActivity.this).isConnectedToInternet()) {
                                showNoInternetDialog();
                            } else {
                                new Snackbars(ResetPasswordActivity.this).snackbar(R.string.snackbar_text_error_occurred);
                            }

                            Log.e(TAG, "Email not sent: " + e.getMessage());
                        });
            } else {
                dismissProgressDialog();
                showNoInternetDialog();
            }
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

    private void showNoInternetDialog() {
        if (mNoInternetDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(ResetPasswordActivity.this);
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

    private void showProgressDialog() {
        if (mProgressDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(ResetPasswordActivity.this);
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

    private void showResetPasswordDialog() {
        if (mResetPasswordDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(ResetPasswordActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mResetPasswordDialog = builder.create();

            if (mResetPasswordDialog.getWindow() != null) {
                mResetPasswordDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.button_ok).setOnClickListener(v -> {
                dismissResetPasswordDialog();
                startSignInActivity();
            });

            view.findViewById(R.id.button_change_email).setOnClickListener(v -> {
                dismissResetPasswordDialog();

                putFocusOn(mEmailField);
                Objects.requireNonNull(mEmailLayout.getEditText()).getText().clear();
            });
        }

        mResetPasswordDialog.show();
    }

    private void dismissResetPasswordDialog() {
        if (mResetPasswordDialog != null) {
            mResetPasswordDialog.dismiss();
        }
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
            if (view.getId() == R.id.textInputEditText_email) {
                getEmail();
            }
        }
    }
}
