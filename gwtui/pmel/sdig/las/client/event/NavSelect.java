package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by rhs on 9/13/15.
 */
public class NavSelect extends GwtEvent<NavSelect.Handler> {

    public static final Type<NavSelect.Handler> TYPE = new Type<NavSelect.Handler>();

    Object selected;
    int targetPanel;

    public NavSelect() {
    }

    public NavSelect(Object selected, int targetPanel) {
        this.selected = selected;
        this.targetPanel = targetPanel;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
        return TYPE;
    }
    @Override
    protected void dispatch(Handler handler) {
        handler.onNavSelect(this);

    }
    public interface Handler extends EventHandler {
        public void onNavSelect(NavSelect event);
    }
    public Object getSelected() {
        return selected;
    }
    public int getTargetPanel() {
        return targetPanel;
    }
    public void setTargetPanel(int targetPanel) {
        this.targetPanel = targetPanel;
    }
}
