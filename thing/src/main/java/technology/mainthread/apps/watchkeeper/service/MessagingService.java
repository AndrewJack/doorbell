package technology.mainthread.apps.watchkeeper.service;

import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import technology.mainthread.apps.watchkeeper.data.CaptureEvent;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (!TextUtils.isEmpty(remoteMessage.getFrom())) {
            CaptureEvent.getInstance().capture();
        }
    }

}
