package net.intensecorp.meeteazy.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.button.MaterialButton;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.adapters.OnboardingPagerAdapter;

public class OnboardingActivity extends AppCompatActivity {

    private int mCurrentSlidePos;
    private ViewPager mOnboardingSlider;
    private MaterialButton mGetStartedButton;
    private MaterialButton mSkipButton;
    private MaterialButton mNextButton;
    ViewPager.OnPageChangeListener mChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            mCurrentSlidePos = position;

            if (position == 0) {
                mGetStartedButton.setVisibility(View.GONE);
                mSkipButton.setVisibility(View.VISIBLE);
                mNextButton.setVisibility(View.VISIBLE);
            } else if (position == 1) {
                mGetStartedButton.setVisibility(View.GONE);
                mSkipButton.setVisibility(View.VISIBLE);
                mNextButton.setVisibility(View.VISIBLE);
            } else if (position == 2) {
                mGetStartedButton.setVisibility(View.GONE);
                mSkipButton.setVisibility(View.VISIBLE);
                mNextButton.setVisibility(View.VISIBLE);
            } else if (position == 3) {
                if (mGetStartedButton.getVisibility() == View.VISIBLE) {
                    Animation skipButtonShowAnimation = AnimationUtils.loadAnimation(OnboardingActivity.this, R.anim.anim_fade_in_from_left_to_right_500);
                    mSkipButton.setAnimation(skipButtonShowAnimation);
                    mSkipButton.setVisibility(View.VISIBLE);

                    Animation nextButtonShowAnimation = AnimationUtils.loadAnimation(OnboardingActivity.this, R.anim.anim_fade_in_from_right_to_left_500);
                    mNextButton.setAnimation(nextButtonShowAnimation);
                    mNextButton.setVisibility(View.VISIBLE);

                    Animation getStartedButtonHideAnimation = AnimationUtils.loadAnimation(OnboardingActivity.this, R.anim.anim_fade_out_from_top_to_bottom_500);
                    mGetStartedButton.setAnimation(getStartedButtonHideAnimation);
                    mGetStartedButton.setVisibility(View.GONE);
                }
            } else {
                Animation skipButtonHideAnimation = AnimationUtils.loadAnimation(OnboardingActivity.this, R.anim.anim_fade_out_from_right_to_left_500);
                mSkipButton.setAnimation(skipButtonHideAnimation);
                mSkipButton.setVisibility(View.GONE);

                Animation nextButtonHideAnimation = AnimationUtils.loadAnimation(OnboardingActivity.this, R.anim.anim_fade_out_from_left_to_right_500);
                mNextButton.setAnimation(nextButtonHideAnimation);
                mNextButton.setVisibility(View.GONE);

                Animation getStartedButtonShowAnimation = AnimationUtils.loadAnimation(OnboardingActivity.this, R.anim.anim_fade_in_from_bottom_to_top_500);
                mGetStartedButton.setAnimation(getStartedButtonShowAnimation);
                mGetStartedButton.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        mOnboardingSlider = findViewById(R.id.viewPager_onboarding_slider_item);
        WormDotsIndicator wormDotsIndicator = findViewById(R.id.wormDotsIndicator);
        mGetStartedButton = findViewById(R.id.button_get_started);
        mSkipButton = findViewById(R.id.button_skip);
        mNextButton = findViewById(R.id.button_next);

        OnboardingPagerAdapter onboardingPagerAdapter = new OnboardingPagerAdapter(OnboardingActivity.this);
        mOnboardingSlider.setAdapter(onboardingPagerAdapter);
        wormDotsIndicator.setViewPager(mOnboardingSlider);
        mOnboardingSlider.addOnPageChangeListener(mChangeListener);

        mNextButton.setOnClickListener(v -> mOnboardingSlider.setCurrentItem(mCurrentSlidePos + 1));

        mSkipButton.setOnClickListener(v -> mOnboardingSlider.setCurrentItem(mCurrentSlidePos = 4));

        mGetStartedButton.setOnClickListener(v -> startSignUpActivity());
    }

    private void startSignUpActivity() {
        Intent signUpIntent = new Intent(getApplicationContext(), SignUpActivity.class);
        signUpIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(signUpIntent);
        finish();
    }
}