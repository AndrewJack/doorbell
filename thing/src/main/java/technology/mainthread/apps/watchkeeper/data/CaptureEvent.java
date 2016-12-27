package technology.mainthread.apps.watchkeeper.data;


import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

public class CaptureEvent {

    private final Subject<String, String> bus;

    public static CaptureEvent getInstance() {
        return new CaptureEvent();
    }

    private CaptureEvent() {
        bus = new SerializedSubject<>(PublishSubject.create());
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
