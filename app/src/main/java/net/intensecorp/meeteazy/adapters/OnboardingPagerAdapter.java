package net.intensecorp.meeteazy.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;

import com.google.android.material.textview.MaterialTextView;

import net.intensecorp.meeteazy.R;

public class OnboardingPagerAdapter extends PagerAdapter {

    private static final int[] ONBOARDING_ILLUSTRATIONS = {
            R.drawable.img_video_meeting,
            R.drawable.img_screen_sharing,
            R.drawable.img_calling,
            R.drawable.img_add_contact,
            R.drawable.img_pip_mode
    };

    private static final int[] ONBOARDING_HEADINGS = {
            R.string.onboarding_heading_first_slide,
            R.string.onboarding_heading_second_slide,
            R.string.onboarding_heading_third_slide,
            R.string.onboarding_heading_fourth_slide,
            R.string.onboarding_heading_fifth_slide
    };

    private static final int[] ONBOARDING_DESCRIPTIONS = {
            R.string.onboarding_description_first_slide,
            R.string.onboarding_description_second_slide,
            R.string.onboarding_description_third_slide,
            R.string.onboarding_description_fourth_slide,
            R.string.onboarding_description_fifth_slide
    };

    Context mContext;
    LayoutInflater mLayoutInflater;

    public OnboardingPagerAdapter(Context context) {
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return ONBOARDING_HEADINGS.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mLayoutInflater.inflate(R.layout.item_onboarding_slider, container, false);

        ImageView illustration = view.findViewById(R.id.imageView_onboarding_slider_image);
        MaterialTextView heading = view.findViewById(R.id.textView_onboarding_slider_heading);
        MaterialTextView description = view.findViewById(R.id.textView_slider_description);

        illustration.setImageResource(ONBOARDING_ILLUSTRATIONS[position]);
        heading.setText(ONBOARDING_HEADINGS[position]);
        description.setText(ONBOARDING_DESCRIPTIONS[position]);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((ConstraintLayout) object);
    }
}