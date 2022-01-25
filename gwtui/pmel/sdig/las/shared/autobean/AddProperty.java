package pmel.sdig.las.shared.autobean;

/**
 * Created by rhs on 1/30/17.
 */
public class AddProperty {

    // DSG Data Source Properties
    public static String DISPLAY_HI = "display_hi";
    public static String MAPANDPLOT = "mapandplot";
    public static String DISPLAY_LO = "display_lo";
    public static String HOURS = "hours";


    String name;
    String value;

    public AddProperty() {
        super();
    }

    public AddProperty(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
