package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by rhs on 6/1/16.
 */
public class PanelCount extends GwtEvent<PanelCount.Handler> {

    public static final Type<PanelCount.Handler> TYPE = new Type<PanelCount.Handler>();

    int count;
    public PanelCount(int count ) {
        this.count = count;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<PanelCount.Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onPanelCountChange(this);
    }
    public interface Handler extends EventHandler {
        public void onPanelCountChange(PanelCount event);
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
}
