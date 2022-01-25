package pmel.sdig.las.shared.autobean;

import java.util.List;

public class Animation {
    String fill_levels;
    String contour_levels;
    String dep_axis_scale;
    boolean hasT;
    List<String> frames;
    String units;

    public String getFill_levels() {
        return fill_levels;
    }

    public void setFill_levels(String fill_levels) {
        this.fill_levels = fill_levels;
    }

    public String getContour_levels() {
        return contour_levels;
    }

    public void setContour_levels(String contour_levels) {
        this.contour_levels = contour_levels;
    }

    public String getDep_axis_scale() {
        return dep_axis_scale;
    }

    public void setDep_axis_scale(String dep_axis_scale) {
        this.dep_axis_scale = dep_axis_scale;
    }

    public boolean hasT() {
        return hasT;
    }

    public void setHasT(boolean hasT) {
        this.hasT = hasT;
    }

    public List<String> getFrames() {
        return frames;
    }

    public void setFrames(List<String> frames) {
        this.frames = frames;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }
}
