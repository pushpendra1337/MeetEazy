package net.intensecorp.meeteazy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.api.ApiMessaging;
import net.intensecorp.meeteazy.models.User;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.FormatterUtility;

import de.hdodenhof.circleimageview.CircleImageView;

public class OutgoingCallActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        ImageView callTypeIcon = findViewById(R.id.imageView_call_type);
        MaterialTextView callTypeView = findViewById(R.id.textView_call_type);
        CircleImageView calleeProfilePictureView = findViewById(R.id.circleImageView_callee_profile_picture);
        MaterialTextView calleeFullNameView = findViewById(R.id.textView_callee_full_name);
        MaterialTextView calleeEmailView = findViewById(R.id.textView_callee_email);
        FloatingActionButton endCallButton = findViewById(R.id.floatingActionButton_end_call);

        Intent outgoingCallIntent = getIntent();
        String callType = outgoingCallIntent.getStringExtra(Extras.EXTRA_CALL_TYPE);
        User callee = (User) outgoingCallIntent.getSerializableExtra(Extras.EXTRA_CALLEE);

        switch (callType) {
            case ApiMessaging.TYPE_VOICE_CALL:
                callTypeIcon.setImageResource(R.drawable.ic_baseline_mic_24);
                callTypeView.setText(R.string.text_call_type_voice);
                break;
            case ApiMessaging.TYPE_VIDEO_CALL:
                callTypeIcon.setImageResource(R.drawable.ic_baseline_videocam_24);
                callTypeView.setText(R.string.text_call_type_video);
                break;
        }

        if (callee.mProfilePictureUrl != null) {
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
    }
}
