package pmel.sdig.las

import grails.gorm.transactions.Transactional
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import pmel.sdig.las.latlon.LatLonUtil
import pmel.sdig.las.tabledap.DataRow
import pmel.sdig.las.tabledap.DataRowComparator
import pmel.sdig.las.type.GeometryType
import ucar.ma2.Array
import ucar.ma2.ArrayBoolean
import ucar.ma2.ArrayByte
import ucar.ma2.ArrayChar
import ucar.ma2.ArrayDouble
import ucar.ma2.ArrayFloat
import ucar.ma2.ArrayInt
import ucar.ma2.ArrayLong
import ucar.ma2.ArrayShort
import ucar.ma2.ArrayString
import ucar.ma2.DataType
import ucar.ma2.InvalidRangeException
import ucar.nc2.Attribute
import ucar.nc2.Dimension
import ucar.nc2.NetcdfFile
import ucar.nc2.NetcdfFileWriter
import ucar.nc2.dataset.NetcdfDataset
import ucar.nc2.time.CalendarDate
import ucar.nc2.time.CalendarDateUnit
import ucar.unidata.geoloc.LatLonPoint
import ucar.unidata.geoloc.LatLonPointImpl

import java.nio.charset.StandardCharsets

@Transactional
class ErddapService {

    def lasProxy = new LASProxy()
    DateTimeService dateTimeService
    ProductService productService

