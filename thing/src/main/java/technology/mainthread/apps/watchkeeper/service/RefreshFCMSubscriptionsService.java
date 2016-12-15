package technology.mainthread.apps.watchkeeper.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessaging;

import rx.Observable;
import technology.mainthread.apps.watchkeeper.R;

public class RefreshFCMSubscriptionsService extends IntentService {

    private static final String ACTION_REFRESH_FCM_SUBSCRIPTIONS = "ACTION_REFRESH_FCM_SUBSCRIPTIONS";
    private FirebaseMessaging messaging;

    public RefreshFCMSubscriptionsService() {
        super(RefreshFCMSubscriptionsService.class.getSimpleName());
    }

    public static Intent getRefreshFCMSubscriptionsIntent(Context context) {
        Intent intent = new Intent(context, RefreshFCMSubscriptionsService.class);
        intent.setAction(ACTION_REFRESH_FCM_SUBSCRIPTIONS);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        messaging = FirebaseMessaging.getInstance();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_REFRESH_FCM_SUBSCRIPTIONS.equals(intent.getAction())) {
            refreshSubscriptions();
        }
    }

    private void refreshSubscriptions() {
        String[] allSubscriptions = getResources().getStringArray(R.array.notif_subscriptions_values);
        Observable.from(allSubscriptions)
                .forEach(subscription -> messaging.subscribeToTopic(subscription));

    }
}
