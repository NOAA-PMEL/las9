package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by rhs on 9/13/15.
 */
public class BreadcrumbSelect extends GwtEvent<BreadcrumbSelect.Handler> {

    public static final Type<BreadcrumbSelect.Handler> TYPE = new Type<BreadcrumbSelect.Handler>();

    Object selected; // At the moment a Dataset or a Variable

    int targetPanel;

    public BreadcrumbSelect() {
    }

    public BreadcrumbSelect(Object selected, int targetPanel) {
        this.selected = selected;
        this.targetPanel = targetPanel;
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }
    @Override
    protected void dispatch(Handler handler) {
        handler.onBreadcrumbSelect(this);

    }
    public interface Handler extends EventHandler {
        public void onBreadcrumbSelect(BreadcrumbSelect event);
    }

    public Object getSelected() {
        return selected;
    }

    public void setSelected(Object selected) {
        this.selected = selected;
    }

    public int getTargetPanel() {
        return targetPanel;
    }

    public void setTargetPanel(int targetPanel) {
        this.targetPanel = targetPanel;
    }

}
