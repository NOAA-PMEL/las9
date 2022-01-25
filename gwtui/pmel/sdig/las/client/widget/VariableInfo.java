package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.ui.MaterialCardTitle;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialImage;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialProgress;
import pmel.sdig.las.client.event.NavSelect;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.util.Util;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.GeoAxisX;
import pmel.sdig.las.shared.autobean.GeoAxisY;
import pmel.sdig.las.shared.autobean.TimeAxis;
import pmel.sdig.las.shared.autobean.Variable;
import pmel.sdig.las.shared.autobean.VerticalAxis;

public class VariableInfo extends Composite {

    @UiField
    MaterialCardTitle title;
    @UiField
    MaterialIcon titleIcon;

    @UiField
    MaterialImage thumbnail;

    @UiField
    MaterialLabel lonMin;
    @UiField
    MaterialLabel lonMax;

    @UiField
    MaterialLabel latMin;
    @UiField
    MaterialLabel latMax;

    @UiField
    MaterialLabel timeStart;
    @UiField
    MaterialLabel timeEnd;

    @UiField
    MaterialLabel zMin;
    @UiField
    MaterialLabel zMax;

    @UiField
    MaterialProgress loader;

    Variable variable;
    Dataset dataset;

    String url = "";

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    interface VariableInfoUiBinder extends UiBinder<MaterialColumn, VariableInfo> {
    }

    private static VariableInfoUiBinder ourUiBinder = GWT.create(VariableInfoUiBinder.class);

    private void init() {
        MaterialColumn rootElement = ourUiBinder.createAndBindUi(this);
        initWidget(rootElement);
    }
    public VariableInfo() {
        init();
    }
    public VariableInfo(Dataset d, Variable v) {
        init();
        this.dataset = d;
        this.variable = v;
        // We know how to construct the URL, but we don't want to try to generate them if they don't already exist.
        // So use it if it's defined, otherwise
        // TODO maybe should trigger an async call generate it for next time.

        //        url = "product/thumbnail/"+dataset.getHash()+"/"+variable.getHash();
        url = variable.getThumbnail();
        if ( url != null && !url.isEmpty() ) {
            thumbnail.setDisplay(Display.BLOCK);
            thumbnail.setUrl(url);
        }
        title.setText(variable.getTitle());
        thumbnail.addLoadHandler(new LoadHandler() {
            @Override
            public void onLoad(LoadEvent loadEvent) {
                loader.setDisplay(Display.NONE);
            }
        });
        GeoAxisX x = d.getGeoAxisX();
        GeoAxisY y = d.getGeoAxisY();
        VerticalAxis z = d.getVerticalAxis();
        TimeAxis t = d.getTimeAxis();
        if ( x != null ) {
            lonMin.setText(Util.format_two(x.getMin()));
            lonMax.setText(Util.format_two(x.getMax()));
        }
        if ( y != null ) {
            latMin.setText(Util.format_two(y.getMin()));
            latMax.setText(Util.format_two(y.getMax()));
        }
        if ( z != null ) {
            zMin.setText(Util.format_two(z.getMin()));
        } else {
            zMin.setText("n/a");
        }
        if ( t != null ) {
            timeEnd.setText(t.getEnd());
            timeStart.setText(t.getStart());
        } else {
            timeStart.setText("n/a");
        }
        titleIcon.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new NavSelect(variable, 1), VariableInfo.this);
            }
        });
        title.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new NavSelect(variable, 1), VariableInfo.this);
            }
        });
        thumbnail.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                eventBus.fireEventFromSource(new NavSelect(variable, 1), VariableInfo.this);
            }
        });
    }
    public Variable getVariable() {
        return this.variable;
    }
    public Dataset getDataset() {
        return this.dataset;
    }
}