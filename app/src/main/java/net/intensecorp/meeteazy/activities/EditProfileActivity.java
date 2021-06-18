package net.intensecorp.meeteazy.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
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
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.Firestore;
import net.intensecorp.meeteazy.utils.NetworkInfoUtility;
import net.intensecorp.meeteazy.utils.Patterns;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;
import net.intensecorp.meeteazy.utils.Snackbars;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = EditProfileActivity.class.getSimpleName();
    private static final String CHILD_PROFILE_PICTURES = "profile_pictures";
    private boolean mIsEmailUpdated = false;
    private CircleImageView mProfilePictureView;
    private MaterialTextView mFirstNameView;
    private MaterialTextView mLastNameView;
    private MaterialTextView mAboutView;
    private FirebaseAuth mAuth;
    private FirebaseStorage mStorage;
    private DocumentReference mUserReference;
    private AlertDialog mEditFirstNameDialog;
    private TextInputLayout mFirstNameLayout;
    private TextInputEditText mFirstNameField;
    private AlertDialog mEditLastNameDialog;
    private TextInputLayout mLastNameLayout;
    private TextInputEditText mLastNameField;
    private AlertDialog mChangeEmailDialog;
    private TextInputLayout mNewEmailLayout;
    private TextInputEditText mNewEmailField;
    private TextInputLayout mConfirmPasswordLayout;
    private TextInputEditText mConfirmPasswordField;
    private AlertDialog mChangePasswordDialog;
    private TextInputLayout mOldPasswordLayout;
    private TextInputEditText mOldPasswordField;
    private TextInputLayout mNewPasswordLayout;
    private TextInputEditText mNewPasswordField;
    private AlertDialog mEditAboutDialog;
    private TextInputLayout mAboutLayout;
    private TextInputEditText mAboutField;
    private AlertDialog mGrantPermissionsDialog;
    private AlertDialog mNoInternetDialog;
    private AlertDialog mProgressDialog;
    private AlertDialog mSigningOutDialog;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        MaterialToolbar materialToolbar = findViewById(R.id.materialToolbar_edit_profile);
        mProfilePictureView = findViewById(R.id.circleImageView_profile_picture);
        mFirstNameView = findViewById(R.id.textView_first_name_holder);
        mLastNameView = findViewById(R.id.textView_last_name_holder);
        MaterialTextView emailView = findViewById(R.id.textView_email_holder);
        mAboutView = findViewById(R.id.textView_about_holder);
        FloatingActionButton updatePictureButton = findViewById(R.id.floatingActionButton_update_picture);
        ImageView editFirstNameButton = findViewById(R.id.imageView_edit_first_name);
        ImageView editLastNameButton = findViewById(R.id.imageView_edit_last_name);
        ImageView editEmailButton = findViewById(R.id.imageView_edit_email);
        ImageView editPasswordButton = findViewById(R.id.imageView_edit_password);
        ImageView editAboutButton = findViewById(R.id.imageView_edit_about);

        setSupportActionBar(materialToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        materialToolbar.setNavigationOnClickListener(v -> onBackPressed());

        mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore store = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        mUserReference = store.collection(Firestore.COLLECTION_USERS).document(uid);

        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(EditProfileActivity.this, SharedPrefsManager.PREF_USER_DATA);
        HashMap<String, String> userData = sharedPrefsManager.getUserDataPrefs();

        mFirstNameView.setText(userData.get(SharedPrefsManager.PREF_FIRST_NAME));
        mLastNameView.setText(userData.get(SharedPrefsManager.PREF_LAST_NAME));
        emailView.setText(mAuth.getCurrentUser().getEmail());
        mAboutView.setText(userData.get(SharedPrefsManager.PREF_ABOUT));

        loadProfilePicture(userData.get(SharedPrefsManager.PREF_PROFILE_PICTURE_URL));

        getFreshData();

        updatePictureButton.setOnClickListener(v -> {

            ImagePickerActivity.clearCache(EditProfileActivity.this);

            Dexter.withContext(EditProfileActivity.this)
                    .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if (report.areAllPermissionsGranted()) {
                                showSetProfilePictureBottomSheetDialog();
                            }

                            if (report.isAnyPermissionPermanentlyDenied()) {
                                showGrantPermissionsDialog();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    })
                    .check();
        });

        editFirstNameButton.setOnClickListener(v -> showEditFirstNameDialog());

        editLastNameButton.setOnClickListener(v -> showEditLastNameDialog());

        editEmailButton.setOnClickListener(v -> showChangeEmailDialog());

        editPasswordButton.setOnClickListener(v -> showChangePasswordDialog());

        editAboutButton.setOnClickListener(v -> showEditAboutDialog());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsEmailUpdated) {
            signOut();
        }
    }

    private void getFreshData() {
        mUserReference.get()
                .addOnSuccessListener(documentSnapshot -> {
                    String firstName = documentSnapshot.getString(Firestore.FIELD_FIRST_NAME);
                    String lastName = documentSnapshot.getString(Firestore.FIELD_LAST_NAME);
                    String about = documentSnapshot.getString(Firestore.FIELD_ABOUT);
                    String profilePictureUrl = documentSnapshot.getString(Firestore.FIELD_PROFILE_PICTURE_URL);

                    mFirstNameView.setText(firstName);
                    mLastNameView.setText(lastName);
                    mAboutView.setText(about);
                    loadProfilePicture(profilePictureUrl);

                    SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(EditProfileActivity.this, SharedPrefsManager.PREF_USER_DATA);
                    sharedPrefsManager.setUserDataPrefs(firstName, lastName, about, profilePictureUrl);

                    Log.d(TAG, "Data successfully loaded.");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load data: " + e.getMessage()));
    }

    private void loadProfilePicture(String profilePictureUrl) {
        Log.d(TAG, "Image path: " + profilePictureUrl);

        Glide.with(getBaseContext())
                .load(profilePictureUrl)
                .centerCrop()
                .placeholder(R.drawable.img_profile_picture)
                .into(mProfilePictureView);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateProfilePicture(Uri profilePictureUri) {
        showProgressDialog();

        if (new NetworkInfoUtility(EditProfileActivity.this).isConnectedToInternet()) {

            StorageReference photoRef = mStorage.getReference().child(CHILD_PROFILE_PICTURES).child(UUID.randomUUID() + ".jpg");

            photoRef.putFile(profilePictureUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Profile picture successfully uploaded.");

                        photoRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    Log.d(TAG, "Profile picture URL successfully fetched.");

                                    Map<String, Object> profilePictureMap = new HashMap<>();
                                    profilePictureMap.put(Firestore.FIELD_PROFILE_PICTURE_URL, uri.toString());

                                    mUserReference.update(profilePictureMap)
                                            .addOnSuccessListener(aVoid -> {
                                                SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(EditProfileActivity.this, SharedPrefsManager.PREF_USER_DATA);
                                                String oldProfilePictureUrl = sharedPrefsManager.getProfilePictureUrlPref();
                                                sharedPrefsManager.setProfilePicturePref(uri.toString());

                                                if (oldProfilePictureUrl != null && !oldProfilePictureUrl.equals("")) {
                                                    deleteOldProfilePicture(oldProfilePictureUrl);
                                                }

                                                dismissProgressDialog();
                                                new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_profile_updated_successfully);

                                                Log.d(TAG, "Profile picture successfully updated.");
                                            })
                                            .addOnFailureListener(e -> {
                                                dismissProgressDialog();
                                                new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);

                                                Log.e(TAG, "Failed to update profile picture: " + e.getMessage());
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    dismissProgressDialog();
                                    new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);

                                    Log.e(TAG, "Failed to fetch profile picture URL: " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();
                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);

                        Log.e(TAG, "Failed to upload profile picture: " + e.getMessage());
                    });
        } else {
            dismissProgressDialog();
            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection);
        }
    }

    private void deleteOldProfilePicture(String oldProfilePictureUrl) {
        StorageReference photoRef = mStorage.getReferenceFromUrl(oldProfilePictureUrl);

        photoRef.delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Old profile picture successfully deleted."))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete old profile picture: " + e.getMessage()));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void removeCurrentPhoto() {
        showProgressDialog();

        if (new NetworkInfoUtility(EditProfileActivity.this).isConnectedToInternet()) {

            SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(EditProfileActivity.this, SharedPrefsManager.PREF_USER_DATA);
            String currentProfilePictureUrl = sharedPrefsManager.getProfilePictureUrlPref();

            if (currentProfilePictureUrl != null && !currentProfilePictureUrl.equals("")) {

                Map<String, Object> profilePictureMap = new HashMap<>();
                profilePictureMap.put(Firestore.FIELD_PROFILE_PICTURE_URL, Firestore.DEFAULT_VALUE_PROFILE_PICTURE_URL);

                mUserReference.update(profilePictureMap)
                        .addOnSuccessListener(aVoid -> {
                            StorageReference photoRef = mStorage.getReferenceFromUrl(currentProfilePictureUrl);

                            photoRef.delete()
                                    .addOnSuccessListener(aVoid1 -> {
                                        sharedPrefsManager.setProfilePicturePref("");
                                        dismissProgressDialog();

                                        mProfilePictureView.setImageResource(R.drawable.img_profile_picture);

                                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_profile_updated_successfully);

                                        Log.d(TAG, "Profile picture successfully deleted from storage.");
                                    })
                                    .addOnFailureListener(e -> {
                                        dismissProgressDialog();

                                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);
                                        Log.e(TAG, "Failed to delete profile picture from storage: " + e.getMessage());
                                    });

                            Log.d(TAG, "Profile picture URL successfully updated.");
                        })
                        .addOnFailureListener(e -> {
                            dismissProgressDialog();
                            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);

                            Log.e(TAG, "Failed to update profile picture URL: " + e.getMessage());
                        });
            }
        } else {
            dismissProgressDialog();
            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection);
        }
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
        String newEmail = Objects.requireNonNull(mNewEmailLayout.getEditText()).getText().toString().trim();

        if (newEmail.isEmpty()) {
            mNewEmailLayout.setError(getString(R.string.error_empty_email));
            putFocusOn(mNewEmailField);
        } else {
            mNewEmailLayout.setErrorEnabled(false);
            return newEmail;
        }

        return null;
    }

    private boolean isEmailValid() {
        if (getEmail() != null) {
            Matcher emailMatcher = Patterns.EMAIL_PATTERN.matcher(getEmail());

            if (emailMatcher.matches()) {
                mNewEmailLayout.setErrorEnabled(false);
                return true;
            } else {
                mNewEmailLayout.setError(getString(R.string.error_invalid_email));
                putFocusOn(mNewEmailField);
                return false;
            }
        } else {
            return false;
        }
    }

    private String getPassword(TextInputLayout mPasswordLayout, TextInputEditText mPasswordField, boolean checkValidity) {
        String password = Objects.requireNonNull(mPasswordLayout.getEditText()).getText().toString().trim();
        Matcher passwordMatcher = Patterns.PASSWORD_PATTERN.matcher(password);

        if (password.isEmpty()) {
            mPasswordLayout.setError(getString(R.string.error_empty_password));
            putFocusOn(mPasswordField);
            return null;
        } else {
            if (checkValidity) {
                if (passwordMatcher.matches()) {
                    mPasswordLayout.setHelperTextEnabled(false);
                    return password;
                } else {
                    mPasswordLayout.setHelperText(getString(R.string.error_invalid_password));
                    putFocusOn(mPasswordField);
                    return null;
                }
            } else {
                mPasswordLayout.setHelperTextEnabled(false);
                mPasswordLayout.setErrorEnabled(false);
                return password;
            }
        }
    }

    private String getAbout() {
        String about = Objects.requireNonNull(mAboutLayout.getEditText()).getText().toString().trim();

        if (about.isEmpty()) {
            mAboutLayout.setError(getString(R.string.error_empty_about));
            putFocusOn(mAboutField);
            return null;
        } else {
            if (about.length() <= 256) {
                mAboutLayout.setErrorEnabled(false);
                return about;
            } else {
                mAboutLayout.setError(getString(R.string.error_invalid_about));
                putFocusOn(mAboutField);
                return null;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateFirstName() {
        showProgressDialog();

        if (new NetworkInfoUtility(EditProfileActivity.this).isConnectedToInternet()) {

            Map<String, Object> firstNameMap = new HashMap<>();
            firstNameMap.put(Firestore.FIELD_FIRST_NAME, getFirstName());

            mUserReference.update(firstNameMap)
                    .addOnSuccessListener(aVoid -> {
                        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(EditProfileActivity.this, SharedPrefsManager.PREF_USER_DATA);
                        sharedPrefsManager.setFirstNamePref(getFirstName());

                        mFirstNameView.setText(getFirstName());

                        dismissProgressDialog();
                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_profile_updated_successfully);

                        Log.d(TAG, "First name successfully updated.");
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();
                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);

                        Log.e(TAG, "Failed to update first name: " + e.getMessage());
                    });
        } else {
            dismissProgressDialog();
            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateLastName() {
        showProgressDialog();

        if (new NetworkInfoUtility(EditProfileActivity.this).isConnectedToInternet()) {

            Map<String, Object> lastNameMap = new HashMap<>();
            lastNameMap.put(Firestore.FIELD_LAST_NAME, getLastName());

            mUserReference.update(lastNameMap)
                    .addOnSuccessListener(aVoid -> {
                        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(EditProfileActivity.this, SharedPrefsManager.PREF_USER_DATA);
                        sharedPrefsManager.setLastNamePref(getLastName());

                        mLastNameView.setText(getLastName());

                        dismissProgressDialog();
                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_profile_updated_successfully);

                        Log.d(TAG, "Last name successfully updated.");
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();
                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);

                        Log.e(TAG, "Failed to update last name: " + e.getMessage());
                    });
        } else {
            dismissProgressDialog();
            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateEmail() {
        showProgressDialog();

        if (new NetworkInfoUtility(EditProfileActivity.this).isConnectedToInternet()) {

            FirebaseUser user = mAuth.getCurrentUser();
            AuthCredential authCredential = EmailAuthProvider.getCredential(Objects.requireNonNull(Objects.requireNonNull(user).getEmail()), Objects.requireNonNull(getPassword(mConfirmPasswordLayout, mConfirmPasswordField, false)));
            user.reauthenticate(authCredential)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User successfully reauthenticated.");
                        user.verifyBeforeUpdateEmail(Objects.requireNonNull(getEmail()))
                                .addOnSuccessListener(aVoid1 -> {
                                    dismissProgressDialog();
                                    showSigningOutDialog();

                                    mIsEmailUpdated = true;

                                    Log.d(TAG, "Verification email successfully sent.");
                                })
                                .addOnFailureListener(e -> {
                                    dismissProgressDialog();

                                    if (e instanceof FirebaseAuthUserCollisionException) {
                                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_email_already_in_use);
                                    } else if (e instanceof FirebaseAuthEmailException) {
                                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_failed_to_send_email);
                                    } else {
                                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);
                                    }

                                    Log.e(TAG, "Can't send verification email: " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();

                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_authentication_failed_wrong_credentials);
                        } else {
                            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);
                        }

                        Log.e(TAG, "User authentication failed: " + e.getMessage());
                    });
        } else {
            dismissProgressDialog();
            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updatePassword() {
        showProgressDialog();

        if (new NetworkInfoUtility(EditProfileActivity.this).isConnectedToInternet()) {
            FirebaseUser user = mAuth.getCurrentUser();
            AuthCredential authCredential = EmailAuthProvider.getCredential(Objects.requireNonNull(Objects.requireNonNull(user).getEmail()), Objects.requireNonNull(getPassword(mOldPasswordLayout, mOldPasswordField, false)));

            user.reauthenticate(authCredential)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User successfully reauthenticated.");

                        user.updatePassword(Objects.requireNonNull(getPassword(mNewPasswordLayout, mNewPasswordField, true)))
                                .addOnSuccessListener(aVoid1 -> {
                                    dismissProgressDialog();
                                    new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_password_changed_successfully);

                                    Log.d(TAG, "Password successfully updated.");
                                })
                                .addOnFailureListener(e -> {
                                    dismissProgressDialog();
                                    new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);

                                    Log.e(TAG, "Failed to update password: " + e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();

                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_authentication_failed_wrong_credentials);
                        } else {
                            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);
                        }

                        Log.e(TAG, "User authentication failed: " + e.getMessage());
                    });
        } else {
            dismissProgressDialog();
            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateAbout() {
        showProgressDialog();

        if (new NetworkInfoUtility(EditProfileActivity.this).isConnectedToInternet()) {

            Map<String, Object> aboutMap = new HashMap<>();
            aboutMap.put(Firestore.FIELD_ABOUT, getAbout());

            mUserReference.update(aboutMap)
                    .addOnSuccessListener(aVoid -> {
                        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(EditProfileActivity.this, SharedPrefsManager.PREF_USER_DATA);
                        sharedPrefsManager.setAboutPref(getAbout());

                        mAboutView.setText(getAbout());

                        dismissProgressDialog();
                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_profile_updated_successfully);

                        Log.d(TAG, "About successfully updated.");
                    })
                    .addOnFailureListener(e -> {
                        dismissProgressDialog();
                        new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_error_occurred);

                        Log.e(TAG, "Failed to update about: " + e.getMessage());
                    });
        } else {
            dismissProgressDialog();
            new Snackbars(EditProfileActivity.this).snackbar(R.string.snackbar_text_check_your_internet_connection);
        }
    }

    private void signOut() {
        if (new NetworkInfoUtility(EditProfileActivity.this).isConnectedToInternet()) {
            showProgressDialog();

            mUserReference.update(Firestore.FIELD_FCM_TOKEN, FieldValue.delete())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully deleted from database"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete token from database: " + e.getMessage()));

            FirebaseMessaging.getInstance()
                    .deleteToken()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully deleted from device"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete token from device: " + e.getMessage()));

            mAuth.signOut();

            SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(EditProfileActivity.this, SharedPrefsManager.PREF_USER_DATA);
            sharedPrefsManager.invalidateSession();

            dismissProgressDialog();
            startSignInActivity();
        } else {
            dismissProgressDialog();
            showNoInternetDialog();
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

    private void showSetProfilePictureBottomSheetDialog() {
        ImagePickerActivity.showSetProfilePictureBottomSheetDialog(EditProfileActivity.this, new ImagePickerActivity.OptionListener() {
            @Override
            public void onTakeAPhotoSelected() {
                startImagePickerActivity(ImagePickerActivity.REQUEST_CAMERA);
            }

            @Override
            public void onSelectFromGallerySelected() {
                startImagePickerActivity(ImagePickerActivity.REQUEST_GALLERY);
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onRemoveCurrentPhotoSelected() {
                removeCurrentPhoto();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showChangeEmailDialog() {
        if (mChangeEmailDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_email, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mChangeEmailDialog = builder.create();

            if (mChangeEmailDialog.getWindow() != null) {
                mChangeEmailDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            ImageView closeButton = view.findViewById(R.id.imageView_close);
            mNewEmailLayout = view.findViewById(R.id.textInputLayout_new_email);
            mNewEmailField = view.findViewById(R.id.textInputEditText_new_email);
            mConfirmPasswordLayout = view.findViewById(R.id.textInputLayout_confirm_password);
            mConfirmPasswordField = view.findViewById(R.id.textInputEditText_confirm_password);
            MaterialButton saveButton = view.findViewById(R.id.button_save);

            mNewEmailField.addTextChangedListener(new ValidationWatcher(mNewEmailField));
            mConfirmPasswordField.addTextChangedListener(new ValidationWatcher(mConfirmPasswordField));

            closeButton.setOnClickListener(v -> {
                hideSoftInput();
                dismissChangeEmailDialog();
            });

            saveButton.setOnClickListener(v -> {
                if (getEmail() != null && isEmailValid()) {
                    hideSoftInput();
                    dismissChangeEmailDialog();
                    updateEmail();
                }
            });
        }

        mChangeEmailDialog.show();
    }

    private void dismissChangeEmailDialog() {
        if (mChangeEmailDialog != null) {
            mChangeEmailDialog.dismiss();
            mChangeEmailDialog = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showChangePasswordDialog() {
        if (mChangePasswordDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mChangePasswordDialog = builder.create();

            if (mChangePasswordDialog.getWindow() != null) {
                mChangePasswordDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            ImageView closeButton = view.findViewById(R.id.imageView_close);
            mOldPasswordLayout = view.findViewById(R.id.textInputLayout_old_password);
            mOldPasswordField = view.findViewById(R.id.textInputEditText_old_password);
            mNewPasswordLayout = view.findViewById(R.id.textInputLayout_new_password);
            mNewPasswordField = view.findViewById(R.id.textInputEditText_new_password);
            MaterialButton saveButton = view.findViewById(R.id.button_save);

            mOldPasswordField.addTextChangedListener(new ValidationWatcher(mOldPasswordField));
            mNewPasswordField.addTextChangedListener(new ValidationWatcher(mNewPasswordField));

            closeButton.setOnClickListener(v -> {
                hideSoftInput();
                dismissChangePasswordDialog();
            });

            saveButton.setOnClickListener(v -> {
                if (getPassword(mOldPasswordLayout, mOldPasswordField, false) != null && getPassword(mNewPasswordLayout, mNewPasswordField, true) != null) {
                    hideSoftInput();
                    dismissChangePasswordDialog();
                    updatePassword();
                }
            });
        }

        mChangePasswordDialog.show();
    }

    private void dismissChangePasswordDialog() {
        if (mChangePasswordDialog != null) {
            mChangePasswordDialog.dismiss();
            mChangePasswordDialog = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showEditAboutDialog() {
        if (mEditAboutDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_about, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mEditAboutDialog = builder.create();

            if (mEditAboutDialog.getWindow() != null) {
                mEditAboutDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            ImageView closeButton = view.findViewById(R.id.imageView_close);
            mAboutLayout = view.findViewById(R.id.textInputLayout_about);
            mAboutField = view.findViewById(R.id.textInputEditText_about);
            MaterialButton saveButton = view.findViewById(R.id.button_save);

            mAboutField.setText(mAboutView.getText());

            mAboutField.addTextChangedListener(new ValidationWatcher(mAboutField));

            closeButton.setOnClickListener(v -> {
                hideSoftInput();
                dismissEditAboutDialog();
            });

            saveButton.setOnClickListener(v -> {
                if (getAbout() != null) {
                    hideSoftInput();
                    dismissEditAboutDialog();
                    updateAbout();
                }
            });
        }

        mEditAboutDialog.show();
    }

    private void dismissEditAboutDialog() {
        if (mEditAboutDialog != null) {
            mEditAboutDialog.dismiss();
            mEditAboutDialog = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showEditFirstNameDialog() {
        if (mEditFirstNameDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_first_name, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mEditFirstNameDialog = builder.create();

            if (mEditFirstNameDialog.getWindow() != null) {
                mEditFirstNameDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            ImageView closeButton = view.findViewById(R.id.imageView_close);
            mFirstNameLayout = view.findViewById(R.id.textInputLayout_first_name);
            mFirstNameField = view.findViewById(R.id.textInputEditText_first_name);
            MaterialButton saveButton = view.findViewById(R.id.button_save);

            mFirstNameField.setText(mFirstNameView.getText());

            mFirstNameField.addTextChangedListener(new ValidationWatcher(mFirstNameField));

            closeButton.setOnClickListener(v -> {
                hideSoftInput();
                dismissEditFirstNameDialog();
            });

            saveButton.setOnClickListener(v -> {
                if (getFirstName() != null) {
                    hideSoftInput();
                    dismissEditFirstNameDialog();
                    updateFirstName();
                }
            });
        }

        mEditFirstNameDialog.show();
    }

    private void dismissEditFirstNameDialog() {
        if (mEditFirstNameDialog != null) {
            mEditFirstNameDialog.dismiss();
            mEditFirstNameDialog = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showEditLastNameDialog() {
        if (mEditLastNameDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_last_name, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mEditLastNameDialog = builder.create();

            if (mEditLastNameDialog.getWindow() != null) {
                mEditLastNameDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            ImageView closeButton = view.findViewById(R.id.imageView_close);
            mLastNameLayout = view.findViewById(R.id.textInputLayout_last_name);
            mLastNameField = view.findViewById(R.id.textInputEditText_last_name);
            MaterialButton saveButton = view.findViewById(R.id.button_save);

            mLastNameField.setText(mLastNameView.getText());

            mLastNameField.addTextChangedListener(new ValidationWatcher(mLastNameField));

            closeButton.setOnClickListener(v -> {
                hideSoftInput();
                dismissEditLastNameDialog();
            });

            saveButton.setOnClickListener(v -> {
                if (getLastName() != null) {
                    hideSoftInput();
                    dismissEditLastNameDialog();
                    updateLastName();
                }
            });
        }

        mEditLastNameDialog.show();
    }

    private void dismissEditLastNameDialog() {
        if (mEditLastNameDialog != null) {
            mEditLastNameDialog.dismiss();
            mEditLastNameDialog = null;
        }
    }

    private void showGrantPermissionsDialog() {
        if (mGrantPermissionsDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_grant_permissions, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mGrantPermissionsDialog = builder.create();

            if (mGrantPermissionsDialog.getWindow() != null) {
                mGrantPermissionsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.button_cancel).setOnClickListener(v -> dismissGrantPermissionsDialog());

            view.findViewById(R.id.button_settings).setOnClickListener(v -> {
                dismissGrantPermissionsDialog();
                startAppSettingsActivity();
            });
        }

        mGrantPermissionsDialog.show();
    }

    private void dismissGrantPermissionsDialog() {
        if (mGrantPermissionsDialog != null) {
            mGrantPermissionsDialog.dismiss();
        }
    }

    private void showNoInternetDialog() {
        if (mNoInternetDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
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

            AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
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

    private void showSigningOutDialog() {
        if (mSigningOutDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_signing_out, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mSigningOutDialog = builder.create();
            mSigningOutDialog.setCancelable(false);

            if (mSigningOutDialog.getWindow() != null) {
                mSigningOutDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.button_ok).setOnClickListener(v -> {
                dismissSigningOutDialog();
                signOut();
            });
        }

        mSigningOutDialog.show();
    }

    private void dismissSigningOutDialog() {
        if (mSigningOutDialog != null) {
            mSigningOutDialog.dismiss();
        }
    }

    private void startAppSettingsActivity() {
        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        appSettingsIntent.setData(uri);
        startActivityForResult(appSettingsIntent, 101);
    }

    private void startSignInActivity() {
        Intent signInIntent = new Intent(EditProfileActivity.this, SignInActivity.class);
        signInIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(signInIntent);
        finish();
    }

    private void startWirelessSettingsActivity() {
        Intent wirelessSettingsIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        wirelessSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(wirelessSettingsIntent);
    }

    private void startImagePickerActivity(int requestCode) {
        Intent imagePickerIntent = new Intent(EditProfileActivity.this, ImagePickerActivity.class);
        imagePickerIntent.putExtra(Extras.EXTRA_IMAGE_PICKER_REQUEST, requestCode);
        imagePickerIntent.putExtra(Extras.EXTRA_IS_ASPECT_RATIO_LOCKED, true);
        imagePickerIntent.putExtra(Extras.EXTRA_ASPECT_RATIO_X, 1);
        imagePickerIntent.putExtra(Extras.EXTRA_ASPECT_RATIO_Y, 1);

        if (requestCode == ImagePickerActivity.REQUEST_CAMERA) {
            imagePickerIntent.putExtra(Extras.EXTRA_IS_BITMAP_HAS_CUSTOM_SIZE, true);
            imagePickerIntent.putExtra(Extras.EXTRA_BITMAP_MAX_WIDTH, 1000);
            imagePickerIntent.putExtra(Extras.EXTRA_BITMAP_MAX_HEIGHT, 1000);
        }

        startActivityForResult(imagePickerIntent, ImagePickerActivity.REQUEST_IMAGE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ImagePickerActivity.REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri imageUri = Objects.requireNonNull(data).getParcelableExtra("path");
                loadProfilePicture(imageUri.toString());
                updateProfilePicture(imageUri);
            }
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
                case R.id.textInputEditText_first_name:
                    getFirstName();
                    break;
                case R.id.textInputEditText_last_name:
                    getLastName();
                    break;
                case R.id.textInputEditText_new_email:
                    getEmail();
                    break;
                case R.id.textInputEditText_confirm_password:
                    getPassword(mConfirmPasswordLayout, mConfirmPasswordField, false);
                    break;
                case R.id.textInputEditText_old_password:
                    getPassword(mOldPasswordLayout, mOldPasswordField, false);
                    break;
                case R.id.textInputEditText_new_password:
                    getPassword(mNewPasswordLayout, mNewPasswordField, true);
                    break;
                case R.id.textInputEditText_about:
                    getAbout();
                    break;
                default:
                    break;
            }
        }
    }
}
