package pmel.sdig.las.shared.autobean;

public class RequestProperty {
	
	long id;
	String type;
    String name;
    String value;

    public RequestProperty() {
        super();
    }

    public RequestProperty(String type, String name, String value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
