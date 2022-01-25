package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by rhs on 6/1/16.
 */
public class PanelControlOpen extends GwtEvent<PanelControlOpen.Handler> {

    public static final Type<PanelControlOpen.Handler> TYPE = new Type<PanelControlOpen.Handler>();

    int targetPanel;
    public PanelControlOpen(int targetPanel) {
        this.targetPanel = targetPanel;
    }

    @Override
    public Type<PanelControlOpen.Handler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(Handler handler) {
        handler.onPanelControlOpen(this);
    }

    public interface Handler extends EventHandler {
        public void onPanelControlOpen(PanelControlOpen event);
    }
    public int getTargetPanel() {
        return targetPanel;
    }
    public void setTargetPanel(int count) {
        this.targetPanel = targetPanel;
    }
}
