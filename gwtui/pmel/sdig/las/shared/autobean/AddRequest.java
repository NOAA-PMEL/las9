package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 1/26/17.
 */
public class AddRequest {

    String url;
    String type;
    List<AddProperty> addProperties;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<AddProperty> getPropertyList() {
        return addProperties;
    }

    public void setAddProperties(List<AddProperty> addProperties) {
        this.addProperties = addProperties;
    }
}
