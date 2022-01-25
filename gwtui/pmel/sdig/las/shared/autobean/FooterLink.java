package pmel.sdig.las.shared.autobean;

public class FooterLink implements Comparable {
    String url;
    String linktext;
    int index;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLinktext() {
        return linktext;
    }

    public void setLinktext(String linktext) {
        this.linktext = linktext;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public int compareTo(Object o) {
        if ( o instanceof FooterLink ) {
            FooterLink fl = (FooterLink) o;
            return index - fl.index;
        }
        return 0;
    }
}
