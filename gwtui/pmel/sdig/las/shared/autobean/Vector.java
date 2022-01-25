package pmel.sdig.las.shared.autobean;

import java.util.Map;

public class Vector {
    String name;
    String type = "vector";
    String title;
    String hash;
    String geometry;

    Map<String, String> attributes;
    Variable u;
    Variable v;
    Variable w;



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Variable getU() {
        return u;
    }

    public void setU(Variable u) {
        this.u = u;
    }

    public Variable getV() {
        return v;
    }

    public void setV(Variable v) {
        this.v = v;
    }

    public Variable getW() {
        return w;
    }

    public void setW(Variable w) {
        this.w = w;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }
}
