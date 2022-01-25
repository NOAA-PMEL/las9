package pmel.sdig.las.shared.autobean;

/**
 * Created by rhs on 8/24/15.
 */
public class Result {
    String name;
    String type;
    String file_type;
    String mime_type;
    String suffix;
    String url;
    String filename;
    boolean linked;

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

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked) {
        this.linked = linked;
    }
    public String getFile_type() {
        return this.file_type;
    }
    public void setFile_type() {
        this.file_type = file_type;
    }
}
