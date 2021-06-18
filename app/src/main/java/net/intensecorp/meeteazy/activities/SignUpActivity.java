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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.Firestore;
import net.intensecorp.meeteazy.utils.NetworkInfoUtility;
import net.intensecorp.meeteazy.utils.Patterns;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;
import net.intensecorp.meeteazy.utils.Snackbars;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = SignUpActivity.class.getSimpleName();
    private TextInputLayout mFirstNameLayout;
    private TextInputEditText mFirstNameField;
    private TextInputLayout mLastNameLayout;
    private TextInputEditText mLastNameField;
    private TextInputLayout mEmailLayout;
    private TextInputEditText mEmailField;
    private TextInputLayout mPasswordLayout;
    private TextInputEditText mPasswordField;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mStore;
    private FirebaseUser mUser;
    private AlertDialog mNoInternetDialog;
    private AlertDialog mProgressDialog;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mFirstNameLayout = findViewById(R.id.textInputLayout_first_name);
        mLastNameLayout = findViewById(R.id.textInputLayout_last_name);
        mEmailLayout = findViewById(R.id.textInputLayout_email);
        mPasswordLayout = findViewById(R.id.textInputLayout_password);
        mFirstNameField = findViewById(R.id.textInputEditText_first_name);
        mLastNameField = findViewById(R.id.textInputEditText_last_name);
        mEmailField = findViewById(R.id.textInputEditText_email);
        mPasswordField = findViewById(R.id.textInputEditText_password);
        MaterialButton signUpButton = findViewById(R.id.button_sign_up);
        MaterialTextView signInActivityLink = findViewById(R.id.textView_sign_in);

        mFirstNameField.addTextChangedListener(new ValidationWatcher(mFirstNameField));
        mLastNameField.addTextChangedListener(new ValidationWatcher(mLastNameField));
        mEmailField.addTextChangedListener(new ValidationWatcher(mEmailField));
        mPasswordField.addTextChangedListener(new ValidationWatcher(mPasswordField));

        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();

        signUpButton.setOnClickListener(v -> {
            hideSoftInput();
            initSignUp();
        });

        signInActivityLink.setOnClickListener(v -> startSignInActivity());
    }

    private String getFirstName() {
        String firstName = Objects.requireNonNull(mFirstNameLayout.getEditText()).getText().toString().trim();
        Matcher firstNameMatcher = Patterns.FIRST_NAME_PATTERN.matcher(firstName);

        if (firstName.isEmpty()) {
            mFirstNameLayout.setError(getString(R.string.error_empty_first_name));
            putFocusOn(mFirstNameField);
            return null;
        } else {
            if (firstNameMatcher.matches()) {
                mFirstNameLayout.setErrorEnabled(false);
                return firstName;
            } else {
                mFirstNameLayout.setError(getString(R.string.error_invalid_first_name));
                putFocusOn(mFirstNameField);
                return null;
            }
        }
    }

    private String getLastName() {
        String lastName = Objects.requireNonNull(mLastNameLayout.getEditText()).getText().toString().trim();
        Matcher lastNameMatcher = Patterns.LAST_NAME_PATTERN.matcher(lastName);

        if (lastName.isEmpty()) {
            mLastNameLayout.setError(getString(R.string.error_empty_last_name));
            putFocusOn(mLastNameField);
            return null;
        } else {
            if (lastNameMatcher.matches()) {
                mLastNameLayout.setErrorEnabled(false);
                return lastName;
            } else {
                mLastNameLayout.setError(getString(R.string.error_invalid_last_name));
                putFocusOn(mLastNameField);
                return null;
            }
        }
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

    private String getPassword() {
        String password = Objects.requireNonNull(mPasswordLayout.getEditText()).getText().toString().trim();
        Matcher passwordMatcher = Patterns.PASSWORD_PATTERN.matcher(password);

        if (password.isEmpty()) {
            mPasswordLayout.setError(getString(R.string.error_empty_password));
            putFocusOn(mPasswordField);
            return null;
        } else {
            if (passwordMatcher.matches()) {
                mPasswordLayout.setHelperTextEnabled(false);
                return password;
            } else {
                mPasswordLayout.setHelperText(getString(R.string.error_invalid_password));
                putFocusOn(mPasswordField);
                return null;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initSignUp() {
        if (getFirstName() != null && getLastName() != null && isEmailValid() && getEmail() != null && getPassword() != null) {

            showProgressDialog();

            if (new NetworkInfoUtility(SignUpActivity.this).isConnectedToInternet()) {

                mAuth.createUserWithEmailAndPassword(getEmail(), getPassword())
                        .addOnSuccessListener(authResult -> {
                            mUser = mAuth.getCurrentUser();

                            if (mUser != null) {
                                Log.d(TAG, "Signed up with: " + mUser.getEmail());
                                setUserData();
                            }
                        })
                        .addOnFailureListener(e -> {
                            dismissProgressDialog();

                            if (e instanceof FirebaseAuthUserCollisionException) {
                                new Snackbars(SignUpActivity.this).snackbar(R.string.snackbar_text_email_already_in_use);
                            } else if (!new NetworkInfoUtility(SignUpActivity.this).isConnectedToInternet()) {
                                showNoInternetDialog();
                            } else {
                                new Snackbars(SignUpActivity.this).snackbar(R.string.snackbar_text_error_occurred);
                            }

                            Log.e(TAG, "Sign up failed: " + e.getMessage());
                        });
            } else {
                dismissProgressDialog();
                showNoInternetDialog();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setUserData() {

        Map<String, Object> userData = new HashMap<>();
        userData.put(Firestore.FIELD_FIRST_NAME, getFirstName());
        userData.put(Firestore.FIELD_LAST_NAME, getLastName());
        userData.put(Firestore.FIELD_EMAIL, getEmail());
        userData.put(Firestore.FIELD_ABOUT, Firestore.DEFAULT_VALUE_ABOUT);
        userData.put(Firestore.FIELD_PROFILE_PICTURE_URL, Firestore.DEFAULT_VALUE_PROFILE_PICTURE_URL);

        mStore.collection(Firestore.COLLECTION_USERS)
                .document(mUser.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data added: " + Firestore.COLLECTION_USERS + "/" + mUser.getUid());

                    SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(SignUpActivity.this, SharedPrefsManager.PREF_USER_DATA);
                    sharedPrefsManager.setUserDataPrefs(getFirstName(), getLastName(), Firestore.DEFAULT_VALUE_ABOUT, Firestore.DEFAULT_VALUE_PROFILE_PICTURE_URL);

                    dismissProgressDialog();
                    startEmailVerificationActivity();
                })
                .addOnFailureListener(e -> {
                    deleteUser(mUser);
                    Log.e(TAG, "Failed to add user data: " + e.getMessage());
                });

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void deleteUser(FirebaseUser user) {
        user.delete()
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();
                    Log.d(TAG, "User deleted: " + user.getUid());
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Log.e(TAG, "Failed to delete user: " + e.getMessage());
                });
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

            AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
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

            AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
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

    private void startEmailVerificationActivity() {
        Intent emailVerificationIntent = new Intent(SignUpActivity.this, EmailVerificationActivity.class);
        emailVerificationIntent.putExtra(Extras.EXTRA_FIRST_NAME, getFirstName());
        emailVerificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(emailVerificationIntent);
        finish();
    }

    private void startSignInActivity() {
        Intent signInIntent = new Intent(SignUpActivity.this, SignInActivity.class);
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
            switch (view.getId()) {
                case R.id.textInputEditText_first_name:
                    getFirstName();
                    break;
                case R.id.textInputEditText_last_name:
                    getLastName();
                    break;
                case R.id.textInputEditText_email:
                    getEmail();
                    break;
                case R.id.textInputEditText_password:
                    getPassword();
                    break;
                default:
                    break;
            }
        }
    }
}
