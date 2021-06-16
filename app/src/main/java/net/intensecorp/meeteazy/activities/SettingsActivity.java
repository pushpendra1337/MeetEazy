package net.intensecorp.meeteazy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import net.intensecorp.meeteazy.R;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

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

        themeLayout.setOnClickListener(v -> {
            // TODO: Show Set Theme dialog
        });
    }

    private void startEditProfileActivity() {
        Intent editProfileIntent = new Intent(SettingsActivity.this, EditProfileActivity.class);
        startActivity(editProfileIntent);
    }
}
