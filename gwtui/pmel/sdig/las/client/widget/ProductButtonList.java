package pmel.sdig.las.client.widget;


import gwt.material.design.client.ui.MaterialPanel;
import pmel.sdig.las.shared.autobean.Product;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by rhs on 9/15/15.
 */
public class ProductButtonList extends MaterialPanel {

    Map<String, ProductGroup> groups = new HashMap<String, ProductGroup>();

    public void init(List<Product> products) {

        clearProducts();

        String first = products.get(0).getName();

        setLineHeight(32.0d);

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            String ui_group = product.getUi_group();
            ProductGroup productGroup;
            if ( !groups.keySet().contains(ui_group) ) {
                productGroup = new ProductGroup();
                productGroup.setUi_group(ui_group);
                productGroup.setProduct_header(ui_group);
                groups.put(ui_group, productGroup);
            } else {
                productGroup = groups.get(ui_group);
            }

            ProductButton pb = new ProductButton(product);
            productGroup.addProductButton(pb);
        }

        Iterator<String> it = groups.keySet().iterator();
        while ( it.hasNext() ) {
            String key = it.next();
            add(groups.get(key));
        }

        setSelected(first);
    }

    public void setSelected(String name) {
        int i = 0;
        for ( Iterator<String> keyIt = groups.keySet().iterator(); keyIt.hasNext(); ) {
            String key= keyIt.next();
            ProductGroup group = groups.get(key);
            List<ProductButton> products = group.getProductButtons();
            for(int j = 0; j < products.size(); j++ ) {
                ProductButton button = products.get(j);
                Product product = button.getProduct();
                button.setValue(false);
                if (product.getName().equals(name)) {
                    button.setValue(true);
                }
            }
            i++;
        }
    }
    public String getSelected() {
        String product = null;
        for ( int i = 0; i < getWidgetCount(); i++ ) {
            ProductGroup group = (ProductGroup) getWidget(i);
            List<ProductButton> products = group.getProductButtons();
            for(int j = 0; j < products.size(); j++ ) {
                ProductButton button = products.get(j);
                if ( button.getValue() ) {
                    Product p = button.getProduct();
                    product = p.getName();
                }
            }
        }
        return product;
    }

    public Product getSelectedProduct() {
        Product product = null;
        for ( int i = 0; i < getWidgetCount(); i++ ) {
            ProductGroup group = (ProductGroup) getWidget(i);
            List<ProductButton> products = group.getProductButtons();
            for(int j = 0; j < products.size(); j++ ) {
                ProductButton button = products.get(j);
                if ( button.getValue() ) {
                    product = button.getProduct();
                }
            }
        }
        return product;
    }

    public void clearProducts() {

        groups.clear();
        clear();

    }
}
