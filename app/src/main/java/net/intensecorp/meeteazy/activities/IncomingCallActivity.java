package net.intensecorp.meeteazy.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

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

import java.net.MalformedURLException;
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
                if (requestType.equals(ApiUtility.REQUEST_TYPE_ENDED)) {
                    finish();
                }
            }
        }
    };
    private String mRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        MaterialTextView incomingCallTypeView = findViewById(R.id.textView_incoming_call_type);
        CircleImageView callerProfilePictureView = findViewById(R.id.circleImageView_caller_profile_picture);
        MaterialTextView callerFullNameView = findViewById(R.id.textView_caller_full_name);
        MaterialTextView callerEmailView = findViewById(R.id.textView_caller_email);
        LinearLayout isCallingYouAndManyOthersLayout = findViewById(R.id.linearLayout_is_calling_you_and_other_or_others);
        MaterialTextView otherCalleesCountView = findViewById(R.id.textView_other_members_count);
        MaterialTextView othersOrOtherView = findViewById(R.id.textView_others_or_other);
        FloatingActionButton rejectCallButton = findViewById(R.id.floatingActionButton_reject_call);
        FloatingActionButton answerCallButton = findViewById(R.id.floatingActionButton_answer_call);

        Intent incomingCallIntent = getIntent();
        String incomingCallType = incomingCallIntent.getStringExtra(Extras.EXTRA_CALL_TYPE);
        String callerFirstName = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_FIRST_NAME);
        String callerLastName = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_LAST_NAME);
        String callerEmail = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_EMAIL);
        String callerProfilePictureUrl = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_PROFILE_PICTURE_URL);
        String callerFcmToken = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_FCM_TOKEN);
        mRoomId = incomingCallIntent.getStringExtra(Extras.EXTRA_ROOM_ID);

        switch (incomingCallType) {
            case ApiUtility.CALL_TYPE_PERSONAL:
                incomingCallTypeView.setText(R.string.text_incoming_call);
                callerEmailView.setText(callerEmail);
                break;

            case ApiUtility.CALL_TYPE_GROUP:
                String otherCalleesCount = incomingCallIntent.getStringExtra(Extras.EXTRA_OTHER_CALLEES_COUNT);
                incomingCallTypeView.setText(R.string.text_incoming_group_call);
                callerEmailView.setVisibility(View.GONE);
                otherCalleesCountView.setText(otherCalleesCount);
                if (otherCalleesCount.equals("1")) {
                    othersOrOtherView.setText(R.string.text_other);
                }
                isCallingYouAndManyOthersLayout.setVisibility(View.VISIBLE);
                break;

            default:
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

                                    SharedPrefsManager sharedPrefsManager = new SharedPrefsManager(IncomingCallActivity.this, SharedPrefsManager.PREF_USER_DATA);
                                    HashMap<String, String> userData = sharedPrefsManager.getUserDataPrefs();
                                    String firstName = userData.get(SharedPrefsManager.PREF_FIRST_NAME);
                                    String lastName = userData.get(SharedPrefsManager.PREF_LAST_NAME);
                                    String email = userData.get(SharedPrefsManager.PREF_EMAIL);
                                    String profilePictureLink = userData.get(SharedPrefsManager.PREF_PROFILE_PICTURE_URL);

                                    String fullName = FormatterUtility.getFullName(firstName, lastName);
                                    URL profilePictureUrl;

                                    JitsiMeetUserInfo jitsiMeetUserInfo = new JitsiMeetUserInfo();
                                    jitsiMeetUserInfo.setDisplayName(fullName);
                                    jitsiMeetUserInfo.setEmail(email);

                                    try {
                                        profilePictureUrl = new URL(profilePictureLink);
                                        jitsiMeetUserInfo.setAvatar(profilePictureUrl);
                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    }

                                    JitsiMeetConferenceOptions.Builder conferenceOptionsBuilder = new JitsiMeetConferenceOptions.Builder()
                                            .setServerURL(ApiUtility.getJitsiMeetServerUrl())
                                            .setWelcomePageEnabled(false)
                                            .setRoom(mRoomId)
                                            .setUserInfo(jitsiMeetUserInfo)
                                            .setVideoMuted(true)
                                            .setAudioMuted(true);

                                    JitsiMeetActivity.launch(IncomingCallActivity.this, conferenceOptionsBuilder.build());
                                    finish();
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
