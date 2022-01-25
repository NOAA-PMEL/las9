package pmel.sdig.las

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.jdom.Document
import org.jdom.Element
import pmel.sdig.las.type.GeometryType

@Transactional(readOnly = true)
class ProductService {

    GrailsApplication grailsApplication
    FerretService ferretService
    ErddapService erddapService
    JsonSlurper jsonSlurper = new JsonSlurper();

    List<Product> findProductsByInterval(String grid, String intervals) {
        List<String> combos = ConfigController.combo(intervals);

        def products = new ArrayList<Product>();

        // For grids the view and data view are the same.
        combos.each {view ->
            List<Product> productsByGeoAndView = Product.findAllByGeometryAndViewAndHidden(grid, view, false)
            log.debug("Checking view + "+view)
            if ( productsByGeoAndView ) {
                productsByGeoAndView.each {product ->
                    products.add(product)
                }
            }
        }
        products.sort{it.product_order}
        products
    }
    def makeAnnotations(File file) {

        List<AnnotationGroup> annotationGroups = new ArrayList<AnnotationGroup>();

        Random r = new Random()
        def root = new XmlSlurper().parse(file)
        root.annotation_group.each {group ->
            AnnotationGroup aGroup = new AnnotationGroup()
            aGroup.setType(group.@type.toString())
            aGroup.setId(r.nextLong())
            List<Annotation> annotationList = new ArrayList<Annotation>()
            group.annotation.each{a ->
                Annotation annotation = new Annotation([type: a.@type.toString(), value: a.value.text()])
                if ( !annotationList.any{ it.value == annotation.value } )
                    annotationList.add(annotation)
            }
            aGroup.annotations = annotationList;
            annotationGroups.add(aGroup)
        }
        annotationGroups;
    }
    private def Animation makeAnimationList(File file) {
        Document doc = new Document();
        Animation animation = new Animation();
        JDOMUtils.XML2JDOM(file, doc)
        Element root = doc.getRootElement();
        if ( root == null ) {
            return animation;
        }
        Element das = root.getChild("dep_axes_scale")
        if ( das ) {
            String dasText = das.getText();
            if ( dasText ) {
                animation.setDep_axis_scale(dasText.trim())
            }
        }
        Element cl = root.getChild("contour_levels")
        if ( cl ) {
            String clText = cl.getText();
            if ( clText ) {
                animation.setContour_levels(clText.trim())
            }

        }
        Element fl = root.getChild("fill_levels")
        if ( fl ) {
            String fillLevels = fl.getText()
            if ( fillLevels ) {
                animation.setFill_levels(fillLevels.trim())
            }
        }
        Element u = root.getChild("units")
        if ( u ) {
            String uText = u.getText()
            if ( uText ) {
                animation.setUnits(uText.trim())
            }
        }
        Element ht = root.getChild("hasT")
        if ( ht ) {
            String hasT = ht.getText()
            if ( hasT != null && hasT.trim().equals("1") ) {
                animation.setHasT(true)
            } else {
                animation.setHasT(false)
            }
        }

        Element framesE = doc.getRootElement().getChild("frames")
        if ( framesE ) {
            List<Element> framesListE = framesE.getChildren("frame")
            List<String> frames = new ArrayList<>()
            for (int i = 0; i < framesListE.size(); i++) {
                frames.add(framesListE.get(i).getText().trim())
            }
            animation.setFrames(frames)
        }
        animation
    }
    def makeMapScale(File mapscaleFile) {
        HashMap<String, String> scale = new HashMap<String, String>();

        mapscaleFile.eachLine{String line ->

            if (line.contains("[1-9]:") && line.contains(" / ")) {
                // Split on the : and use the second half...
                String[] halves = line.split(":");
                if (halves.length > 1) {
                    // See below...
                    String[] parts = halves[1].split("\"");
                    scale.put(parts[1], parts[3]);
                }
            }
            /*
             * Simpler style without the mulitple line numbers, ":" and "/"
             * "PPL$XMIN" "122.2" "PPL$XMAX" "288.8" "PPL$YMIN" "-35.00"
             * "PPL$YMAX" "45.00" "PPL$XPIXEL" "776" "PPL$YPIXEL" "483"
             * "PPL$WIDTH" "12.19" "PPL$HEIGHT" "7.602" "PPL$XORG" "1.200"
             * "VP_TOP_MARGIN" "1.4" "VP_RT_MARGIN" "1" "AX_HORIZ" "X"
             * "AX_HORIZ_POSTV" " " "AX_VERT" "Z" "AX_VERT_POSTV" "down"
             * "DATA_EXISTS" "1" "DATA_MIN" "3.608" "DATA_MAX" "8.209"
             */
            else {
                // Just split on the quotes
                // See JavaDoc on split for explanation
                // of why we get 4 parts with the repeated quotes.
                String[] parts = line.split("\"");
                scale.put(parts[1], parts[3]);
            }

        }
        String s1 = scale.get('PPL$XPIXEL');
        String s2 = scale.get('PPL$WIDTH');
        float xppi = 0.0f;
        if (s1 != null && s2 != null) {
            xppi = Float.valueOf(s1) / Float.valueOf(s2);
        }

        float yppi = 0.0f;
        s1 = scale.get('PPL$YPIXEL');
        s2 = scale.get('PPL$HEIGHT');
        if (s1 != null && s2 != null) {
            yppi = Float.valueOf(s1) / Float.valueOf(s2);
        }

        float x_image_size = 0.0f;
        s1 = scale.get('PPL$XPIXEL');
        if (s1 != null) {
            x_image_size = Float.valueOf(s1);
        }

        float y_image_size = 0.0f;
        s1 = scale.get('PPL$YPIXEL');
        if (s1 != null) {
            y_image_size = Float.valueOf(s1);
        }

        float xOffset_left = 0.0f;
        s1 = scale.get('PPL$XORG');
        if (s1 != null) {
            xOffset_left = xppi * Float.valueOf(s1);
        }

        float yOffset_bottom = 0.0f;
        s1 = scale.get('PPL$YORG');
        if (s1 != null) {
            yOffset_bottom = yppi * Float.valueOf(s1);
        }

        float xOffset_right = 0.0f;
        s1 = scale.get('VP_RT_MARGIN');
        if (s1 != null) {
            xOffset_right = xppi * Float.valueOf(s1);
        }

        float yOffset_top = 0.0f;
        s1 = scale.get('VP_TOP_MARGIN');
        if (s1 != null) {
            yOffset_top = yppi * Float.valueOf(s1);
        }

        float plotWidth = 0.0f;
        s1 = scale.get('PPL$XLEN');
        if (s1 != null) {
            plotWidth = xppi * Float.valueOf(s1);
        }

        float plotHeight = 0.0f;
        s1 = scale.get('PPL$YLEN');
        if (s1 != null) {
            plotHeight = yppi * Float.valueOf(s1);
        }

        float axisLLX = 0.0f;
        s1 = scale.get('XAXIS_MIN');
        if (s1 != null) {
            axisLLX = Float.valueOf(s1);
        }
        float axisLLY = 0.0f;
        s1 = scale.get('YAXIS_MIN');
        if (s1 != null) {
            axisLLY = Float.valueOf(s1);
        }

        float axisURX = 0.0f;
        s1 = scale.get('XAXIS_MAX');
        if (s1 != null) {
            axisURX = Float.valueOf(s1);
        }

        float axisURY = 0.0f;
        s1 = scale.get('YAXIS_MAX');
        if (s1 != null) {
            axisURY = Float.valueOf(s1);
        }

        float data_min = 0.0f;
        s1 = scale.get('DATA_MIN');
        if (s1 != null && !s1.equals(' ') && !s1.equals('')) {
            data_min = Float.valueOf(s1);
        }

        float data_max = 0.0f;
        s1 = scale.get('DATA_MAX');
        if (s1 != null && !s1.equals(' ') && !s1.equals('')) {
            data_max = Float.valueOf(s1);
        }

        int data_exists = 0;
        s1 = scale.get('DATA_EXISTS');
        if (s1 != null) {
            data_exists = Integer.valueOf(s1).intValue();
        }

        int xStride = 0;
        s1 = scale.get('XSTRIDE');
        if (s1 != null) {
            xStride = Integer.valueOf(s1).intValue();
        }

        int yStride = 0;
        s1 = scale.get('YSTRIDE');
        if (s1 != null) {
            yStride = Integer.valueOf(s1).intValue();
        }

        String time_min = null;
        s1 = scale.get('HAXIS_TSTART');
        if (s1 != null) {
            time_min = s1;
        }

        String time_max = null;
        s1 = scale.get('HAXIS_TEND');
        if (s1 != null) {
            time_max = s1;
        }

        if (time_min == null || time_max == null) {
            s1 = scale.get('VAXIS_TSTART');
            if (s1 != null) {
                time_min = s1;
            }

            s1 = scale.get('VAXIS_TEND');
            if (s1 != null) {
                time_max = s1;
            }
        }

        String time_units = null;
        s1 = scale.get('HAXIS_TUNITS');
        if (s1 != null) {
            time_units = s1;
        }
        if (time_units == null) {
            s1 = scale.get('VAXIS_TUNITS');
            if (s1 != null) {
                time_units = s1;
            }
        }

        String time_origin = null;
        s1 = scale.get('HAXIS_TORIGIN');
        if (s1 != null) {
            time_origin = s1;
        }
        if (time_origin == null) {
            s1 = scale.get('VAXIS_TORIGIN');
            if (s1 != null) {
                time_origin = s1;
            }
        }

        String calendar = null;
        s1 = scale.get('HAXIS_TCALENDAR');
        if (s1 != null) {
            calendar = s1;
        }
        if ( calendar == null ) {
            s1 = scale.get('VAXIS_TCALENDAR');
            if ( s1 != null ) {
                calendar = s1;
            }
        }

        String levels_string = null;
        s1 = scale.get("LEVELS_STRING")
        if ( s1 != null ) {
            levels_string = s1
        }

        MapScale mapScaleInstance = new MapScale();
        mapScaleInstance.setXxxPixelsPerInch(xppi);
        mapScaleInstance.setYyyPixelsPerInch(yppi);
        mapScaleInstance.setXxxImageSize(String.format("%d", (int)x_image_size));
        mapScaleInstance.setYyyImageSize(String.format("%d", (int)y_image_size));
        mapScaleInstance.setXxxPlotSize(String.format("%d", (int)plotWidth));
        mapScaleInstance.setYyyPlotSize(String.format("%d", (int)plotHeight))
        mapScaleInstance.setXxxOffsetFromLeft(String.format("%d", (int)xOffset_left))
        mapScaleInstance.setYyyOffsetFromBottom(String.format("%d", (int)yOffset_bottom))
        mapScaleInstance.setXxxOffsetFromRight(String.format("%d", (int)xOffset_right))
        mapScaleInstance.setYyyOffsetFromTop(String.format("%d", (int)yOffset_top))
        mapScaleInstance.setXxxAxisLowerLeft(String.format("%.8g", axisLLX));
        mapScaleInstance.setXxxAxisUpperRight(String.format("%.8g",axisURX))
        mapScaleInstance.setAxis_horizontal(scale.get("AX_HORIZ"))
        mapScaleInstance.setAxis_vertical(scale.get("AX_VERT"));
        mapScaleInstance.setAxis_vertical_positive(scale.get("AX_VERT_POSTV"));
        mapScaleInstance.setLevels_string(levels_string)
        if ( mapScaleInstance.getAxis_vertical_positive().equals(" ")) mapScaleInstance.setAxis_vertical_positive("up")

        if ( mapScaleInstance.getAxis_vertical_positive().equals("down") ) {
            mapScaleInstance.setYyyAxisLowerLeft(String.format("%.8g", axisURY))
            mapScaleInstance.setYyyAxisUpperRight(String.format("%.8g", axisLLY))
        } else {
            mapScaleInstance.setYyyAxisLowerLeft(String.format("%.8g", axisLLY))
            mapScaleInstance.setYyyAxisUpperRight(String.format("%.8g", axisURY))
        }


        mapScaleInstance.setAxis_horizontal_positive(scale.get("AX_HORIZ_POSTV"));
        if ( mapScaleInstance.getAxis_horizontal_positive().equals(" ")) mapScaleInstance.setAxis_horizontal_positive("right")
        mapScaleInstance.setData_min(String.format("%.8g",data_min));
        mapScaleInstance.setData_max(String.format("%.8g",data_max));
        mapScaleInstance.setData_exists(String.format("%d",data_exists));
        mapScaleInstance.setXxxStride(scale.get("XSTRIDE"));
        mapScaleInstance.setYyyStride(scale.get("YSTRIDE"));

        // The autobean mechanism on the client requires an ID.
        mapScaleInstance.id = 99l;

        if (time_min != null) {
            mapScaleInstance.setTime_min(time_min);
        }
        if (time_max != null) {
            mapScaleInstance.setTime_max(time_max);
        }
        if (time_units != null) {
            mapScaleInstance.setTime_units(time_units);
        }
        if (time_origin != null) {
            mapScaleInstance.setTime_origin(time_origin);
        }
        if ( calendar != null ) {
            mapScaleInstance.setCalendar(calendar);
        }

        mapScaleInstance

    }

