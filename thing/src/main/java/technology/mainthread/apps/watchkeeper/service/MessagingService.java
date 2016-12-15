package technology.mainthread.apps.watchkeeper.service;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        startService(CaptureIntentService.getCaptureIntent(this));
    }

}
