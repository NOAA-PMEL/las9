package pmel.sdig.las

import grails.converters.JSON

class ConfigController {

    ProductService productService
    IngestService ingestService
    def regions() {
        def regions = new ArrayList<Region>()
        regions.addAll(Region.findAll())
        JSON.use("deep") {
            render regions as JSON
        }
    }

    /**
     * A config is the set of Products and Regions that apply to a geometry type and set of "intervals" or axes.
     *
     * For example, a rectangular grid with intervals of xyt has a set of products and regions not including any z-axis products.
     *
     *   The set of Products is defined by all the combinations of Product.geometry, Product.view and Product.data_view
     *
     *   Right now a data view is implied by the Product.geometry. For example, a profile map has a view of "xy" and a
     *   data_view of "xyzt".
     *
     *   We'll send all combinations back in a map with each geometry type and set of intervals named in a string of the form: type_xyzt
     */
    def json() {

        def id = params.id

        Dataset parent = Dataset.get(id)
        DatasetProperty p = parent.getDatasetProperties().find{it.name=="default"}


        //TODO use the default data set property to find operations if defined.

        Map<String, Map<String, Object>> config = new HashMap<>()

        def regions = new ArrayList<Region>()
        regions.addAll(Region.findAll())

        List<Variable> variables = parent.getVariables();
        for (int i = 0; i < variables.size(); i++) {

            Variable var = variables.get(i)
            String grid = var.getGeometry()
            String intervals = var.getIntervals()
            String config_key = grid+"_"+intervals

            if ( !config.containsKey(config_key) ) {
                def products = productService.findProductsByInterval(grid, intervals)
                def c = [products: products, regions: regions]
                config.put(config_key, c)
            }

        }
        List<Vector> vectors = parent.getVectors()
        for (int i = 0; i < vectors.size(); i++) {
            Vector vec = vectors.get(i)
            String grid = vec.getGeometry()
            String intervals = vec.getU().getIntervals()
            String config_key = grid + "_" + intervals
            if ( !config.containsKey(config_key) ) {
                def products = productService.findProductsByInterval(grid, intervals)
                def c = [products: products, regions: regions]
                config.put(config_key, c)
            }
        }

        def configSet = [config: config]

        JSON.use("deep") {
            render configSet as JSON
        }


    }

    /**.
     * Get all the possible combinations of the characters in a string.  Combo routines based on code by Robert Sedgewick and Kevin Wayne.
     * from their book Introduction to Programming in Java published by Adison Wesley.
     * @param s
     * @return
     */
    public static List<String> combo(String s) {
        return combo("", s);
    }
    /**
     * Get combinations of the characters in a string.
     * @param prefix A prefix for the combinations
     * @param s the string to scramble
     * @return the combinations
     */
    public static List<String> combo(String prefix, String s) {
        ArrayList<String> comboList = new ArrayList<String>();

        if (!prefix.equals("")) {
            comboList.add(prefix);
        }

        if ( s.equals("") ) {
            return comboList;
        }
        for ( int i = 0; i < s.length(); i++ ) {
            comboList.addAll(combo(prefix + s.charAt(i), s.substring(i+1)));
        }
        return comboList;
    }


    def productsByInterval (){
        String intervals = params.intervals
        String grid = params.grid
        List<Product> products = productService.findProductsByInterval(grid, intervals)
        JSON.use("deep") {
            render products as JSON
        }
    }
}