    def ResultSet doRequest(LASRequest lasRequest, Product product, String hash, temp_dir, base, String outputPath, String ferret_temp) {

        def productName = product.getName()
        String view = product.getView()

        List<RequestProperty> properties = lasRequest.getRequestProperties();
        List<Analysis> analysis = lasRequest.getAnalysis()

        /*
        Loop through all of the operations. Accumulate the results from each operation into a global ResultsSet
         */

        ResultSet allResults = new ResultSet()
        allResults.setProduct(product.getName())
        allResults.setTargetPanel(lasRequest.getTargetPanel())

        def operations = product.getOperations()


        List<String> chainedUrls = new ArrayList<String>();
        List<String> datasetHashes = lasRequest.getDatasetHashes()
        List<String> variableHashes = lasRequest.getVariableHashes()
        List<DataConstraint> constraints = lasRequest.getDataConstraints();


        for (int o = 0; o < operations.size(); o++) {
            Operation operation = operations.get(o)

            int ostep = o + 1
            writePulse(hash, outputPath, "Doing step " + ostep + " of " + operations.size() + " of this " + product.getTitle(), null, null, null, PulseType.STARTED)
            if (operation.getType() == "ferret") {

                StringBuffer jnl = new StringBuffer()

                /** All symbols for an XY plot...
                 DEFINE SYMBOL data_0_ID = airt
                 DEFINE SYMBOL data_0_dataset_ID = coads_climatology_cdf
                 DEFINE SYMBOL data_0_dataset_name = COADS climatology
                 DEFINE SYMBOL data_0_dataset_url = file:coads_climatology
                 DEFINE SYMBOL data_0_dsid = coads_climatology_cdf
                 DEFINE SYMBOL data_0_ftds_url = http://gazelle.weathertopconsulting.com:8282/thredds/dodsC/las/coads_climatology_cdf/data_coads_climatology.jnl
                 DEFINE SYMBOL data_0_grid_type = regular
                 DEFINE SYMBOL data_0_intervals = xyt
                 DEFINE SYMBOL data_0_name = AIR TEMPERATURE
                 DEFINE SYMBOL data_0_points = xyt
                 DEFINE SYMBOL data_0_region = region_0
                 DEFINE SYMBOL data_0_title = AIR TEMPERATURE
                 DEFINE SYMBOL data_0_units = DEG C
                 DEFINE SYMBOL data_0_url = coads_climatology
                 DEFINE SYMBOL data_0_var = airt
                 DEFINE SYMBOL data_0_xpath = /lasdata/datasets/coads_climatology_cdf/variables/airt
                 DEFINE SYMBOL data_count = 1
                 DEFINE SYMBOL ferret_annotations = file
                 DEFINE SYMBOL ferret_fill_type = fill
                 DEFINE SYMBOL ferret_image_format = gif
                 DEFINE SYMBOL ferret_land_type = shade
                 DEFINE SYMBOL ferret_service_action = Plot_2D_XY
                 DEFINE SYMBOL ferret_size = .8333
                 DEFINE SYMBOL ferret_view = xy
                 DEFINE SYMBOL las_debug = false
                 DEFINE SYMBOL las_output_type = xml
                 DEFINE SYMBOL operation_ID = Plot_2D_XY_zoom
                 DEFINE SYMBOL operation_key = B070A6828DCD95F39BB5D58F17277A50
                 DEFINE SYMBOL operation_name = Color plot
                 DEFINE SYMBOL operation_service = ferret
                 DEFINE SYMBOL operation_service_action = Plot_2D_XY
                 DEFINE SYMBOL product_server_clean_age = 168
                 DEFINE SYMBOL product_server_clean_interval = 24
                 DEFINE SYMBOL product_server_clean_time = 00:01
                 DEFINE SYMBOL product_server_clean_units = hour
                 DEFINE SYMBOL product_server_default_catid = ocean_atlas_subset
                 DEFINE SYMBOL product_server_default_dsid = ocean_atlas_subset
                 DEFINE SYMBOL product_server_default_operation = Plot_2D_XY_zoom
                 DEFINE SYMBOL product_server_default_option = Options_2D_image_contour_xy_7
                 DEFINE SYMBOL product_server_default_varid = TEMP-ocean_atlas_subset
                 DEFINE SYMBOL product_server_default_view = xy
                 DEFINE SYMBOL product_server_ps_timeout = 3600
                 DEFINE SYMBOL product_server_ui_timeout = 10
                 DEFINE SYMBOL product_server_use_cache = true
                 DEFINE SYMBOL product_server_version = 7.3
                 DEFINE SYMBOL region_0_t_hi = 15-Jan
                 DEFINE SYMBOL region_0_t_lo = 15-Jan
                 DEFINE SYMBOL region_0_x_hi = 360
                 DEFINE SYMBOL region_0_x_lo = 0
                 DEFINE SYMBOL region_0_y_hi = 90
                 DEFINE SYMBOL region_0_y_lo = -90
                 DEFINE SYMBOL result_annotations_ID = annotations
                 DEFINE SYMBOL result_annotations_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_annotations.xml
                 DEFINE SYMBOL result_annotations_type = annotations
                 DEFINE SYMBOL result_cancel_ID = cancel
                 DEFINE SYMBOL result_cancel_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_cancel.txt
                 DEFINE SYMBOL result_cancel_type = cancel
                 DEFINE SYMBOL result_count = 11
                 DEFINE SYMBOL result_debug_ID = debug
                 DEFINE SYMBOL result_debug_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_debug.txt
                 DEFINE SYMBOL result_debug_type = debug
                 DEFINE SYMBOL result_map_scale_ID = map_scale
                 DEFINE SYMBOL result_map_scale_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_map_scale.xml
                 DEFINE SYMBOL result_map_scale_type = map_scale
                 DEFINE SYMBOL result_plot_image_ID = plot_image
                 DEFINE SYMBOL result_plot_image_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_plot_image.png
                 DEFINE SYMBOL result_plot_image_type = image
                 DEFINE SYMBOL result_plot_pdf_ID = plot_pdf
                 DEFINE SYMBOL result_plot_pdf_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_plot_pdf.pdf
                 DEFINE SYMBOL result_plot_pdf_type = pdf
                 DEFINE SYMBOL result_plot_ps_ID = plot_ps
                 DEFINE SYMBOL result_plot_ps_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_plot_ps.ps
                 DEFINE SYMBOL result_plot_ps_type = ps
                 DEFINE SYMBOL result_plot_svg_ID = plot_svg
                 DEFINE SYMBOL result_plot_svg_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_plot_svg.svg
                 DEFINE SYMBOL result_plot_svg_type = svg
                 DEFINE SYMBOL result_ref_map_ID = ref_map
                 DEFINE SYMBOL result_ref_map_filename = /home/rhs/tomcat/webapps/struts2/output/B070A6828DCD95F39BB5D58F17277A50_ref_map.png
                 DEFINE SYMBOL result_ref_map_type = image
                 DEFINE SYMBOL result_rss_ID = rss
                 DEFINE SYMBOL result_rss_filename = /home/rhs/tomcat/webapps/struts2/output/50C7105E454C06D199E6B62844E08B67_rss.rss
                 DEFINE SYMBOL result_rss_type = rss
                 */

                /** Symbols for a difference..
                 *
                 ! Symbols from the server
                 DEFINE SYMBOL data_0_ID = TEMP-ocean_atlas_subset
                 DEFINE SYMBOL data_0_dataset_ID = ocean_atlas_subset
                 DEFINE SYMBOL data_0_dataset_name = Subset of World Ocean Atlas monthly 1994 Monthly Means
                 DEFINE SYMBOL data_0_dataset_url = ocean_atlas_subset
                 DEFINE SYMBOL data_0_dsid = ocean_atlas_subset
                 DEFINE SYMBOL data_0_ftds_url = http://dunkel.pmel.noaa.gov:8920/thredds/dodsC/las/ocean_atlas_subset/data_ocean_atlas_subset.jnl
                 DEFINE SYMBOL data_0_grid_type = regular
                 DEFINE SYMBOL data_0_intervals = xyzt
                 DEFINE SYMBOL data_0_name = Temperature
                 DEFINE SYMBOL data_0_points = xyzt
                 DEFINE SYMBOL data_0_region = region_0
                 DEFINE SYMBOL data_0_title = Temperature
                 DEFINE SYMBOL data_0_units = Deg C
                 DEFINE SYMBOL data_0_url = http://dunkel.pmel.noaa.gov:8920/thredds/dodsC/las/ocean_atlas_subset/data_ocean_atlas_subset.jnl
                 DEFINE SYMBOL data_0_var = TEMP
                 DEFINE SYMBOL data_1_ID = TEMP-ocean_atlas_subset
                 DEFINE SYMBOL data_1_dataset_ID = ocean_atlas_subset
                 DEFINE SYMBOL data_1_dataset_name = Subset of World Ocean Atlas monthly 1994 Monthly Means
                 DEFINE SYMBOL data_1_dataset_url = ocean_atlas_subset
                 DEFINE SYMBOL data_1_dsid = ocean_atlas_subset
                 DEFINE SYMBOL data_1_ftds_url = http://dunkel.pmel.noaa.gov:8920/thredds/dodsC/las/ocean_atlas_subset/data_ocean_atlas_subset.jnl
                 DEFINE SYMBOL data_1_grid_type = regular
                 DEFINE SYMBOL data_1_intervals = xyzt
                 DEFINE SYMBOL data_1_name = Temperature
                 DEFINE SYMBOL data_1_points = xyzt
                 DEFINE SYMBOL data_1_region = region_1
                 DEFINE SYMBOL data_1_title = Temperature
                 DEFINE SYMBOL data_1_units = Deg C
                 DEFINE SYMBOL data_1_url = http://dunkel.pmel.noaa.gov:8920/thredds/dodsC/las/ocean_atlas_subset/data_ocean_atlas_subset.jnl
                 DEFINE SYMBOL data_1_var = TEMP
                 DEFINE SYMBOL data_1_xpath = /lasdata/datasets/ocean_atlas_subset/variables/TEMP-ocean_atlas_subset
                 DEFINE SYMBOL data_count = 2
                 DEFINE SYMBOL ferret_annotations = file
                 DEFINE SYMBOL ferret_fill_type = fill
                 DEFINE SYMBOL ferret_image_format = gif
                 DEFINE SYMBOL ferret_land_type = contour
                 DEFINE SYMBOL ferret_service_action = Compare_Plot
                 DEFINE SYMBOL ferret_size = .8333
                 DEFINE SYMBOL ferret_view = xy
                 DEFINE SYMBOL las_debug = false
                 DEFINE SYMBOL las_output_type = xml
                 DEFINE SYMBOL operation_ID = Compare_Plot
                 DEFINE SYMBOL operation_key = D1F0FFE15252EDFC5563D822A193F274
                 DEFINE SYMBOL operation_name = Difference plot
                 DEFINE SYMBOL operation_service = ferret
                 DEFINE SYMBOL operation_service_action = Compare_Plot
                 DEFINE SYMBOL product_server_clean_age = 168
                 DEFINE SYMBOL product_server_clean_interval = 24
                 DEFINE SYMBOL product_server_clean_time = 00:01
                 DEFINE SYMBOL product_server_clean_units = hour
                 DEFINE SYMBOL product_server_default_catid = ocean_atlas_subset
                 DEFINE SYMBOL product_server_default_dsid = ocean_atlas_subset
                 DEFINE SYMBOL product_server_default_operation = Plot_2D_XY_zoom
                 DEFINE SYMBOL product_server_default_option = Options_2D_image_contour_xy_7
                 DEFINE SYMBOL product_server_default_varid = TEMP-ocean_atlas_subset
                 DEFINE SYMBOL product_server_default_view = xy
                 DEFINE SYMBOL product_server_ps_timeout = 3600
                 DEFINE SYMBOL product_server_ui_timeout = 20
                 DEFINE SYMBOL product_server_use_cache = true
                 DEFINE SYMBOL product_server_version = 8.4
                 DEFINE SYMBOL region_0_t_hi = 16-Jan-0001 00:00
                 DEFINE SYMBOL region_0_t_lo = 16-Jan-0001 00:00
                 DEFINE SYMBOL region_0_x_hi = 378.5
                 DEFINE SYMBOL region_0_x_lo = 20.5
                 DEFINE SYMBOL region_0_y_hi = 88.5
                 DEFINE SYMBOL region_0_y_lo = -89.5
                 DEFINE SYMBOL region_0_z_hi = 0
                 DEFINE SYMBOL region_0_z_lo = 0
                 DEFINE SYMBOL region_1_t_hi = 16-Feb-0001 00:00
                 DEFINE SYMBOL region_1_t_lo = 16-Feb-0001 00:00
                 DEFINE SYMBOL region_1_z_hi = 0
                 DEFINE SYMBOL region_1_z_lo = 0
                 DEFINE SYMBOL result_annotations_ID = annotations
                 DEFINE SYMBOL result_annotations_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_annotations.xml
                 DEFINE SYMBOL result_annotations_type = annotations
                 DEFINE SYMBOL result_cancel_ID = cancel
                 DEFINE SYMBOL result_cancel_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_cancel.txt
                 DEFINE SYMBOL result_cancel_type = cancel
                 DEFINE SYMBOL result_count = 11
                 DEFINE SYMBOL result_debug_ID = debug
                 DEFINE SYMBOL result_debug_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_debug.txt
                 DEFINE SYMBOL result_debug_type = debug
                 DEFINE SYMBOL result_map_scale_ID = map_scale
                 DEFINE SYMBOL result_map_scale_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_map_scale.map_scale
                 DEFINE SYMBOL result_map_scale_type = map_scale
                 DEFINE SYMBOL result_plot_image_ID = plot_image
                 DEFINE SYMBOL result_plot_image_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_plot_image.png
                 DEFINE SYMBOL result_plot_image_type = image
                 DEFINE SYMBOL result_plot_pdf_ID = plot_pdf
                 DEFINE SYMBOL result_plot_pdf_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_plot_pdf.pdf
                 DEFINE SYMBOL result_plot_pdf_type = pdf
                 DEFINE SYMBOL result_plot_ps_ID = plot_ps
                 DEFINE SYMBOL result_plot_ps_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_plot_ps.ps
                 DEFINE SYMBOL result_plot_ps_type = ps
                 DEFINE SYMBOL result_plot_svg_ID = plot_svg
                 DEFINE SYMBOL result_plot_svg_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_plot_svg.svg
                 DEFINE SYMBOL result_plot_svg_type = svg
                 DEFINE SYMBOL result_ref_map_ID = ref_map
                 DEFINE SYMBOL result_ref_map_filename = /home/users/rhs/tomcat/webapps/las/output/D1F0FFE15252EDFC5563D822A193F274_ref_map.png
                 DEFINE SYMBOL result_ref_map_type = image
                 DEFINE SYMBOL result_rss_ID = rss
                 DEFINE SYMBOL result_rss_filename = /home/users/rhs/tomcat/webapps/las/output/8B3C55B5A1F07D023C0D3A941D14BE71_rss.rss
                 DEFINE SYMBOL result_rss_type = rss
                 GO ($operation_service_action)

                 */

                Dataset dataset = null
                Variable variable = null
                def lasVersion = grailsApplication.getMetadata().getApplicationVersion()
                // There is one data set has entry and one variable hash entry for each variable in the request
                // even if the variables are from the same data set.
                for (int h = 0; h < datasetHashes.size(); h++) {
                    // Apply the analysis to the variable URL
                    dataset = Dataset.findByHash(datasetHashes.get(h))
                    variable = dataset.variables.find { Variable v -> v.hash == variableHashes.get(h) }

                    String variable_url;

                    // TODO all variables are in one file. This might ought not be an array
                    if ( chainedUrls.size() > 0 ) {
                        variable_url = chainedUrls.get(0)
                    } else {
                        variable_url = variable.getUrl()
                    }

                    String variable_name = variable.getName()
                    String variable_title = variable.getTitle()
                    String varable_hash = variable.getHash()
                    String dataset_hash = dataset.getHash()


                    def analysis_axes = new ArrayList<String>();
                    // TODO other variables?
                    // TODO for now only analysis on first variable, but don't really know what this should look like
                    if (h == 0 && analysis != null && analysis.get(h) != null) {

                        Analysis a = analysis.get(h)

                        String type = a.getTransformation();

                        // Make dataset specific directory
                        String dir = temp_dir + File.separator + "dynamic" + File.separator + dataset_hash + File.separator + varable_hash
                        File ftds_dir = new File(dir)
                        if (!ftds_dir.exists()) {
                            ftds_dir.mkdirs()
                        }


                        analysis_axes = a.getAxes()
                        List<AnalysisAxis> axes = a.getAnalysisAxes()


                        StringBuffer ftds_jnl = new StringBuffer()

                        ftds_jnl.append("use \"" + variable_url + "\";\n");

                        String allAxis = ""
                        for (int i = 0; i < axes.size(); i++) {
                            AnalysisAxis ax = axes.get(i)
                            String axisType = ax.getType()
                            String hi = ax.getHi()
                            String lo = ax.getLo()
                            String axisString = axisType + "="
                            if (axisType.equals("t")) {
                                axisString = axisString + "\"" + lo + "\":\"" + hi + "\""
                            } else {
                                axisString = axisString + "" + lo + ":" + hi + ""
                            }
                            if ( allAxis.isEmpty() ) {
                                allAxis = axisString + "@" + type
                            } else {
                                allAxis = allAxis + "," + axisString + "@" + type
                            }
                        }
                        ftds_jnl.append("let/d=1 " + variable.getName() + "_transformed = " + variable.getName() + "[d=1," + allAxis  + "];\n")
                        variable_title = variable.getName() + "[d=1," + allAxis.replace("\"", "")  + "]"
                        variable_name = variable.getName() + "_transformed"
                        ftds_jnl.append("SET ATT/LIKE=" + variable.getName() + " " + variable.getName() + "_transformed ;\n")

                        File sp = File.createTempFile("ftds_" + a.hash() + "_", ".jnl", ftds_dir);
                        sp.withWriter { out ->
                            out.writeLine(ftds_jnl.toString().stripIndent())
                        }

                        // TODO Assign the new title
                        // TODO Redefine the URL that will be used, data_set_hash/variable_hash/jnl_hash.nc

                        if ( !base.endsWith("/") ) base = base + "/"
                        variable_url = base + "las/thredds/dodsC/las/" + dataset_hash + "/" + varable_hash + "/" + sp.getName()

                    }

                    def tabledap = dataset.getDatasetPropertyGroup("tabledap_access")
                    tabledap.each {
                        jnl.append("DEFINE SYMBOL " + it.type + "_" + h + "_" + it.name + " = " + it.value + "\n")
                    }

                    if ( constraints ) {
                        for (int cidx = 0; cidx < constraints.size(); cidx++) {
                            DataConstraint c = constraints.get(cidx);
                            jnl.append("DEFINE SYMBOL constraint_${cidx}_lhs = ${c.lhs}\n")
                            jnl.append("DEFINE SYMBOL constraint_${cidx}_type = ${c.type}\n")
                            jnl.append("DEFINE SYMBOL constraint_${cidx}_op = ${c.op}\n")
                            jnl.append("DEFINE SYMBOL constraint_${cidx}_rhs = ${c.rhs}\n")
                        }
                    }

                    // TODO merge variable, dataset and global properties

                    // TODO GRID vs regular

                    Set<VariableProperty> variableProperties = variable.getVariableProperties()
                    Iterator<VariableProperty> vpIt = variableProperties.iterator()
                    while( vpIt.hasNext() ) {
                        VariableProperty vp = vpIt.next()
                        jnl.append("DEFINE SYMBOL ${vp.getType()}_${vp.getName()} = ${vp.getValue()}\n")
                    }
                    jnl.append("DEFINE SYMBOL data_${h}_dataset_name = ${dataset.title}\n")
                    jnl.append("DEFINE SYMBOL data_${h}_dataset_url = ${variable_url}\n")
                    jnl.append("DEFINE SYMBOL data_${h}_grid_type = regular\n")
                    jnl.append("DEFINE SYMBOL data_${h}_name = ${variable_name}\n")
                    jnl.append("DEFINE SYMBOL data_${h}_ID = ${variable_name}\n")
                    jnl.append("DEFINE SYMBOL data_${h}_region = region_0\n")
                    jnl.append("DEFINE SYMBOL data_${h}_title = ${variable_title.replaceAll("'","")}\n")
                    if (variable.units) jnl.append("DEFINE SYMBOL data_${h}_units = ${variable.units}\n")
                    jnl.append("DEFINE SYMBOL data_${h}_url = ${variable_url}\n")
                    jnl.append("DEFINE SYMBOL data_${h}_var = ${variable_name}\n")

                    // TODO stop sending nulls maybe
                    // Is there an axesset?
                    if ( lasRequest.getAxesSets().size() > h ) {
                        // Does it have non-null values?
                        if ( lasRequest.getAxesSets().get(h).getThi() && lasRequest.getAxesSets().get(h).getTlo()) {
                            jnl.append("DEFINE SYMBOL region_${h}_t_hi = ${lasRequest.getAxesSets().get(h).getThi()}\n")
                            jnl.append("DEFINE SYMBOL region_${h}_t_lo = ${lasRequest.getAxesSets().get(h).getTlo()}\n")
                        }
                        jnl.append("DEFINE SYMBOL region_${h}_x_hi = ${lasRequest.getAxesSets().get(h).getXhi()}\n")
                        jnl.append("DEFINE SYMBOL region_${h}_x_lo = ${lasRequest.getAxesSets().get(h).getXlo()}\n")

                        jnl.append("DEFINE SYMBOL region_${h}_y_hi = ${lasRequest.getAxesSets().get(h).getYhi()}\n")
                        jnl.append("DEFINE SYMBOL region_${h}_y_lo = ${lasRequest.getAxesSets().get(h).getYlo()}\n")

                        if (lasRequest.getAxesSets().get(h).getZlo()) jnl.append("DEFINE SYMBOL region_${h}_z_lo = ${lasRequest.getAxesSets().get(h).getZlo()}\n")
                        if (lasRequest.getAxesSets().get(h).getZhi()) jnl.append("DEFINE SYMBOL region_${h}_z_hi = ${lasRequest.getAxesSets().get(h).getZhi()}\n")
                    }

//                        if (lasRequest.getAxesSets().get(1) != null) {
//
//
//                            if (!lasRequest.getAxesSets().get(1).getThi().equals("null")) jnl.append("DEFINE SYMBOL region_1_t_hi = ${lasRequest.getAxesSets().get(1).getThi()}\n")
//                            if (!lasRequest.getAxesSets().get(1).getTlo().equals("null")) jnl.append("DEFINE SYMBOL region_1_t_lo = ${lasRequest.getAxesSets().get(1).getTlo()}\n")
//                            // TODO value null
//                            // FIXME maybe don't send null from client
//                            if (lasRequest.getAxesSets().get(1).getXhi()) jnl.append("DEFINE SYMBOL region_1_x_hi = ${lasRequest.getAxesSets().get(1).getXhi()}\n")
//                            if (lasRequest.getAxesSets().get(1).getXlo()) jnl.append("DEFINE SYMBOL region_1_x_lo = ${lasRequest.getAxesSets().get(1).getXlo()}\n")
//
//                            if (lasRequest.getAxesSets().get(1).getYhi()) jnl.append("DEFINE SYMBOL region_1_y_hi = ${lasRequest.getAxesSets().get(1).getYhi()}\n")
//                            if (lasRequest.getAxesSets().get(1).getYlo()) jnl.append("DEFINE SYMBOL region_1_y_lo = ${lasRequest.getAxesSets().get(1).getYlo()}\n")
//
//                            if (lasRequest.getAxesSets().get(1).getZlo()) jnl.append("DEFINE SYMBOL region_1_z_lo = ${lasRequest.getAxesSets().get(1).getZlo()}\n")
//                            if (lasRequest.getAxesSets().get(1).getZhi()) jnl.append("DEFINE SYMBOL region_1_z_hi = ${lasRequest.getAxesSets().get(1).getZhi()}\n")
//
//                        }

                }
                jnl.append("DEFINE SYMBOL data_count = ${datasetHashes.size()}\n")
                jnl.append("DEFINE SYMBOL ferret_annotations = file\n")
//        jnl.append("DEFINE SYMBOL ferret_fill_type = fill\n")
//        jnl.append("DEFINE SYMBOL ferret_image_format = gif\n")
//        jnl.append("DEFINE SYMBOL ferret_land_type = shade\n")
                jnl.append("DEFINE SYMBOL ferret_service_action = ${product.operations.get(0).service_action}\n")
                jnl.append("DEFINE SYMBOL ferret_size = 1.0\n")
                // TODO this is a hack.
                // For some reason a view of "t" causes all the time series trajectory lines to be the same color
                if ( product.name == "Trajectory_timeseries_plot" || product.name == "Timeseries_station_plot") {
                    view = product.getData_view()
                }
                jnl.append("DEFINE SYMBOL ferret_las_version = ${lasVersion}\n")
                jnl.append("DEFINE SYMBOL ferret_view = " + view + "\n")
                jnl.append("DEFINE SYMBOL las_debug = false\n")
                jnl.append("DEFINE SYMBOL las_output_type = xml\n")
                jnl.append("DEFINE SYMBOL operation_ID = ${product.name}\n")
                jnl.append("DEFINE SYMBOL operation_key = ${hash}\n")
                jnl.append("DEFINE SYMBOL operation_name = ${product.name}\n")
                jnl.append("DEFINE SYMBOL operation_service = ferret\n")
                jnl.append("DEFINE SYMBOL operation_service_action = ${product.operations.get(0).service_action}\n")

                // TODO this has to come from the config
                jnl.append("DEFINE SYMBOL product_server_ps_timeout = 3600\n")
                jnl.append("DEFINE SYMBOL product_server_ui_timeout = 10\n")
                jnl.append("DEFINE SYMBOL product_server_use_cache = true\n")
                //ha ha jnl.append("DEFINE SYMBOL product_server_version = 7.3")
                //TODO one for each variable
                // TODO check the value for null before applying

                if ( properties ) {
                    for (int i = 0; i < properties.size(); i++) {
                        RequestProperty p = properties.get(i)
                        jnl.append("DEFINE SYMBOL " + p.getType() + "_" + p.getName() + " = " + p.getValue() + "\n")
                    }
                }

                def resultSet = operation.getResultSet()

                resultSet.results.each { Result result ->

                    /*
String name
String type
String mime_type
String suffix
String url
String filename
         */

                    jnl.append("DEFINE SYMBOL result_${result.name}_ID = ${result.name}\n")
                    jnl.append("DEFINE SYMBOL result_${result.name}_filename = ${result.filename}\n")
                    jnl.append("DEFINE SYMBOL result_${result.name}_type = ${result.type}\n")


                }


                jnl.append("go ${operation.service_action}\n")


                File sp = File.createTempFile("script", ".jnl", new File(ferret_temp));
                writePulse(hash, outputPath, "Running pyferret process.", sp.getName(), null, null, PulseType.STARTED)

                def ferretResult = ferretService.runScript(jnl, sp)

                def error = ferretResult["error"];
                if (error) {
                    log.error(ferretResult["message"]);
                    ResultSet errorSet = new ResultSet();
                    errorSet.setTargetPanel(lasRequest.getTargetPanel())
                    errorSet.setProduct(product.getName())
                    errorSet.setError(ferretResult["message"])
                    removePulse(hash, outputPath);
                    return errorSet
                }

                ferretService.addResults(resultSet, allResults, product.getName())


            } else if (operation.type == "erddap") {

                log.debug("Making erddap product."); // debug

                // TODO for now assume there is only one data set on an ERDDAP operation
                Dataset dataset = Dataset.findByHash(datasetHashes.get(0))

                try {
                    String chainedURL = erddapService.makeNetcdfFile(lasRequest, hash, outputPath, productName, operation, dataset)
                    chainedUrls.add(chainedURL)
                } catch (Exception e) {
                    ResultSet errorSet = new ResultSet();
                    errorSet.setError(e.getMessage())
                    errorSet.setTargetPanel(lasRequest.getTargetPanel())
                    removePulse(hash, outputPath)
                    return errorSet;
                }

                // TODO now what? with the file

            } // operation type

        } // loop on operations
        writePulse(hash, outputPath, "Finished making product.", null, null, null, PulseType.COMPLETED)
        allResults;
    }
    def Map cacheCheck(Product product, String hash, String outputPath) {
        def operations = product.getOperations()
        ResultSet cacheResultSet;
        boolean cache = true;
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i)
            cacheResultSet = operation.getResultSet()
            cacheResultSet.results.each { Result result ->
                File file = new File(result.filename)
                cache = cache && file.exists()
            }
        }

