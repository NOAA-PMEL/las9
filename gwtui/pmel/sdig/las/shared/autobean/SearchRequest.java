package pmel.sdig.las.shared.autobean;

import java.util.List;

public class SearchRequest {
    String query;
    List<DatasetProperty> datasetProperties;
    List<VariableProperty> variableProperties;
    int offset;
    int count;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<DatasetProperty> getDatasetProperties() {
        return datasetProperties;
    }

    public void setDatasetProperties(List<DatasetProperty> datasetProperties) {
        this.datasetProperties = datasetProperties;
    }

    public List<VariableProperty> getVariableProperties() {
        return variableProperties;
    }

    public void setVariableProperties(List<VariableProperty> variableProperties) {
        this.variableProperties = variableProperties;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String toString() {
        StringBuilder q = new StringBuilder();
        if ( query != null ) {
            q.append(query);
        }
        if ( datasetProperties != null ) {
            for (int i = 0; i < datasetProperties.size(); i++) {
                if (q.length() > 0 )  {
                    q.append(" ");
                }
                DatasetProperty dp = datasetProperties.get(i);
                q.append(dp.getName()+"="+dp.getValue());
            }
        }
        if ( variableProperties != null ) {
            for (int i = 0; i < variableProperties.size(); i++) {
                if ( q.length() > 0 ) {
                    q.append(" ");
                }
                VariableProperty vp = variableProperties.get(i);
                q.append(vp.getName()+"="+vp.getValue());
            }
        }
        return q.toString();
    }
}
