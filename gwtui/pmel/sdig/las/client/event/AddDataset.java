package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import pmel.sdig.las.shared.autobean.AddRequest;

/**
 * Created by rhs on 1/18/17.
 */
public class AddDataset extends GwtEvent<AddDataset.Handler> {

    AddRequest addRequest;

    public static Type<AddDataset.Handler> TYPE = new Type<AddDataset.Handler>();

    public AddDataset(AddRequest addRequest) {
        this.addRequest = addRequest;
    }


    public Type<AddDataset.Handler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(Handler handler) {
        handler.onAddDataset(this);
    }

    public interface Handler extends EventHandler {
        void onAddDataset(AddDataset event);
    }
    public AddRequest getAddRequest() {
        return this.addRequest;
    }
}

