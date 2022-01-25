package pmel.sdig.las.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.EventBus;
import gwt.material.design.client.ui.MaterialRadioButton;
import pmel.sdig.las.client.event.ProductSelected;
import pmel.sdig.las.client.main.ClientFactory;
import pmel.sdig.las.shared.autobean.Product;

/**
 * Created by rhs on 9/15/15.
 */
public class ProductButton extends MaterialRadioButton {


    Product product;

    ClientFactory clientFactory = GWT.create(ClientFactory.class);
    EventBus eventBus = clientFactory.getEventBus();

    public ProductButton(Product product) {
        super("products", product.getTitle());
        this.product = product;
        addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                fireClickEvent();
            }
        });

    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    private void fireClickEvent() {
        eventBus.fireEventFromSource(new ProductSelected(), ProductButton.this);
    }

}