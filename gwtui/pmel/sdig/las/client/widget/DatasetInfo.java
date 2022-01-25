package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.addins.client.carousel.MaterialCarousel;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.ui.MaterialCard;
import gwt.material.design.client.ui.MaterialCardTitle;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialRow;
import pmel.sdig.las.client.event.Browse;
import pmel.sdig.las.client.event.Info;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.Variable;

public class DatasetInfo extends Composite {

    int count = 0;

    @UiField
    MaterialPanel rows;

    @UiField
    MaterialCardTitle title;
    @UiField
    MaterialIcon titleIcon;

    MaterialRow currentRow;
    MaterialCarousel carousel = new MaterialCarousel();

    Dataset dataset;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    interface DatasetInfoUiBinder extends UiBinder<MaterialPanel, DatasetInfo> {
    }

    private static DatasetInfoUiBinder ourUiBinder = GWT.create(DatasetInfoUiBinder.class);

    private void init() {
        MaterialPanel rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
        carousel.setSlidesToShow(4);
        carousel.setSlidesToScroll(5);
    }
    public DatasetInfo() {
        init();
    }

    public DatasetInfo(Dataset dataset) {
        this.dataset = dataset;
        init();
        configure(dataset);
    }

    public void configure(Dataset dataset) {

        Dataset parent = dataset.getParent();
        String parentTitle = "";
        if ( parent != null ) {
            parentTitle = parent.getTitle() + " - ";
        }
        title.setText(parentTitle + dataset.getTitle());
        title.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new Info(dataset.getId()), title);
            }
        });
        titleIcon.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new Info(dataset.getId()), title);
            }
        });
    }
    public void clear() {
        count = 0;
        rows.clear();
    }

    public void addVariable(Variable variable) {

        final VariableInfo info = new VariableInfo(dataset, variable);

        int position = count % 3;
        if (position == 0) {
            MaterialRow row = new MaterialRow();
            currentRow = row;
            rows.add(row);
        }
        currentRow.add(info);

        count++;

    }
}
