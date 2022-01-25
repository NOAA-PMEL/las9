package pmel.sdig.las

class LASRequest {

    String operation
    int targetPanel;

    List variableHashes
    List datasetHashes
    List requestProperties;
    List dataConstraints
    List dataQualifiers
    List analysis;
    List axesSets

    static hasMany = [requestProperties: RequestProperty, datasetHashes: String, variableHashes: String, analysis: Analysis, dataConstraints: DataConstraint, dataQualifiers: DataQualifier, axesSets: AxesSet]


    static constraints = {
        targetPanel(nullable: true)
    }

    def List<RequestProperty> getPropertyGroup(String group_name) {
        def group = []
        if ( requestProperties ) {
            for (int i = 0; i < requestProperties.size(); i++) {
                RequestProperty rp = requestProperties.get(i)
                if (rp.type == group_name) {
                    group.add(rp)
                }
            }
        }
        group
    }
}
