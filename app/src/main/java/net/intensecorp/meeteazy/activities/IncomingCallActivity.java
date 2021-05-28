package net.intensecorp.meeteazy.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.api.ApiClient;
import net.intensecorp.meeteazy.api.ApiService;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.FormatterUtility;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingCallActivity extends AppCompatActivity {

    private static final String TAG = IncomingCallActivity.class.getSimpleName();
    BroadcastReceiver mCallEndRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String requestType = intent.getStringExtra(Extras.EXTRA_REQUEST_TYPE);

            if (requestType != null) {
                if (ApiUtility.REQUEST_TYPE_ENDED.equals(requestType)) {
                    finish();
                }
            }
        }
    };
    private String mRoomId;
    private String mIncomingCallType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        ImageView incomingCallTypeIconView = findViewById(R.id.imageView_incoming_call_type);
        MaterialTextView incomingCallTypeView = findViewById(R.id.textView_incoming_call_type);
        CircleImageView callerProfilePictureView = findViewById(R.id.circleImageView_caller_profile_picture);
        MaterialTextView callerFullNameView = findViewById(R.id.textView_caller_full_name);
        MaterialTextView callerEmailView = findViewById(R.id.textView_caller_email);
        FloatingActionButton rejectCallButton = findViewById(R.id.floatingActionButton_reject_call);
        FloatingActionButton answerCallButton = findViewById(R.id.floatingActionButton_answer_call);

        Intent incomingCallIntent = getIntent();
        mIncomingCallType = incomingCallIntent.getStringExtra(Extras.EXTRA_CALL_TYPE);
        String callerFirstName = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_FIRST_NAME);
        String callerLastName = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_LAST_NAME);
        String callerEmail = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_EMAIL);
        String callerProfilePictureUrl = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_PROFILE_PICTURE_URL);
        String callerFcmToken = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_FCM_TOKEN);
        mRoomId = incomingCallIntent.getStringExtra(Extras.EXTRA_ROOM_ID);

        switch (mIncomingCallType) {
            case ApiUtility.CALL_TYPE_VOICE:
                incomingCallTypeIconView.setImageResource(R.drawable.ic_baseline_mic_24);
                incomingCallTypeView.setText(R.string.text_incoming_call_type_voice);
                break;

            case ApiUtility.CALL_TYPE_VIDEO:
                incomingCallTypeIconView.setImageResource(R.drawable.ic_baseline_videocam_24);
                incomingCallTypeView.setText(R.string.text_incoming_call_type_video);
                break;

            default:
                Log.e(TAG, "Incoming Call type unknown.");
                break;
        }

        if (!callerProfilePictureUrl.equals("null")) {
            Glide.with(IncomingCallActivity.this)
                    .load(callerProfilePictureUrl)
                    .centerCrop()
                    .placeholder(R.drawable.img_profile_picture)
                    .into(callerProfilePictureView);
        } else {
            callerProfilePictureView.setImageResource(R.drawable.img_profile_picture);
        }

        callerFullNameView.setText(FormatterUtility.getFullName(callerFirstName, callerLastName));

        callerEmailView.setText(callerEmail);

        rejectCallButton.setOnClickListener(v -> craftCallResponseMessageBody(callerFcmToken, ApiUtility.RESPONSE_TYPE_REJECTED));

        answerCallButton.setOnClickListener(v -> craftCallResponseMessageBody(callerFcmToken, ApiUtility.RESPONSE_TYPE_ANSWERED));
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mCallEndRequestReceiver, new IntentFilter(ApiUtility.MESSAGE_TYPE_CALL_REQUEST));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mCallEndRequestReceiver);
    }

    public void craftCallResponseMessageBody(String callerFcmToken, String responseType) {

        try {
            JSONArray callerFcmTokensArray = new JSONArray();
            callerFcmTokensArray.put(callerFcmToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(ApiUtility.KEY_MESSAGE_TYPE, ApiUtility.MESSAGE_TYPE_CALL_RESPONSE);
            data.put(ApiUtility.KEY_RESPONSE_TYPE, responseType);

            body.put(ApiUtility.JSON_OBJECT_DATA, data);
            body.put(ApiUtility.JSON_OBJECT_REGISTRATION_IDS, callerFcmTokensArray);

            sendCallResponseMessage(body.toString(), responseType);

        } catch (Exception exception) {
            Log.d(TAG, "Message can't be crafted: " + exception.getMessage());
        }
    }

    private void sendCallResponseMessage(String messageBody, String responseType) {
        ApiClient.getClient()
                .create(ApiService.class)
                .sendRemoteMessage(ApiUtility.getMessageHeaders(), messageBody)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.isSuccessful()) {
                            switch (responseType) {
                                case ApiUtility.RESPONSE_TYPE_ANSWERED:

                                    try {
                                        SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(IncomingCallActivity.this, SharedPrefsManager.PREF_USER_DATA);
                                        HashMap<String, String> userData = sharedPrefsManager.getUserDataPrefs();
                                        String firstName = userData.get(SharedPrefsManager.PREF_FIRST_NAME);
                                        String lastName = userData.get(SharedPrefsManager.PREF_LAST_NAME);
                                        String email = userData.get(SharedPrefsManager.PREF_EMAIL);
                                        URL profilePictureUrl = new URL(userData.get(SharedPrefsManager.PREF_PROFILE_PICTURE_URL));
                                        String fullName = FormatterUtility.getFullName(firstName, lastName);

                                        JitsiMeetUserInfo jitsiMeetUserInfo = new JitsiMeetUserInfo();
                                        jitsiMeetUserInfo.setDisplayName(fullName);
                                        jitsiMeetUserInfo.setEmail(email);
                                        jitsiMeetUserInfo.setAvatar(profilePictureUrl);

                                        JitsiMeetConferenceOptions.Builder conferenceOptionsBuilder = new JitsiMeetConferenceOptions.Builder()
                                                .setServerURL(ApiUtility.getJitsiMeetServerUrl())
                                                .setWelcomePageEnabled(false)
                                                .setRoom(mRoomId)
                                                .setUserInfo(jitsiMeetUserInfo);

                                        switch (mIncomingCallType) {
                                            case ApiUtility.CALL_TYPE_VOICE:
                                                conferenceOptionsBuilder.setAudioOnly(true);
                                                conferenceOptionsBuilder.setVideoMuted(true);
                                                break;
                                            case ApiUtility.CALL_TYPE_VIDEO:
                                                conferenceOptionsBuilder.setAudioOnly(false);
                                                conferenceOptionsBuilder.setVideoMuted(false);
                                                break;
                                            default:
                                                break;
                                        }

                                        JitsiMeetActivity.launch(IncomingCallActivity.this, conferenceOptionsBuilder.build());
                                        finish();
                                    } catch (Exception exception) {
                                        Log.e(TAG, "Failed to attend call: " + exception.getMessage());

                                        Toast.makeText(IncomingCallActivity.this, "Failed to attend call: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                        finish();
                                    }

                                    break;

                                case ApiUtility.RESPONSE_TYPE_REJECTED:
                                    Log.d(TAG, "Call rejected response message sent successfully.");
                                    break;

                                default:
                                    Log.e(TAG, "Unknown response");
                                    break;
                            }
                        } else {
                            Log.d(TAG, "Response of sent message: " + response.message());
                        }
                        finish();
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        Log.e(TAG, "Message not sent: " + t.getMessage());
                        finish();
                    }
                });
    }
}
