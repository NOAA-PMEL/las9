package pmel.sdig.las.shared.autobean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rhs on 9/19/15.
 */
public class LASRequest {

    long id;

    int targetPanel;

    String operation;
    List<String> variableHashes;
    List<String> datasetHashes;
    List<Analysis> analysis;
    List<AxesSet> axesSets = new ArrayList<>();
    List<DataQualifier> dataQualifiers;

    List<DataConstraint> dataConstraints;

    List<RequestProperty> requestProperties;

    public List<Analysis> getAnalysis() {
        return analysis;
    }

    public void setAnalysis(List<Analysis> analysis) {
        this.analysis = analysis;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getTargetPanel() {
        return targetPanel;
    }

    public void setTargetPanel(int targetPanel) {
        this.targetPanel = targetPanel;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public List<String> getVariableHashes() {
        return variableHashes;
    }

    public void setVariableHashes(List<String> variableHashes) {
        this.variableHashes = variableHashes;
    }

    public List<String> getDatasetHashes() {
        return datasetHashes;
    }

    public void setDatasetHashes(List<String> datasetHashes) {
        this.datasetHashes = datasetHashes;
    }

    public List<RequestProperty> getRequestProperties() {
        return requestProperties;
    }

    public String getRequestPropertyValue(String type, String name) {
        if ( requestProperties != null ) {
            for (int i = 0; i < requestProperties.size(); i++) {
                RequestProperty rp = requestProperties.get(i);
                if ( rp.getType().equals(type) && rp.getName().equals(name) ) {
                    return rp.getValue();
                }
            }
        }
        return null;
    }

    public void addRequestProperty(RequestProperty requestProperty) {
        if ( this.requestProperties == null ) {
            this.requestProperties = new ArrayList<>();
        }
        this.requestProperties.add(requestProperty);
    }
    public void setRequestProperties(List<RequestProperty> requestProperties) {
        this.requestProperties = requestProperties;
    }

    public List<AxesSet> getAxesSets() {
        return axesSets;
    }

    public void setAxesSets(List<AxesSet> axesSets) {
        this.axesSets = axesSets;
    }

    public void addProperty(RequestProperty requestProperty) {
        if ( requestProperties == null ) {
            requestProperties = new ArrayList<>();
        }
        requestProperties.add(requestProperty);
    }
    public List<DataConstraint> getDataConstraints() {
        return dataConstraints;
    }
    public void setDataConstraints(List<DataConstraint> constraints) {
        this.dataConstraints = constraints;
    }

    public void addDataConstraint(DataConstraint constraint) {
        if ( dataConstraints == null ) {
            dataConstraints = new ArrayList<>();
        }
        dataConstraints.add(constraint);
    }
    public List<DataQualifier> getDataQualifiers() {
        return dataQualifiers;
    }

    public void setDataQualifiers(List<DataQualifier> dataQualifiers) {
        this.dataQualifiers = dataQualifiers;
    }
    public void addDataQualifier(DataQualifier dataQualifier) {
        if ( dataQualifiers == null ) {
            dataQualifiers = new ArrayList<>();
        }
        this.dataQualifiers.add(dataQualifier);
    }
}

