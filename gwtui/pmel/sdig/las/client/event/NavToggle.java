package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by rhs on 4/21/17.
 */
public class NavToggle extends GwtEvent<NavToggleHandler> {
    public static Type<NavToggleHandler> TYPE = new Type<NavToggleHandler>();

    public Type<NavToggleHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(NavToggleHandler handler) {
        handler.onNavToggle(this);
    }
}
