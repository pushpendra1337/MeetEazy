package net.intensecorp.meeteazy.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textview.MaterialTextView;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.listener.UsersListener;
import net.intensecorp.meeteazy.models.User;
import net.intensecorp.meeteazy.utils.FormatterUtility;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<User> mUsers;
    private UsersListener mUsersListener;

    public UsersAdapter(List<User> users, UsersListener usersListener) {
        this.mUsers = users;
        this.mUsersListener = usersListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UserViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(mUsers.get(position));
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        private MaterialTextView fullNameView;
        private MaterialTextView emailView;
        private CircleImageView profilePictureView;
        private ImageView videoCallButton;
        private ImageView voiceCallButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            fullNameView = itemView.findViewById(R.id.textView_full_name);
            emailView = itemView.findViewById(R.id.textView_email);
            profilePictureView = itemView.findViewById(R.id.circleImageView_user_profile_picture);
            voiceCallButton = itemView.findViewById(R.id.imageView_voice_call);
            videoCallButton = itemView.findViewById(R.id.imageView_video_call);
        }

        private void setUserData(User user) {
            fullNameView.setText(FormatterUtility.getFullName(user.mFirstName, user.mLastName));
            emailView.setText(user.mEmail);

            if (user.mProfilePictureUrl != null) {
                Glide.with(itemView.getContext())
                        .load(user.mProfilePictureUrl)
                        .centerCrop()
                        .placeholder(R.drawable.img_profile_picture)
                        .into(profilePictureView);
            } else {
                profilePictureView.setImageResource(R.drawable.img_profile_picture);
            }

            voiceCallButton.setOnClickListener(v -> mUsersListener.initiateVoiceCall(user));

            videoCallButton.setOnClickListener(v -> mUsersListener.initiateVideoCall(user));
        }
    }
}
