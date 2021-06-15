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
import net.intensecorp.meeteazy.listener.ActionListener;
import net.intensecorp.meeteazy.models.Contact;
import net.intensecorp.meeteazy.utils.Extras;
import net.intensecorp.meeteazy.utils.FormatterUtility;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.UserViewHolder> {

    public static List<Contact> mSelectedContacts;
    private List<Contact> mContacts;
    private ActionListener mActionListener;

    public ContactsAdapter(List<Contact> contacts, ActionListener actionListener) {
        this.mContacts = contacts;
        this.mActionListener = actionListener;
        mSelectedContacts = new ArrayList<>();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UserViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        if (mSelectedContacts.size() <= 0) {
            holder.selectedStateLayout.setVisibility(View.GONE);
            holder.callButton.setVisibility(View.VISIBLE);
        } else {
            holder.callButton.setVisibility(View.GONE);
        }

        holder.setContactData(mContacts.get(position));
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        private ConstraintLayout contactContainer;
        private MaterialTextView fullNameView;
        private MaterialTextView emailView;
        private CircleImageView profilePictureView;
        private ConstraintLayout selectedStateLayout;
        private ImageView callButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            contactContainer = itemView.findViewById(R.id.constraintLayout_user_container);
            fullNameView = itemView.findViewById(R.id.textView_full_name);
            emailView = itemView.findViewById(R.id.textView_email);
            profilePictureView = itemView.findViewById(R.id.circleImageView_user_profile_picture);
            selectedStateLayout = itemView.findViewById(R.id.constraintLayout_selected_state_layout);
            callButton = itemView.findViewById(R.id.imageView_call);
        }

        private void setContactData(Contact contact) {
            fullNameView.setText(FormatterUtility.getFullName(contact.mFirstName, contact.mLastName));
            emailView.setText(contact.mEmail);

            if (!contact.mProfilePictureUrl.equals("null")) {
                Glide.with(itemView.getContext())
                        .load(contact.mProfilePictureUrl)
                        .centerCrop()
                        .placeholder(R.drawable.img_profile_picture)
                        .into(profilePictureView);
            } else {
                profilePictureView.setImageResource(R.drawable.img_profile_picture);
            }

            contactContainer.setOnClickListener(v -> {
                Intent profileIntent = new Intent(itemView.getContext(), ProfileActivity.class);
                profileIntent.putExtra(Extras.EXTRA_USER, contact);
                itemView.getContext().startActivity(profileIntent);
            });

            contactContainer.setOnLongClickListener(v -> {

                switch (selectedStateLayout.getVisibility()) {
                    case View.GONE:
                        mSelectedContacts.add(contact);
                        selectedStateLayout.setVisibility(View.VISIBLE);
                        callButton.setVisibility(View.GONE);
                        break;

                    case View.VISIBLE:
                        mSelectedContacts.remove(contact);
                        selectedStateLayout.setVisibility(View.GONE);
                        callButton.setVisibility(View.VISIBLE);
                        break;

                    case View.INVISIBLE:
                        break;
                }

                mActionListener.handleSelection(mSelectedContacts);

                return true;
            });

            callButton.setOnClickListener(v -> mActionListener.initiatePersonalCall(contact));
        }
    }
}