    def makeNetcdfFile(LASRequest lasRequest, String hash, String outputPath, String productName, Operation operation, Dataset dataset) {

        boolean hasLon360 = false;
        List<Variable> lookFor360 = dataset.getVariables()
        for (int i = 0; i < lookFor360.size(); i++) {
            Variable lonV = lookFor360.get(i)
            if ( lonV.getName().equals("lon360") ) {
                hasLon360 = true;
            }
        }

        productService.writePulse(hash, outputPath, "Preparing for netCDF file download from ERDDAP.", null, null, null, PulseType.STARTED)

        def variableHashes = lasRequest.getVariableHashes();
        def operations

        List<RequestProperty> requestProperties = lasRequest.getRequestProperties();

        //If exception occurs in try/catch block, 'causeOfError' was the cause.
        String causeOfError = "Unexpected error: ";

        try {


            //get the name for the .nc results file
            causeOfError = "Unable to get backend request's resultsAsFile(netcdf): ";
            Result file = operation.getResultSet().getResults().find { it.name == "netcdf" }
            log.debug("Got netcdf filename: " + file.getFilename());


            // TODO maybe in future, this choice of product will be passed in
            def netcdfFilename = file.getFilename()
            def type = ".ncCF"


            def cancelFilename = netcdfFilename.replace("netcdf.nc", "cancel.txt")
            File cancelFile = new File(cancelFilename)
            /*
            Ok, this is a bit awkward. The data set is repeated for each variable.

            So we loop on the variable hashes and assume it's all from the same dataset

             */

            // This is where we are starting to make the variable query string for the ERDDAP request.


            // get url (almost always ending in "/tabledap/")
            causeOfError = "Could not get url from backend request: ";

            // TODO are these request properties. Do we need to "harmonize"
            def decid = dataset.getDatasetPropertyValue("tabledap_access", "decimated_id")
            def url = dataset.getUrl()
            def id = url.substring(url.lastIndexOf("/") + 1)

            List<RequestProperty> ferret_prop = lasRequest.getPropertyGroup("ferret")
            List<RequestProperty> download = lasRequest.getPropertyGroup("download");

            def downloadvars
            def full = false;
            if (download != null) {
                RequestProperty dall_prop = download.find { it.name == "all_data" };

                def dall = null
                if (dall_prop)
                    dall_prop.value

                if (dall != null) {
                    downloadvars = "standard";
                }
                def data_varsProperty = download.find { it.name == "data_variables" };
                def data_vars = null
                if (data_varsProperty) {
                    data_vars = data_varsProperty.value
                }

                if (data_vars && ("minimal".equals(data_vars) ||
                        "standard".equals(data_vars) ||
                        "everything".equals(data_vars))) {
                    downloadvars = data_vars;
                }
            }
            def full_dataProp = ferret_prop.find { it.name == "full_data" }
            String full_option = null
            if (full_dataProp) {
                full_option = full_dataProp.value;
            }

            if (full_option != null && full_option.equalsIgnoreCase("yes")) {
                full = true;
            }


            StringBuilder query = new StringBuilder();
            StringBuilder constraints = new StringBuilder();

            // If there is no need for the second query, just do the thing and carry on...

            // This was a method in the old service. We're just chunking along here in-line

            causeOfError = "Could not get trajectory id from backend request: ";

            String tid = dataset.getDatasetPropertyValue("tabledap_access", "trajectory_id")
            String pid = dataset.getDatasetPropertyValue("tabledap_access", "profile_id")
            String sid = dataset.getDatasetPropertyValue("tabledap_access", "timeseries_id");

            def cruiseid = null;

            if (tid) {
                cruiseid = tid
            } else if (pid) {
                cruiseid = pid
            } else if (sid) {
                cruiseid = sid
            }


            String lon_domain = dataset.getDatasetPropertyValue("tabledap_access", "lon_domain")

            causeOfError = "Could not get time column name from backend request: ";
            String time = dataset.getDatasetPropertyValue("tabledap_access", "time")
            required(time, causeOfError);

            def latname = dataset.getDatasetPropertyValue("tabledap_access", "latitude");
            def lonname = dataset.getDatasetPropertyValue("tabledap_access", "longitude");
            def zname = dataset.getDatasetPropertyValue("tabledap_access", "altitude")

            def orderby = dataset.getDatasetPropertyValue("tabledap_access", "orderby");
            def dummy = dataset.getDatasetPropertyValue("tabledap_access", "dummy");
            List<String> modulo_vars = new ArrayList();

            def modulo_vars_comma_list = dataset.getDatasetPropertyValue("tabledap_access", "modulo")
            if (modulo_vars_comma_list != null) {
                String[] mods = modulo_vars_comma_list.split(",");

                for (int i = 0; i < mods.length; i++) {
                    modulo_vars.add(mods[i].trim());
                }
            }

            boolean download_everytime = false;
            def dletProp = lasRequest.getRequestProperties().find { it.name == "download_everytime" }

            def dlet = null
            if (dletProp) {
                dlet = dletProp.value.trim()
            }
            if (dlet != null && !dlet.isEmpty() && dlet.toLowerCase().equals("true")) {
                download_everytime = true;
            }

            ArrayList<String> vars = []
            List<Variable> datasetVariables = dataset.getVariables()
            for (int i = 0; i < variableHashes.size(); i++) {
                Variable v = datasetVariables.find { it.hash == variableHashes.get(i) }
                vars.add(v.getName())
            }
            // If the operation is the prop-prop plot, we need all the variables.
            if (productName != null && productName.equals("Trajectgory_thumbnails")) {
                String all = dataset.getDatasetPropertyValue("tabledap_access", "thumbnails")
                if (all) query.append(all.trim());
            } else if (productName != null && productName.equals("Trajectgory_correlation") && !download_everytime) {
                String all = dataset.getDatasetPropertyValue("tabledap_access", "all_variables")
                query.append(all);
            } else if ("standard".equals(downloadvars)) {
                String all = dataset.getDatasetPropertyValue("tabledap_access", "downloadall_variables")
                // If the custom download all is not set, use the default.
                if (all == null || all.equals("")) {
                    all = dataset.getDatasetPropertyValue("tabledap_access", "all_variables")
                }

                query.append(all);

                for (Iterator varIt = vars.iterator(); varIt.hasNext();) {
                    String v = (String) varIt.next();
                    if (!all.contains(v) && !v.equals(latname) && !v.equals(lonname) && !v.equals(zname) && !v.equals(time)) {
                        query.append("," + v);
                    }
                }
            } else if ("everything".equals(downloadvars)) {
                String all = dataset.getDatasetePropertyValue("tabledap_access", "all_variables")

                query.append(all);

                for (Iterator varIt = vars.iterator(); varIt.hasNext();) {
                    String v = (String) varIt.next();
                    if (!all.contains(v) && !v.equals(latname) && !v.equals(lonname) && !v.equals(zname) && !v.equals(time)) {
                        query.append("," + v);
                    }
                }
            } else {

                // Only add the extras if the variable list does not come from configuration.
                // Some things might need something besides x,y,z and t in the file so...
                String extra_metadata = dataset.getDatasetPropertyValue("tabledap_access", "extra_metadata")
                if (extra_metadata != null && !extra_metadata.equals("")) {
                    if (extra_metadata.contains(",")) {
                        String[] extras = extra_metadata.split(",");
                        for (int i = 0; i < extras.length; i++) {
                            String e = extras[i].trim();
                            if (query.indexOf(e) < 0) {
                                if (query.length() > 0 && !query.toString().endsWith(",")) {
                                    query.append(",");
                                }
                                vars.remove(e);
                                query.append(e);
                            }
                        }

                    } else {
                        if (query.indexOf(extra_metadata) < 0) {
                            if (query.length() > 0 && !query.toString().endsWith(",")) {
                                query.append(",");
                            }
                            vars.remove(extra_metadata);
                            query.append(extra_metadata);
                        }
                    }
                }

                // If lat, lon and z are included as data variables, knock them out of this list.
                vars.remove(latname);
                vars.remove(lonname);
                vars.remove(zname);
                vars.remove(time);

                if (productName != null && (productName.equals("Profile_2D_poly") ||
                                            productName.equals("Time_Series_Location_Plot") ||
                                            productName.equals("Trajectory_2D_poly") ||
                                            productName.equals("Trajectory_Profile_Plot_2D") ||
                                            productName.equals("Trajectory_profile_interactive_plot"))) {
                    String mapvars = dataset.getDatasetPropertyValue("tabledap_access", "map_variables")
                    if (mapvars != null && !mapvars.equals("")) {
                        if (mapvars.contains(",")) {
                            String[] extras = mapvars.split(",");
                            for (int i = 0; i < extras.length; i++) {
                                String e = extras[i].trim();
                                if (query.indexOf(e) < 0) {
                                    if (query.length() > 0 && !query.toString().endsWith(",")) {
                                        query.append(",");
                                    }
                                    vars.remove(e);
                                    query.append(e);
                                }
                            }

                        } else {
                            if (query.indexOf(mapvars) < 0) {
                                if (query.length() > 0 && !query.toString().endsWith(",")) {
                                    query.append(",");
                                }
                                vars.remove(mapvars);
                                query.append(mapvars);
                            }
                        }
                    }
                }
                String variables = "";
                for (Iterator varIt = vars.iterator(); varIt.hasNext();) {
                    String variable = (String) varIt.next();
                    // Apparently ERDDAP gets mad if you list the trajectory_id in the request...
                    if (cruiseid == null || (cruiseid != null && !variable.equals(cruiseid))) {
                        variables = variables + variable;
                        if (varIt.hasNext()) {
                            variables = variables + ",";
                        }
                    }
                }

                if (variables.endsWith(",")) {
                    variables = variables.substring(0, variables.length() - 1);
                }

                if (!variables.equals("")) {
                    if (query.length() > 0 && !query.toString().endsWith(",")) {
                        query.append(",");
                    }
                    variables = variables.replaceAll(" ", "")
                    query.append(variables);
                } else {
                    if (query.length() > 0 && !query.toString().endsWith(",")) {
                        query.append(",");
                    }
                    query.append(dummy);
                }
            }

            Map<String, DataConstraint> constrained_modulo_vars_lt = new HashMap<String, DataConstraint>();
            Map<String, DataConstraint> constrained_modulo_vars_gt = new HashMap<String, DataConstraint>();


            //then variable constraints
            List constraintElements = lasRequest.getDataConstraints()

            // For now we will not use the decimated data set when there is any constraint applied to the request.
            // In the future we may need to distinguish between a sub-set variable constraint and a variable constraint.
            // The two types below should be enough to tell the difference.

            if (constraintElements && constraintElements.size() > 0) {

                Iterator cIt = constraintElements.iterator();
                while (cIt.hasNext()) {
                    def constraint = (DataConstraint) cIt.next();
                    String lhsString = constraint.getLhs()
                    String opString = constraint.getOp()
                    String rhsString = constraint.getRhs()
                    String tType = constraint.getType()
                    if (tType.equals("variable")) {
                        constraints.append("&" + constraint.getAsString());  //op is now <, <=, ...
                        // Gather lt and gt constraint so see if modulo variable treatment is required.
                        if (modulo_vars.contains(lhsString) && (opString.equals("lt") || opString.equals("le"))) {
                            constrained_modulo_vars_lt.put(lhsString, constraint);
                        }
                        if (modulo_vars.contains(lhsString) && (opString.equals("gt") || opString.equals("ge"))) {
                            constrained_modulo_vars_gt.put(lhsString, constraint);
                        }
                    } else if (tType.equals("text")) {
                        constraints.append("&" + constraint.getAsERDDAPString());  //op is now <, <=, ...
                    }
                }
            }
            List<String> modulo_required = new ArrayList<String>();
            for (Iterator cvarIt = constrained_modulo_vars_lt.keySet().iterator(); cvarIt.hasNext();) {
                String cvar = (String) cvarIt.next();
                if (constrained_modulo_vars_gt.keySet().contains(cvar)) {
                    // Potential for min to be > that max requiring a modulo treatment of the query.
                    String max = constrained_modulo_vars_lt.get(cvar).getRhs();
                    String min = constrained_modulo_vars_gt.get(cvar).getRhs();
                    try {
                        double mind = Double.valueOf(min);
                        double maxd = Double.valueOf(max);
                        if (mind > maxd) {
                            modulo_required.add(cvar);
                        }
                    } catch (Exception e) {
                        //
                    }
                }
            }


            //get region constraints
            causeOfError = "Unable to get required database properties.";
            AxesSet axesSet = lasRequest.getAxesSets().get(0)
            String xlo = axesSet.getXlo();  //don't constrain to +-180?  getDatabaseXlo?
            String xhi = axesSet.getXhi();
            String ylo = axesSet.getYlo();
            String yhi = axesSet.getYhi();
            String zlo = axesSet.getZlo();
            String zhi = axesSet.getZhi();

            String s = axesSet.getTlo();  //in Ferret format (except for the dashbaord which is from the data set)
            String tlo = s;
            if (s.length() > 0 && !s.contains("Z")) {
                s = JDOMUtils.decode(s, "UTF-8");
                s = s.replace("\"", "");
                tlo = dateTimeService.isoFromFerret(s, "proleptic_gregorian")
            }

            s = axesSet.getThi();  //in Ferret format
            String thi = s;
            if (s.length() > 0 && !s.contains("Z")) {
                s = JDOMUtils.decode(s, "UTF-8");
                s = s.replace("\"", "");
                thi = dateTimeService.isoFromFerret(s, "proleptic_gregorian")
            }

            //add region constraints other than lon
            if (ylo && ylo.length() > 0) constraints.append("&" + latname + ">=" + ylo);
            if (yhi && yhi.length() > 0) constraints.append("&" + latname + "<=" + yhi);
            if (zlo && zlo.length() > 0) constraints.append("&" + zname + ">=" + zlo);
            if (zhi && zhi.length() > 0) constraints.append("&" + zname + "<=" + zhi);
            if (tlo && tlo.length() > 0) constraints.append("&" + time + ">=" + tlo);
            if (thi && thi.length() > 0) constraints.append("&" + time + "<=" + thi);


            // TODO get various qualifiers from the request
            List<DataQualifier> qualifierList = lasRequest.getDataQualifiers();
            if (qualifierList) {
                for (int i = 0; i < qualifierList.size(); i++) {
                    DataQualifier dq = qualifierList.get(i);
                    if (dq.isDistinct()) {
                        constraints.append("&distinct()")
                    } else if (!dq.getType().isEmpty()) {
                        constraints.append("&" + dq.getType() + "(\"")
                        List<String> vs = dq.getVariables()
                        for (int j = 0; j < vs.size(); j++) {
                            constraints.append(vs.get(j))
                            if (j < vs.size() - 1)
                                constraints.append(",")
                        }
                        constraints.append("\")")
                    }

                }
            }

            // Required or you get a file that is unordered and thus not compliant
            if (!dataset.getGeometry().equals(GeometryType.POINT)) {
                if (orderby != null) {
                    if (!orderby.equals("") && !orderby.equals("none")) {
                        constraints.append("&orderBy(\"" + orderby + "\")");
                        query.insert(0, orderby+",");
                    } else {
                        if (!orderby.equals("none") && cruiseid != null) {
                            constraints.append("&orderBy(\"" + cruiseid + "," + time + "\")");
                            query.insert(0, cruiseid+",");
                        } else {
                            constraints.append("&orderBy(\"" + time + "\")");
                            query.insert(0, time+",");
                        }
                    }
                } else {
                    if (cruiseid != null) {
                        constraints.append("&orderBy(\"" + cruiseid + "," + time + "\")");
                        query.insert(0, curiseid+",");
                        query.insert(0, time+",");
                    } else {
                        constraints.append("&orderBy(\"" + time + "\")");
                        query.insert(0, time+",");
                    }
                }
            }

            //get the data
            causeOfError = "Could not convert the data source to a netCDF file: ";


            //            Table data = new Table();

            boolean smallarea = false;


            // The client applied a constraint in X
            if (xlo.length() > 0 && xhi.length() > 0) {

                double xhiDbl = Double.valueOf(xhi).doubleValue();
                double xloDbl = Double.valueOf(xlo).doubleValue();

                double xspan = Math.abs(xhiDbl - xloDbl);
                double yloDbl = -90.0d;
                double yhiDbl = 90.0d;
                if (ylo.length() > 0) {
                    yloDbl = Double.valueOf(ylo);
                }
                if (yhi.length() > 0) {
                    yhiDbl = Double.valueOf(yhi);
                }
                double yspan = Math.abs(yhiDbl - yloDbl);

                double fraction = ((xspan + yspan) / (360.0d + 180.0d));

                if (fraction < 0.1d) {
                    smallarea = true;
                }

                // Check the span before normalizing and if it's big, just forget about the lon constraint all together.
                if (Math.abs(xhiDbl - xloDbl) < 355.0d) {
                    if (modulo_required.size() > 0) {
                        causeOfError = "Cannot handle two modulo variables in the same request (longitude and " + modulo_required.get(0) + ")";
                    }


                    // Here for now if there are two constraints we will ignore them. In reality we should teach Ferret
                    // to stich together the two halves either in the grahics window or as a file.

                    List<String> lonConstraint = LatLonUtil.getLonConstraint(xloDbl, xhiDbl, hasLon360, lon_domain, lonname)

                    if ( lonConstraint.size() == 1 )
                        constraints.append(lonConstraint.get(0))

                } // Span the whole globe so leave off the lon query all together.
            // Any other circumstance, don't bother to constrain lon and deal with the extra on the client (or not).
            } else {
                //  If they are not both defined, add the one that is...  There will be no difficulties with dateline crossings...
                if (xlo.length() > 0) constraints.append("&" + lonname + ">=" + xlo);
                if (xhi.length() > 0) constraints.append("&" + lonname + "<=" + xhi);
            }

            // This changes the data set to the decimated data set if it exists.
            // We have decided to try all ribbon plots with the decimated data set...
            // so we will remove the !hasConstraints
            if (!smallarea && productName.equals("Trajectory_2D_poly") && !decid.equals("") && !full) {
                url = url.replace(id, decid)
            }



            DateTime dt = new DateTime();

            File temp_file = new File(netcdfFilename + ".temp");


            try {
                if (cancelFile.exists()) {
                    cancelFile.delete()
                    throw new Exception("Request canceled.")
                }
                productService.writePulse(hash, outputPath, "Downloading netCDF file.", null, temp_file.getAbsolutePath(), null, PulseType.STARTED)
                String cons = URLEncoder.encode(constraints.toString(), "UTF-8").
                        replaceAll("\\+", "%20").
                        replaceAll("%3F", "?").
                        replaceAll("%26", "&");
                String dsUrl = url + type + "?" + query + cons;  //don't include ".dods"; readOpendapSequence does that

                dt = new DateTime();
                log.debug("TableDapTool query=" + dsUrl);
                log.info("{TableDapTool starting file pull for the only file at " + dt.toString(ISODateTimeFormat.dateHourMinuteSecondMillis()));
                lasProxy.executeERDDAPMethodAndSaveResult(dsUrl, temp_file, null);
                dt = new DateTime();

//                                    if (lasBackendRequest.isCanceled()) {
//                                        lasBackendResponse.setError("ERDDAP data request canceled.");
//                                        return lasBackendResponse;
//                                    }
                log.info("TableDapTool finished file pull for the only file at " + dt.toString(ISODateTimeFormat.dateHourMinuteSecondMillis()));
                //TODO was the request canceled?
//                                    if (lasBackendRequest.isCanceled()) {
//                                        lasBackendResponse.setError("ERDDAP data request canceled.");
//                                        return lasBackendResponse;
//                                    }
                if (cancelFile.exists()) {
                    cancelFile.delete()
                    throw new Exception("Request canceled.")
                }
                if ( !temp_file.exists() ) {
                    log.error("ERDDAP download failed.")
                    throw new Exception("Unable to download ERDDAP data set.")
                }
                temp_file.renameTo(new File(netcdfFilename));
                dt = new DateTime();
                log.info("Tabledap tool renamed the netcdf file to " + netcdfFilename + " at " + dt.toString(ISODateTimeFormat.dateHourMinuteSecondMillis()));
            } catch (Exception e) {
                String message = e.getMessage();
                if (e.getMessage().contains("com.cohort")) {
                    message = message.substring(message.indexOf("com.cohort.util.SimpleException: "), message.length());
                    message = message.substring(0, message.indexOf(")"));
                }
                throw new Exception(message)
            }
            productService.writePulse(hash, outputPath, "Finished making netCDF file.", null, null, null, PulseType.STARTED)
            netcdfFilename

        } catch (Exception e) {
            throw e;
        }
    }


// Helper methods for dealing with netCDF files when doing a query that crosses the dateline.
    def merge(String netcdfFilename, File temp_file1, File temp_file2, String cruiseid, String lonname, String latname, String zname, String time) throws IOException, InvalidRangeException  {

        def all = []
        def datarows = []

        NetcdfFile trajset1 = (NetcdfFile) NetcdfDataset.open(temp_file1.getAbsolutePath());
        NetcdfFile trajset2 = (NetcdfFile) NetcdfDataset.open(temp_file2.getAbsolutePath());
        NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, netcdfFilename);
        List<ucar.nc2.Variable> vars = trajset1.getVariables();
        Dimension obsdim1 = null;
        String obsdimname = null;
        ucar.nc2.Variable obscount1 = null;
        ucar.nc2.Variable obscount2 = null;
        String trajidname = null;
        Array trajids1 = null;
        int trajidwidth = 64;
        Dimension trajdim_org = null;
        for (Iterator iterator = vars.iterator(); iterator.hasNext();) {
            ucar.nc2.Variable variable = (ucar.nc2.Variable) iterator.next();
            Attribute td = variable.findAttribute("sample_dimension");
            if ( td != null ) {
                obsdimname = td.getStringValue();
                obscount1 = variable;
            }
            Attribute tid = variable.findAttribute("cf_role");
            if ( tid != null && (tid.getStringValue().equals("trajectory_id") || tid.getStringValue().equals("profile_id") || tid.getStringValue().equals("timeseries_id")) ) {
                trajdim_org = variable.getDimension(0);
                trajidname = variable.getShortName();
                trajids1 = variable.read();
            }
        }
        Map<String, Array> subsetvars1 = new HashMap<String, Array>();
        Map<String, Array> subsetvars2 = new HashMap<String, Array>();
        for (Iterator iterator = vars.iterator(); iterator.hasNext();) {
            ucar.nc2.Variable variable = (ucar.nc2.Variable) iterator.next();
            if ( trajdim_org != null && variable.getDimension(0).getShortName().equals(trajdim_org.getShortName())) {
                Array a1 = variable.read();
                subsetvars1.put(variable.getShortName(), a1);
                ucar.nc2.Variable v2 = trajset2.findVariable(variable.getShortName());
                Array a2 = v2.read();
                subsetvars2.put(v2.getShortName(), a2);
            } else if ( variable.getDimension(0).getShortName().equals(obsdimname)) {
                all.add(variable.getShortName());
            }
        }

