package net.intensecorp.meeteazy.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textview.MaterialTextView;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.activities.ProfileActivity;
import net.intensecorp.meeteazy.listener.UsersListener;
import net.intensecorp.meeteazy.models.User;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.FormatterUtility;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public static List<User> mSelectedUsers;
    private List<User> mUsers;
    private UsersListener mUsersListener;

    public UsersAdapter(List<User> users, UsersListener usersListener) {
        this.mUsers = users;
        this.mUsersListener = usersListener;
        mSelectedUsers = new ArrayList<>();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UserViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        if (mSelectedUsers.size() <= 0) {
            holder.selectedStateLayout.setVisibility(View.GONE);
            holder.callButton.setVisibility(View.VISIBLE);
        } else {
            holder.callButton.setVisibility(View.GONE);
        }

        holder.setUserData(mUsers.get(position));
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        private ConstraintLayout userContainer;
        private MaterialTextView fullNameView;
        private MaterialTextView emailView;
        private CircleImageView profilePictureView;
        private ConstraintLayout selectedStateLayout;
        private ImageView callButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            userContainer = itemView.findViewById(R.id.constraintLayout_user_container);
            fullNameView = itemView.findViewById(R.id.textView_full_name);
            emailView = itemView.findViewById(R.id.textView_email);
            profilePictureView = itemView.findViewById(R.id.circleImageView_user_profile_picture);
            selectedStateLayout = itemView.findViewById(R.id.constraintLayout_selected_state_layout);
            callButton = itemView.findViewById(R.id.imageView_call);
        }

        private void setUserData(User user) {
            fullNameView.setText(FormatterUtility.getFullName(user.mFirstName, user.mLastName));
            emailView.setText(user.mEmail);

            if (!user.mProfilePictureUrl.equals("null")) {
                Glide.with(itemView.getContext())
                        .load(user.mProfilePictureUrl)
                        .centerCrop()
                        .placeholder(R.drawable.img_profile_picture)
                        .into(profilePictureView);
            } else {
                profilePictureView.setImageResource(R.drawable.img_profile_picture);
            }

            userContainer.setOnClickListener(v -> {
                Intent profileIntent = new Intent(itemView.getContext(), ProfileActivity.class);
                profileIntent.putExtra(Extras.EXTRA_USER, user);
                itemView.getContext().startActivity(profileIntent);
            });

            userContainer.setOnLongClickListener(v -> {

                switch (selectedStateLayout.getVisibility()) {
                    case View.GONE:
                        mSelectedUsers.add(user);
                        selectedStateLayout.setVisibility(View.VISIBLE);
                        callButton.setVisibility(View.GONE);
                        break;

                    case View.VISIBLE:
                        mSelectedUsers.remove(user);
                        selectedStateLayout.setVisibility(View.GONE);
                        callButton.setVisibility(View.VISIBLE);
                        break;

                    case View.INVISIBLE:
                        break;
                }

                mUsersListener.initiateGroupCall(mSelectedUsers);

                return true;
            });

            callButton.setOnClickListener(v -> mUsersListener.initiatePersonalCall(user));
        }
    }
}
