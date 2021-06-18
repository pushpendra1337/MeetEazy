package net.intensecorp.meeteazy.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.api.ApiClient;
import net.intensecorp.meeteazy.api.ApiService;
import net.intensecorp.meeteazy.models.Contact;
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

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingCallActivity extends AppCompatActivity {

    private static final String TAG = OutgoingCallActivity.class.getSimpleName();
    private static final String ROOM_ID = Patterns.generateRoomId();
    SharedPrefsManager mSharedPrefsManager;
    private ArrayList<Contact> mCallees;
    private String mOutgoingCallType;
    private String mOtherCalleesCount;
    private int mCallRejectionCount = 0;
    private int mTotalCallees = 0;
    private String mEmail;
    BroadcastReceiver mCallResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String responseType = intent.getStringExtra(Extras.EXTRA_RESPONSE_TYPE);

            if (responseType != null) {

                switch (responseType) {
                    case ApiUtility.RESPONSE_TYPE_REJECTED:
                        mCallRejectionCount += 1;
                        if (mCallRejectionCount == mTotalCallees) {
                            finish();
                        }
                        break;

                    case ApiUtility.RESPONSE_TYPE_ANSWERED:

                        HashMap<String, String> userData = mSharedPrefsManager.getUserDataPrefs();
                        String firstName = userData.get(SharedPrefsManager.PREF_FIRST_NAME);
                        String lastName = userData.get(SharedPrefsManager.PREF_LAST_NAME);
                        String profilePictureLink = userData.get(SharedPrefsManager.PREF_PROFILE_PICTURE_URL);

                        String fullName = FormatterUtility.getFullName(firstName, lastName);
                        URL profilePictureUrl;

                        JitsiMeetUserInfo jitsiMeetUserInfo = new JitsiMeetUserInfo();
                        jitsiMeetUserInfo.setDisplayName(fullName);
                        jitsiMeetUserInfo.setEmail(mEmail);

                        try {
                            profilePictureUrl = new URL(profilePictureLink);
                            jitsiMeetUserInfo.setAvatar(profilePictureUrl);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }

                        JitsiMeetConferenceOptions.Builder conferenceOptionsBuilder = new JitsiMeetConferenceOptions.Builder()
                                .setServerURL(ApiUtility.getJitsiMeetServerUrl())
                                .setWelcomePageEnabled(false)
                                .setRoom(ROOM_ID)
                                .setVideoMuted(true)
                                .setAudioMuted(false);

                        JitsiMeetActivity.launch(OutgoingCallActivity.this, conferenceOptionsBuilder.build());
                        finish();
                        break;

                    default:
                        break;
                }
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        MaterialTextView outgoingCallTypeView = findViewById(R.id.textView_outgoing_call_type);
        CircleImageView calleeProfilePictureView = findViewById(R.id.circleImageView_callee_profile_picture);
        MaterialTextView calleeFullNameView = findViewById(R.id.textView_callee_full_name);
        MaterialTextView calleeEmailView = findViewById(R.id.textView_callee_email);
        LinearLayout plusOthersOrOtherLayout = findViewById(R.id.linearLayout_plus_others_or_other);
        MaterialTextView otherCalleesCountView = findViewById(R.id.textView_other_callees_count);
        MaterialTextView othersOrOtherView = findViewById(R.id.textView_others_or_other);
        FloatingActionButton endCallButton = findViewById(R.id.floatingActionButton_end_call);

        mEmail = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail();

        Intent outgoingCallIntent = getIntent();
        mOutgoingCallType = outgoingCallIntent.getStringExtra(Extras.EXTRA_CALL_TYPE);

        Contact callee = (Contact) outgoingCallIntent.getSerializableExtra(Extras.EXTRA_CALLEE);

        switch (mOutgoingCallType) {
            case ApiUtility.CALL_TYPE_PERSONAL:
                outgoingCallTypeView.setText(R.string.text_outgoing_call);
                calleeEmailView.setText(callee.email);
                mTotalCallees = 1;
                break;

            case ApiUtility.CALL_TYPE_GROUP:
                outgoingCallTypeView.setText(R.string.text_outgoing_group_call);
                Type type = new TypeToken<ArrayList<Contact>>() {
                }.getType();
                mCallees = new Gson().fromJson(outgoingCallIntent.getStringExtra(Extras.EXTRA_CALLEES), type);
                mTotalCallees = mCallees.size();
                mOtherCalleesCount = outgoingCallIntent.getStringExtra(Extras.EXTRA_OTHER_CALLEES_COUNT);
                calleeEmailView.setVisibility(View.GONE);
                otherCalleesCountView.setText(mOtherCalleesCount);
                if (mOtherCalleesCount.equals("1")) {
                    othersOrOtherView.setText(R.string.text_other);
                }
                plusOthersOrOtherLayout.setVisibility(View.VISIBLE);
                break;

            default:
                break;
        }

        if (!callee.profilePictureUrl.equals("null")) {
            Glide.with(OutgoingCallActivity.this)
                    .load(callee.profilePictureUrl)
                    .centerCrop()
                    .placeholder(R.drawable.img_profile_picture)
                    .into(calleeProfilePictureView);
        } else {
            calleeProfilePictureView.setImageResource(R.drawable.img_profile_picture);
        }

        calleeFullNameView.setText(FormatterUtility.getFullName(callee.firstName, callee.lastName));

        endCallButton.setOnClickListener(v -> {
            if (mOutgoingCallType.equals(ApiUtility.CALL_TYPE_PERSONAL)) {
                craftCallEndRequestMessageBody(callee.fcmToken, null);
            } else {
                craftCallEndRequestMessageBody(null, mCallees);
            }

            finish();
        });

        craftCallInitiateRequestMessageBody(mOutgoingCallType, callee.fcmToken, mCallees);
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

    public void craftCallInitiateRequestMessageBody(String callType, String calleeFcmToken, ArrayList<Contact> callees) {

        try {
            JSONArray calleeFcmTokensArray = new JSONArray();

            if (calleeFcmToken != null) {
                calleeFcmTokensArray.put(calleeFcmToken);
            }

            if (callees != null && callees.size() > 0) {
                for (int i = 0; i < callees.size(); i++) {
                    calleeFcmTokensArray.put(callees.get(i).fcmToken);
                }
            }

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            mSharedPrefsManager = new SharedPrefsManager(OutgoingCallActivity.this, SharedPrefsManager.PREF_USER_DATA);
            HashMap<String, String> userData = mSharedPrefsManager.getUserDataPrefs();
            String firstName = userData.get(SharedPrefsManager.PREF_FIRST_NAME);
            String lastName = userData.get(SharedPrefsManager.PREF_LAST_NAME);
            String profilePictureUrl = userData.get(SharedPrefsManager.PREF_PROFILE_PICTURE_URL);

            data.put(ApiUtility.KEY_MESSAGE_TYPE, ApiUtility.MESSAGE_TYPE_CALL_REQUEST);
            data.put(ApiUtility.KEY_REQUEST_TYPE, ApiUtility.REQUEST_TYPE_INITIATED);
            data.put(ApiUtility.KEY_CALL_TYPE, callType);
            data.put(ApiUtility.KEY_CALLER_FIRST_NAME, firstName);
            data.put(ApiUtility.KEY_CALLER_LAST_NAME, lastName);
            data.put(ApiUtility.KEY_CALLER_EMAIL, mEmail);
            if (callType.equals(ApiUtility.CALL_TYPE_GROUP)) {
                data.put(ApiUtility.KEY_OTHER_CALLEES_COUNT, mOtherCalleesCount);
            }
            data.put(ApiUtility.KEY_CALLER_PROFILE_PICTURE_URL, profilePictureUrl);
            data.put(ApiUtility.KEY_CALLER_FCM_TOKEN, mSharedPrefsManager.getFcmTokenPref());
            data.put(ApiUtility.KEY_ROOM_ID, ROOM_ID);

            body.put(ApiUtility.JSON_OBJECT_DATA, data);
            body.put(ApiUtility.JSON_OBJECT_REGISTRATION_IDS, calleeFcmTokensArray);

            sendCallRequestMessage(body.toString(), ApiUtility.REQUEST_TYPE_INITIATED);

        } catch (Exception exception) {
            Log.d(TAG, "Message body can't be crafted: " + exception.getMessage());
        }
    }

    public void craftCallEndRequestMessageBody(String calleeFcmToken, ArrayList<Contact> callees) {
        try {
            JSONArray calleeFcmTokensArray = new JSONArray();
            if (calleeFcmToken != null) {
                calleeFcmTokensArray.put(calleeFcmToken);
            }

            if (callees != null && callees.size() > 0) {
                for (int i = 0; i < callees.size(); i++) {
                    calleeFcmTokensArray.put(callees.get(i).fcmToken);
                }
            }

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(ApiUtility.KEY_MESSAGE_TYPE, ApiUtility.MESSAGE_TYPE_CALL_REQUEST);
            data.put(ApiUtility.KEY_REQUEST_TYPE, ApiUtility.REQUEST_TYPE_ENDED);

            body.put(ApiUtility.JSON_OBJECT_DATA, data);
            body.put(ApiUtility.JSON_OBJECT_REGISTRATION_IDS, calleeFcmTokensArray);

            sendCallRequestMessage(body.toString(), ApiUtility.REQUEST_TYPE_ENDED);

        } catch (Exception exception) {
            Log.e(TAG, "Message body can't be crafted: " + exception.getMessage());
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
                                    Log.d(TAG, "Call initiate request message successfully sent.");
                                    break;

                                case ApiUtility.REQUEST_TYPE_ENDED:
                                    Log.d(TAG, "Call end request message successfully sent.");
                                    finish();
                                    break;

                                default:
                                    Log.e(TAG, "Unknown request.");
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
