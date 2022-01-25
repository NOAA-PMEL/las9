package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import gwt.material.design.addins.client.window.MaterialWindow;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.Display;
import gwt.material.design.client.constants.IconPosition;
import gwt.material.design.client.constants.IconType;
import gwt.material.design.client.constants.ProgressType;
import gwt.material.design.client.events.CollapseEvent;
import gwt.material.design.client.events.ExpandEvent;
import gwt.material.design.client.ui.MaterialCheckBox;
import gwt.material.design.client.ui.MaterialCollapsible;
import gwt.material.design.client.ui.MaterialCollapsibleBody;
import gwt.material.design.client.ui.MaterialCollapsibleItem;
import gwt.material.design.client.ui.MaterialCollection;
import gwt.material.design.client.ui.MaterialColumn;
import gwt.material.design.client.ui.MaterialIcon;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialSwitch;
import gwt.material.design.client.ui.MaterialTextBox;
import gwt.material.design.client.ui.html.Div;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;
import pmel.sdig.las.client.event.AutoColors;
import pmel.sdig.las.client.event.BreadcrumbSelect;
import pmel.sdig.las.client.event.PanelControlOpen;
import pmel.sdig.las.client.event.PlotOptionChange;
import pmel.sdig.las.client.event.Search;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.client.map.OLMapWidget;
import pmel.sdig.las.client.state.State;
import pmel.sdig.las.client.util.Constants;
import pmel.sdig.las.shared.autobean.Annotation;
import pmel.sdig.las.shared.autobean.AnnotationGroup;
import pmel.sdig.las.shared.autobean.Dataset;
import pmel.sdig.las.shared.autobean.SearchRequest;
import pmel.sdig.las.shared.autobean.SearchResults;
import pmel.sdig.las.shared.autobean.Site;
import pmel.sdig.las.shared.autobean.TimeAxis;
import pmel.sdig.las.shared.autobean.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by rhs on 1/6/17.
 */
public class ComparePanel extends Composite {

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    @UiField
    MaterialColumn panel;

    @UiField
    OutputPanel outputPanel;

    @UiField
    Div chart;

    @UiField
    MaterialPanel breadcrumbs;

    @UiField
    MaterialCollapsibleBody annotations;


    @UiField
    MaterialPanel annotationPanel;

    @UiField
    MaterialCollapsible annotationsCollapse;

    @UiField
    MaterialIcon panelHome;

    @UiField
    MaterialSwitch difference;


    @UiField
    MaterialTextBox autocolors;
    @UiField
    MaterialCheckBox useAutoColors;

    @UiField
    MaterialTextBox txtSearch;

    MaterialIcon searchIcon;

    @UiField
    MaterialIcon trigger;


    Dataset dataset;
    Variable variable; // ?? do we need the list?
    Variable newVariable; // Used to hold new variable when switching...

    double xlo;
    double xhi;
    double ylo;
    double yhi;
    String zlo;
    String zhi;
    String tlo;
    String thi;

    int index;

    @UiField
    MaterialWindow settingsWindow;
    @UiField
    MaterialCollection panelDatasets;
    @UiField
    MaterialCollapsible navcollapsible;
    @UiField
    MaterialCollapsibleItem dataItem;
    @UiField
    MaterialPanel mapPanel;
    @UiField
    MaterialPanel dateTimePanel;
    @UiField
    MaterialPanel zaxisPanel;
    @UiField
    MaterialIcon back;

    MaterialIcon gear = new MaterialIcon(IconType.SETTINGS);
    List<Breadcrumb> holdBreadcrumbs = new ArrayList<>();

    String tile_server;
    String tile_layer;

    OLMapWidget refMap;
    public DateTimeWidget dateTimeWidget = new DateTimeWidget();
    AxisWidget zAxisWidget = new AxisWidget();

    String view;

    interface ComparePanelUiBinder extends UiBinder<MaterialColumn, ComparePanel> {
    }

    private static ComparePanelUiBinder ourUiBinder = GWT.create(ComparePanelUiBinder.class);

    public ComparePanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        annotationsCollapse.addExpandHandler(new ExpandEvent.ExpandHandler<MaterialCollapsibleItem>() {
            @Override
            public void onExpand(ExpandEvent<MaterialCollapsibleItem> expandEvent) {
                trigger.setIconType(IconType.EXPAND_LESS);
            }
        });
        annotationsCollapse.addCollapseHandler(new CollapseEvent.CollapseHandler<MaterialCollapsibleItem>() {
            @Override
            public void onCollapse(CollapseEvent<MaterialCollapsibleItem> collapseEvent) {
                trigger.setIconType(IconType.EXPAND_MORE);
            }
        });

