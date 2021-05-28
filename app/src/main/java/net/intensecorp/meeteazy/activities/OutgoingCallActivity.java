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
import net.intensecorp.meeteazy.models.User;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.FormatterUtility;
import net.intensecorp.meeteazy.utils.Patterns;
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

public class OutgoingCallActivity extends AppCompatActivity {

    private static final String TAG = OutgoingCallActivity.class.getSimpleName();
    private final SharedPrefsManager mSharedPrefsManager = new SharedPrefsManager(OutgoingCallActivity.this, SharedPrefsManager.PREF_USER_DATA);
    private final HashMap<String, String> mUserData = mSharedPrefsManager.getUserDataPrefs();
    private final String mFirstName = mUserData.get(SharedPrefsManager.PREF_FIRST_NAME);
    private final String mLastName = mUserData.get(SharedPrefsManager.PREF_LAST_NAME);
    private final String mEmail = mUserData.get(SharedPrefsManager.PREF_EMAIL);
    private final String mProfilePictureUrl = mUserData.get(SharedPrefsManager.PREF_PROFILE_PICTURE_URL);
    private String mOutgoingCallType;
    BroadcastReceiver mCallResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String responseType = intent.getStringExtra(Extras.EXTRA_RESPONSE_TYPE);
            String roomId = intent.getStringExtra(Extras.EXTRA_ROOM_ID);
            if (responseType != null) {

                switch (responseType) {
                    case ApiUtility.RESPONSE_TYPE_REJECTED:
                        finish();
                        break;
                    case ApiUtility.RESPONSE_TYPE_ANSWERED:
                        try {

                            URL profilePictureUrl = new URL(mProfilePictureUrl);
                            String fullName = FormatterUtility.getFullName(mFirstName, mLastName);

                            JitsiMeetUserInfo jitsiMeetUserInfo = new JitsiMeetUserInfo();
                            jitsiMeetUserInfo.setDisplayName(fullName);
                            jitsiMeetUserInfo.setEmail(mEmail);
                            jitsiMeetUserInfo.setAvatar(profilePictureUrl);

                            JitsiMeetConferenceOptions.Builder conferenceOptionsBuilder = new JitsiMeetConferenceOptions.Builder()
                                    .setServerURL(ApiUtility.getJitsiMeetServerUrl())
                                    .setWelcomePageEnabled(false)
                                    .setRoom(roomId);

                            switch (mOutgoingCallType) {
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

                            JitsiMeetActivity.launch(OutgoingCallActivity.this, conferenceOptionsBuilder.build());
                            finish();
                        } catch (Exception exception) {
                            Log.e(TAG, "Failed to attend call: " + exception.getMessage());

                            Toast.makeText(OutgoingCallActivity.this, "Failed to attend call: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        ImageView outgoingCallTypeIconView = findViewById(R.id.imageView_outgoing_call_type);
        MaterialTextView outgoingCallTypeView = findViewById(R.id.textView_outgoing_call_type);
        CircleImageView calleeProfilePictureView = findViewById(R.id.circleImageView_callee_profile_picture);
        MaterialTextView calleeFullNameView = findViewById(R.id.textView_callee_full_name);
        MaterialTextView calleeEmailView = findViewById(R.id.textView_callee_email);
        FloatingActionButton endCallButton = findViewById(R.id.floatingActionButton_end_call);

        Intent outgoingCallIntent = getIntent();
        mOutgoingCallType = outgoingCallIntent.getStringExtra(Extras.EXTRA_CALL_TYPE);
        User callee = (User) outgoingCallIntent.getSerializableExtra(Extras.EXTRA_CALLEE);

        switch (mOutgoingCallType) {
            case ApiUtility.CALL_TYPE_VOICE:
                outgoingCallTypeIconView.setImageResource(R.drawable.ic_baseline_mic_24);
                outgoingCallTypeView.setText(R.string.text_outgoing_call_type_voice);
                break;

            case ApiUtility.CALL_TYPE_VIDEO:
                outgoingCallTypeIconView.setImageResource(R.drawable.ic_baseline_videocam_24);
                outgoingCallTypeView.setText(R.string.text_outgoing_call_type_video);
                break;

            default:
                Log.e(TAG, "Outgoing Call type unknown.");
                break;
        }

        if (!callee.mProfilePictureUrl.equals("null")) {
            Glide.with(OutgoingCallActivity.this)
                    .load(callee.mProfilePictureUrl)
                    .centerCrop()
                    .placeholder(R.drawable.img_profile_picture)
                    .into(calleeProfilePictureView);
        } else {
            calleeProfilePictureView.setImageResource(R.drawable.img_profile_picture);
        }

        calleeFullNameView.setText(FormatterUtility.getFullName(callee.mFirstName, callee.mLastName));

        calleeEmailView.setText(callee.mEmail);

        endCallButton.setOnClickListener(v -> {
            craftCallEndRequestMessageBody(callee.mFcmToken);
            finish();
        });

        craftCallInitiateRequestMessageBody(callee.mFcmToken, mOutgoingCallType);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mCallResponseReceiver, new IntentFilter(ApiUtility.MESSAGE_TYPE_CALL_RESPONSE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mCallResponseReceiver);
    }

    public void craftCallInitiateRequestMessageBody(String calleeFcmToken, String callType) {

        try {
            JSONArray calleeFcmTokensArray = new JSONArray();
            calleeFcmTokensArray.put(calleeFcmToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(ApiUtility.KEY_MESSAGE_TYPE, ApiUtility.MESSAGE_TYPE_CALL_REQUEST);
            data.put(ApiUtility.KEY_REQUEST_TYPE, ApiUtility.REQUEST_TYPE_INITIATED);
            data.put(ApiUtility.KEY_CALL_TYPE, callType);
            data.put(ApiUtility.KEY_CALLER_FIRST_NAME, mFirstName);
            data.put(ApiUtility.KEY_CALLER_LAST_NAME, mLastName);
            data.put(ApiUtility.KEY_CALLER_EMAIL, mEmail);
            data.put(ApiUtility.KEY_CALLER_PROFILE_PICTURE_URL, mProfilePictureUrl);
            data.put(ApiUtility.KEY_CALLER_FCM_TOKEN, mSharedPrefsManager.getFcmTokenPref());
            data.put(ApiUtility.KEY_ROOM_ID, Patterns.generateRoomId());

            body.put(ApiUtility.JSON_OBJECT_DATA, data);
            body.put(ApiUtility.JSON_OBJECT_REGISTRATION_IDS, calleeFcmTokensArray);

            sendCallRequestMessage(body.toString(), ApiUtility.REQUEST_TYPE_INITIATED);

        } catch (Exception exception) {
            Log.d(TAG, "Message body can't be crafted: " + exception.getMessage());
        }
    }

    public void craftCallEndRequestMessageBody(String calleeFcmToken) {

        try {
            JSONArray calleeFcmTokensArray = new JSONArray();
            calleeFcmTokensArray.put(calleeFcmToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(ApiUtility.KEY_MESSAGE_TYPE, ApiUtility.MESSAGE_TYPE_CALL_REQUEST);
            data.put(ApiUtility.KEY_REQUEST_TYPE, ApiUtility.REQUEST_TYPE_ENDED);

            body.put(ApiUtility.JSON_OBJECT_DATA, data);
            body.put(ApiUtility.JSON_OBJECT_REGISTRATION_IDS, calleeFcmTokensArray);

            sendCallRequestMessage(body.toString(), ApiUtility.REQUEST_TYPE_ENDED);

        } catch (Exception exception) {
            Log.d(TAG, "Message body can't be crafted: " + exception.getMessage());
        }
    }

    private void sendCallRequestMessage(String messageBody, String requestType) {
        ApiClient.getClient()
                .create(ApiService.class)
                .sendRemoteMessage(ApiUtility.getMessageHeaders(), messageBody)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.isSuccessful()) {
                            switch (requestType) {
                                case ApiUtility.REQUEST_TYPE_INITIATED:
                                    Log.d(TAG, "Call initiate request message sent successfully.");
                                    break;

                                case ApiUtility.REQUEST_TYPE_ENDED:
                                    Log.d(TAG, "Call end request message sent successfully.");
                                    finish();
                                    break;
                            }
                        } else {
                            Log.d(TAG, "Response of sent message: " + response.message());
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        Log.e(TAG, "Message not sent: " + t.getMessage());
                        finish();
                    }
                });
    }
}
