package pmel.sdig.las

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import grails.gorm.transactions.Transactional
import grails.util.Environment
import grails.util.Holders
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import pmel.sdig.las.latlon.LatLonUtil

import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

import static grails.async.Promises.task

@Transactional(readOnly = true)
class ProductController {
//    static scaffold = Product
    FerretService ferretService
    ProductService productService
    LASProxy lasProxy = new LASProxy()
    JsonParser jsonParser = new JsonParser()
    ResultsService resultsService
    DateTimeService dateTimeService

    def summary () {
        def webAppDirectory = request.getSession().getServletContext().getRealPath("")
        // Until such time as "-" file names are allowed, deal with it with a link named without the "-"
        // i.e. webapp instead of web-app
        webAppDirectory = webAppDirectory.replaceAll("-", "")

        if (!webAppDirectory.endsWith(File.separator)) {
            webAppDirectory = webAppDirectory + File.separator
        }
        FerretEnvironment fe = FerretEnvironment.first()
        String variable_url
        def data = fe.getFer_data().tokenize().each{
            String testpath = it+File.separator+"etopo20.cdf"
            File f = new File(testpath)
            if ( f.exists() ) variable_url = f.getPath()
        }
        if ( variable_url ) {
            String variable_name = "ROSE"
            String variable_title = "Relief of the Surface of the Earth"
            String variable_units = "M"
            String hash = "rose20_full"

            StringBuffer jnl = new StringBuffer()

            jnl.append("DEFINE SYMBOL data_0_dataset_name = World Map\n")
            jnl.append("DEFINE SYMBOL data_0_dataset_url = ${variable_url}\n")
            jnl.append("DEFINE SYMBOL data_0_grid_type = regular\n")
            jnl.append("DEFINE SYMBOL data_0_name = ${variable_name}\n")
            jnl.append("DEFINE SYMBOL data_0_ID = ${variable_name}\n")
            jnl.append("DEFINE SYMBOL data_0_region = region_0\n")
            jnl.append("DEFINE SYMBOL data_0_title = ${variable_title}\n")
            jnl.append("DEFINE SYMBOL data_0_units = ${variable_units}\n")
            jnl.append("DEFINE SYMBOL data_0_url = ${variable_url}\n")
            jnl.append("DEFINE SYMBOL data_0_var = ${variable_name}\n")

            jnl.append("DEFINE SYMBOL region_0_x_hi = 0\n")
            jnl.append("DEFINE SYMBOL region_0_x_lo = 360\n")

            jnl.append("DEFINE SYMBOL region_0_y_hi = 90\n")
            jnl.append("DEFINE SYMBOL region_0_y_lo = -90\n")

            jnl.append("DEFINE SYMBOL data_count = 1\n")
            jnl.append("DEFINE SYMBOL ferret_annotations = file\n")
            jnl.append("DEFINE SYMBOL ferret_service_action = Plot_2D_XY\n")
            jnl.append("DEFINE SYMBOL operation_name = Plot_2D_XY\n")

            jnl.append("DEFINE SYMBOL ferret_size = .85\n")
            jnl.append("DEFINE SYMBOL ferret_view = xy\n")
            jnl.append("DEFINE SYMBOL las_debug = false\n")
            jnl.append("DEFINE SYMBOL las_output_type = xml\n")
            jnl.append("DEFINE SYMBOL operation_ID = Plot_2D_XY\n")
            jnl.append("DEFINE SYMBOL operation_key = ${hash}\n")
            jnl.append("DEFINE SYMBOL operation_service = ferret\n")


            jnl.append("DEFINE SYMBOL ferret_service_action = Plot_2D_XY\n")
            jnl.append("DEFINE SYMBOL operation_name = Plot_2D_XY\n")

            // TODO this has to come from the config
            jnl.append("DEFINE SYMBOL product_server_ps_timeout = 3600\n")
            jnl.append("DEFINE SYMBOL product_server_ui_timeout = 10\n")
            jnl.append("DEFINE SYMBOL product_server_use_cache = true\n")
            //ha ha jnl.append("DEFINE SYMBOL product_server_version = 7.3")]

            jnl.append("DEFINE SYMBOL ferret_fill_levels = 60c\n")
            jnl.append("DEFINE SYMBOL ferret_memsize = 128\n")
            jnl.append("DEFINE SYMBOL ferret_contour_levels = vc\n")
            jnl.append("DEFINE SYMBOL ferret_palette = topo_osmc_blue_brown\n")
            jnl.append("DEFINE SYMBOL ferret_land_type = none\n")


            //TODO one for each variable
            // TODO check the value for null before applying

            ResultSet resultSet = resultsService.getThumbnailResults()
            def cache = true
            def cache_filename
            resultSet.results.each { Result result ->
                // All we care about is the plot
                result.url = "output${File.separator}${hash}_${result.name}${result.suffix}"
                result.filename = "${webAppDirectory}output${File.separator}${hash}_${result.name}${result.suffix}"
                // The plot file is the only cache result we care about
                if (result.name == "plot_image") {
                    File file = new File(result.filename)
                    cache = cache && file.exists()
                    if (cache) {
                        cache_filename = result.getFilename()
                    }
                }
            }

            if (cache) {
                render file: cache_filename, contentType: 'image/png'
            } else {
                for (int i = 0; i < resultSet.getResults().size(); i++) {

                    def result = resultSet.getResults().get(i)

                    jnl.append("DEFINE SYMBOL result_${result.name}_ID = ${result.name}\n")
                    if (result.type == "image") {
                        jnl.append("DEFINE SYMBOL result_${result.name}_filename = ${webAppDirectory}output${File.separator}${hash}_${result.name}_base_${result.suffix}\n")
                    } else {
                        jnl.append("DEFINE SYMBOL result_${result.name}_filename = ${webAppDirectory}output${File.separator}${hash}_${result.name}${result.suffix}\n")
                    }
                    jnl.append("DEFINE SYMBOL result_${result.name}_type = ${result.type}\n")


                }
                jnl.append("go Plot_2D_XY\n")

                def datasets = Dataset.findAllByVariableChildren(true)
                for (int i = 0; i < datasets.size(); i++) {
                    def dataset = datasets.get(i)
                    if (dataset.getVariables() && dataset.getVariables().size() > 0) {
                        def variable = dataset.getVariables().get(0)
                        def xaxis = variable.getGeoAxisX()
                        def yaxis = variable.getGeoAxisY()


                        def x = xaxis.getMin() + "," + xaxis.getMin() + "," + xaxis.getMax() + "," + xaxis.getMax()
                        def y = yaxis.getMin() + "," + yaxis.getMax() + "," + yaxis.getMax() + "," + yaxis.getMin()
                        jnl.append("LET xoutline = YSEQUENCE({${x}})\n")
                        jnl.append("LET youtline = YSEQUENCE({${y}})\n")
                        jnl.append("POLYGON/OVER/MODULO/LINE/COLOR=${ferretService.getFerretColorValue(i)}/THICK=2/TITLE=\"${dataset.title}\" xoutline, youtline\n")

                    }
                }

                jnl.append("FRAME/FORMAT=PNG/FILE=\"${webAppDirectory}output${File.separator}${hash}_plot_image.png\"\n");

                def ferretResult = ferretService.runScript(jnl)
                def error = ferretResult["error"];
                // TODO error image???
                if (error) {
                    log.error(ferretResult["message"]);
                    render file: "/tmp/error.png", contentType: 'image/png'
                } else {
                    ResultSet allResults = new ResultSet()
                    ferretService.addResults(resultSet, allResults, "Plot_2D_XY")
                    Result r = allResults.results.find { it.name == "plot_image" }
                    render file: r.getFilename(), contentType: 'image/png'
                }


            }
        } else {
            render file: "/tmp/error.png", contentType: 'image/png'
        }
    }
    def cancel() {

        Ferret ferret = Ferret.first();

        def base = ferret.getBase_url()
        if ( !base ) {
            base = request.requestURL.toString()
            base = base.substring(0, base.indexOf("product/make"))
        }

        def requestJSON = request.JSON

        log.debug(requestJSON.toString())

        def hash = IngestService.getDigest(requestJSON.toString());
        LASRequest lasRequest = new LASRequest(requestJSON);
        File outputFile = Holders.grailsApplication.mainContext.getResource("output").file
        String outputPath = outputFile.getAbsolutePath()

        def pulse = productService.checkPulse(hash, outputPath)
        File pfile = new File(pulse.getPulseFile())
        pfile.delete();
        File cancelFile = new File("${outputPath}${File.separator}${hash}_cancel.txt")
        cancelFile.write(requestJSON.toString())
        if ( pulse.getFerretScript() ) {
            def kill = 'kill -9 ' + pulse.getPid()
            def k = kill.execute()
            cancelFile.delete();
        }
        render "Product request canceled"
    }
    def stream() {
        def urls = params.datalink;
        urls = urls.replaceAll("_amp_", "&")
        def parts = urls.split("\\?")
        def url = parts[0] + "?" + URLEncoder.encode(parts[1], "UTF-8")
        lasProxy.executeGetMethodAndStreamResult(url, response);
    }
    def make() {

        Ferret ferret = Ferret.first();

        def ferret_temp = ferret.tempDir

        def base = ferret.getBase_url()
        if ( !base ) {
            base = request.requestURL.toString()
            base = base.substring(0, base.indexOf("product/make"))
        }

        def requestJSON = request.JSON

        log.debug(requestJSON.toString())

        def hash = IngestService.getDigest(requestJSON.toString());
        LASRequest lasRequest = new LASRequest(requestJSON);
        File outputFile = Holders.grailsApplication.mainContext.getResource("output").file
        String outputPath = outputFile.getAbsolutePath()

        def pulse = productService.checkPulse(hash, outputPath)


        Product product = Product.findByName(lasRequest.operation, [fetch: [operations: 'eager']])

        boolean wait_forever = false;
        def operations = product.getOperations()
        List<RequestProperty> batch = lasRequest.getPropertyGroup("batch")
        if ( batch && batch.size() > 0 ) {
            // Wait and do not return the intermediate response for animation or correlation.
            // Use will close window to cancel.
            RequestProperty rp = batch.find{it.name=="wait"}
            if ( rp && rp.getValue().equals("true") ) {
                wait_forever = true
            }
        }

        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i)
            ResultSet rs = operation.getResultSet()
            List<Result> results = rs.getResults();
            for (int j = 0; j < results.size(); j++) {
                Result result = results.get(j)
                result.setUrl("output${File.separator}${hash}_${result.name}${result.suffix}")
                result.setFilename("${outputPath}${File.separator}${hash}_${result.name}${result.suffix}")
            }
        }

        def resultSet
        // Do animation and correlation requests without pulse; user should be able to just close the window to cancel3
        if ( wait_forever ) {
            resultSet = productService.doRequest(lasRequest, product, hash, ferret.getTempDir(), base, outputPath, ferret_temp);
        } else {
            if (pulse.hasPulse) {
                // If it has a pulse, check it status and return the appropriate response
                if (pulse.getState().equals(PulseType.COMPLETED)) {
                    Map cacheMap = productService.cacheCheck(product, hash, outputPath)
                    resultSet = cacheMap.resultSet
                    resultSet.setTargetPanel(lasRequest.getTargetPanel())
                    resultSet.setProduct(product.getName())
                    if (!cacheMap.cache) {
                        // Cache was invalid, so start the job again
                        // TODO NOT DRY see below
                        // If the current request does not have a pulse, start of request and wait 20 seconds for it to finish
                        def p = task {
                            productService.doRequest(lasRequest, product, hash, ferret.getTempDir(), base, outputPath, ferret_temp);
                        }
                        try {
                            resultSet = p.get(10l, TimeUnit.SECONDS)
                            // End of request
                            log.debug("Finished product request, rendering response...")
                        } catch (TimeoutException e) {
                            resultSet = productService.pulseResult(lasRequest, hash, ferret.getTempDir(), base, outputPath, product)
                        }
                    }
                } else if (pulse.getState().equals(PulseType.ERROR)) {
                    resultSet = productService.errorResult(lasRequest, hash, ferret.getTempDir(), base, outputPath, product);
                    File pulseFile = new File(pulse.getPulseFile())
                    pulseFile.delete()
                } else {
                    // If there is a download file and no ferret script update download, otherwise
                    // update the ferret progress iff ferretScript
                    String downloadFile = pulse.getDownloadFile()
                    String ferretScript = pulse.getFerretScript()
                    if (downloadFile && !ferretScript) {
                        productService.writePulse(hash, outputPath, "Downloading data from ERDDAP", ferretScript, downloadFile, null, PulseType.STARTED)
                    } else if (ferretScript) {
                        def pinfo = productService.getProcessInfo(ferretScript)
                        productService.writePulse(hash, outputPath, "PyFerret process is running", ferretScript, null, pinfo, PulseType.STARTED)
                    }
                    resultSet = productService.pulseResult(lasRequest, hash, ferret.getTempDir(), base, outputPath, product)
                }
            } else {
                // If the current request does not have a pulse, start of request and wait 20 seconds for it to finish
                def p = task {
                    productService.doRequest(lasRequest, product, hash, ferret.getTempDir(), base, outputPath, ferret_temp);
                }
                try {
                    long to = 20l
                    if (Environment.isDevelopmentMode()) {
                        to = Long.MAX_VALUE
                        to = 10l // DEBUG DEBUG DEBUG the DEBUG
                    }
                    resultSet = p.get(to, TimeUnit.SECONDS)
                    // End of request
                    log.debug("Finished product request, rendering response...")
                } catch (TimeoutException e) {
                    String ferretScript = pulse.getFerretScript()
                    if (ferretScript) {
                        def pinfo = productService.getProcessInfo(ferretScript)
                        productService.writePulse(hash, outputPath, "PyFerret process is running", ferretScript, null, pinfo, PulseType.STARTED)
                    }
                    resultSet = productService.pulseResult(lasRequest, hash, ferret.getTempDir(), base, outputPath, product);
                }
            }
        }
        if (resultSet) {
            withFormat {
                json {
                    respond resultSet // uses the custom templates in views/resultset
                }
            }
        }
    }

    def erddapDataRequest() {

        def requestJSON = request.JSON
        // TODO This is the cache key. Must make caching aware of data requests.
        def hash = IngestService.getDigest(requestJSON.toString());
        def reason = ""
        def lasRequest = new LASRequest(requestJSON);
        List<RequestProperty> requestProperties = lasRequest.getRequestProperties();
        if ( requestProperties ) {
            requestProperties.each {
                if ( it.type == "dashboard" && it.name == "request_type" ) {
                    reason = it.value
                }
            }
        }
        Dataset dataset = Dataset.findByHash(lasRequest.getDatasetHashes().get(0))
        def url = dataset.getUrl()+".json?";
        String vars = ""
        for (int i = 0; i < lasRequest.getVariableHashes().size(); i++) {
            def vhash = lasRequest.getVariableHashes().get(i);
            Variable variable = dataset.variables.find {Variable v -> v.hash == vhash}
            vars = vars + variable.getName();
            if ( i < lasRequest.getVariableHashes().size() - 1 ) {
                vars = vars + ","
            }
        }

        String constraint = ""
        List<AxesSet> axesSets = lasRequest.getAxesSets()
        if ( axesSets && axesSets.size() > 0 ) {
            String tlo = axesSets.get(0).getTlo();
            String thi = axesSets.get(0).getThi();

            if ( tlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time>=" + tlo;
            }
            if ( thi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time<=" + thi
            }


            // TODO use names from data set in constraints????

            String xlo = axesSets.get(0).getXlo();
            String xhi = axesSets.get(0).getXhi();

            // Direct ERDDAP request, must convert to -180 to 180
            double dxlo = LatLonUtil.anglePM180(Double.valueOf(xlo).doubleValue())
            double dxhi = LatLonUtil.anglePM180(Double.valueOf(xhi).doubleValue())

            if ( xlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "longitude>=" + String.valueOf(dxlo);
            }
            if ( xhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "longitude<=" + String.valueOf(dxhi)
            }

            String ylo = axesSets.get(0).getYlo();
            String yhi = axesSets.get(0).getYhi();

            if ( ylo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "latitude>=" + ylo;
            }
            if ( yhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "latitude<=" + yhi
            }
        }
        List<DataQualifier> qualifierList = lasRequest.getDataQualifiers();
        if ( qualifierList ) {
            for (int i = 0; i < qualifierList.size(); i++) {
                DataQualifier dq = qualifierList.get(i);
                if (dq.isDistinct()) {
                    if ( constraint.isEmpty() ) {
                        constraint = "distinct()"
                    } else {
                        constraint = constraint + "&distinct()"
                    }
                } else if (!dq.getType().isEmpty()) {
                    if ( constraint.isEmpty() ) {
                        constraint = dq.getType() + "("
                    } else {
                        constraint = constraint + "&" + dq.getType() + "("
                    }
                    List<String> vs = dq.getVariables()
                    for (int j = 0; j < vs.size(); j++) {
                        constraint = constraint + vs.get(j)
                        if (j < vs.size() - 1)
                            constraint = constraint + ","
                    }
                    constraint = constraint = +")"
                }

            }
        }

        def afterq = vars;

        url = url + URLEncoder.encode(vars + "&" + constraint, StandardCharsets.UTF_8.name())
        try {
            log.info(reason)
            log.info(url)
            String data = lasProxy.executeGetMethodAndReturnResult(url);
            render data;
        } catch (Exception e) {
            throw e;
        }

    }
    def datatable () {

        dateTimeService.init(null);
        // N.B. Right now this is only used to make time series so it makes the assumption that the JSON looks like this:

        /*
        {
  "table": {
    "columnNames": ["time", "trajectory", "BARO_PRES_MEAN"],
    "columnTypes": ["String", "String", "double"],
    "columnUnits": ["UTC", null, "hPa"],
    "rows": [
      ["2018-07-27T00:00:00Z", "1024.0", 1017.58],
      ["2018-07-27T00:01:00Z", "1024.0", 1017.58],
      ["2018-07-27T00:02:00Z", "1024.0", 1017.59],
      ["2018-07-27T00:03:00Z", "1024.0", 1017.59],
      ["2018-07-27T00:04:00Z", "1024.0", 1017.59],
      ["2018-07-27T00:05:00Z", "1024.0", 1017.6],
      ["2018-07-27T00:06:00Z", "1024.0", 1017.61],
      ["2018-07-27T00:07:00Z", "1024.0", 1017.63],

       There may be more data columns and there will be multiple values for the ID, which will be in column 2

        Make something that looks like this:



        {
          "cols": [
                   {"id":"","label":"Topping","pattern":"","type":"string"},
                   {"id":"","label":"Slices","pattern":"","type":"number"}
                  ],
          "rows": [
                   {"c":[{"v":"Mushrooms","f":null},{"v":3,"f":null}]},
                   {"c":[{"v":"Onions","f":null},{"v":1,"f":null}]},
                   {"c":[{"v":"Olives","f":null},{"v":1,"f":null}]},
                   {"c":[{"v":"Zucchini","f":null},{"v":1,"f":null}]},
                   {"c":[{"v":"Pepperoni","f":null},{"v":2,"f":null}]}
                  ]
        }


       The data table code below is not working. When rendering rows with null values it mak


         */


        DateTimeFormatter iso = ISODateTimeFormat.dateTimeNoMillis();
        def requestJSON = request.JSON
        // This is the cache key. Must make caching aware of data requests.
        def hash = IngestService.getDigest(requestJSON.toString());

        // A request is either an LAS request for which the ERDDAP URL must be formed, or
        // it is a request that contains the ERDDAP URL as a query parameter called "url"
        def url = null
        def reason = ""
        String constraint = ""
        if (requestJSON) {
            def lasRequest = new LASRequest(requestJSON);

            List<RequestProperty> requestProperties = lasRequest.getRequestProperties();
            if ( requestProperties ) {
                requestProperties.each {
                    if ( it.type == "dashboard" && it.name == "request_type" ) {
                        reason = it.value
                    }
                }
            }

            boolean hasLon360 = false;
            Dataset dataset = Dataset.findByHash(lasRequest.getDatasetHashes().get(0))
            List<Variable> lookForLon = dataset.getVariables();
            for (int i = 0; i < lookForLon.size(); i++) {
                Variable lonV = lookForLon.get(i)
                if ( lonV.getName().equals("lon360")) hasLon360 = true;
            }
            def idVar = null
            dataset.getVariables().each{Variable variable ->
                if ( variable.isDsgId() ) {
                    idVar = variable
                }
            }
            def varNames = ""
            lasRequest.getVariableHashes().each { String vhash ->
                Variable variable = dataset.variables.find { Variable v -> v.hash == vhash }
                if ( !varNames.isEmpty() ) varNames = varNames + ",";
                varNames = varNames + variable.name
                if ( variable.name != "time" && variable.name != "latitude" && variable.name != "longitude" && variable.name != idVar.name ) {
                    if ( !constraint.isEmpty() ) constraint = constraint + "&"
                    constraint = constraint + variable.name + "!=NaN"
                }
            }



            def latname = dataset.getDatasetPropertyValue("tabledap_access","latitude");
            def lonname = dataset.getDatasetPropertyValue("tabledap_access","longitude");
            def zname = dataset.getDatasetPropertyValue("tabledap_access","altitude")


            String tlo = lasRequest.getAxesSets().get(0).getTlo();
            String thi = lasRequest.getAxesSets().get(0).getThi();

            if ( tlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time>=" + tlo;
            }
            if ( thi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + "time<=" + thi
            }

            String xlo = lasRequest.getAxesSets().get(0).getXlo()
            String xhi = lasRequest.getAxesSets().get(0).getXhi()
            String lon_domain = dataset.getDatasetPropertyValue("tabledap_access", "lon_domain")
            if (xlo.length() > 0 && xhi.length() > 0) {

                double xhiDbl = Double.valueOf(xhi).doubleValue();
                double xloDbl = Double.valueOf(xlo).doubleValue();



                List<String> lon_constraint = LatLonUtil.getLonConstraint(xloDbl, xhiDbl, hasLon360, lon_domain, lonname)
                if ( lon_constraint.size() == 1 ) {
                    String lon_con = lon_constraint.get(0)
                    constraint = constraint + lon_con
                } // Else 0 or 2 can't constraint send extra data



                // Any other circumstance, don't bother to constrain lon and deal with the extra on the client (or not).
            } else {
                //  If they are not both defined, add the one that is...  There will be no difficulties with dateline crossings...
                if (xlo.length() > 0) {
                    if (!constraint.isEmpty()) constraint = constraint + "&"
                    constraint = constraint + lonname + ">=" + xlo;
                }
                if (xhi.length() > 0) {
                    if (!constraint.isEmpty()) constraint = constraint + "&"
                    constraint = constraint + lonname + "<=" + xhi
                }
            }

            String ylo = lasRequest.getAxesSets().get(0).getYlo()
            String yhi = lasRequest.getAxesSets().get(0).getYhi()

            if ( ylo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + latname + ">=" + ylo;
            }
            if ( yhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + latname + "<=" + yhi
            }

            String zlo = lasRequest.getAxesSets().get(0).getZlo()
            String zhi = lasRequest.getAxesSets().get(0).getZhi()

            if ( zlo ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + zname + ">=" + zlo;
            }
            if ( zhi ) {
                if ( !constraint.isEmpty() ) constraint = constraint + "&"
                constraint = constraint + zname + "<=" + zhi
            }

            if ( !constraint.isEmpty() ) constraint = "&" + constraint



            List constraintElements = lasRequest.getDataConstraints()

            // For now we will not use the decimated data set when there is any constraint applied to the request.
            // In the future we may need to distinguish between a sub-set variable constraint and a variable constraint.
            // The two types below should be enough to tell the difference.

            if ( constraintElements && constraintElements.size() > 0 ) {

                Iterator cIt = constraintElements.iterator();
                while (cIt.hasNext()) {
                    def dc = (DataConstraint) cIt.next();
                    String lhsString = dc.getLhs()
                    String opString = dc.getOp()
                    String rhsString = dc.getRhs()
                    String tType = dc.getType()
                    if (tType.equals("variable")) {
                        constraint = constraint + "&" + dc.getAsString();  //op is now <, <=, ...
                        // Gather lt and gt constraint so see if modulo variable treatment is required.
//
//                        TODO what to do about this
//
//                        if (modulo_vars.contains(lhsString) && (opString.equals("lt") || opString.equals("le"))) {
//                            constrained_modulo_vars_lt.put(lhsString, constraint);
//                        }
//                        if (modulo_vars.contains(lhsString) && (opString.equals("gt") || opString.equals("ge"))) {
//                            constrained_modulo_vars_gt.put(lhsString, constraint);
//                        }

                    } else if (tType.equals("text")) {
                        constraint = constraint + "&" + dc.getAsERDDAPString()  //op is now <, <=, ...
                    }
                }
            }

            // TODO Add data qualifiers (distinct and orderByMax in this case).
            List<DataQualifier> qualifierList = lasRequest.getDataQualifiers();
            if ( qualifierList ) {
                for (int i = 0; i < qualifierList.size(); i++) {
                    DataQualifier dq = qualifierList.get(i)
                    if (dq.isDistinct()) {
                        constraint = constraint + "&distinct()"
                    } else if (!dq.getType().isEmpty()) {
                        constraint = constraint + "&" + dq.getType() + "(\""
                        List<String> vs = dq.getVariables()
                        for (int j = 0; j < vs.size(); j++) {
                            constraint = constraint + vs.get(j)
                            if (j < vs.size() - 1)
                                constraint = constraint + ","
                        }
                        constraint = constraint + "\")"
                    }

                }
            }

            url = dataset.getUrl() + ".json?" + URLEncoder.encode(varNames + constraint, StandardCharsets.UTF_8.name());
        } else {
            url = params.url
        }
        if (url) {

            log.info(reason);
            log.info(url);



            String jsonText = lasProxy.executeGetMethodAndReturnResult(url)

            JsonElement json = jsonParser.parse(jsonText)
            def table = json.getAsJsonObject().get("table")

            def rows = table.get("rows")
            JsonArray names = table.get("columnNames").asJsonArray
            JsonArray types = table.get("columnTypes").asJsonArray

            /*

            The json looks like:

            Date, ID, parameter, parameter, parameter

            TIME1  ID001  A  B  C
            TIME2  ID001  D  E  F
            ...
            TIME1 ID002  G  H  I
            TIME3  ID002  J  K  L
            ...
            TIME4  ID003  M  N  O
            TIME1  ID003  P  Q  R
            TIME5  ID003  S  T  U

            And you want it to look like

            TIME1 A B C TIME1 G H I TIME4 M N O
            TIME2 D E F TIME3 J K L TIME1 P Q R
            null x  x x null  x x x TIME5 S T U

             */
            Map<Pair<String, String>, JsonArray> byPlatformAndTime = new HashMap<Pair<String, String>, JsonArray>()
            Set<String> platforms = new HashSet<>();
            Set<String> times = new HashSet<>();
            def row_size;
            rows.each { JsonArray row ->
                def row_time = row.get(0).asString
                def row_id = row.get(1).asString
                row_size = row.size()
                times.add(row_time)
                platforms.add(row_id)
                byPlatformAndTime.put(new ImmutablePair(row_id, row_time), row)
            }


            List<String> times_sorted = times.asList();
            Collections.sort(times_sorted)
            List<String> platforms_sorted = platforms.asList()
            Collections.sort(platforms_sorted)
            List<List<Object>> tableform = new ArrayList<List<Object>>();
            def index = 0;
            def column_names = []
            column_names.add("Time")
            for (int j = 0; j < times_sorted.size(); j++) {
                List<Object> table_row = new ArrayList<>();
                String t = times_sorted.get(j)
                table_row.add(t)
                for (int i = 0; i < platforms_sorted.size(); i++) {
                    Pair<String, String> p = new ImmutablePair<>(platforms_sorted.get(i), t)
                    JsonArray row = byPlatformAndTime.get(p)
                    if ( row ) {
                        for (int k = 2; k < row.size(); k++) {
                            if ( j == 0 ) {
                                String n = platforms_sorted.get(i) + " - " + names.get(k).asString
                                column_names.add(n)
                            }
                            JsonElement je = row.get(k);
                            if (je == null || je.isJsonNull()) {
                                table_row.add(null)
                            } else {
                                table_row.add(new Double(je.asDouble))
                            }
                        }
                    } else {
                        for (int k = 2; k < row_size; k++) {
                            if ( j == 0 ) {
                                String n = platforms_sorted.get(i) + " - " + names.get(k).asString
                                column_names.add(n)
                            }
                            table_row.add(null)
                        }
                    }
                }
                tableform.add(table_row)
            }


            JSONObject datatable = new JSONObject();
            JSONArray cols = new JSONArray();
            datatable.accumulate("cols", cols)
            // transform the table_form list of lists into a data table.
            for (int i = 0; i < column_names.size(); i++) {
                JSONObject col = new JSONObject();
                col.accumulate("id", "");
                col.accumulate("pattern", "")
                String name = column_names.get(i)
                col.accumulate("label", name)
                if (name.startsWith("Time")) {
                    col.accumulate("type", "datetime")
                } else {
                    col.accumulate("type", "number")
                }
                cols.add(col)
            }

            JSONArray jsonRows = new JSONArray();
            datatable.accumulate("rows", jsonRows)
            for (int i = 0; i < tableform.size(); i++) {
                def table_row = tableform.get(i)
                JSONObject tableRow = new JSONObject()
                jsonRows.add(tableRow)
                for (int j = 0; j < table_row.size(); j++) {
                    JSONObject rowValues = new JSONObject()
                    tableRow.accumulate("c", rowValues)
                    Object jItem = table_row.get(j)
                    if (jItem != null && jItem instanceof String) {
                        String dtstring = (String) jItem;
                        DateTime dt = dateTimeService.dateTimeFromIso(dtstring);
                        int year = dt.getYear()
                        int month = dt.getMonthOfYear() - 1
                        int day = dt.getDayOfMonth()
                        rowValues.accumulate("v", "Date("+ year + "," +  month + "," + day + "," + dt.getHourOfDay() + "," + dt.getMinuteOfHour() + "," + dt.getSecondOfMinute() + "," + dt.getMillisOfSecond() +")")
                        rowValues.accumulate("f", null)
                    } else if (jItem != null && jItem instanceof Double) {
                        double jValue = ((Double) jItem).doubleValue()
                        rowValues.accumulate("v", jValue)
                        rowValues.accumulate("f", null)
                    } else {
                        rowValues.accumulate("v", null)
                    }
                }
            }

            render datatable
        } else {
            throw new Exception("url parameter not provided");
        }


    }

}
