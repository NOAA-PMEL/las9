package pmel.sdig.las.shared.autobean;

import java.util.List;
import java.util.Map;

/**
 * Created by rhs on 5/21/15.
 */
public class Site {

    String title;

    int total;
    int grids;
    int profile;
    int timeseries;
    int trajectory;
    int point;
    int discrete;

    Map<String, List<String>> attributes;

    boolean toast;
    boolean dashboard;

    String infoUrl;

    List<Dataset> datasets;
    List<FooterLink> footerLinks;

    public boolean isDashboard() {
        return dashboard;
    }

    public void setDashboard(boolean dashboard) {
        this.dashboard = dashboard;
    }

    public boolean isToast() {
        return toast;
    }

    public void setToast(boolean toast) {
        this.toast = toast;
    }

    public String getInfoUrl() {
        return infoUrl;
    }

    public void setInfoUrl(String infoUrl) {
        this.infoUrl = infoUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Dataset> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Dataset> datasets) {
        this.datasets = datasets;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getGrids() {
        return grids;
    }

    public void setGrids(int grids) {
        this.grids = grids;
    }

    public int getDiscrete() {
        return discrete;
    }

    public void setDiscrete(int discrete) {
        this.discrete = discrete;
    }

    public int getProfile() {
        return profile;
    }

    public void setProfile(int profile) {
        this.profile = profile;
    }

    public int getTimeseries() {
        return timeseries;
    }

    public void setTimeseries(int timeseries) {
        this.timeseries = timeseries;
    }

    public int getTrajectory() {
        return trajectory;
    }

    public void setTrajectory(int trajectory) {
        this.trajectory = trajectory;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }
    public List<FooterLink> getFooterLinks() {
        return footerLinks;
    }
    public void setFooterLinks(List<FooterLink> footerLinks) {
        this.footerLinks = footerLinks;
    }
}
