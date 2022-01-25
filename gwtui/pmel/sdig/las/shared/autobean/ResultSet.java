package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 9/21/15.
 */
public class ResultSet {

    String product;

    List<Result> results;
    MapScale mapScale;
    List<AnnotationGroup> annotationGroups;

    int targetPanel;

    String error;

    Animation animation;

    public Animation getAnimation() {
        return animation;
    }

    public void setAnimation(Animation animation) {
        this.animation = animation;
    }

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
    }

    public MapScale getMapScale() {
        return mapScale;
    }

    public void setMapScale(MapScale mapScale) {
        this.mapScale = mapScale;
    }

    public List<AnnotationGroup> getAnnotationGroups() {
        return annotationGroups;
    }

    public void setAnnotationGroups(List<AnnotationGroup> annotationGroups) {
        this.annotationGroups = annotationGroups;
    }

    public int getTargetPanel() {
        return targetPanel;
    }

    public void setTargetPanel(int targetPanel) {
        this.targetPanel = targetPanel;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
    public void setProduct(String product) {
        this.product = product;
    }
    public String getProduct() {
        return product;
    }
    public Result getResultByType(String type) {
        Result myresult = null;
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if (result.getType().equals(type)) {
                myresult = result;
            }
        }
        return myresult;
    }
    public Result getResultByTypeAndFile_type(String type, String file_type) {
        Result myresult = null;
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if (result.getType().equals(type) && result.getFile_type().equals(file_type)) {
                myresult = result;
            }
        }
        return myresult;
    }
}