        obsdim1 = trajset1.findDimension(obsdimname);
        Dimension obsdim2 = trajset2.findDimension(obsdimname);
        if ( obscount1 != null ) {
            obscount2 = trajset2.findVariable(obscount1.getShortName());
        }


        ucar.nc2.Variable tv = trajset2.findVariable(trajidname);
        Array trajids2 = tv.read();

        Set<String> trajIDs = new HashSet<String>();
        ArrayChar.D2 tid1 = null;
        ArrayChar.D2 tid2 = null;
        // Merge the values to find the number of unique IDs
        if ( trajids1 != null && trajids1 instanceof ArrayChar.D2 && trajids2 instanceof ArrayChar.D2 ) {
            // This is what I expect for now, but it could be something different.
            tid1 = (ArrayChar.D2)trajids1;
            tid2 = (ArrayChar.D2)trajids2;
            for(int index = 0; index < tid1.getShape()[0]; index++) {
                String id = tid1.getString(index);
                trajIDs.add(id);
            }
            for(int index = 0; index < tid2.getShape()[0]; index++) {
                String id = tid2.getString(index);
                trajIDs.add(id);
            }
        }

        if ( obscount1 != null && obscount2 != null && tid1 != null && tid2 != null ) {
            Array oc1 = obscount1.read();
            Array oc2 = obscount2.read();
            for (int index = 0; index < obscount1.getShape(0); index++) {
                String id = tid1.getString(index);
                int count = oc1.getInt(index);
                Map<String, Object> subset = new HashMap<String, Object>();
                for (Iterator subsetIt = subsetvars1.keySet().iterator(); subsetIt.hasNext();) {
                    String key = (String) subsetIt.next();
                    Array a = (Array) subsetvars1.get(key);
                    subset.put(key, getObject(a, index));
                }
                for (int j = 0; j < count; j++) {
                    DataRow datarow = new DataRow();
                    datarow.setId(id);
                    datarow.setSubsets(subset);
                    datarows.add(datarow);
                }
            }
            for (int index = 0; index < obscount2.getShape(0); index++) {
                String id = tid2.getString(index);
                int count = oc2.getInt(index);
                Map<String, Object> subset = new HashMap<String, Object>();
                for (Iterator subsetIt = subsetvars2.keySet().iterator(); subsetIt.hasNext();) {
                    String key = (String) subsetIt.next();
                    Array a = (Array) subsetvars2.get(key);
                    subset.put(key, getObject(a, index));
                }
                for (int j = 0; j < count; j++) {
                    DataRow datarow = new DataRow();
                    datarow.setId(id);
                    datarow.setSubsets(subset);
                    datarows.add(datarow);
                }
            }
        }
        if ( obsdim1 != null && obsdim2 != null ) {
            int both = obsdim1.getLength()+obsdim2.getLength();
            Dimension trajdim = ncfile.addDimension(null, "trajectory", trajIDs.size());
            Dimension dim = ncfile.addDimension(null, "obs", both);
            List<ucar.nc2.Variable> allvars = trajset1.getVariables();
            for (Iterator varsIt = allvars.iterator(); varsIt.hasNext(); ) {
                ucar.nc2.Variable var1 = (ucar.nc2.Variable) varsIt.next();
                String varname = var1.getShortName();
                ucar.nc2.Variable var2 = trajset2.findVariable(varname);
                List<Dimension> dimlist = new ArrayList<Dimension>();


                if ( all.contains(varname) ) {

                    // It's a data variable, so it has obs dimension
                    dimlist.add(dim);
                    if ( var1.getDataType() == DataType.CHAR ) {
                        Dimension chardim1 = var1.getDimension(1);
                        Dimension chardim2 = var2.getDimension(1);
                        Dimension nchardim = ncfile.addDimension(null, chardim1.getShortName(), Math.max(chardim1.getLength(), chardim2.getLength()));
                        dimlist.add(nchardim);
                    }
                    ucar.nc2.Variable nv = ncfile.addVariable(null, var1.getShortName(), var1.getDataType(), dimlist);
                    List<Attribute> attributes = var1.getAttributes();
                    for (Iterator attIt = attributes.iterator(); attIt.hasNext();) {
                        Attribute attribute = (Attribute) attIt.next();
                        ncfile.addVariableAttribute(nv, attribute);
                    }

                    Array d1 = var1.read();
                    Array d2 = var2.read();
                    String name = var1.getShortName();

                    fill(d1, name, 0, datarows, cruiseid, time);
                    fill(d2, name, d1.getShape()[0], datarows, cruiseid, time);


                } else {
                    // If it has the trajectory dimension then it's a sub-set variable.
                    dimlist.add(trajdim);
                    if ( var1.getDataType() == DataType.CHAR ) {
                        Dimension chardim1 = var1.getDimension(1);
                        Dimension chardim2 = var2.getDimension(1);
                        if ( var1.getShortName().equals(trajidname)) {
                            trajidwidth = Math.max(chardim1.getLength(), chardim2.getLength());
                        }
                        Dimension nchardim = ncfile.addDimension(null, chardim1.getShortName(), Math.max(chardim1.getLength(), chardim2.getLength()));
                        dimlist.add(nchardim);
                    }
                    ucar.nc2.Variable nv = ncfile.addVariable(null, var1.getShortName(), var1.getDataType(), dimlist);
                    List<Attribute> attributes = var1.getAttributes();
                    for (Iterator attIt = attributes.iterator(); attIt.hasNext();) {
                        Attribute attribute = (Attribute) attIt.next();
                        ncfile.addVariableAttribute(nv, attribute);
                    }
                }

            }
            List<Attribute> globals = trajset1.getGlobalAttributes();
            for (Iterator gatIt = globals.iterator(); gatIt.hasNext();) {
                Attribute gatt = (Attribute) gatIt.next();
                ncfile.addGroupAttribute(null, gatt);
            }
            ncfile.create();
        } else {
            System.out.println("obsdim not found");
        }

