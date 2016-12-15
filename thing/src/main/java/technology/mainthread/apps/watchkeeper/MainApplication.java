package technology.mainthread.apps.watchkeeper;

import android.app.Application;

import technology.mainthread.apps.watchkeeper.service.RefreshFCMSubscriptionsService;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        startService(RefreshFCMSubscriptionsService.getRefreshFCMSubscriptionsIntent(this));
    }
}
