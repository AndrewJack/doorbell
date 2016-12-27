package technology.mainthread.apps.watchkeeper.view;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
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
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;
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
     * A {@link Handler} for running Cloud tasks in the background.
     */
//    private Handler mCloudHandler;
    /**
     * An additional thread for running Cloud tasks that shouldn't block the UI.
     */
//    private HandlerThread mCloudThread;

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

//        mCloudThread = new HandlerThread("CloudThread");
//        mCloudThread.start();
//        mCloudHandler = new Handler(mCloudThread.getLooper());

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
//        mCloudThread.quitSafely();
        subscription.unsubscribe();
    }

    /**
     * Handle image processing in Firebase and Cloud Vision.
     */
    private void onPictureTaken(final byte[] imageBytes) {
        Log.d(TAG, "onPictureTaken");
        if (imageBytes != null) {
            StorageReference storageRef = mStorage.getReferenceFromUrl("gs://gatekeeper-kew.appspot.com").child(String.format("%s.jpg", UUID.randomUUID()));

            UploadTask uploadTask = storageRef.putBytes(imageBytes);
            uploadTask.addOnCompleteListener(task -> {
                UploadTask.TaskSnapshot result = task.getResult();
                if (result != null && result.getDownloadUrl() != null) {
                    Uri downloadUrl = result.getDownloadUrl();
                    final DatabaseReference log = mDatabase.getReference("logs").push();
                    // upload image to firebase
                    log.child("url").setValue(downloadUrl.toString());
                    log.child("timestamp").setValue(ServerValue.TIMESTAMP);
                }
            });

//            mCloudHandler.post(() -> {
//                Log.d(TAG, "sending image to cloud vision");
//                // annotate image by uploading to Cloud Vision API
//                try {
//                    Map<String, Float> annotations = CloudVisionUtils.annotateImage(imageBytes);
//                    Log.d(TAG, "cloud vision annotations:" + annotations);
//                    if (annotations != null) {
//                        log.child("annotations").setValue(annotations);
//                    }
//                } catch (IOException e) {
//                    Log.e(TAG, "Cloud Vison API error: ", e);
//                }
//            });
        }
    }
}