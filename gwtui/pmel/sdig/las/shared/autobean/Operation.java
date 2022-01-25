package pmel.sdig.las.shared.autobean;

import java.util.List;
import java.util.Set;

/**
 * Created by rhs on 8/24/15.
 */
public class Operation {

    String output_template;
    String service_action;
    String type;
    ResultSet resultSet;
    List<Operation> operations;
    List<MenuOption> menuOptions;
    List<TextOption> textOptions;
    List<YesNoOption> yesNoOptions;

    public String getOutput_template() {
        return output_template;
    }

    public void setOutput_template(String output_template) {
        this.output_template = output_template;
    }

    public String getService_action() {
        return service_action;
    }

    public void setService_action(String service_action) {
        this.service_action = service_action;
    }

    public ResultSet getResults() {
        return resultSet;
    }

    public void setResults(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    public List<MenuOption> getMenuOptions() {
        return menuOptions;
    }

    public void setMenuOptions(List<MenuOption> menuOptions) {
        this.menuOptions = menuOptions;
    }

    public List<TextOption> getTextOptions() {
        return textOptions;
    }

    public void setTextOptions(List<TextOption> textOptions) {
        this.textOptions = textOptions;
    }
    public List<YesNoOption> getYesNoOptions() {
        return yesNoOptions;
    }
    public void setYesNoOptions(List<YesNoOption> yesNoOptions) {
        this.yesNoOptions = yesNoOptions;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }
}
