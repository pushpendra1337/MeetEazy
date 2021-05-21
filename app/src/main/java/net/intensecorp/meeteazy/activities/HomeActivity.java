package net.intensecorp.meeteazy.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.Firestore;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setTitle(R.string.toolbar_title_search_in_rooms);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();

        if (isUserValid()) {
            getFcmToken();
        }

        toolbar.setNavigationOnClickListener(v -> Toast.makeText(HomeActivity.this, "Search", Toast.LENGTH_SHORT).show());

        toolbar.setOnClickListener(v -> Toast.makeText(HomeActivity.this, "Search", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);

        MenuItem menuItem = menu.findItem(R.id.item_profile);
        View view = menuItem.getActionView();

        CircleImageView profilePicture = view.findViewById(R.id.circleImageView_profile_picture);

        profilePicture.setOnClickListener(v -> Toast.makeText(HomeActivity.this, "Profile", Toast.LENGTH_SHORT).show());

        return super.onCreateOptionsMenu(menu);
    }

    private boolean isUserValid() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    private void getFcmToken() {
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(s -> {
                    Log.d(TAG, "FCM token: " + s);

                    setFcmToken(s);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to get token: " + e.getMessage()));
    }

    private void setFcmToken(String token) {
        FirebaseFirestore store = FirebaseFirestore.getInstance();
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        DocumentReference userReference = store.collection(Firestore.COLLECTION_USERS).document(uid);
        userReference.update(Firestore.FIELD_FCM_TOKEN, token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully updated: " + token))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update token: " + e.getMessage()));
    }
}