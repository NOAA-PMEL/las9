package pmel.sdig.las.shared.autobean;

/**
 * Created by rhs on 8/24/15.
 */
public class TextOption {

    String name;
    String title;
    String help;
    String value;
    String defaultValue;
    String hint;

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

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    public void setHing(String hint) {
        this.hint = hint;
    }
    public String getHint() {
        return this.hint;
    }
}
