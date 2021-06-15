package net.intensecorp.meeteazy.services;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.intensecorp.meeteazy.activities.IncomingCallActivity;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.Firestore;

import java.util.Objects;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = MessagingService.class.getSimpleName();
    private FirebaseAuth mAuth;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "FCM remote message: " + Objects.requireNonNull(remoteMessage.getData()));

        String messageType = remoteMessage.getData().get(ApiUtility.KEY_MESSAGE_TYPE);

        if (messageType != null) {
            switch (messageType) {
                case ApiUtility.MESSAGE_TYPE_CALL_REQUEST:
                    String requestType = remoteMessage.getData().get(ApiUtility.KEY_REQUEST_TYPE);
                    if (requestType != null) {
                        switch (requestType) {
                            case ApiUtility.REQUEST_TYPE_INITIATED:
                                startIncomingCallActivity(remoteMessage);
                                break;

                            case ApiUtility.REQUEST_TYPE_ENDED:
                                Intent incomingCallIntent = new Intent(ApiUtility.MESSAGE_TYPE_CALL_REQUEST);
                                incomingCallIntent.putExtra(Extras.EXTRA_REQUEST_TYPE, requestType);
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(incomingCallIntent);
                                break;

                            default:
                                break;
                        }
                    }
                    break;

                case ApiUtility.MESSAGE_TYPE_CALL_RESPONSE:
                    String responseType = remoteMessage.getData().get(ApiUtility.KEY_RESPONSE_TYPE);
                    Intent outgoingCallIntent = new Intent(ApiUtility.MESSAGE_TYPE_CALL_RESPONSE);
                    outgoingCallIntent.putExtra(Extras.EXTRA_RESPONSE_TYPE, responseType);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(outgoingCallIntent);
                    break;

                default:
                    break;
            }
        }
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

    private void setFcmToken(String fcmToken) {
        FirebaseFirestore store = FirebaseFirestore.getInstance();
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        DocumentReference userReference = store.collection(Firestore.COLLECTION_USERS).document(uid);
        userReference.update(Firestore.FIELD_FCM_TOKEN, fcmToken)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully updated: " + fcmToken))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update token: " + e.getMessage()));
    }

    private void startIncomingCallActivity(RemoteMessage remoteMessage) {
        Intent incomingCallIntent = new Intent(getApplicationContext(), IncomingCallActivity.class);
        incomingCallIntent.putExtra(Extras.EXTRA_CALL_TYPE, remoteMessage.getData().get(ApiUtility.KEY_CALL_TYPE));
        incomingCallIntent.putExtra(Extras.EXTRA_CALLER_FIRST_NAME, remoteMessage.getData().get(ApiUtility.KEY_CALLER_FIRST_NAME));
        incomingCallIntent.putExtra(Extras.EXTRA_CALLER_LAST_NAME, remoteMessage.getData().get(ApiUtility.KEY_CALLER_LAST_NAME));
        incomingCallIntent.putExtra(Extras.EXTRA_CALLER_EMAIL, remoteMessage.getData().get(ApiUtility.KEY_CALLER_EMAIL));
        if (Objects.equals(remoteMessage.getData().get(ApiUtility.KEY_CALL_TYPE), ApiUtility.CALL_TYPE_GROUP)) {
            incomingCallIntent.putExtra(Extras.EXTRA_OTHER_CALLEES_COUNT, remoteMessage.getData().get(ApiUtility.KEY_OTHER_CALLEES_COUNT));
        }
        incomingCallIntent.putExtra(Extras.EXTRA_CALLER_PROFILE_PICTURE_URL, remoteMessage.getData().get(ApiUtility.KEY_CALLER_PROFILE_PICTURE_URL));
        incomingCallIntent.putExtra(Extras.EXTRA_CALLER_FCM_TOKEN, remoteMessage.getData().get(ApiUtility.KEY_CALLER_FCM_TOKEN));
        incomingCallIntent.putExtra(Extras.EXTRA_ROOM_ID, remoteMessage.getData().get(ApiUtility.KEY_ROOM_ID));
        incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(incomingCallIntent);
    }
}
