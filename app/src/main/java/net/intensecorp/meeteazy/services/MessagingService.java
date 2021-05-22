package net.intensecorp.meeteazy.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.intensecorp.meeteazy.utils.Firestore;

import java.util.Objects;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = MessagingService.class.getSimpleName();
    private FirebaseAuth mAuth;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "FCM remote message: " + Objects.requireNonNull(remoteMessage.getNotification()).getBody());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        if (isUserValid()) {
            Log.d(TAG, "FCM token: " + token);

            setFcmToken(token);
        }
    }

    private boolean isUserValid() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null && user.isEmailVerified();
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
