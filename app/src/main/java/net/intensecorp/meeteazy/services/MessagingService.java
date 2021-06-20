package net.intensecorp.meeteazy.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.activities.IncomingCallActivity;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.Firestore;
import net.intensecorp.meeteazy.utils.FormatterUtility;

import java.util.Objects;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = MessagingService.class.getSimpleName();
    private static final String NOTIFICATION_CHANNEL_INCOMING_CALLS = "Incoming call notifications";
    private static final String NOTIFICATION_CHANNEL_DESCRIPTION_INCOMING_CALLS = "Incoming call notifications to notify about personal and group calls.";
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
                                long requestCraftTime = Long.parseLong(Objects.requireNonNull(remoteMessage.getData().get(ApiUtility.KEY_REQUEST_TIMESTAMP)));
                                long currentTime = System.currentTimeMillis();

                                if ((currentTime - requestCraftTime) >= 5000) {
                                    Log.d(TAG, "Remote message ignored.");
                                } else {
                                    pushIncomingCallNotification(remoteMessage);
                                    startIncomingCallActivity(remoteMessage);
                                }
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

    public void pushIncomingCallNotification(RemoteMessage remoteMessage) {
        RemoteViews notificationViews = new RemoteViews(getPackageName(), R.layout.notification_incoming_call);
        String callerFullName = FormatterUtility.getFullName(remoteMessage.getData().get(ApiUtility.KEY_CALLER_FIRST_NAME), remoteMessage.getData().get(ApiUtility.KEY_CALLER_LAST_NAME));
        notificationViews.setTextViewText(R.id.textView_caller_full_name, callerFullName);

        switch (Objects.requireNonNull(remoteMessage.getData().get(ApiUtility.KEY_CALL_TYPE))) {
            case ApiUtility.CALL_TYPE_PERSONAL:
                notificationViews.setTextViewText(R.id.textView_incoming_call_type, "Incoming call");
                break;
            case ApiUtility.CALL_TYPE_GROUP:
                notificationViews.setTextViewText(R.id.textView_incoming_call_type, "Incoming group call");
                break;
            default:
                break;
        }

        Uri callRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        Intent incomingCallIntent = new Intent(getApplicationContext(), IncomingCallActivity.class);
        incomingCallIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Notification notification = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_baseline_call_24)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(notificationViews)
                    .setCustomBigContentView(notificationViews)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setSound(callRingtoneUri)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                    .setLights(Color.BLUE, 500, 500)
                    .setAutoCancel(false)
                    .build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, notification);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel notificationChannel = new NotificationChannel("0", NOTIFICATION_CHANNEL_INCOMING_CALLS, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription(NOTIFICATION_CHANNEL_DESCRIPTION_INCOMING_CALLS);
            notificationChannel.enableLights(true);
            notificationChannel.canBypassDnd();
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);

            Notification notification = new NotificationCompat.Builder(getApplicationContext(), "0")
                    .setSmallIcon(R.drawable.ic_baseline_call_24)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomContentView(notificationViews)
                    .setCustomBigContentView(notificationViews)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setSound(callRingtoneUri)
                    .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                    .setLights(Color.BLUE, 500, 500)
                    .setAutoCancel(false)
                    .build();

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            notificationManagerCompat.notify(1, notification);
        }
    }
}
