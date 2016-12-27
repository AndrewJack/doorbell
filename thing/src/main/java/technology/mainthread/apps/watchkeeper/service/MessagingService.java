package technology.mainthread.apps.watchkeeper.service;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import technology.mainthread.apps.watchkeeper.data.CaptureEvent;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        CaptureEvent.getInstance().capture();
    }

}