        txtSearch.getLabel().addClickHandler(event -> {
            String search = txtSearch.getText();
            startSearch(search);
        });

        txtSearch.addKeyPressHandler(event -> {
            if ( event.getCharCode() == KeyCodes.KEY_ENTER ) {
                String search = txtSearch.getText();
                startSearch(search);
            }
        });



        // Initialize the local axes widgets for to set axes orthogonal to the view...
        Element wmsserver = DOM.getElementById("wms-server");
        if ( wmsserver != null )
            tile_server = wmsserver.getPropertyString("content");
        Element wmslayer = DOM.getElementById("wms-layer");
        if ( wmslayer != null )
            tile_layer = wmslayer.getPropertyString("content");

        refMap = new OLMapWidget("128px", "256px", tile_server, tile_layer);

        mapPanel.add(refMap);
        dateTimePanel.add(dateTimeWidget);
        zaxisPanel.add(zAxisWidget);



        gear.setIconPosition(IconPosition.LEFT);
        gear.setDisplay(Display.INLINE);
        gear.setIconColor(Color.BLUE);
        gear.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openSettings();
                holdBreadcrumbs = getBreadcrumbs();
                for (int i = 0; i < holdBreadcrumbs.size(); i++) {
                    removeBreadcrumb(holdBreadcrumbs.get(i));
                }
                back.setVisible(false);
                // Always start at the top when the window opens
                navcollapsible.setActive(1, true);
                navcollapsible.setActive(1, true);
                eventBus.fireEventFromSource(new PanelControlOpen(index), ComparePanel.this);
                event.stopPropagation();
            }
        });

        settingsWindow.addCloseHandler(new CloseHandler() {
            @Override
            public void onClose(CloseEvent event) {
                Object currentEnd = null;
                if (getBreadcrumbs().size() > 0) {
                    currentEnd = getBreadcrumbs().get(getBreadcrumbs().size() - 1).getSelected();
                }
                if (getBreadcrumbs().size() <= 0 || (currentEnd != null && !(currentEnd instanceof Variable))) {
                    // If it's not a variable, clear it out and put back what was before
                    for (int i = 0; i < getBreadcrumbs().size(); i++) {
                        removeBreadcrumb(getBreadcrumbs().get(i));
                    }
                    for (int i = 0; i < holdBreadcrumbs.size(); i++) {
                        addBreadcrumb(holdBreadcrumbs.get(i));
                    }
                }

            }
        });
        breadcrumbs.add(gear);
        panelHome.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dataItem.showProgress(ProgressType.INDETERMINATE);
                eventBus.fireEventFromSource(new PanelControlOpen(index), ComparePanel.this);
                event.stopPropagation();
            }
        });
        difference.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                eventBus.fireEventFromSource(new PlotOptionChange(), event.getSource());
            }
        });
        searchIcon = txtSearch.getIcon();
        searchIcon.addClickHandler(onSearchClick);
        searchIcon.setPaddingTop(20);
    }
    public MethodCallback<Site> siteCallback = new MethodCallback<Site>() {

        public void onSuccess(Method method, Site site) {
            panelDatasets.clear();
            clearBreadcrumbs();
            dataItem.hideProgress();
            if ( site.getDatasets().size() > 0 ) {
                List<Dataset> siteDatasets = site.getDatasets();
                Collections.sort(siteDatasets);
                for (int i = 0; i < siteDatasets.size(); i++) {
                    final Dataset d = siteDatasets.get(i);
                    DataItem dataItem = new DataItem(d, index);
                    // TODO I don't know why the width for these links defaults to just the size of text here,
                    //      but fills the space in the nav. For now I'm forcing it to be the same size as the nav.
                    dataItem.link.setWidth("319px");
                    panelDatasets.add(dataItem);
                }

            }

        }
        public void onFailure(Method method, Throwable exception) {
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
            dataItem.hideProgress();
        }
    };
    public void openSettings() {
        navcollapsible.setActive(1, true);
        settingsWindow.setLayoutPosition(Style.Position.ABSOLUTE);
        settingsWindow.setLeft(gear.getAbsoluteLeft());
        settingsWindow.setTop(gear.getAbsoluteTop());
        settingsWindow.setWidth(Constants.navWidth+"px");
        settingsWindow.open();
    }
    public void closeSettings() {
        if ( settingsWindow.isOpen() ) {
            settingsWindow.close();
        }
    }
    private List<DataItem> getDataItems() {
        List<DataItem> dataItems = new ArrayList<>();
        for (int i = 0; i < panelDatasets.getWidgetCount(); i++) {
            Widget w = panelDatasets.getWidget(i);
            if ( w instanceof DataItem ) {
                dataItems.add((DataItem)w);
            }
        }
        return dataItems;
    }
    public MethodCallback <SearchResults> searchCallback = new MethodCallback<SearchResults>() {
        @Override
        public void onFailure(Method method, Throwable throwable) {
            Window.alert("Unable to perform search.");
        }

        @Override
        public void onSuccess(Method method, SearchResults searchResults) {
            List<Dataset> searchDatasets = searchResults.getDatasetList();
            if (searchDatasets != null && searchDatasets.size() > 0) {
                dataItem.hideProgress();
                panelDatasets.clear();
                Collections.sort(searchDatasets);
                for (int i = 0; i < searchDatasets.size(); i++) {
                    Dataset d = searchDatasets.get(i);
                    DataItem dataItem = new DataItem(d, index);
                    dataItem.link.setWidth("319px");
                    panelDatasets.add(dataItem);
                }
            } else {
                Window.alert("Not matching data sets found for your search terms.");
            }
        }
    };
    public MethodCallback<Dataset> datasetCallback = new MethodCallback<Dataset>() {
        @Override
        public void onFailure(Method method, Throwable exception) {
            dataItem.hideProgress();
            Window.alert("Failed to download data set information for this dataset." + exception.getMessage());
        }

        @Override
        public void onSuccess(Method method, Dataset newDataset) {
            navcollapsible.setActive(1, true);
            dataItem.hideProgress();
            panelDatasets.clear();
            dataset = newDataset;
            if ( dataset.getDatasets().size() > 0 ) {
                List<Dataset> returnedDatasets = dataset.getDatasets();
                Collections.sort(returnedDatasets);
                for (int i = 0; i < returnedDatasets.size(); i++) {
                    Dataset d = returnedDatasets.get(i);
                    DataItem dataItem = new DataItem(d, index);
                    panelDatasets.add(dataItem);
                }
            }
            if ( dataset.getVariables().size() > 0 ) {
                List<Variable> variables = dataset.getVariables();
                Collections.sort(variables);
                for (int i = 0; i < variables.size(); i++) {
                    Variable v = variables.get(i);
                    DataItem dataItem = new DataItem(v, index);
                    panelDatasets.add(dataItem);
                }
            }
            dataItem.setActive(true);
        }
    };
    public void clearAnnotations() {
        annotationPanel.clear();
    }
    public void addAnnotation(Widget w) {
        annotationPanel.add(w);
    }
    public void setState(State state) {
        String error = state.getPanelState(this.getTitle()).getResultSet().getError();
        annotationPanel.clear();
        if ( error != null && !error.equals("") ) {
            MaterialLabel l = new MaterialLabel();
            l.setText(error);
            annotationPanel.add(l);
            outputPanel.plotImage = null;
            outputPanel.clearPlot();
        } else {
            outputPanel.setState(state);
            List<AnnotationGroup> groups = state.getPanelState(this.getTitle()).getResultSet().getAnnotationGroups();
            annotationPanel.clear();
            for (Iterator<AnnotationGroup> gIt = groups.iterator(); gIt.hasNext(); ) {
                AnnotationGroup ag = gIt.next();
                for (Iterator<Annotation> aIt = ag.getAnnotations().iterator(); aIt.hasNext(); ) {
                    MaterialLabel l = new MaterialLabel();
                    Annotation a = aIt.next();
                    l.setText(a.getValue());
                    annotationPanel.add(l);
                }
            }
        }
        annotationsCollapse.open(1);
    }
    public void openAnnotations() {
        annotationsCollapse.open(1);
    }
    public OutputPanel getOutputPanel() {
        return outputPanel;
    }
    public Div getChart() { return chart; }

    public void clearBreadcrumbs() {
//        holdBreadcrumbs.clear();
        List<Breadcrumb> remove = new ArrayList<>();
        int total = breadcrumbs.getChildrenList().size();
        for (int i = 0; i < total; i++) {
            Widget w = breadcrumbs.getWidget(i);
            if ( w instanceof Breadcrumb ) {
                Breadcrumb bc = (Breadcrumb) w;
                remove.add(bc);
            }
        }
        for (int i = 0; i < remove.size(); i++) {
            breadcrumbs.remove(remove.get(i));
        }
    }

    public void addBreadcrumb(Breadcrumb b) {

        back.setVisible(true);
        int bindex = getBreadcrumbs().size();
        if ( b.getSelected() instanceof Dataset ) {
            dataset = (Dataset) b.getSelected();
        }
        if ( bindex > 0 ) {
            Breadcrumb tail = (Breadcrumb) getBreadcrumbs().get(bindex - 1);
            Object tailObject = tail.getSelected();
            if ( tailObject instanceof Variable) {
                removeBreadcrumb(tail);
                breadcrumbs.add(b);
            } else {
                breadcrumbs.add(b);
            }
        } else {
            breadcrumbs.add(b);
        }
    }

    public void removeBreadcrumb(Breadcrumb tail) {
        breadcrumbs.remove(tail);
        List<Breadcrumb> remaining = getBreadcrumbs();
        if ( remaining.size() > 0 ) {
            Breadcrumb last = remaining.get(remaining.size() - 1);
            if ( last.getSelected() instanceof Dataset ) {
                dataset = (Dataset) last.getSelected();
            }
        }
    }

    public void setGrid(String grid) {
        panel.setGrid(grid);
    }

    public List<Breadcrumb> getBreadcrumbs() {

        List<Breadcrumb> crumbs = new ArrayList<Breadcrumb>();
        for (int i = 0; i < breadcrumbs.getWidgetCount(); i++) {
            Widget w = breadcrumbs.getWidget(i);
            if ( w instanceof Breadcrumb ) {
                crumbs.add((Breadcrumb) w);
            }
        }

        return crumbs;
    }
    public MaterialPanel getBreadcrumbContainer() {
        return breadcrumbs;
    }

    public void scale() {
        outputPanel.scale();
    }
    public void scale(int navWidth) {
        outputPanel.scale(navWidth);
    }

    public void initializeAxes(String view, String mapView, Dataset dd, Variable vv_ia) {

        // Turn them all on
        mapPanel.setDisplay(Display.BLOCK);
        dateTimePanel.setDisplay(Display.BLOCK);
        zaxisPanel.setDisplay(Display.BLOCK);

        TimeAxis tAxis = dd.getTimeAxis();

        if ( tAxis != null ) {
            dateTimeWidget.init(tAxis, false);
        }

        if ( dd.getVerticalAxis() != null ) {
            zAxisWidget.init(dd.getVerticalAxis());
        }


        double xmin = dd.getGeoAxisX().getMin();
        double xmax = dd.getGeoAxisX().getMax();
        double ymin = dd.getGeoAxisY().getMin();
        double ymax = dd.getGeoAxisY().getMax();
        refMap.setDataExtent(ymin, ymax, xmin, xmax, dd.getGeoAxisX().getDelta());
        refMap.setTool(mapView);

        if ( tAxis != null ) {
            String display_hi = tAxis.getDisplay_hi();
            String display_lo = tAxis.getDisplay_lo();

            if (display_hi != null) {
                dateTimeWidget.setHi(display_hi);
            }
            if (display_lo != null) {
                dateTimeWidget.setLo(display_lo);
            }
        }
        hideViewAxes(view, dd);
    }
    public void hideViewAxes(String view, Dataset hdd) {
        this.view = view;

        if ( view.contains("xy") ) {
            mapPanel.setDisplay(Display.NONE);
        }
        if ( view.contains("z") || hdd.getVerticalAxis() == null ) {
            zaxisPanel.setDisplay(Display.NONE);
        }
        if ( view.contains("t") ) {
            dateTimePanel.setDisplay(Display.NONE);
        }
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Variable getVariable() {
        return variable;
    }
    public void setIndex(int index) {
        this.index = index;
    }
    public double getXlo() {
        return refMap.getXlo();
    }
    public double getXhi() {
        return refMap.getXhi();
    }
    public double getYlo() {
        return refMap.getYlo();
    }
    public double getYhi() {
        return refMap.getYhi();
    }
    public String getZlo() {
        return zAxisWidget.getLo();
    }
    public String getZhi() {
        return zAxisWidget.getHi();
    }
    public boolean isZRange() {
        return zAxisWidget.isRange();
    }
    public void setFerretDateLo(String tlo) {
        dateTimeWidget.setLo(tlo);
    }
    public void setFerretDateHi(String hi) {
        dateTimeWidget.setHi(hi);
    }
    public void setMapSelection(double ylo, double yhi, double xlo, double xhi) {
        refMap.setCurrentSelection(ylo, yhi, xlo, xhi);
    }
    public void setZlo(String zlo) {
        zAxisWidget.setLo(zlo);
    }
    public void setZhi(String zhi) {
        zAxisWidget.setHi(zhi);
    }
    public String getFerretDateLo() {
        return dateTimeWidget.getFerretDateLo();
    }
    public String getFerretDateHi() {
        return dateTimeWidget.getFerretDateHi();
    }
    public void enableDifference(boolean b) {
        difference.setEnabled(b);
    }
    public boolean isDifference() {
        return difference.getValue();
    }
    public void setDifference(boolean value) {
        difference.setValue(value, false);
    }
//    public void switchVariables(Variable newVariable) {
//        this.newVariable = newVariable;
//        if ( variable != null ) {
//            // Save current state
//            xlo = refMap.getXlo();
//            xhi = refMap.getXhi();
//            ylo = refMap.getYlo();
//            yhi = refMap.getYhi();
//            if (variable.getVerticalAxis() != null) {
//                zlo = zAxisWidget.getLo();
//                zhi = zAxisWidget.getHi();
//            } else {
//                zlo = null;
//                zhi = null;
//            }
//            if (variable.getTimeAxis() != null) {
//                tlo = dateTimeWidget.getISODateLo();
//                thi = dateTimeWidget.getISODateHi();
//            } else {
//                tlo = null;
//                thi = null;
//            }
//        }
//
//        // newVariable was set before rpc to get config.
//        String mapView = view;
//        if ( !newVariable.getGeometry().equals(Constants.GRID) ) {
//            mapView = "xy";
//        }
//        initializeAxes(view, mapView, newVariable);
//
//        // Put the values back if we've been here before
//        if ( variable != null ) {
//            refMap.setCurrentSelection(ylo, yhi, xlo, xhi);
//            dateTimeWidget.setLo(tlo);
//            dateTimeWidget.setHi(thi);
//            if (variable.getVerticalAxis() != null) {
//                zAxisWidget.setLo(zlo);
//                zAxisWidget.setHi(zhi);
//            }
//        }
//        variable = newVariable;
//    }

    public String getLevels() {
        return autocolors.getText();
    }
    public void setLevels(String levels) {
        autocolors.setText(levels);
    }
    public void setAutoLevelsOn(boolean value) {
        autocolors.setText("");
        useAutoColors.setValue(value);
    }
    public int countAnnotations() {
        int count = 0;
        for (int i = 0; i < annotationPanel.getWidgetCount(); i++) {
            Widget w = annotationPanel.getWidget(i);
            if ( w instanceof MaterialLabel ) {
                count++;
            }
        }
        return count;
    }
    public void padAnnotations(int pad) {
        for (int i = 0; i < pad; i++) {
            MaterialLabel label = new MaterialLabel("");
            label.getElement().setInnerHTML("&nbsp");
            annotationPanel.add(label);
        }
    }
    @UiHandler("useAutoColors")
    void onUseAutoColors(ClickEvent event) {
        eventBus.fireEventFromSource(new AutoColors(useAutoColors.getValue()), useAutoColors);
    }
    ClickHandler onSearchClick = new ClickHandler() {
        @Override
        public void onClick(ClickEvent clickEvent) {
            String search = txtSearch.getText();
            startSearch(search);
        }
    };
    @UiHandler("back")
    void onBack(ClickEvent click) {
        if ( getBreadcrumbs().size() > 0 ) {
            getBreadcrumbContainer().remove(getBreadcrumbContainer().getWidgetCount()-1);
        }
        if (getBreadcrumbs().size() > 0 ) {
            Breadcrumb bc = (Breadcrumb) getBreadcrumbContainer().getWidget(getBreadcrumbContainer().getWidgetCount()-1);
            eventBus.fireEventFromSource(new BreadcrumbSelect(bc.getSelected(), 2), bc);
        } else {
            eventBus.fireEventFromSource(new PanelControlOpen(index), ComparePanel.this);
        }
        click.stopPropagation();
    }
    private void startSearch(String search) {
        SearchRequest sr = new SearchRequest();
        sr.setOffset(0);
        sr.setCount(10);
        sr.setQuery(search);
        eventBus.fireEventFromSource(new Search(sr), ComparePanel.this);
    }
    public List<Widget> getAnnotations() {
        return annotationPanel.getChildrenList();
    }
    public void setZRange(boolean range) {
        zAxisWidget.setRange(range);
    }
}
