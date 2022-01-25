package pmel.sdig.las.shared.autobean;

import java.util.List;

/**
 * Created by rhs on 1/6/17.
 */
public class AnnotationGroup {
    String type;
    List<Annotation> annotations;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }
}