        if ( cache ) {
            def allResults = new ResultSet()
            for (int i = 0; i < operations.size(); i++) {
                Operation operation = operations.get(i)
                if (operation.getType() == "ferret") {
                    def resultSet = product.operations.get(i).getResultSet()
                    ferretService.addResults(resultSet, allResults, product.getName())
                }
            }
            return [cache: cache, resultSet: allResults]
        } else {
            return [cache: cache, resultSet: cacheResultSet]
        }

    }
    void removePulse(String hash, String outputPath) {
        File pulseFile = new File(outputPath + File.separator + hash+"_pulse.json")
        pulseFile.delete();
    }
    Pulse checkPulse(String hash, String outputPath) {
        File pulseFile = new File(outputPath + File.separator + hash+"_pulse.json")
        boolean hasPulse = pulseFile.exists()
        def pulse;
        if ( hasPulse ) {
            try {
                def pulseJson = jsonSlurper.parse(pulseFile);
                pulse = new Pulse(pulseJson)
                pulse.setHasPulse(hasPulse)
            } catch ( Exception e ) {
                pulse = initializePulse(pulseFile, hash, outputPath, hasPulse)
            }
        } else {
            pulse = initializePulse(pulseFile, hash, outputPath, hasPulse)
        }
        pulse
    }
    Pulse initializePulse(File pulseFile, String hash, String outputPath, boolean hasPulse) {
        Pulse pulse = new Pulse();
        pulse.setHasPulse(hasPulse)
        pulse.setPulseFile(pulseFile.getAbsolutePath())
        pulse.setState(PulseType.STARTED)
        pulse.addMessage("LAS has started making your product.")
        String j = JsonOutput.toJson(pulse)
        pulseFile.write(j)
        pulse
    }

    void writePulse(String hash, String outputPath, String message, String ferretScript, String downloadFile, Map processInfo, String state) {
        Pulse pulse = new Pulse();
        File pulseFile = new File(outputPath + File.separator + hash+"_pulse.json")
        boolean hasPulse = pulseFile.exists()
        pulse.setPulseFile(pulseFile.getAbsolutePath())
        pulse.setHasPulse(hasPulse)
        pulse.setState(state)
        pulse.setFerretScript(ferretScript)
        pulse.setDownloadFile(downloadFile)
        pulse.addMessage(message)
        if ( downloadFile ) {
            File dlf = new File(downloadFile)
            if ( dlf.exists() ) {
                def size = dlf.length()
                def fsize = String.format("%,d", size);
                pulse.addMessage(fsize + " bytes of data have been downloaded so far.")
            }
        }
        if ( processInfo ) {
            pulse.setPid(processInfo.PID)
            if ( processInfo.TIME) {
                pulse.setTime(processInfo.TIME);
                pulse.addMessage("Accumulated time for the PyFerret process: " + processInfo.TIME)
            }
            if ( processInfo.MEMORY) {
                pulse.setMemory(processInfo.MEMORY);
                def s = Long.valueOf(processInfo.MEMORY)
                def msize = String.format("%,d", s)
                pulse.addMessage("PyFerret is using " + msize + " bytes of memory.")
            }
        }
        String j = JsonOutput.toJson(pulse)
        pulseFile.write(j)
    }
    def ResultSet pulseResult(LASRequest lasRequest, String hash, String tempDir, String base, String outputPath, Product product) {
        Pulse pulse = checkPulse(hash, outputPath)
        ResultSet pulseResult = new ResultSet()
        pulseResult.setTargetPanel(lasRequest.getTargetPanel())
        pulseResult.setProduct(lasRequest.getOperation())
        Result result = new Result()
        result.setType("pulse")
        result.setName("Pulse")
        pulseResult.addToResults(result)
        AnnotationGroup pulseGroup = new AnnotationGroup()
        pulseGroup.setType("pulse")
        for (int i = 0; i < pulse.getMessages().size(); i++) {
            Annotation annotation = new Annotation()
            annotation.setType("progress")
            annotation.setValue(pulse.getMessages().get(i))
            pulseGroup.addToAnnotations(annotation)
        }
        pulseResult.addToAnnotationGroups(pulseGroup)
        pulseResult
    }
    def ResultSet errorResult(LASRequest lasRequest, String hash, String tempDir, String base, String outputPath, Product product) {

    }
    Map getProcessInfo(processName) {
        def processes = 'ps -eo uid,pid,etime,drs,args'.execute().text.split('\n')
        def line = processes.find {it.contains processName }
        def fields = line?.split()
        if ( fields ) {
            // UID        PID  PPID  C STIME TTY          TIME CMD
            return [UID: fields[0], PID: fields[1], TIME: fields[2], MEMORY: fields[3]]
        }
        return null;
    }
}
