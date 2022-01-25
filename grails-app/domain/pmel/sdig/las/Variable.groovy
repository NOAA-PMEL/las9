package pmel.sdig.las


class Variable {
    String name
    String url
    // This could be a java BreadcrumbType on the rhs, but in the end it's a string
    String type = "variable"
    String title
    String standard_name
    String hash
    String intervals
    String geometry;
    String units;
    String thumbnail;
    Map<String, String> attributes

    boolean subset = false; // This applies only to DSG data. Whether the collection can be segmented  or "sub-setted" by the value of this variable. E.G. the geometry ID, the platform name, the PI, etc.
                     // Mostly false, always false for gridded. Only true for some DSG and must be set.

    boolean dsgId = false; // True if the is the ID variable for the data set to which it belongs. Only applies to DSG data sets.

    Dataset dataset
    static belongsTo = [Dataset, Vector]

    static hasMany = [variableProperties: VariableProperty, variableAttributes: VariableAttribute]
    static hasOne = [stats: Stats]
    static mapping = {
        sort "title"
        url type: "text"
        title type: "text"
    }


    static searchable = {

        only = ['title', 'name', 'geometry', 'standard_name']

    }
    static constraints = {
        stats(nullable: true)
        variableProperties(nullable: true)
        units(nullable:true)
        dataset (nullable: true)
        thumbnail (nullable: true)
        standard_name(nullable: true)
    }

    @Override
    def String toString() {
        if ( title ) return title
        return name
    }

}
