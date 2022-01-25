package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 9/15/15.
 */
public class Product {

    String name;
    String title;
    String geometry;
    String view;
    String data_view;
    String ui_group;
    String product_order;
    int minArgs;
    int maxArgs;
    List<Operation> operations;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public String getData_view() {
        return data_view;
    }

    public void setData_view(String data_view) {
        this.data_view = data_view;
    }

    public String getUi_group() {
        return ui_group;
    }

    public void setUi_group(String ui_group) {
        this.ui_group = ui_group;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    public String getProduct_order() {
        return this.product_order;
    }
    public void setProduct_order(String product_order) {
        this.product_order = product_order;
    }
    public boolean isClientPlot() {
        for (int i = 0; i < operations.size(); i++) {
            if ( operations.get(i).getService_action().equals("client_plot") ) {
                return true;
            }
        }
        return false;
    }

    public int getMinArgs() {
        return minArgs;
    }

    public void setMinArgs(int minArgs) {
        this.minArgs = minArgs;
    }

    public int getMaxArgs() {
        return maxArgs;
    }

    public void setMaxArgs(int maxArgs) {
        this.maxArgs = maxArgs;
    }
}

