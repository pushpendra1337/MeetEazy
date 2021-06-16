package net.intensecorp.meeteazy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;

import java.util.HashMap;

public class SplashScreenActivity extends AppCompatActivity {

    public static final int SPLASH_TIMER = 2000;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        SharedPrefsManager themePrefsManager = new SharedPrefsManager(SplashScreenActivity.this, SharedPrefsManager.PREF_THEME);

        switch (themePrefsManager.getThemePref()) {
            case SharedPrefsManager.PREF_THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case SharedPrefsManager.PREF_THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SharedPrefsManager.PREF_THEME_SYSTEM_DEFAULT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            default:
                break;
        }

        Animation slideDownAnimation = AnimationUtils.loadAnimation(SplashScreenActivity.this, R.anim.anim_fade_in_from_top_to_bottom_1500);
        Animation slideUpAnimation = AnimationUtils.loadAnimation(SplashScreenActivity.this, R.anim.anim_fade_in_from_bottom_to_top_1000);

        ImageView brandLogo = findViewById(R.id.imageView_logo);
        MaterialTextView brandName = findViewById(R.id.textView_brand_name);
        MaterialTextView textMadeWith = findViewById(R.id.textView_made_with);
        ImageView loveIcon = findViewById(R.id.imageView_love);
        MaterialTextView textInIndia = findViewById(R.id.textView_in_india);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        brandLogo.setAnimation(slideDownAnimation);
        brandName.setAnimation(slideUpAnimation);
        textMadeWith.setAnimation(slideUpAnimation);
        loveIcon.setAnimation(slideUpAnimation);
        textInIndia.setAnimation(slideUpAnimation);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            SharedPrefsManager userPrefsManager = new SharedPrefsManager(SplashScreenActivity.this, SharedPrefsManager.PREF_USER_DATA);

            SharedPrefsManager onboardingPrefsManager = new SharedPrefsManager(SplashScreenActivity.this, SharedPrefsManager.PREF_ONBOARDING);
            boolean isFirstRun = onboardingPrefsManager.getOnboardingPrefs();

            if (isFirstRun) {

                if (mUser != null) {
                    mAuth.signOut();
                    userPrefsManager.invalidateSession();
                }

                onboardingPrefsManager.setOnboardingPrefs(false);

                startOnboardingActivity();

            } else if (mUser != null && userPrefsManager.getIsSignedIn() && !mUser.isEmailVerified()) {
                HashMap<String, String> userData = userPrefsManager.getUserDataPrefs();
                String firstName = userData.get(SharedPrefsManager.PREF_FIRST_NAME);
                String email = userData.get(SharedPrefsManager.PREF_EMAIL);

                startEmailVerificationActivity(email, firstName);

            } else if (mUser != null && userPrefsManager.getIsSignedIn() && mUser.isEmailVerified()) {

                startHomeActivity();

            } else {
                mAuth.signOut();
                userPrefsManager.invalidateSession();

                startSignInActivity();
            }
        }, SPLASH_TIMER);
    }

    private void startEmailVerificationActivity(String firstName, String email) {
        Intent emailVerificationIntent = new Intent(SplashScreenActivity.this, EmailVerificationActivity.class);
        emailVerificationIntent.putExtra(Extras.EXTRA_FIRST_NAME, firstName);
        emailVerificationIntent.putExtra(Extras.EXTRA_EMAIL, email);
        emailVerificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(emailVerificationIntent);
        finish();
    }

    private void startHomeActivity() {
        Intent homeIntent = new Intent(SplashScreenActivity.this, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
        finish();
    }

    private void startOnboardingActivity() {
        Intent onboardingIntent = new Intent(SplashScreenActivity.this, OnboardingActivity.class);
        onboardingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(onboardingIntent);
        finish();
    }

    private void startSignInActivity() {
        Intent signInIntent = new Intent(SplashScreenActivity.this, SignInActivity.class);
        signInIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(signInIntent);
        finish();
    }
}
