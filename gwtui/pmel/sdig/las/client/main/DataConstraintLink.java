package pmel.sdig.las.client.main;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import gwt.material.design.client.ui.MaterialLink;
import pmel.sdig.las.shared.autobean.DataConstraint;

public class DataConstraintLink extends MaterialLink {
    DataConstraint dataConstraint;
    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();
    public DataConstraintLink(String type, String lhs, String op, String rhs) {
        dataConstraint = new DataConstraint(type, lhs, op, rhs);
        setText("(x) " + dataConstraint.getAsString() );
        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new ChangeConstraint("remove", "", "", "", ""), DataConstraintLink.this);
            }
        });
        addStyleName("constraintHeight");
    }

    public DataConstraint getDataConstraint() {
        return dataConstraint;
    }

    public void setDataConstraint(DataConstraint dataConstraint) {
        this.dataConstraint = dataConstraint;
    }
}
