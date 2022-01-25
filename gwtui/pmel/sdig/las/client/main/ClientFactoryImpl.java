package pmel.sdig.las.client.main;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;

/**
 * Created by rhs on 9/13/15.
 */
public class ClientFactoryImpl implements ClientFactory {
    private static final EventBus eventBus = new SimpleEventBus();
    public ClientFactoryImpl()  {
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

}
