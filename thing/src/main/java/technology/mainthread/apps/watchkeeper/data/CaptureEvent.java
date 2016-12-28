package technology.mainthread.apps.watchkeeper.data;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

public class CaptureEvent {

    private static CaptureEvent instance;
    private final Subject<String, String> bus;

    private CaptureEvent() {
        bus = new SerializedSubject<>(PublishSubject.create());
    }

    public static CaptureEvent getInstance() {
        if (instance == null) {
            instance = new CaptureEvent();
        }
        return instance;
    }

    public void capture() {
        bus.onNext("CAPTURE");
    }

    public Observable<String> observe() {
        return bus;
    }

    public boolean hasObservers() {
        return bus.hasObservers();
    }
}
