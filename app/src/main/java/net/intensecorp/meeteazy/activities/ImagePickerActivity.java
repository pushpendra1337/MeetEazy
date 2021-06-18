package net.intensecorp.meeteazy.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.yalantis.ucrop.UCrop;

import net.intensecorp.meeteazy.R;
import net.intensecorp.meeteazy.utils.Extras;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static androidx.core.content.FileProvider.getUriForFile;

public class ImagePickerActivity extends AppCompatActivity {

    public static final int REQUEST_IMAGE = 100;
    public static final int REQUEST_CAMERA = 0;
    public static final int REQUEST_GALLERY = 1;
    private static final String TAG = ImagePickerActivity.class.getSimpleName();
    private static String sFileName;
    private boolean mIsAspectRatioLocked;
    private boolean mIsBitmapHasCustomSize;
    private int mAspectRatioX;
    private int mAspectRatioY;
    private int mBitmapMaxWidth;
    private int mBitmapMaxHeight;
    private int mImageCompressionQuality;

    public static void showSetProfilePictureBottomSheetDialog(Context context, OptionListener optionListener) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.StyleBottomSheetDialog);
        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_dialog_set_profile_picture, bottomSheetDialog.findViewById(R.id.linearLayout_bottom_sheet_dialog_container));

        bottomSheetView.findViewById(R.id.linearLayout_take_a_photo).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            optionListener.onTakeAPhotoSelected();
        });

        bottomSheetView.findViewById(R.id.linearLayout_select_from_gallery).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            optionListener.onSelectFromGallerySelected();
        });

        bottomSheetView.findViewById(R.id.linearLayout_remove_current_photo).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            optionListener.onRemoveCurrentPhotoSelected();
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private static String queryName(ContentResolver resolver, Uri uri) {
        Cursor returnCursor = resolver.query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }

    public static void clearCache(Context context) {
        File path = new File(context.getExternalCacheDir(), "camera");
        if (path.exists() && path.isDirectory()) {
            for (File child : Objects.requireNonNull(path.listFiles())) {
                child.delete();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_picker);

        Intent imagePickerIntent = getIntent();
        mAspectRatioX = imagePickerIntent.getIntExtra(Extras.EXTRA_ASPECT_RATIO_X, 16);
        mAspectRatioY = imagePickerIntent.getIntExtra(Extras.EXTRA_ASPECT_RATIO_Y, 9);
        mImageCompressionQuality = imagePickerIntent.getIntExtra(Extras.EXTRA_IMAGE_COMPRESSION_QUALITY, 80);
        mIsAspectRatioLocked = imagePickerIntent.getBooleanExtra(Extras.EXTRA_IS_ASPECT_RATIO_LOCKED, false);
        mIsBitmapHasCustomSize = imagePickerIntent.getBooleanExtra(Extras.EXTRA_IS_BITMAP_HAS_CUSTOM_SIZE, false);
        mBitmapMaxWidth = imagePickerIntent.getIntExtra(Extras.EXTRA_BITMAP_MAX_WIDTH, 1000);
        mBitmapMaxHeight = imagePickerIntent.getIntExtra(Extras.EXTRA_BITMAP_MAX_HEIGHT, 1000);
        int requestCode = imagePickerIntent.getIntExtra(Extras.EXTRA_IMAGE_PICKER_REQUEST, -1);

        if (requestCode == REQUEST_CAMERA) {
            takeAPhoto();
        } else {
            selectImageFromGallery();
        }
    }

    private void takeAPhoto() {
        Dexter.withContext(ImagePickerActivity.this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @SuppressLint("QueryPermissionsNeeded")
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            sFileName = System.currentTimeMillis() + ".jpg";
                            Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, getCachedImagePath(sFileName));
                            if (imageCaptureIntent.resolveActivity(getPackageManager()) != null) {
                                startActivityForResult(imageCaptureIntent, REQUEST_CAMERA);
                            }
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .check();
    }

    private void selectImageFromGallery() {
        Dexter.withContext(ImagePickerActivity.this)
                .withPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickImageIntent, REQUEST_GALLERY);
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .check();

    }

    private Uri getCachedImagePath(String fileName) {
        File file = new File(getExternalCacheDir(), "camera");
        if (!file.exists()) file.mkdirs();
        File imageFile = new File(file, fileName);
        return getUriForFile(ImagePickerActivity.this, getPackageName() + ".provider", imageFile);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    cropImage(getCachedImagePath(sFileName));
                } else {
                    setResultCancelled();
                }
                break;
            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK) {
                    Uri imageUri = data.getData();
                    cropImage(imageUri);
                } else {
                    setResultCancelled();
                }
                break;
            case UCrop.REQUEST_CROP:
                if (resultCode == RESULT_OK) {
                    handleUCropResult(data);
                } else {
                    setResultCancelled();
                }
                break;
            case UCrop.RESULT_ERROR:
                final Throwable cropError = UCrop.getError(data);
                Log.e(TAG, "Crop error: " + cropError);
                setResultCancelled();
                break;
            default:
                setResultCancelled();
                break;
        }
    }

    private void cropImage(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), queryName(getContentResolver(), sourceUri)));

        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(mImageCompressionQuality);
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.colorStatusBar));
        options.setToolbarColor(ContextCompat.getColor(this, R.color.colorToolbarBackground));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.colorPrimary));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.colorPrimary));

        if (mIsAspectRatioLocked)
            options.withAspectRatio(mAspectRatioX, mAspectRatioY);

        if (mIsBitmapHasCustomSize)
            options.withMaxResultSize(mBitmapMaxWidth, mBitmapMaxHeight);

        UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .start(this);
    }

    private void handleUCropResult(Intent data) {
        if (data == null) {
            setResultCancelled();
            return;
        }
        final Uri resultUri = UCrop.getOutput(data);
        setResultOk(resultUri);
    }

    private void setResultOk(Uri imageUri) {
        Intent intent = new Intent();
        intent.putExtra("path", imageUri);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void setResultCancelled() {
        Intent intent = new Intent();
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }

    public interface OptionListener {
        void onTakeAPhotoSelected();

        void onSelectFromGallerySelected();

        void onRemoveCurrentPhotoSelected();
    }
}
