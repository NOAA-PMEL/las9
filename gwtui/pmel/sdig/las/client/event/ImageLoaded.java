package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by rhs on 9/13/15.
 */
public class ImageLoaded extends GwtEvent<ImageLoaded.Handler> {

    public static final Type<ImageLoaded.Handler> TYPE = new Type<ImageLoaded.Handler>();


    public ImageLoaded() {
        super();
    }
    @Override
    public Type<Handler> getAssociatedType() {
        return TYPE;
    }
    @Override
    protected void dispatch(Handler handler) {
        handler.onImageLoaded(this);

    }
    public interface Handler extends EventHandler {
        public void onImageLoaded(ImageLoaded event);
    }
}
