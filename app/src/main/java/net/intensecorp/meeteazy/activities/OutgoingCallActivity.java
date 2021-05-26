package net.intensecorp.meeteazy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.api.ApiClient;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.api.ApiService;
import net.intensecorp.meeteazy.models.User;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.FormatterUtility;
import net.intensecorp.meeteazy.utils.SharedPrefsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingCallActivity extends AppCompatActivity {

    private static final String TAG = OutgoingCallActivity.class.getSimpleName();
    private SharedPrefsManager mSharedPrefsManager;

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
        String outgoingCallType = outgoingCallIntent.getStringExtra(Extras.EXTRA_CALL_TYPE);
        User callee = (User) outgoingCallIntent.getSerializableExtra(Extras.EXTRA_CALLEE);

        mSharedPrefsManager = new SharedPrefsManager(OutgoingCallActivity.this, SharedPrefsManager.PREF_USER_DATA);

        switch (outgoingCallType) {
            case ApiUtility.TYPE_VOICE_CALL:
                outgoingCallTypeIconView.setImageResource(R.drawable.ic_baseline_mic_24);
                outgoingCallTypeView.setText(R.string.text_outgoing_call_type_voice);
                break;
            case ApiUtility.TYPE_VIDEO_CALL:
                outgoingCallTypeIconView.setImageResource(R.drawable.ic_baseline_videocam_24);
                outgoingCallTypeView.setText(R.string.text_outgoing_call_type_video);
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

        endCallButton.setOnClickListener(v -> finish());

        if (outgoingCallType != null && callee != null) {
            craftCallRequestMessageBody(callee.mFcmToken, outgoingCallType);
        }
    }


    public void craftCallRequestMessageBody(String calleeFcmToken, String callType) {

        try {
            JSONArray calleeFcmTokensArray = new JSONArray();
            calleeFcmTokensArray.put(calleeFcmToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            String messageType = ApiUtility.MESSAGE_TYPE_CALL_REQUEST;

            HashMap<String, String> userData = mSharedPrefsManager.getUserDataPrefs();
            String callerFirstName = userData.get(SharedPrefsManager.PREF_FIRST_NAME);
            String callerLastName = userData.get(SharedPrefsManager.PREF_LAST_NAME);
            String callerEmail = userData.get(SharedPrefsManager.PREF_EMAIL);
            String callerProfilePictureUrl = userData.get(SharedPrefsManager.PREF_PROFILE_PICTURE_URL);
            String callerFcmToken = userData.get(SharedPrefsManager.PREF_FCM_TOKEN);

            data.put(ApiUtility.KEY_MESSAGE_TYPE, messageType);
            data.put(ApiUtility.KEY_CALL_TYPE, callType);
            data.put(ApiUtility.KEY_CALLER_FIRST_NAME, callerFirstName);
            data.put(ApiUtility.KEY_CALLER_LAST_NAME, callerLastName);
            data.put(ApiUtility.KEY_CALLER_EMAIL, callerEmail);
            data.put(ApiUtility.KEY_CALLER_PROFILE_PICTURE_URL, callerProfilePictureUrl);
            data.put(ApiUtility.KEY_CALLER_FCM_TOKEN, callerFcmToken);

            body.put(ApiUtility.JSON_OBJECT_DATA, data);
            body.put(ApiUtility.JSON_OBJECT_REGISTRATION_IDS, calleeFcmTokensArray);

            sendMessage(body.toString(), messageType);

        } catch (Exception exception) {
            Log.d(TAG, "Message can't be crafted: " + exception.getMessage());
        }
    }

    private void sendMessage(String messageBody, String messageType) {
        ApiClient.getClient()
                .create(ApiService.class)
                .sendRemoteMessage(ApiUtility.getMessageHeaders(), messageBody)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.isSuccessful()) {
                            if (messageType.equals(ApiUtility.MESSAGE_TYPE_CALL_REQUEST)) {
                                Log.d(TAG, "Call request message sent successfully");
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
