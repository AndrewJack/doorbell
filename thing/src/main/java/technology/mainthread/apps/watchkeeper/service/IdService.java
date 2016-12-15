package technology.mainthread.apps.watchkeeper.service;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceIdService;

public class IdService extends FirebaseInstanceIdService {

    private static final String TAG = IdService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {
        Log.d(TAG, "onTokenRefresh");
    }
}
