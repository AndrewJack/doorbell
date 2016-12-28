package technology.mainthread.apps.watchkeeper.view;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.nio.ByteBuffer;
import java.util.UUID;

import rx.Subscription;
import rx.subscriptions.Subscriptions;
import technology.mainthread.apps.watchkeeper.BuildConfig;
import technology.mainthread.apps.watchkeeper.camera.DoorbellCamera;
import technology.mainthread.apps.watchkeeper.data.CaptureEvent;

/**
 * Capture activity that capture a picture from the Raspberry Pi 3
 * Camera on a button press and post it to Firebase and Google Cloud
 * Vision API.
 */
public class CaptureActivity extends Activity {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private DoorbellCamera mCamera;
    /**
     * A {@link Handler} for running Camera tasks in the background.
     */
    private Handler mCameraHandler;
    /**
     * An additional thread for running Camera tasks that shouldn't block the UI.
     */
    private HandlerThread mCameraThread;

    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            reader -> {
                Image image = reader.acquireLatestImage();
                // get image bytes
                ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                final byte[] imageBytes = new byte[imageBuf.remaining()];
                imageBuf.get(imageBytes);
                image.close();

                onPictureTaken(imageBytes);
            };
    private Subscription subscription = Subscriptions.empty();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Doorbell Activity created.");

        // We need permission to access the camera
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.d(TAG, "No permission");
            return;
        }

        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();

        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        // Camera code is complicated, so we've shoved it all in this closet class for you.
        mCamera = DoorbellCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

        subscription = CaptureEvent.getInstance().observe().subscribe(s -> {
            mCamera.takePicture();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.shutDown();

        mCameraThread.quitSafely();
        subscription.unsubscribe();
    }

    /**
     * Handle image processing in Firebase and Cloud Vision.
     */
    private void onPictureTaken(final byte[] imageBytes) {
        Log.d(TAG, "onPictureTaken");
        if (imageBytes != null) {
            String fileName = String.format("%s.jpg", UUID.randomUUID());
            StorageReference storageRef = mStorage.getReferenceFromUrl(BuildConfig.BUCKET_URL).child(fileName);

            UploadTask uploadTask = storageRef.putBytes(imageBytes);
            uploadTask.addOnCompleteListener(task -> {
                UploadTask.TaskSnapshot result = task.getResult();
                if (result != null) {
                    final DatabaseReference log = mDatabase.getReference("logs").push();
                    // upload image to firebase
                    log.child("fileName").setValue(fileName);
                    log.child("timestamp").setValue(ServerValue.TIMESTAMP);
                }
            });
        }
    }
}