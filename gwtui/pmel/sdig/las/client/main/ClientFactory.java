package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.EventBus;


/**
 * ClientFactory helpful to use a factory or dependency injection framework like
 * GIN to obtain references to objects needed throughout your application like
 * the {@link EventBus}, {@link PlaceController} and views.
 */
public interface ClientFactory {

	EventBus getEventBus();

}
