package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Holds the definition of an analysis transformation.
 *
 */
public class Analysis {

    /**
     * The name of the transformation, one of Average, Minimum, Maximum, Sum and Variance
     */
    String transformation;
    /**
     * The concatenation of xyzt for the axes that are going to be transformed
     */
    String axes;
    /**
     * The individual axes to be transformed as a list with the end points of the range defined
     */
    List<AnalysisAxis> analysisAxes;

    /**
     * The English name of the axis or axes over which the transform will be made, one of Area, Longitude, Latitude, Time, Height/Depth
     */
    String over;

    public String getAxes() {
        return axes;
    }

    public void setAxes(String axes) {
        this.axes = axes;
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    public List<AnalysisAxis> getAnalysisAxes() {
        return analysisAxes;
    }

    public void setAnalysisAxes(List<AnalysisAxis> analysisAxes) {
        this.analysisAxes = analysisAxes;
    }

    public String getOver() {
        return over;
    }

    public void setOver(String over) {
        this.over = over;
    }
    public AnalysisAxis getAnalysisAxis(String type) {
        for (int i = 0; i < getAnalysisAxes().size(); i++) {
            AnalysisAxis ax = getAnalysisAxes().get(i);
            if ( ax.getType().equalsIgnoreCase(type) ) {
                return ax;
            }
        }
        return null;
    }
}
