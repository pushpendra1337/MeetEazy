package net.intensecorp.meeteazy.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import net.intensecorp.meeteazy.R;

public class SnackbarUtility {

    private final Activity mActivity;
    private Snackbar mSnackbar;

    public SnackbarUtility(Activity activity) {
        this.mActivity = activity;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void snackbar(int messageResId) {
        mSnackbar = Snackbar.make(((ViewGroup) mActivity.findViewById(android.R.id.content)).getChildAt(0), "", Snackbar.LENGTH_SHORT);
        @SuppressLint("InflateParams") View view = mActivity.getLayoutInflater().inflate(R.layout.snackbar_without_action, null);
        MaterialTextView snackbarText = view.findViewById(R.id.textView_snackbar_text);
        snackbarText.setText(messageResId);
        ViewCompat.setElevation(view, 6f);
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) mSnackbar.getView();
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT);
        snackbarLayout.setPadding(0, 0, 0, 0);
        snackbarLayout.addView(view, 0);
        mSnackbar.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void snackbar(int messageResId, int actionTextResId, View.OnClickListener onClickListener) {
        mSnackbar = Snackbar.make(((ViewGroup) mActivity.findViewById(android.R.id.content)).getChildAt(0), "", Snackbar.LENGTH_LONG);
        @SuppressLint("InflateParams") View view = mActivity.getLayoutInflater().inflate(R.layout.snackbar_with_action, null);
        MaterialTextView snackbarText = view.findViewById(R.id.textView_snackbar_text);
        MaterialButton snackbarActionButton = view.findViewById(R.id.button_snackbar_action);
        snackbarText.setText(messageResId);
        snackbarActionButton.setText(actionTextResId);
        snackbarActionButton.setOnClickListener(onClickListener);
        ViewCompat.setElevation(view, 6f);
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) mSnackbar.getView();
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT);
        snackbarLayout.setPadding(0, 0, 0, 0);
        snackbarLayout.addView(view, 0);
        mSnackbar.show();
    }
}

