package pmel.sdig.las.client.state;

import pmel.sdig.las.shared.autobean.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rhs on 6/2/16.
 */
public class PanelState {

    LASRequest lasRequest;
    ResultSet resultSet;

    int frameIndex = 0;
    List<ResultSet> completedFrames = new ArrayList();


    public String getImageUrl() {

        List<Result> results = resultSet.getResults();

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if ( result.getType().equals("image") ) {
                return result.getUrl();
            }
        }
        return null;
    }

    public List<ResultSet> getCompletedFrames() {
        return completedFrames;
    }
    public void setImageUrl(String url) {
        List<Result> results = resultSet.getResults();

        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            if ( result.getType().equals("image") ) {
                result.setUrl(url);
            }
        }
    }
    public void addFrame(ResultSet resultSet) {
        completedFrames.add(resultSet);
    }
    public ResultSet getCurrentFrame() {
        if ( frameIndex < completedFrames.size() ) {
            return completedFrames.get(frameIndex);
        }
        return null;
    }
    public void clearFrames() {
        completedFrames.clear();
    }
    public MapScale getMapScale() {
        return resultSet.getMapScale();
    }

    public LASRequest getLasRequest() {
        return lasRequest;
    }

    public void setLasRequest(LASRequest lasRequest) {
        this.lasRequest = lasRequest;
    }

    public void setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
    }
    public ResultSet getResultSet() {
        return resultSet;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }
}
