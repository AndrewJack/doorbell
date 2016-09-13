package com.google.samples.mysample;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;

import java.util.Collections;

import static android.content.Context.CAMERA_SERVICE;

/**
 * Helper class to deal with methods to deal with images from the camera.
 */
public class DoorbellCamera {

    public static final String TAG = "DoorbellCamera";

    public CameraCaptureSession mCaptureSession;

    private CameraDevice mCameraDevice;

    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "Opened camera.");
            mCameraDevice = cameraDevice;
            prepareStillPicture(cameraDevice, mImageReader);
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "Camera disconnected, closing.");
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            Log.d(TAG, "Camera device error, closing.");
            cameraDevice.close();
        }

        @Override
        public void onClosed(CameraDevice cameraDevice) {
            Log.d(TAG, "Closed camera, releasing");
            mCameraDevice = null;
        }
    };

    // Lazy-loaded singleton, so only one instance of the camera is created.
    private DoorbellCamera() {}

    private static class InstanceHolder {
        private static DoorbellCamera mCamera = new DoorbellCamera();
    }

    public static DoorbellCamera getInstance() {
        return InstanceHolder.mCamera;
    }

    private CameraCaptureSession.CaptureCallback createCaptureCallback(
            final CameraDevice cameraDevice) {
        return new CameraCaptureSession.CaptureCallback() {

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                            @NonNull CaptureRequest request,
                                            @NonNull CaptureResult partialResult) {
                Log.d(TAG, "Partial result");
            }

            @Override
            public void onCaptureCompleted(CameraCaptureSession session,
                                           CaptureRequest request,
                                           TotalCaptureResult result) {
                Log.d(TAG, "Total result");
            }
        };
    }

    public void takePicture() {
        try {
            if (mCaptureSession == null) {
                Log.d(TAG, "Can't start capture, no capture session initialized!");
                return;
            }
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            CameraCaptureSession.CaptureCallback callback = createCaptureCallback(mCameraDevice);
            Log.d(TAG, "In takePicture, calling capture.");
            mCaptureSession.capture(captureBuilder.build(), callback, null);
            Log.d(TAG, "Capture started.");

        } catch (CameraAccessException cae) {
            Log.d(TAG, "Access exception while taking picture", cae);
        }
    }

    public void initializeCamera(Handler backgroundHandler,
                                 ImageReader.OnImageAvailableListener imageAvailableListener,
                                 Context context) {

        final int IMAGE_WIDTH = 320;
        final int IMAGE_HEIGHT = 240;
        final int maxImages = 1; // Number of images that can be read concurrently, not total.

        mBackgroundHandler = backgroundHandler;

        dumpFormatInfo(context);

        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                ImageFormat.JPEG, maxImages);

        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting IDs", e);
        }
        if (camIds.length < 1) {
            Log.d(TAG, "No cameras found");
            return;
        }
        String id = camIds[0];
        Log.d(TAG, "Using camera id " + id);

        mImageReader.setOnImageAvailableListener(
                imageAvailableListener, mBackgroundHandler);
        try {
            manager.openCamera(id, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException cae) {
            Log.d(TAG, "Camera access exception", cae);
        }
    }

    public void prepareStillPicture(final CameraDevice mCameraDevice,
                                           final ImageReader mImageReader ) {
        // Here, we create a CameraCaptureSession for capturing still images.
        try {
            mCameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (mCameraDevice == null) {
                                return;
                            }

                            // When the session is ready, we start capture.
                            mCaptureSession = cameraCaptureSession;
                            Log.d(TAG, "Session initialized.");
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.w(TAG, "Failed to configure camera");
                        }
                    }, null
            );
        } catch (CameraAccessException cae) {
            Log.d(TAG, "access exception while preparing pic", cae);
        }
    }

    /**
     * Helpful debugging method:  Dump all supported camera formats to log.  You don't need to run
     * this for normal operation, but it's very helpful when porting this code to different
     * hardware.
     */
    public static void dumpFormatInfo(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting IDs");
        }
        if (camIds.length < 1) {
            Log.d(TAG, "No cameras found");
        }
        String id = camIds[0];
        Log.d(TAG, "Using camera id " + id);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            StreamConfigurationMap configs = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            for (int format : configs.getOutputFormats()) {
                Log.d(TAG, "Getting sizes for format: " + format);
                for (Size s : configs.getOutputSizes(format)) {
                    Log.d(TAG, "\t" + s.toString());
                }
            }
            int[] effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
            for (int effect : effects) {
                Log.d(TAG, "Effect available: " + effect);
            }
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception getting characteristics.");
        }
    }

    public void shutDown() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }
}