        Collections.sort(datarows, new DataRowComparator());
        List<String> sortedTraj = new ArrayList<String>();
        for (Iterator idIt = trajIDs.iterator(); idIt.hasNext();) {
            String id = (String) idIt.next();
            sortedTraj.add(id);
        }
        Collections.sort(sortedTraj);
        int total = 0;

        // Create the new data for the sample dimension
        int[] shape = new int[1]
        shape[0] = sortedTraj.size()
        def counts = Array.factory(DataType.INT, shape)
        int index = 0;
        for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
            String id = (String) idIt.next();
            int idcount = 0;
            for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                DataRow dr = (DataRow) drIt.next();
                if ( dr.getId().equals(id) ) {
                    idcount++;
                }

            }
            counts.setInt(index, idcount);
            index++;
            total = total + idcount;
        }
        // Write the lengths of the trajectories.
        ucar.nc2.Variable v = ncfile.findVariable(obscount1.getShortName());
        ncfile.write(v, counts);

        ucar.nc2.Variable ids = ncfile.findVariable(trajidname);
        ArrayChar.D2 idsData = new ArrayChar.D2(sortedTraj.size(), trajidwidth);
        int idindex = 0;
        for (Iterator sortedIt = sortedTraj.iterator(); sortedIt.hasNext();) {
            String id = (String) sortedIt.next();
            idsData.setString(idindex, id);
            idindex++;
        }
        ncfile.write(ids, idsData);


        DataRow sampleRow = datarows.get(0);
        Map<String, Object> sampleSubsets = sampleRow.getSubsets();
        Map<String, Object> sampleData = sampleRow.getData();

        for (Iterator subsIt = sampleSubsets.keySet().iterator(); subsIt.hasNext();) {
            String subsetvar = (String) subsIt.next();
            ucar.nc2.Variable var = ncfile.findVariable(subsetvar);
            // Write all the subset variables except the count which has already been done above.
            if ( !var.getShortName().equals(obscount1.getShortName()) ) {
                if ( var.getDataType() == DataType.BOOLEAN ) {
                    ArrayBoolean.D1 a = new ArrayBoolean.D1(var.getShape(0));
                    int trajindex = 0;
                    for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
                        String id = (String) idIt.next();
                        // This is a named looped and named break to stop looking one the first matching row is found.
                        // A hack, but...
                        D: for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                            DataRow dr = (DataRow) drIt.next();
                            if ( dr.getId().equals(id) ) {
                                Boolean b = (Boolean) dr.getSubsets().get(subsetvar);
                                a.set(trajindex, b);
                                break D;
                            }
                        }
                        trajindex++;
                    }
                    ncfile.write(var, a);
                } else if ( var.getDataType() == DataType.BYTE ) {
                    ArrayByte.D1 a = new ArrayByte.D1(var.getShape(0));
                    int trajindex = 0;
                    for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
                        String id = (String) idIt.next();
                        D: for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                            DataRow dr = (DataRow) drIt.next();
                            if ( dr.getId().equals(id) ) {
                                Byte b = (Byte) dr.getSubsets().get(subsetvar);
                                a.set(trajindex, b);
                                break D;
                            }
                        }
                        trajindex++;
                    }
                    ncfile.write(var, a);
                } else if ( var.getDataType() == DataType.CHAR ) {
                    int size = var.getShape(0);
                    int width = var.getShape(1);
                    ArrayChar.D2 a = new ArrayChar.D2(size, width);
                    int trajindex = 0;
                    for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
                        String id = (String) idIt.next();
                        D: for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                            DataRow dr = (DataRow) drIt.next();
                            if ( dr.getId().equals(id) ) {
                                String b = (String) dr.getSubsets().get(subsetvar);
                                a.setString(trajindex, b);
                                break D;
                            }
                        }
                        trajindex++;
                    }
                    ncfile.write(var, a);
                } else if ( var.getDataType() == DataType.DOUBLE ) {
                    ArrayDouble.D1 a = new ArrayDouble.D1(var.getShape(0));
                    int trajindex = 0;
                    for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
                        String id = (String) idIt.next();
                        D: for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                            DataRow dr = (DataRow) drIt.next();
                            if ( dr.getId().equals(id) ) {
                                Double b = (Double) dr.getSubsets().get(subsetvar);
                                a.set(trajindex, b);
                                break D;
                            }
                        }
                        trajindex++;
                    }
                    ncfile.write(var, a);
                } else if ( var.getDataType() == DataType.FLOAT ) {
                    ArrayFloat.D1 a = new ArrayFloat.D1(var.getShape(0));
                    int trajindex = 0;
                    for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
                        String id = (String) idIt.next();
                        D: for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                            DataRow dr = (DataRow) drIt.next();
                            if ( dr.getId().equals(id) ) {
                                Float b = (Float) dr.getSubsets().get(subsetvar);
                                a.set(trajindex, b);
                                break D;
                            }
                        }
                        trajindex++;
                    }
                    ncfile.write(var, a);
                } else if ( var.getDataType() == DataType.INT ) {
                    ArrayInt.D1 a = new ArrayInt.D1(var.getShape(0));
                    int trajindex = 0;
                    for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
                        String id = (String) idIt.next();
                        D: for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                            DataRow dr = (DataRow) drIt.next();
                            if ( dr.getId().equals(id) ) {
                                Integer b = (Integer) dr.getSubsets().get(subsetvar);
                                a.set(trajindex, b);
                                break D;
                            }
                        }
                        trajindex++;
                    }
                    ncfile.write(var, a);
                } else if ( var.getDataType() == DataType.LONG ) {
                    ArrayLong.D1 a = new ArrayLong.D1(var.getShape(0));
                    int trajindex = 0;
                    for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
                        String id = (String) idIt.next();
                        D: for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                            DataRow dr = (DataRow) drIt.next();
                            if ( dr.getId().equals(id) ) {
                                Long b = (Long) dr.getSubsets().get(subsetvar);
                                a.set(trajindex, b);
                                break D;
                            }
                        }
                        trajindex++;
                    }
                    ncfile.write(var, a);
                } else if ( var.getDataType() == DataType.SHORT ) {
                    ArrayShort.D1 a = new ArrayShort.D1(var.getShape(0));
                    int trajindex = 0;
                    for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
                        String id = (String) idIt.next();
                        D: for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                            DataRow dr = (DataRow) drIt.next();
                            if ( dr.getId().equals(id) ) {
                                Short b = (Short) dr.getSubsets().get(subsetvar);
                                a.set(trajindex, b);
                                break D;
                            }
                        }
                        trajindex++;
                    }
                    ncfile.write(var, a);
                } else if ( var.getDataType() == DataType.STRING ) {
                    ArrayString.D1 a = new ArrayString.D1(var.getShape(0));
                    int trajindex = 0;
                    for (Iterator idIt = sortedTraj.iterator(); idIt.hasNext();){
                        String id = (String) idIt.next();
                        D: for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                            DataRow dr = (DataRow) drIt.next();
                            if ( dr.getId().equals(id) ) {
                                String b = (String) dr.getSubsets().get(subsetvar);
                                a.set(trajindex, b);
                                break D;
                            }
                        }
                        trajindex++;
                    }
                    ncfile.write(var, a);

                }
            }
        }
        for (Iterator dataIt = sampleData.keySet().iterator(); dataIt.hasNext();) {
            String varname = (String) dataIt.next();
            ucar.nc2.Variable var = ncfile.findVariable(varname);
            if ( var.getDataType() == DataType.BOOLEAN ) {
                ArrayBoolean.D1 a = new ArrayBoolean.D1(var.getShape(0));
                int drindex = 0;
                for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                    DataRow dr = (DataRow) drIt.next();
                    Boolean b = (Boolean) dr.getData().get(varname);
                    a.set(drindex, b);
                    drindex++;
                }
                ncfile.write(var, a);
            } else if ( var.getDataType() == DataType.BYTE ) {
                ArrayByte.D1 a = new ArrayByte.D1(var.getShape(0));
                int drindex = 0;
                for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                    DataRow dr = (DataRow) drIt.next();
                    Byte b = (Byte) dr.getData().get(varname);
                    a.set(drindex, b);
                    drindex++;
                }
                ncfile.write(var, a);
            } else if ( var.getDataType() == DataType.CHAR ) {
                ArrayChar.D2 a = new ArrayChar.D2(var.getShape(0), var.getShape(1));
                int drindex = 0;
                for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                    DataRow dr = (DataRow) drIt.next();
                    String b = (String) dr.getData().get(varname);
                    a.setString(drindex, b);
                    drindex++;
                }
                ncfile.write(var, a);
            } else if ( var.getDataType() == DataType.DOUBLE ) {
                ArrayDouble.D1 a = new ArrayDouble.D1(var.getShape(0));
                int drindex = 0;

                // Don't use the missing or fill value for the actual range.
                Attribute missing = var.findAttribute("missing_value");
                Double mv = new Double(-1);
                if ( missing != null ) {
                    mv = (Double) missing.getNumericValue();
                }
                Double fill = new Double(-1);
                Attribute fillValue = var.findAttribute("_FillValue");
                if ( fillValue != null ) {
                    fill = (Double) fillValue.getNumericValue();
                }


                double min = Double.MAX_VALUE;
                double max = Double.MIN_VALUE;
                for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                    DataRow dr = (DataRow) drIt.next();
                    Double b = (Double) dr.getData().get(varname);
                    boolean isMissing = false;
                    boolean isFill = false;

                    if( missing != null && !mv.equals(Double.NaN) ) {
                        isMissing = !(Math.abs(mv - b) > 0.0001);
                    } else if ( b.equals(Double.NaN) ) {
                        isMissing = true;
                    }
                    if ( fillValue != null && !fill.equals(Double.NaN) ) {
                        isFill = !(Math.abs(fill - b) > 0.0001);
                    } else if ( b.equals(Double.NaN) ) {
                        isFill = true;
                    }
                    if ( b < min && !isMissing && !isFill ) {
                        min = b;
                    }
                    if ( b > max && !isMissing && !isFill ) {
                        max = b;
                    }
                    a.set(drindex, b);
                    drindex++;
                }
                ncfile.write(var, a);
                if ( !(min - Double.MAX_VALUE < 0.001d && max - Double.MIN_VALUE < 0.001d) ) {
                    ArrayDouble.D1 minmax = new ArrayDouble.D1(2);
                    minmax.set(0, min);
                    minmax.set(1, max);
                    Attribute actual_range = new Attribute("actual_range", minmax);
                    if ( var.findAttributeIgnoreCase("actual_range") != null ) {
                        ncfile.updateAttribute(var, actual_range);
                    }
                }
            } else if ( var.getDataType() == DataType.FLOAT ) {
                ArrayFloat.D1 a = new ArrayFloat.D1(var.getShape(0));
                int drindex = 0;

                // Don't using the missing of fill value in the actual range.

                Attribute missing = var.findAttribute("missing_value");
                Float mv = new Float(-1);
                if ( missing != null ) {
                    mv = (Float) missing.getNumericValue();
                }
                Float fill = new Float(-1);
                Attribute fillValue = var.findAttribute("_FillValue");
                if ( fillValue != null ) {
                    fill = (Float) fillValue.getNumericValue();
                }

                float min = Float.MAX_VALUE;
                float max = Float.MIN_VALUE;


                for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                    boolean isMissing = false;
                    boolean isFill = false;
                    DataRow dr = (DataRow) drIt.next();
                    Float b = (Float) dr.getData().get(varname);
                    if( missing != null && !mv.equals(Float.NaN) ) {
                        isMissing = !(Math.abs(mv - b) > 0.0001);
                    } else if ( b.equals(Float.NaN) ) {
                        isMissing = true;
                    }
                    if ( fillValue != null && !fill.equals(Float.NaN) ) {
                        isFill = !(Math.abs(fill -b) > 0.0001);
                    } else if ( b.equals(Float.NaN) ) {
                        isFill = true;
                    }
                    if ( b < min && !isMissing && !isFill ) {
                        min = b;
                    }
                    if ( b > max ) {
                        max = b;
                    }
                    a.set(drindex, b);
                    drindex++;
                }
                ncfile.write(var, a);
                ArrayFloat.D1 minmax = new ArrayFloat.D1(2);
                minmax.set(0, min);
                minmax.set(1, max);
                Attribute actual_range = new Attribute("actual_range", minmax);
                ncfile.updateAttribute(var, actual_range);
            } else if ( var.getDataType() == DataType.INT ) {
                ArrayInt.D1 a = new ArrayInt.D1(var.getShape(0));
                int drindex = 0;
                for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                    DataRow dr = (DataRow) drIt.next();
                    Integer b = (Integer) dr.getData().get(varname);
                    a.set(drindex, b);
                    drindex++;
                }
                ncfile.write(var, a);
            } else if ( var.getDataType() == DataType.LONG ) {
                ArrayLong.D1 a = new ArrayLong.D1(var.getShape(0));
                int drindex = 0;
                for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                    DataRow dr = (DataRow) drIt.next();
                    Long b = (Long) dr.getData().get(varname);
                    a.set(drindex, b);
                    drindex++;
                }
                ncfile.write(var, a);
            } else if ( var.getDataType() == DataType.SHORT ) {
                ArrayShort.D1 a = new ArrayShort.D1(var.getShape(0));
                int drindex = 0;
                for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                    DataRow dr = (DataRow) drIt.next();
                    Short b = (Short) dr.getData().get(varname);
                    a.set(drindex, b);
                    drindex++;
                }
                ncfile.write(var, a);
            } else if ( var.getDataType() == DataType.STRING ) {
                ArrayString.D1 a = new ArrayString.D1(var.getShape(0));
                int drindex = 0;
                for (Iterator drIt = datarows.iterator(); drIt.hasNext();) {
                    DataRow dr = (DataRow) drIt.next();
                    String b = (String) dr.getData().get(varname);
                    a.set(drindex, b);
                    drindex++;
                }
                ncfile.write(var, a);
            }
        }
        ucar.nc2.Variable var = ncfile.findVariable(lonname);
        Attribute ar = var.findAttribute("actual_range");

        if ( ar != null ) {
            try {
                ncfile.updateAttribute(null, new Attribute("geospatial_lon_min", ar.getNumericValue(0)));
            } catch (Exception e) {
                // Bummer, but we'll deal.
            }
            try {
                ncfile.updateAttribute(null, new Attribute("Westernmost_Easting", ar.getNumericValue(0)));
            } catch (Exception e) {
                // Bummer, but we'll deal.
            }
            try {
                ncfile.updateAttribute(null, new Attribute("geospatial_lon_max", ar.getNumericValue(1)));
            } catch (Exception e) {
                // Bummer, but we'll deal.
            }
            try {
                ncfile.updateAttribute(null, new Attribute("Easternmost_Easting", ar.getNumericValue(1)));
            } catch (Exception e) {
                // Bummer, but we'll deal.
            }
        }


        var = ncfile.findVariable(latname);
        ar = var.findAttribute("actual_range");

        if ( ar != null ) {
            try {
                ncfile.updateAttribute(null, new Attribute("geospatial_lat_min", ar.getNumericValue(0)));
            } catch (Exception e) {
                // Bummer, but we'll deal.
            }
            try {
                ncfile.updateAttribute(null, new Attribute("Southernmost_Northing", ar.getNumericValue(0)));
            } catch (Exception e) {
                // Bummer, but we'll deal.
            }
            try {
                ncfile.updateAttribute(null, new Attribute("geospatial_lat_max", ar.getNumericValue(1)));
            } catch (Exception e) {
                // Bummer, but we'll deal.
            }
            try {
                ncfile.updateAttribute(null, new Attribute("Northernmost_Northing", ar.getNumericValue(0)));
            } catch (Exception e) {
                // Bummer, but we'll deal.
            }
        }

        if ( zname != null ) {
            var = ncfile.findVariable(zname);
            if ( var != null ) {
                ar = var.findAttribute("actual_range");

                if ( ar != null ) {
                    try {
                        ncfile.updateAttribute(null, new Attribute("geospatial_vertical_min", ar.getNumericValue(0)));
                    } catch (Exception e) {
                        // Bummer, but we'll deal.
                    }
                    try {
                        ncfile.updateAttribute(null, new Attribute("geospatial_vertical_max", ar.getNumericValue(1)));
                    } catch (Exception e) {
                        // Bummer, but we'll deal.
                    }
                }
            }
        }

        var = ncfile.findVariable(time);
        ar = var.findAttribute("actual_range");
        if ( ar != null ) {
            Attribute unitsAttr = var.findAttribute("units");
            Attribute calAttr = var.findAttribute("calendar");
            CalendarDateUnit cdu = null;
            if ( unitsAttr != null ) {
                if ( calAttr !=  null ) {
                    cdu = CalendarDateUnit.of(calAttr.getStringValue(), unitsAttr.getStringValue());
                } else {
                    cdu = CalendarDateUnit.of("gregorian", unitsAttr.getStringValue());
                }
            }
            if ( cdu != null ) {
                CalendarDate start = cdu.makeCalendarDate((Double)ar.getNumericValue(0));
                CalendarDate end = cdu.makeCalendarDate((Double) ar.getNumericValue(1));
                try {
                    ncfile.updateAttribute(null, new Attribute("time_coverage_start", start.toString()));
                } catch (Exception e) {
                    // Bummer
                }
                try {
                    ncfile.updateAttribute(null, new Attribute("time_coverage_end", end.toString()));
                } catch (Exception e) {
                    // Bummer
                }
            }
        }



        ncfile.close();
        trajset1.close();
        trajset2.close();

    }
