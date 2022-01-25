package pmel.sdig.las.client.widget;

import com.google.gwt.dom.client.Style;
import gwt.material.design.client.constants.Color;
import gwt.material.design.client.constants.HeadingSize;
import gwt.material.design.client.ui.MaterialCollapsible;
import gwt.material.design.client.ui.MaterialCollapsibleBody;
import gwt.material.design.client.ui.MaterialCollapsibleHeader;
import gwt.material.design.client.ui.MaterialCollapsibleItem;
import gwt.material.design.client.ui.MaterialCollection;
import gwt.material.design.client.ui.MaterialCollectionItem;
import gwt.material.design.client.ui.MaterialContainer;
import gwt.material.design.client.ui.MaterialLabel;
import gwt.material.design.client.ui.MaterialLink;
import gwt.material.design.client.ui.MaterialPanel;
import gwt.material.design.client.ui.MaterialRow;
import gwt.material.design.client.ui.html.Heading;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rhs on 9/16/15.
 */
public class ProductGroup extends MaterialPanel {

    MaterialRow product_header = new MaterialRow();
//
    MaterialPanel products = new MaterialPanel();

    String ui_group;

    public ProductGroup() {
        super();
        product_header.setMarginLeft(16);
        product_header.setMarginBottom(0);
        products.setMarginLeft(32);
        products.setMarginBottom(0);
        add(product_header);
        add(products);
    }

    public void setProduct_header(String title) {
        product_header.clear();
        setId(title);
        Heading header = new Heading(HeadingSize.H5);
        header.setFontSize("1.1em");
        header.setFontWeight(Style.FontWeight.BOLD);
        header.setTextColor(Color.BLUE);
        header.setText(title);
        product_header.add(header);
    }

    public void setUi_group(String ui_group) {
        this.ui_group = ui_group;
    }
    public String getUi_group() {
        return ui_group;
    }
    public void addProductButton(ProductButton radio) {
        MaterialRow row = new MaterialRow();
        row.add(radio);
        products.add(row);
    }
    public List<ProductButton> getProductButtons() {
        List<ProductButton> buttons = new ArrayList<ProductButton>();
        for (int i = 0; i < products.getWidgetCount(); i++ ) {
            MaterialRow row = (MaterialRow) products.getWidget(i);
            row.setMarginBottom(0);
            buttons.add((ProductButton) row.getWidget(0));
        }
        return buttons;
    }
}