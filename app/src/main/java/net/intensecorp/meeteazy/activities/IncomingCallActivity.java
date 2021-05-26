package net.intensecorp.meeteazy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.ApiUtility;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.FormatterUtility;

import de.hdodenhof.circleimageview.CircleImageView;

public class IncomingCallActivity extends AppCompatActivity {

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
        String incomingCallType = incomingCallIntent.getStringExtra(Extras.EXTRA_CALL_TYPE);
        String callerFirstName = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_FIRST_NAME);
        String callerLastName = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_LAST_NAME);
        String callerEmail = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_EMAIL);
        String callerProfilePictureUrl = incomingCallIntent.getStringExtra(Extras.EXTRA_CALLER_PROFILE_PICTURE_URL);

        switch (incomingCallType) {
            case ApiUtility.TYPE_VOICE_CALL:
                incomingCallTypeIconView.setImageResource(R.drawable.ic_baseline_mic_24);
                incomingCallTypeView.setText(R.string.text_incoming_call_type_voice);
                break;
            case ApiUtility.TYPE_VIDEO_CALL:
                incomingCallTypeIconView.setImageResource(R.drawable.ic_baseline_videocam_24);
                incomingCallTypeView.setText(R.string.text_incoming_call_type_video);
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

        rejectCallButton.setOnClickListener(v -> finish());

        answerCallButton.setOnClickListener(v -> Toast.makeText(IncomingCallActivity.this, "Call answered", Toast.LENGTH_SHORT).show());
    }
}
