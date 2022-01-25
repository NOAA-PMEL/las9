package pmel.sdig.las.shared.autobean;

/**
 * Created by rhs on 9/14/15.
 */
public class Region {

    String name;
    String title;
    double westLon;
    double eastLon;
    double northLat;
    double southLat;

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

    public double getWestLon() {
        return westLon;
    }

    public void setWestLon(double westLon) {
        this.westLon = westLon;
    }

    public double getEastLon() {
        return eastLon;
    }

    public void setEastLon(double eastLon) {
        this.eastLon = eastLon;
    }

    public double getNorthLat() {
        return northLat;
    }

    public void setNorthLat(double northLat) {
        this.northLat = northLat;
    }

    public double getSouthLat() {
        return southLat;
    }

    public void setSouthLat(double southLat) {
        this.southLat = southLat;
    }
}