// Make sure to return the correct Java object based on the Array data type.
// Assumes the array is D1 (or D2 for CHAR) since these are points.
    private Object getObject(Array a, int index) {
        if ( a instanceof ArrayBoolean.D1 ) {
            return a.getBoolean(index);
        } else if (a instanceof ArrayByte.D1 ) {
            return a.getByte(index);
        } else if ( a instanceof ArrayChar.D2 ){
            ArrayChar.D2 s = (ArrayChar.D2)a;
            return s.getString(index);
        } else if ( a instanceof ArrayDouble.D1 ) {
            return a.getDouble(index);
        } else if ( a instanceof ArrayFloat.D1 ) {
            return a.getDouble(index);
        } else if ( a instanceof ArrayInt.D1 ) {
            return a.getInt(index);
        } else if ( a instanceof ArrayLong.D1 ) {
            return a.getLong(index);
        } else if ( a instanceof ArrayShort.D1 ) {
            return a.getShort(index);
        } else if ( a instanceof ArrayString.D1 ){
            ArrayString.D1 s = (ArrayString.D1)a;
            return s.get(index);
        } else {
            // Hopefully it never comes to this.
            return a.getObject(index);
        }
    }
    private void fill(Array d, String name, int offset, List datarows, String cruiseid, String time) {


        if ( d instanceof ArrayBoolean.D1 ) {
            for ( int index = 0; index < d.getShape()[0]; index++ ) {
                DataRow rowdata = datarows.get(offset+index);
                rowdata.getData().put(name, d.getBoolean(index));
            }
        } else if (d instanceof ArrayByte.D1 ) {
            for ( int index = 0; index < d.getShape()[0]; index++ ) {
                DataRow rowdata = datarows.get(offset+index);
                rowdata.getData().put(name, d.getByte(index));
            }
        } else if ( d instanceof ArrayChar.D2 ){
            ArrayChar.D2 s = (ArrayChar.D2)d;
            for ( int index = 0; index < s.getShape()[0]; index++ ) {
                DataRow rowdata = datarows.get(offset+index);
                rowdata.getData().put(name, s.getString(index));
                if ( cruiseid != null && name.equals(cruiseid) ) {
                    rowdata.setId(s.getString(index));
                }
            }
        } else if ( d instanceof ArrayDouble.D1 ) {
            for ( int index = 0; index < d.getShape()[0]; index++ ) {
                DataRow rowdata = datarows.get(offset+index);
                rowdata.getData().put(name, d.getDouble(index));
                if ( name.equals(time) ) {
                    rowdata.setTime(d.getDouble(index));
                }
            }
        } else if ( d instanceof ArrayFloat.D1 ) {
            for ( int index = 0; index < d.getShape()[0]; index++ ) {
                DataRow rowdata = datarows.get(offset+index);
                rowdata.getData().put(name, d.getFloat(index));
            }
        } else if ( d instanceof ArrayInt.D1 ) {
            for ( int index = 0; index < d.getShape()[0]; index++ ) {
                DataRow rowdata = datarows.get(offset+index);
                rowdata.getData().put(name, d.getInt(index));
            }
        } else if ( d instanceof ArrayLong.D1 ) {
            for ( int index = 0; index < d.getShape()[0]; index++ ) {
                DataRow rowdata = datarows.get(index);
                rowdata.getData().put(name, d.getLong(index));
            }
        } else if ( d instanceof ArrayShort.D1 ) {
            for ( int index = 0; index < d.getShape()[0]; index++ ) {
                DataRow rowdata = datarows.get(offset+index);
                rowdata.getData().put(name, d.getShort(index));
            }
        } else if ( d instanceof ArrayString.D1 ){
            ArrayString.D1 s = (ArrayString.D1)d;
            for ( int index = 0; index < s.getShape()[0]; index++ ) {
                DataRow rowdata = datarows.get(offset+index);
                rowdata.getData().put(name, s.get(index));
                if ( cruiseid != null && name.equals(cruiseid) ) {
                    rowdata.setId(s.get(index));
                }
            }
        }
    }

/*
 * This throws an LASException("Required value wasn't specified: " + id)
 * if s is null or "".
 *
 *
 * @param s a string which may be null or ""
 * @param id e.g., "database_access property 'url'"
 * @return s (for convenience)
 * @throws LASException("Required value wasn't specified: " + id)
 * if s is null or "".
 *
 * This is Bob's style, but hey
 */
    static String required(String s, String id) throws Exception {
        if (s == null || s.equals(""))
            throw new Exception ("Required value wasn't specified: " + id + ".");
        return s;
    }
}
