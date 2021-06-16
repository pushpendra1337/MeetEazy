package net.intensecorp.meeteazy.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private AlertDialog mSetThemeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar materialToolbar = findViewById(R.id.materialToolbar_settings);
        LinearLayout accountLayout = findViewById(R.id.linearLayout_account);
        LinearLayout themeLayout = findViewById(R.id.linearLayout_theme);
        setSupportActionBar(materialToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        materialToolbar.setNavigationOnClickListener(v -> onBackPressed());

        accountLayout.setOnClickListener(v -> startEditProfileActivity());

        themeLayout.setOnClickListener(v -> showSetThemeDialog());
    }

    private void showSetThemeDialog() {
        if (mSetThemeDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_set_theme, findViewById(R.id.scrollView_dialog_container));
            builder.setView(view);
            mSetThemeDialog = builder.create();

            if (mSetThemeDialog.getWindow() != null) {
                mSetThemeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(SettingsActivity.this, SharedPrefsManager.PREF_THEME);

            view.findViewById(R.id.imageView_close).setOnClickListener(v -> dismissSetThemeDialog());

            view.findViewById(R.id.linearLayout_light).setOnClickListener(v -> {
                dismissSetThemeDialog();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                sharedPrefsManager.setThemePref(SharedPrefsManager.PREF_THEME_LIGHT);
            });

            view.findViewById(R.id.linearLayout_dark).setOnClickListener(v -> {
                dismissSetThemeDialog();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                sharedPrefsManager.setThemePref(SharedPrefsManager.PREF_THEME_DARK);
            });

            view.findViewById(R.id.linearLayout_system_default).setOnClickListener(v -> {
                dismissSetThemeDialog();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                sharedPrefsManager.setThemePref(SharedPrefsManager.PREF_THEME_SYSTEM_DEFAULT);
            });
        }

        mSetThemeDialog.show();
    }

    private void dismissSetThemeDialog() {
        if (mSetThemeDialog != null) {
            mSetThemeDialog.dismiss();
            mSetThemeDialog = null;
        }
    }

    private void startEditProfileActivity() {
        Intent editProfileIntent = new Intent(SettingsActivity.this, EditProfileActivity.class);
        startActivity(editProfileIntent);
    }
}
