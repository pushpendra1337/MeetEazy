package net.intensecorp.meeteazy.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SharedPrefsManager {

    public static final String PREF_ONBOARDING = "PREF_ONBOARDING";
    public static final String PREF_FIRST_RUN = "PREF_FIRST_RUN";
    public static final String PREF_USER_DATA = "PREF_USER_DATA";
    public static final String PREF_FIRST_NAME = "PREF_FIRST_NAME";
    public static final String PREF_LAST_NAME = "PREF_LAST_NAME";
    public static final String PREF_EMAIL = "PREF_EMAIL";
    public static final String PREF_ABOUT = "PREF_ABOUT";
    public static final String PREF_PROFILE_IMAGE_URL = "PREF_PROFILE_IMAGE_URL";
    public static final String PREF_SIGNED_IN = "PREF_SIGNED_IN";
    private final SharedPreferences mSharedPreferences;
    private final SharedPreferences.Editor mSharedPrefsEditor;

    @SuppressLint("CommitPrefEdits")
    public SharedPrefsManager(Context context, String sharedPrefsName) {
        mSharedPreferences = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);
        mSharedPrefsEditor = mSharedPreferences.edit();
    }

    public boolean getOnboardingPrefs() {
        return mSharedPreferences.getBoolean(PREF_FIRST_RUN, true);
    }

    public void setOnboardingPrefs(boolean isFirstRun) {
        mSharedPrefsEditor.putBoolean(PREF_FIRST_RUN, isFirstRun);
        mSharedPrefsEditor.apply();
    }

    public HashMap<String, String> getUserDataPrefs() {
        HashMap<String, String> storedUserData = new HashMap<>();
        storedUserData.put(PREF_FIRST_NAME, mSharedPreferences.getString(PREF_FIRST_NAME, null));
        storedUserData.put(PREF_LAST_NAME, mSharedPreferences.getString(PREF_LAST_NAME, null));
        storedUserData.put(PREF_EMAIL, mSharedPreferences.getString(PREF_EMAIL, null));
        storedUserData.put(PREF_ABOUT, mSharedPreferences.getString(PREF_ABOUT, null));
        storedUserData.put(PREF_PROFILE_IMAGE_URL, mSharedPreferences.getString(PREF_PROFILE_IMAGE_URL, null));
        return storedUserData;
    }

    public void setUserDataPrefs(String firstName, String lastName, String email, String about, String profileImageUrl) {
        mSharedPrefsEditor.putBoolean(PREF_SIGNED_IN, true);
        mSharedPrefsEditor.putString(PREF_FIRST_NAME, firstName);
        mSharedPrefsEditor.putString(PREF_LAST_NAME, lastName);
        mSharedPrefsEditor.putString(PREF_EMAIL, email);
        mSharedPrefsEditor.putString(PREF_ABOUT, about);
        mSharedPrefsEditor.putString(PREF_PROFILE_IMAGE_URL, profileImageUrl);

        mSharedPrefsEditor.apply();
    }

    public boolean getIsSignedIn() {
        return mSharedPreferences.getBoolean(PREF_SIGNED_IN, false);
    }

    public void invalidateSession() {
        mSharedPrefsEditor.clear();
        mSharedPrefsEditor.apply();
    }
}
