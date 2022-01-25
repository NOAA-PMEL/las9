package pmel.sdig.las

class Dataset {

    String status
    String title
    String history;
    String hash
    String url
    String geometry
    String type = "dataset"
    Boolean variableChildren
    List variables
    List vectors

    static String INGEST_NOT_STARTED = "Ingest not started"
    static String INGEST_STARTED = "Ingest started"
    static String INGEST_FAILED = "Ingest failed"
    static String INGEST_FINISHED = "Ingest finished"

    String message;

    // A data set can contain other datasets or variables.
    List datasets

    static hasMany = [datasetProperties: DatasetProperty, variables: Variable, datasets: Dataset, vectors: Vector]
    static hasOne = [timeAxis: TimeAxis, geoAxisX: GeoAxisX, geoAxisY: GeoAxisY, verticalAxis: VerticalAxis]

    Dataset parent;
    static belongsTo = [Dataset, Site]

    static searchable = {
        variables component: true
        vectors component: true
        only = ['variables', 'vectors', 'title', 'history', 'geometry', 'variableChildren']
    }

    static mapping = {
        sort "title"
        url type: "text"
        title type: "text"
        history type: "text"
        datasets cascade: 'all'
        datasetProperties cascade: 'all-delete-orphan'
        variables cascade: 'all-delete-orphan'
        vectors cascade: 'all-delete-orphan'
    }


    static constraints = {
        geometry(nullable: true)
        parent(nullable: true)
        url(nullable: true)
//        title(nullable: true)
        variables(nullable: true)
        datasets(nullable: true)
        datasetProperties(nullable: true)
        status(nullable: true)
        variableChildren (nullable: true)
        vectors (nullable: true)
        message(nullable: true)
        history(nullable: true)
        geoAxisX(nullable: true)
        geoAxisY(nullable: true)
        verticalAxis(nullable: true)
        timeAxis(nullable: true)
    }

    String getDatasetPropertyValue(String type, String name) {
        DatasetProperty dsp = datasetProperties.find{it.type == type && it.name == name}
        if ( dsp && dsp.value ) {
            dsp.value.trim()
        } else {
            null
        }
    }

//    @Override
//    int compareTo(Object o) {
//        if ( o instanceof Dataset ) {
//            Dataset od = (Dataset) o
//            return this.title.compareTo(od.title)
//
//        } else {
//            return 0
//        }
//    }
//
//    @Override
//    String toString() {
//        title
//    }
    def getDatasetPropertyGroup(String s) {
        datasetProperties.findAll{it.type == s}
    }
}
