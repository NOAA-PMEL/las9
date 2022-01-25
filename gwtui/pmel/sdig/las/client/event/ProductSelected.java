package pmel.sdig.las.client.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by rhs on 2/10/17.
 */
public class ProductSelected extends GwtEvent<ProductSelectedHandler> {
    public static Type<ProductSelectedHandler> TYPE = new Type<ProductSelectedHandler>();

    public Type<ProductSelectedHandler> getAssociatedType() {
        return TYPE;
    }

    protected void dispatch(ProductSelectedHandler handler) {
        handler.onProductSelected(this);
    }
}
