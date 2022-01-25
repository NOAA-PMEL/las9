package pmel.sdig.las

class SearchRequest {

    String query;
    List<DatasetProperty> datasetProperties;
    List<VariableProperty> variableProperties;
    int offset
    int count
    static constraints = {
    }
}
