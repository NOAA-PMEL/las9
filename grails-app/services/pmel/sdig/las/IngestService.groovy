package pmel.sdig.las

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Utf8
import com.google.gson.*
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import grails.web.context.ServletContextHolder
import opendap.dap.AttributeTable
import opendap.dap.DAS
import org.apache.http.HttpException
import org.apache.http.client.utils.URIBuilder
import org.joda.time.*
import org.joda.time.chrono.GregorianChronology
import org.joda.time.format.*
import org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx.SignalFxMetricsExportAutoConfiguration
import pmel.sdig.las.tabledap.JsonMetadata
import pmel.sdig.las.type.GeometryType

import thredds.client.catalog.Access
import thredds.client.catalog.Catalog
import thredds.client.catalog.ServiceType
import thredds.client.catalog.builder.CatalogBuilder
import ucar.nc2.Attribute
import ucar.nc2.constants.FeatureType
import ucar.nc2.dataset.CoordinateAxis
import ucar.nc2.dataset.CoordinateAxis1D
import ucar.nc2.dataset.CoordinateAxis1DTime
import ucar.nc2.dataset.CoordinateAxis2D
import ucar.nc2.dt.GridCoordSystem
import ucar.nc2.dt.GridDataset
import ucar.nc2.dt.GridDatatype
import ucar.nc2.ft.FeatureDatasetFactoryManager
import ucar.nc2.time.CalendarDate
import ucar.nc2.time.CalendarDateRange
import ucar.nc2.time.CalendarDateUnit
import ucar.nc2.units.TimeUnit

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DecimalFormat
import java.util.regex.Pattern

@Transactional
class IngestService {

    IngestStatusService ingestStatusService;
    DateTimeService dateTimeService

    def servletContext = ServletContextHolder.servletContext

    LASProxy lasProxy = new LASProxy()
    PeriodFormatter pf = ISOPeriodFormat.standard()

    String TRAJECTORY = "cdm_trajectory_variables";
    String PROFILE = "cdm_profile_variables";
    String TIMESERIES = "cdm_timeseries_variables";
    String POINT = "cdm_point_variables";
    String NC_GLOBAL = "NC_GLOBAL"


    DecimalFormat df = new DecimalFormat("#.##");
    DecimalFormat decimalFormat = new DecimalFormat("###############.###############");

    DateTimeFormatter hoursfmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
    DateTimeFormatter shortFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy").withChronology(GregorianChronology.getInstance(DateTimeZone.UTC)).withZone(DateTimeZone.UTC);
    DateTimeFormatter mediumFerretForm = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm").withChronology(GregorianChronology.getInstance(DateTimeZone.UTC)).withZone(DateTimeZone.UTC);


    Dataset processRequest(AddRequest addRequest, Object parent) {

        if (addRequest.getType().equals("netcdf")) {
            return ingest(null, addRequest.getUrl())
        } else if (addRequest.getType().equals("dsg")) {
            return ingestDSG(addRequest)
        } else if (addRequest.getType().equals("tabledap")) {
            return ingestAllFromErddap(addRequest.getUrl(), addRequest.getAddProperties())
        } else if (addRequest.getType().equals("griddap")) {
            return datasetFromGriddapInfo(addRequest.getUrl())
        } else if (addRequest.getType().equals("thredds")) {
            def phash = null;
            if (parent instanceof Dataset) {
                phash = parent.getHash()
            }
            def ingesturl = addRequest.getUrl();
            if (ingesturl.endsWith(".html")) ingesturl = ingesturl.replace(".html", ".xml")
            return ingestFromThredds(ingesturl, phash, null, false)
        }
    }

    Dataset ingestDSG(AddRequest addRequest) {


        List<AddProperty> properties = addRequest.getAddProperties()


        def url = addRequest.getUrl()

        if (url.endsWith(".html")) url = url.replace(".html", "")

        Dataset dataset = ingestFromErddap_using_json(url, properties)

        dataset

    }

    String[] getMinMax(JsonObject bounds, String name) {
        JsonArray rows = (JsonArray) ((JsonObject) (bounds.get("table"))).get("rows")
        JsonArray names = (JsonArray) ((JsonObject) (bounds.get("table"))).get("columnNames")
        int index = -1
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).getAsString().equals(name)) {
                index = i
            }
        }
        JsonArray row1 = (JsonArray) rows.get(0)
        JsonArray row2 = (JsonArray) rows.get(1)

        String min = ((JsonElement) row1.get(index)).getAsString()
        String max = ((JsonElement) row2.get(index)).getAsString()
        String[] minmax = new String[2]
        minmax[0] = min
        minmax[1] = max
        return minmax
    }

    boolean updateTime(long id) {
        Dataset dataset = Dataset.get(id)
        if (dataset.variableChildren) {
            log.info("Updating time axis information for " + dataset.getTitle())
            Dataset nd = null;
            if (dataset.geometry == GeometryType.GRID) {
                nd = ingest(null, dataset.getUrl())
            } else {
                nd = ingestFromErddap_using_json(dataset.getUrl(), new ArrayList<AddProperty>())
            }
            if (nd && nd.getVariables()) {
                List<Variable> nvariables = nd.getVariables();
                List<Variable> ovariables = dataset.getVariables();
                for (int i = 0; i < nvariables.size(); i++) {
                    Variable nv = nvariables.get(i)
                    for (int j = 0; j < ovariables.size(); j++) {
                        Variable ov = ovariables.get(j)
                        // Not every variable has a time axis!
                        if (nv.name == ov.name && ov.getTimeAxis() && nv.getTimeAxis()) {
                            TimeAxis ot = ov.getTimeAxis();
                            TimeAxis nt = nv.getTimeAxis();
                            ot.setStart(nt.getStart())
                            ot.setEnd(nt.getEnd())
                            ot.setSize(nt.getSize())
                            ot.setPeriod(nt.getPeriod())
                            // In the IOOS models, the units change to the first date of the period.
                            ot.setUnits(nt.getUnits())
                            ot.save(flush: true)
                        }
                    }
                }
                dataset.save(flush: true)
            }
            log.info("Finished updating time information axis for " + dataset.getTitle())
        }
        true
    }

    Dataset ingest(String parentHash, String url) {

        // Set the status of the parent for each variable using the parentHash key.

        // Is it a netCDF data source?

        def hash = getDigest(url)
        def dataset = new Dataset([url: url, hash: hash])
        if (!parentHash) parentHash = dataset.getHash();

        // TODO catch exepctions and keep going...

        Formatter error = new Formatter()

        GridDataset gridDs
        try {
            ingestStatusService.saveProgress(parentHash, "Reading the OPeNDAP data source for the variables.")
            gridDs = (GridDataset) FeatureDatasetFactoryManager.open(FeatureType.GRID, url, null, error)
        } catch (IOException e) {
            dataset.setMessage(e.getMessage())
            dataset.setStatus(Dataset.INGEST_FAILED)
            return dataset
        }

        if (gridDs != null) {

            log.debug("Grid data set found ... ")
            List<Attribute> globals = gridDs.getGlobalAttributes()
            // Get the DRS information


            Map<String, String> drsParams = new HashMap<String, String>()

            String title = url
            for (Iterator iterator = globals.iterator(); iterator.hasNext();) {
                Attribute attribute = (Attribute) iterator.next()
                if (attribute.getShortName().equals("title")) {
                    title = attribute.getStringValue()
                } else if (attribute.getShortName().equals("dataset_title")) {
                    title = attribute.getStringValue();
                } else if (attribute.getShortName().toLowerCase().equals("history")) {
                    dataset.setHistory(attribute.getStringValue())
                }
                drsParams.put(attribute.getShortName(), attribute.getStringValue())
            }

            dataset.setTitle(title)

            List<GridDatatype> grids = gridDs.getGrids()
            String m = grids.size() + " variables were found. Processing..."
            ingestStatusService.saveProgress(parentHash, m)
            if (grids.size() <= 0) {
                dataset.setStatus(Dataset.INGEST_FAILED)
                dataset.setMessage("No data on a regular grid found. May be a projected data set.");
                return dataset
            }
            for (int i = 0; i < grids.size(); i++) {

                GridDatatype gridDatatype = (GridDatatype) grids.get(i);

                // The variable basics
                String vname = gridDatatype.getShortName()

                log.debug("Processing variable " + vname + " ...")

                def vtitle = null
                def sname = null;

                Attribute ln_attr = gridDatatype.findAttributeIgnoreCase("long_name")
                Attribute sn_attr = gridDatatype.findAttributeIgnoreCase("standard_name")
                if (ln_attr) {
                    vtitle = ln_attr.getStringValue()
                }
                if (!vtitle && sn_attr) {
                    vtitle = sn_attr.getStringValue()
                }
                if (!vtitle) {
                    vtitle = gridDatatype.getName()
                }
                if (sn_attr) {
                    sname = sn_attr.getStringValue()
                }

                String vhash = getDigest(url + ":" + gridDatatype.getDescription())

                // Set the variable name in the DRS params...
                drsParams.put("shortname", gridDatatype.getShortName())

                GridCoordSystem gcs = gridDatatype.getCoordinateSystem()


                String units = gridDatatype.getUnitsString()
                // Axes are next...
                long tIndex = -1
                long zIndex = -1
                Variable variable = new Variable()
                TimeAxis tAxis = null
                if (gcs.hasTimeAxis()) {

                    if (gcs.hasTimeAxis1D()) {
                        log.debug("1D time axis found ... ")
                        CoordinateAxis1DTime time = gcs.getTimeAxis1D()
                        CalendarDateRange range = time.getCalendarDateRange()

                        // Get the basics
                        String start = range.getStart().toString()
                        String end = range.getEnd().toString()
                        long size = time.getSize()
                        tIndex = size / 2
                        if (tIndex <= 0) tIndex = 1
                        String timeunits = time.getUnitsString()
                        Attribute cal = time.findAttribute("calendar")
                        String calendar = "standard"
                        if (cal != null) {
                            calendar = cal.getStringValue(0)
                        }
                        String shortname = time.getShortName()
                        String timetitle = time.getFullName()

                        // Figure out the delta (as a period string) and where the time is marked (beginning, middle, or end of the period
                        double[] tb1 = time.getBound1()
                        double[] tb2 = time.getBound2()
                        double[] times = time.getCoordValues()

                        CalendarDateUnit cdu = CalendarDateUnit.of(calendar, timeunits)
                        Period p0 = null
                        String position0 = getPosition(times[0], tb1[0], tb2[0])
                        boolean constant_position = true
                        boolean regular = time.isRegular()
                        tAxis = new TimeAxis()

                        TimeUnit tu = time.getTimeResolution()
                        double du = tu.getValue()
                        String u = tu.getUnitString()

                        if (times.length > 1) {
                            if (regular) {
                                // TODO sec, week, year?
                                if (u.contains("second")) {
                                    // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                    int seconds = (int) du
                                    p0 = new Period(0, 0, 0, 0, 0, 0, seconds, 0)
                                    int hours = p0.normalizedStandard(PeriodType.hours()).getHours();
                                    int days = p0.normalizedStandard(PeriodType.days()).getDays();
                                    if (hours < 24) {
                                        p0 = new Period(0, 0, 0, 0, hours, 0, 0, 0);
                                    } else if (days > 27) {
                                        p0 = new Period(0, 1, 0, 0, 0, 0, 0, 0);
                                    } else {
                                        p0 = new Period(0, 0, 0, days, 0, 0, 0, 0);
                                    }
                                } else if (u.contains("hour")) {
                                    for (int d = 0; d < 27; d++) {
                                        if (du < 23.5 * d && du < 23.5 * d + 1) {
                                            // The unit is hours and the delta is less than 25 so use the hours delta
                                            // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                            int hrs = (int) du;
                                            p0 = new Period(0, 0, 0, 0, hrs, 0, 0, 0)
                                        }
                                    }
                                    if (p0 == null) {
                                        if (du > 28 * 24 && du < 33 * 24) {
                                            p0 = new Period(0, 1, 0, 0, 0, 0, 0, 0)
                                        }
                                    }

                                } else if (u.contains("day")) {
                                    if (du < 1) {
                                        int hours = du * 24.0d
                                        p0 = new Period(0, 0, 0, 0, hours, 0, 0, 0)
                                    } else {
                                        p0 = new Period(0, 0, 0, (int) du, 0, 0, 0, 0)
                                    }

                                }
                            } else {
                                p0 = getPeriod(cdu, times[0], times[1])
                                if (!isMonotonic(times)) {
                                    log.debug("Time axis is not monotonic... quitting.")
                                }
                                int hours = p0.getHours()
                                int days = p0.getDays()
                                int months = p0.getMonths()
                                int years = p0.getYears()
                                if (days >= 28) {
                                    p0 = new Period(0, 1, 0, 0, 0, 0, 0, 0)
                                } else if (hours == 0 && days > 0) {
                                    p0 = new Period(0, 0, 0, days, 0, 0, 0, 0)
                                } else if (hours > 0) {
                                    p0 = new Period(0, 0, 0, 0, hours, 0, 0, 0)
                                }
                            }

                            Period period = getPeriod(cdu, times[0], times[times.length - 1])

                            log.debug("Setting delta " + pf.print(p0))
                            if (p0 != null) {
                                tAxis.setDelta(pf.print(p0))
                            } else {
                                tAxis.setDelta("P1D")
                            }
                            log.debug("Setting data set period to " + pf.print(period))
                            if (period != null) {
                                tAxis.setPeriod(pf.print(period))
                            }

                        } else if (times.length) {
                            tAxis.setDelta(pf.print(Period.ZERO));
                            tAxis.setPeriod(pf.print(Period.ZERO))
                        }


                        tAxis.setStart(start)
                        tAxis.setEnd(end)
                        if (start.contains("0000") && end.contains("0000")) {
                            tAxis.setClimatology(true)
                        } else {
                            tAxis.setClimatology(false)
                        }
                        tAxis.setSize(size)
                        tAxis.setUnits(time.getUnitsString())
                        tAxis.setCalendar(calendar)
                        tAxis.setTitle(title)
                        tAxis.setName(shortname)

                        if (constant_position) {
                            tAxis.setPosition(position0)
                        }


                    } else {

                    }
                }

                CoordinateAxis xca = gcs.getXHorizAxis()
                GeoAxisX xAxis = null
                if (xca instanceof CoordinateAxis1D) {
                    CoordinateAxis1D x = (CoordinateAxis1D) xca
                    xAxis = new GeoAxisX()
                    xAxis.setType("x")
                    xAxis.setTitle(x.getFullName())
                    xAxis.setName(x.getShortName())
                    xAxis.setUnits(x.getUnitsString())
                    xAxis.setRegular(x.isRegular())
                    if (x.isRegular()) {
                        xAxis.setDelta(x.getIncrement())
                    }
                    def xmin = x.getMinValue()
                    def xmax = x.getMaxValue()
                    if (!xmin || !xmax || Double.isNaN(xmin) || Double.isNaN(xmax)) {
                        if (x.isRegular()) {
                            xmin = x.getStart();
                            xmax = x.getSize() * x.getIncrement();
                        } else {
                            def dim = x.getDimension(0)
                            xmin = x.getCoordValue(0)
                            xmax = x.getCoordValue(dim.length - 1)
                        }
                    }
                    xAxis.setMin(Math.min(xmin, xmax))
                    xAxis.setMax(Math.max(xmin, xmax))
                    xAxis.setSize(x.getSize())
                    xAxis.setDimensions(1)

                } else if (xca instanceof CoordinateAxis2D) {
                    CoordinateAxis2D x = (CoordinateAxis2D) xca
                    xAxis = new GeoAxisX()
                    xAxis.setType("x")
                    xAxis.setTitle(x.getFullName())
                    xAxis.setName(x.getShortName())
                    xAxis.setUnits(x.getUnitsString())
                    xAxis.setRegular(false)
                    xAxis.setDimensions(2)
                    xAxis.setMin(x.getMinValue())
                    xAxis.setMax(x.getMaxValue())
                    xAxis.setSize(x.getSize())
                    VariableProperty vp1 = new VariableProperty()
                    vp1.setType("ferret")
                    vp1.setName("curvi_coord_lon")
                    vp1.setValue(x.getShortName())
                    variable.addToVariableProperties(vp1)
                    VariableProperty vp2 = new VariableProperty()
                    vp2.setType("ferret")
                    vp2.setName("curv_lon_min")
                    vp2.setValue(String.valueOf(x.getMinValue()))
                    variable.addToVariableProperties(vp2)
                    VariableProperty vp3 = new VariableProperty()
                    vp3.setType("ferret")
                    vp3.setName("curv_lon_max")
                    vp3.setValue(String.valueOf(x.getMaxValue()))
                    variable.addToVariableProperties(vp3)
                    double xmax = x.getMaxValue()
                    double xmin = x.getMinValue()
                    double range = xmax - xmin
                    double delta = range / Double.valueOf(x.getSize() + 1).doubleValue()
                    double max_span = 360.0d - delta
                    VariableProperty vp4 = new VariableProperty()
                    vp4.setType("ferret")
                    vp4.setName("lon_modulo")
                    if (Math.abs(range) >= max_span) {
                        vp4.setValue("1")
                    } else {
                        vp4.setValue("0")
                    }
                    variable.addToVariableProperties(vp4)
                }
                GeoAxisY yAxis = null
                CoordinateAxis yca = gcs.getYHorizAxis()
                if (yca instanceof CoordinateAxis1D) {
                    CoordinateAxis1D y = (CoordinateAxis1D) yca
                    yAxis = new GeoAxisY()
                    yAxis.setType("y")
                    yAxis.setTitle(y.getFullName())
                    yAxis.setName(y.getShortName())
                    yAxis.setUnits(y.getUnitsString())
                    yAxis.setRegular(y.isRegular())
                    if (y.isRegular()) {
                        yAxis.setDelta(y.getIncrement())
                    }
                    yAxis.setMin(y.getMinValue())
                    yAxis.setMax(y.getMaxValue())
                    yAxis.setSize(y.getSize())
                    yAxis.setDimensions(1)
                } else {
                    CoordinateAxis2D y = (CoordinateAxis2D) yca
                    yAxis = new GeoAxisY()
                    yAxis.setType("y")
                    yAxis.setTitle(y.getFullName())
                    yAxis.setName(y.getShortName())
                    yAxis.setUnits(y.getUnitsString())
                    yAxis.setRegular(false)
                    yAxis.setMin(y.getMinValue())
                    yAxis.setMax(y.getMaxValue())
                    yAxis.setSize(y.getSize())
                    yAxis.setDimensions(2)
                    VariableProperty vp1 = new VariableProperty()
                    vp1.setType("ferret")
                    vp1.setName("curvi_coord_lat")
                    vp1.setValue(y.getShortName())
                    variable.addToVariableProperties(vp1)
                }
                CoordinateAxis1D z = gcs.getVerticalAxis()
                VerticalAxis zAxis = null
                if (z != null) {
                    // Use the first z. It's probably more interesting.
                    zIndex = 1
                    zAxis = new VerticalAxis()
                    zAxis.setSize(z.getSize())
                    zAxis.setType("z")
                    zAxis.setTitle(z.getFullName())
                    zAxis.setName(z.getShortName())
                    zAxis.setMin(z.getMinValue())
                    zAxis.setMax(z.getMaxValue())
                    zAxis.setRegular(z.isRegular())
                    zAxis.setUnits(z.getUnitsString())
                    if (zAxis.isRegular()) {
                        zAxis.setDelta(z.getIncrement())
                    }
                    double[] v = z.getCoordValues()
                    List<Zvalue> values = new ArrayList<Zvalue>()
                    for (int j = 0; j < v.length; j++) {
                        Zvalue zv = new Zvalue()
                        zv.setZ(v[j])
                        values.add(zv)
                    }
                    zAxis.setZV(values)
                    String posi = z.getPositive()
                    if (posi != null) {
                        zAxis.setPositive(posi)
                    } else {
                        zAxis.setPositive("not specified")
                    }
                }

                String intervals = ""
                if (xAxis && xAxis.size > 1) {
                    intervals = intervals + "x"
                }
                if (yAxis && yAxis.size > 1) {
                    intervals = intervals + "y"
                }
                if (zAxis && zAxis.size > 1) {
                    intervals = intervals + "z"
                }
                if (tAxis) {
                    intervals = intervals + "t"
                }


                variable.setUrl(url)
                variable.setName(vname)
                variable.setHash(vhash)
                variable.setTitle(vtitle)
                variable.setStandard_name(sname)
                variable.setGeometry(GeometryType.GRID)
                variable.setIntervals(intervals)
                variable.setUnits(units)

                if (dataset.getGeoAxisX() == null) {
                    dataset.setGeoAxisX(xAxis)
                    xAxis.setDataset(dataset)
                } else {
                    if (!sameAxis(xAxis, dataset.getGeoAxisX())) {
                        log.error("Second X-axis found in data set.")
                    }
                }

                if (dataset.getGeoAxisY() == null) {
                    dataset.setGeoAxisY(yAxis)
                    yAxis.setDataset(dataset)
                } else {
                    if (!sameAxis(yAxis, dataset.getGeoAxisY())) {
                        log.error("Second Y-axis found in data set.")
                    }
                }

                if (zAxis) {
                    if (dataset.getVerticalAxis() == null) {
                        dataset.setVerticalAxis(zAxis)
                        zAxis.setDataset(dataset)
                    } else {
                        if (!sameAxis(zAxis, dataset.getVerticalAxis())) {
                            log.error("Second vertical axis found in data set")
                        }
                    }
                }

                if (tAxis) {
                    if (dataset.getTimeAxis() == null) {
                        dataset.setTimeAxis(tAxis)
                        tAxis.setDataset(dataset)
                    }
                    // Not worried about multiple times at the moment
                }


                log.debug("Adding " + variable.getTitle() + " to data set")
                dataset.addToVariables(variable)
                dataset.variableChildren = true;
                dataset.geometry = GeometryType.GRID

                int done = i + 1;
                String m2 = done + " variables out of a total of " + grids.size() + " have been processed."
                ingestStatusService.saveProgress(parentHash, m2)

            }

            if (!dataset.validate()) {
                dataset.errors.each {
                    log.debug(it.toString())
                }
            }

        } else {
            // Is it a THREDDS catalog?
        }
        addVectors(dataset)
        dataset
        // Is it an ESGF catalog or data set?
    }

    boolean sameAxis(GeoAxisX a1, GeoAxisX a2) {
        def delta_f = 0.001
        if (a1.type == a2.type &&
                Math.abs(a1.delta - a2.delta) < delta_f &&
                a1.size == a2.size &&
                Math.abs(a1.max - a2.max) < delta_f &&
                Math.abs(a1.min - a2.min) < delta_f) {
            return true
        } else {
            return false
        }
    }

    boolean sameAxis(GeoAxisY a1, GeoAxisY a2) {
        def delta_f = 0.001
        if (a1.type == a2.type &&
                Math.abs(a1.delta - a2.delta) < delta_f &&
                a1.size == a2.size &&
                Math.abs(a1.max - a2.max) < delta_f &&
                Math.abs(a1.min - a2.min) < delta_f) {
            return true
        } else {
            return false
        }
    }

    boolean sameAxis(VerticalAxis a1, VerticalAxis a2) {
        def delta_f = 0.001
        if (a1.type == a2.type &&
                a1.size == a2.size &&
                Math.abs(a1.max - a2.max) < delta_f &&
                Math.abs(a1.min - a2.min) < delta_f) {
            if (a1.zvalues && a2.zvalues) {
                if (a1.zvalues.size() != a2.zvalues.size()) {
                    return false;
                } else {
                    for (int zi = 0; zi < a1.zvalues.size(); zi++) {
                        if (Math.abs(a1.zvalues.get(zi).getValue() - a2.zvalues.get(zi).getValue()) > delta_f) {
                            log.error('Found differing z values')
                            log.error('axis 1 at ' + zi + ' with value ' + a1.zvalues.get(zi).getValue())
                            log.error('axis 2 at ' + zi + ' with value ' + a2.zvalues.get(zi).getValue())
                            return false;
                        }
                    }
                }
            }
            return true
        } else {
            return false
        }
    }

    Boolean addVariablesAndSaveFromThredds(String url, String parentHash, String erddap, boolean full) {
        Dataset.withNewTransaction {
            Dataset dataset = Dataset.findByUrl(url)
            // If this is being done by the background process, it is possible that a user already requested this data set be loaded.
            log.debug("dataset found" + url)
            try {
                log.debug("Loading the catalog for" + url)
                ingestStatusService.saveProgress(parentHash, "Loading the THREDDS catalog for these variables.")
                Dataset temp = ingestFromThredds(url, parentHash, erddap, full)
                log.debug("Finished loading variables for " + url)

                // There will be a layer that represents the catalog at the top with the variables in a data set one level down.

                if (temp.getDatasets() && temp.getDatasets().size() == 1) {
                    Dataset temp2 = temp.getDatasets().get(0)
                    if (temp2 && temp2.getVariables()) {
                        def t2v = temp2.getVariables();
                        log.debug(t2v.size() + " variables found " + dataset.getUrl())
                        t2v.each { Variable v ->
                            dataset.addToVariables(v)
                            v.save(failOnError: true, flush: true)
                        }
                        dataset.setStatus(Dataset.INGEST_FINISHED)
                        if (dataset.validate()) {
                            addVectors(dataset)
                            dataset.save(flush: true)
                        }
                    } else {
                        if (temp2.getStatus() == Dataset.INGEST_FAILED) {
                            dataset.setStatus(Dataset.INGEST_FAILED)
                            dataset.save(failOnError: true, flush: true)
                        }
                    }
                } else {
                    log.debug("No variables found for " + dataset.getUrl())
                    dataset.setStatus(Dataset.INGEST_FAILED)
                    dataset.save(failOnError: true, flush: true)
                }
            } catch (Exception e) {
                log.debug("Ingest failed " + e.getMessage())
                dataset.setStatus(Dataset.INGEST_FAILED)
                dataset.save(failOnError: true, flush: true)
            }
        }
        return true
    }

    Dataset ingestFromThredds(String url, String parentHash, String erddap, boolean full) {
        // Just cheat...
        if (url.contains("data.pmel.noaa.gov/uaf")) {
            erddap = "https://upwell.pfeg.noaa.gov/erddap/"
        }
        log.debug("Starting ingest of " + url)
        dateTimeService.init()
        String tdsid;
        String urlwithid;
        if (url.contains("#")) {
            urlwithid = url.substring(0, url.indexOf("#"))
            tdsid = url.substring(url.indexOf("#") + 1, url.length())
            if (!tdsid.equals("null")) {
                urlwithid = urlwithid + "?dataset=" + tdsid;
            }
        } else {
            urlwithid = url;
        }
        ingestStatusService.saveProgress(parentHash, "Reading the catalog from the remote server.")
        CatalogBuilder builder = new CatalogBuilder();
        Catalog catalog = builder.buildFromLocation(urlwithid, null);
        if (erddap == null) { // Just a thredds catalog, no supporting ERDDAP
            ingestStatusService.saveProgress(parentHash, "Catalog read. Looking for data sources.")
            return createDatasetFromCatalog(catalog, parentHash, full);
        } else {
            return createDatasetFromUAF(catalog, erddap);
        }
        log.debug("Finished ingest of " + url)
    }

    Dataset createDatasetFromUAF(Catalog catalog, String erddap) {
        Dataset dataset = new Dataset()
        String cname = catalog.getName();
        if (cname.equals("THREDDS Server Default Catalog : You must change this to fit your server!"))
            cname = "Data Catalog";
        if (!cname) {
            dataset.setTitle(catalog.getUriString())
        } else {
            dataset.setTitle(cname)
        }
        dataset.setUrl(catalog.getUriString())
        dataset.setHash(getDigest(catalog.getUriString()))
        def children = catalog.getDatasetsLogical();
        for (int i = 0; i < children.size(); i++) {
            thredds.client.catalog.Dataset invDataset = children.get(i)
            if (!invDataset.getName().toLowerCase().equals("tds quality rubric")) {
                List<Dataset> childDatasets = processUAFDataset(invDataset, erddap)
                for (int j = 0; j < childDatasets.size(); j++) {
                    Dataset child = childDatasets.get(j)
                    dataset.addToDatasets(child)
                }
            }
        }
        dataset.setStatus(Dataset.INGEST_FINISHED)
        dataset
    }

    List<Dataset> processUAFDataset(thredds.client.catalog.Dataset invDataset, String erddap) {

        List<Dataset> all = new ArrayList<Dataset>()
        if (invDataset.hasAccess() && invDataset.getAccess(ServiceType.OPENDAP) != null) {
            List<Dataset> a = createFromUAFDataset(invDataset, erddap)
            if (a.size() > 0)
                all.addAll(a)
        }
        if (invDataset.hasNestedDatasets()) {
            List<thredds.client.catalog.Dataset> children = invDataset.getDatasetsLogical()
            thredds.client.catalog.Dataset rubric = children.get(children.size() - 1)
            if (rubric.getName().equals("")) {
                children.remove(rubric)
            }

            Dataset childDataset = new Dataset([name: invDataset.getName(), title: invDataset.getName(), hash: getDigest(invDataset.getName() + invDataset.getCatalogUrl())])
            for (int i = 0; i < children.size(); i++) {
                thredds.client.catalog.Dataset child = children.get(i)
                List<Dataset> kids = processUAFDataset(child, erddap)
                for (int j = 0; j < kids.size(); j++) {
                    childDataset.addToDatasets(kids.get(j))
                }
            }
            all.add(childDataset)
        }
        all
    }
    @NotTransactional
    def griddapDirect(String url, Object parent) {
        if ( url.endsWith("griddap") || url.endsWith("griddap/") ) {
            if ( !url.endsWith("/") ) url = url + "/"
            url = url + "index.json?page=1&itemsPerPage=20000"
        }
        ObjectMapper objectMapper = new ObjectMapper()
        Map<String, List<String>> collected_datasets = new HashMap<String, List<String>>();
        // You have to find the index of the items you want from the name row 'cause the differ for
        // different (versions?) ERDDAP servers.
        try {
            Map<String, Object> datasets = objectMapper.readValue(new URL(url), Map.class);
            Map<String, Object> table = (Map) datasets.get("table");
            List<String> col_names = (List) table.get("columnNames")
            int griddap_url_index = 0;
            int institution_index = 15;
            for (i in 0..<col_names.size()) {
                String n = col_names.get(i)
                if (n.equals("griddap")) {
                    griddap_url_index = i
                }
                if ( n.equals("Institution") ) {
                    institution_index = i
                }
            }
            List<List<String>> rows = (List) table.get("rows");
            for (int i = 0; i < rows.size(); i++) {
                String inst = rows.get(i).get(institution_index);
                inst = inst.trim()
                if (!inst.equals("???")) {
                    Dataset inst_dataset = new Dataset()
                    if (inst.contains(">"))
                        inst = inst.substring(0, inst.indexOf(">")).trim()
                    if (inst.contains(";"))
                        inst = inst.substring(0, inst.indexOf(";")).trim()
                } else {
                    log.debug("Skipped questionable institution.")
                }
                inst = inst.trim()
                String data = rows.get(i).get(griddap_url_index);
                data = data.trim()
                List<String> data_urls = collected_datasets.get(inst);
                if (data_urls == null) {
                    data_urls = new ArrayList<String>();
                    collected_datasets.put(inst, data_urls);
                }
                data_urls.add(data);
            }

            Set<String> keys = collected_datasets.keySet();
            int tt_count = 0
            keys.each {String inst ->
                List<String> inst_data = collected_datasets.get(inst)
                System.out.println("Institution: " + inst)
                for (ifx in 0..<inst_data.size()) {
                    tt_count++
                    System.out.println("\t" + inst_data.get(ifx))
                }
            }
            log.debug("=-=-=-=-=-=-=-=-=-=- Griddap Ingest Report=-=-=-=-=-=-=-=-=-=-")
            log.debug("Ingesting " + url)
            log.debug(keys.size() + "  Institutions found.")
            log.debug(tt_count + "   total endpoints found.")
            log.debug("-=-=-=-=-=-=-=-=-=-=-=-=-  End of Ingest Report =-=-=-=-=-=-=-=-=-=-=-=-")
            keys.each {
                String inst_title = it
                def inst_dataset = new Dataset()
                inst_dataset.setTitle(inst_title)
                inst_dataset.setHash(getDigest(inst_title))
                List<String> inst_url_list = collected_datasets.get(inst_title)
                log.info("Adding data sets for " + inst_title)
                inst_url_list.each { String ds_url ->
                    Dataset end_point = datasetFromGriddapInfo(ds_url)
                    if (end_point != null)
                        inst_dataset.addToDatasets(end_point)
                }
                if ( inst_dataset.getDatasets() && inst_dataset.getDatasets().size() > 0) {
                    if ( parent instanceof Site) {
                        Site site = parent;
                        Site.withTransaction {
                            site.addToDatasets(inst_dataset)
                            site.save(flush: true)
                        }
                    } else if (parent instanceof Dataset) {
                        Dataset dp = parent
                        Dataset.withTransaction {
                            dp.addToDatasets(inst_dataset)
                            dp.save(flush:true)
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Dataset datasetFromGriddapInfo(String url) {
        def info_url = url
        if (!info_url.endsWith("/")) info_url = url + "/"
        info_url = info_url.replace("griddap", "info") + "index.json"
        log.info("\tBuilding LAS data set for " + url)
        String sourceUrl = null;
        LASProxy lasProxy = new LASProxy();
        JsonParser jsonParser = new JsonParser();
        Dataset dataset = new Dataset()
        dataset.setHash(getDigest(url))
        TimeAxis timeAxis = new TimeAxis()
        timeAxis.setUnits("seconds since 1970-01-01T00:00:00Z")
        timeAxis.setName("time")
        timeAxis.setTitle("Time")
        Period p0;
        // Will get set below if attribute exists
        timeAxis.setCalendar("gregorian")
        GeoAxisX geoAxisX = new GeoAxisX()
        geoAxisX.setName("longitude")
        geoAxisX.setTitle("Longitude")
        geoAxisX.setType("x")
        GeoAxisY geoAxisY = new GeoAxisY()
        geoAxisY.setName("latitude")
        geoAxisY.setTitle("Latitude")
        geoAxisY.setType("y")
        VerticalAxis verticalAxis = new VerticalAxis()
        verticalAxis.setName("depth")
        verticalAxis.setTitle("Depth")
        verticalAxis.setType("z")
        // May get changed below
        verticalAxis.setPositive("down")

        try {
            String metadataJSONString = lasProxy.executeGetMethodAndReturnResult(info_url);
            if (metadataJSONString != null) {
                if (metadataJSONString != null) {

                    JsonObject metadata = jsonParser.parse(metadataJSONString).getAsJsonObject();
                    JsonObject metadata_table = metadata.get("table").getAsJsonObject();

                    // Assuming the positions in the array are always the same.
                    // Risky?
                    int typeIndex = 0;

                    // TODO use the title string, or combine data sets on different axes from the same data source.
                    JsonArray metadata_rows = metadata_table.getAsJsonArray("rows");

                    for (int mi = 0; mi < metadata_rows.size(); mi++) {
                        JsonArray metaRow = metadata_rows.get(mi).getAsJsonArray();
                        String metaType = metaRow.get(typeIndex).getAsString();
                        if (metaType.equalsIgnoreCase("dimension")) {
                            String dimName = metaRow.get(1).getAsString();
                            // Time size
                            if (dimName.equals("time")) {
                                String info = metaRow.get(4).getAsString();
                                String[] majorParts = info.split(",");
                                String[] parts = majorParts[0].split("=");
                                timeAxis.setSize(Long.valueOf(parts[1]).longValue());

                                // Time step has to be derived from the average spacing which involves parsing out the values and deciding the units to use.
                                /*
                        Grab the first one.

                        if it is days around 30 use it as monthsd
                        if it is days less than 27 use days
                        if it is hours use hours

                         */


                                int size = (int) (timeAxis.getSize())
                                if (size > 1) {
                                    String[] deltaParts = majorParts[2].split("=");
                                    String[] timeParts = deltaParts[1].split(" ");
                                    if (timeParts[0].contains("infinity")) {
                                        log.debug("Problem with the time axis.")
                                        return null;
                                    } else if (timeParts[1].contains("year")) {
                                        timeAxis.setUnits("year");
                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                        p0 = new Period(1, 0, 0, 0, 0, 0, 0, 0)
                                    } else if (timeParts[1].contains("day")) {
                                        // Make a number out of the days;
                                        int days = Integer.valueOf(timeParts[0]).intValue();
                                        if (days < 27) {
                                            timeAxis.setUnits("day");
                                            // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                            p0 = new Period(0, 0, 0, days, 0, 0, 0, 0)
                                        } else if (days >= 27 && days < 33) {
                                            timeAxis.setUnits("month");
                                            // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                            p0 = new Period(0, 1, 0, 0, 0, 0, 0, 0)
                                        } else if (days >= 88 && days < 93) {
                                            timeAxis.setUnits("month");
                                            // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                            p0 = new Period(0, 3, 0, 0, 0, 0, 0, 0)
                                        } else if (days >= 175 && days < 188) {
                                            timeAxis.setUnits("month");
                                            // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                            p0 = new Period(0, 6, 0, 0, 0, 0, 0, 0)
                                        } else if (days > 357) {
                                            timeAxis.setUnits("year");
                                            // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                            p0 = new Period(1, 0, 0, 0, 0, 0, 0, 0)
                                        }

                                    } else if (timeParts[0].contains("h")) {
                                        String step = timeParts[0].replace("h", "");
                                        timeAxis.setUnits("hour");
                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                        p0 = new Period(0, 0, 0, 0, Integer.valueOf(step).intValue(), 0, 0, 0)
                                    }
                                } else {
                                    String[] valueParts = majorParts[1].split("=");
                                    NameValuePair nv = new NameValuePair()
                                    nv.setName(valueParts[1])
                                    nv.setValue(valueParts[1])
                                    timeAxis.addToNameValuePairs(nv);
                                    timeAxis.setUnits("none")
                                    p0 = new Period(1, 0, 0, 0, 0, 0, 0, 0)
                                    timeAxis.setDelta(pf.print(p0))
                                }
                                // Lon size
                            } else if (dimName.equals("longitude")) {
                                // Found a lon axis
                                String info = metaRow.get(4).getAsString();
                                String[] majorParts = info.split(",");
                                String[] parts = majorParts[0].split("=");
                                geoAxisX.setSize(Long.valueOf(parts[1]).longValue());
                                if (majorParts.length == 3) {
                                    parts = majorParts[2].split("=");
                                    geoAxisX.setDelta(Double.valueOf(parts[1]).doubleValue());
                                }
                                //
                                // Lat size
                            } else if (dimName.equals("latitude")) {
                                String info = metaRow.get(4).getAsString();
                                String[] majorParts = info.split(",");
                                String[] parts = majorParts[0].split("=");
                                geoAxisY.setSize(Long.valueOf(parts[1]).longValue());
                                if (majorParts.length == 3) {
                                    parts = majorParts[2].split("=");
                                    geoAxisY.setDelta(Double.valueOf(parts[1]).doubleValue());
                                }
                            } else {
                                String zstring = lasProxy.executeGetMethodAndReturnResult(url + ".json?" + dimName);
                                JsonObject zobject = jsonParser.parse(zstring).getAsJsonObject();
                                JsonObject ztable = zobject.get("table").getAsJsonObject();
                                JsonArray zarray = ztable.getAsJsonArray("rows");

                                for (int zi = 0; zi < zarray.size(); zi++) {
                                    // TODO positive, regular, delta, etc maybe we don't care
                                    Zvalue zValue = new Zvalue();
                                    zValue.setZ(zarray.get(zi).getAsDouble())
                                    verticalAxis.addToZvalues(zValue)
                                }
                            }
                        } else if (metaType.equalsIgnoreCase("attribute")) {
                            String metaVar = metaRow.get(1).getAsString();
                            // See if it's a attribute for a variable. Guaranteed to have encountered the variable before its attributes.
                            Variable atvar
                            if (dataset.variables) {
                                atvar = (Variable) dataset.variables.find { Variable variable ->
                                    variable.getName().equals(metaVar);
                                }
                            }
                            if (metaVar.equals("NC_GLOBAL")) {
                                String metaName = metaRow.get(2).getAsString();
                                // Time start
                                if (metaName.equals("time_coverage_start")) {
                                    timeAxis.setStart(metaRow.get(4).getAsString());
                                    if (timeAxis.getStart().contains("0000")) {
                                        timeAxis.setClimatology(true)
                                    } else {
                                        timeAxis.setClimatology(false)

                                    }
                                    // Time end
                                } else if (metaName.equals("time_coverage_end")) {
                                    timeAxis.setEnd(metaRow.get(4).getAsString());
                                    // Lon start
                                } else if (metaName.equals("Westernmost_Easting")) {
                                    geoAxisX.setMin(metaRow.get(4).getAsDouble());
                                    // Lon end
                                } else if (metaName.equals("Easternmost_Easting")) {
                                    geoAxisX.setMax(metaRow.get(4).getAsDouble());
                                    // Lon step
                                } else if (metaName.equals("geospatial_lon_resolution")) {
                                    geoAxisX.setDelta(metaRow.get(4).getAsDouble());
                                    // Lat start
                                } else if (metaName.equals("geospatial_lat_min")) {
                                    geoAxisY.setMin(metaRow.get(4).getAsDouble());
                                    // Lat end
                                } else if (metaName.equals("geospatial_lat_max")) {
                                    geoAxisY.setMax(metaRow.get(4).getAsDouble());
                                    // Lat step
                                } else if (metaName.equals("geospatial_lat_resolution")) {
                                    geoAxisY.setDelta(metaRow.get(4).getAsDouble());
                                } else if (metaName.equals("title")) {
                                    dataset.setTitle(metaRow.get(4).getAsString())
                                } else if (metaName.equals("cdm_data_type") ) {
                                    String geo = metaRow.get(4).getAsString().toLowerCase(Locale.ENGLISH)
                                    if ( geo.equals('grid') ) {
                                        dataset.setGeometry(geo)
                                    } else {
                                        log.debug("Rejecting grid type " + geo + " for griddap ERDDAP.")
                                        return null;
                                    }
                                } else if ( metaName.equals("sourceUrl") ) {
                                    sourceUrl = metaRow.get(4).getAsString()
                                    if ( sourceUrl.startsWith("http") ) {
                                        dataset.setHash(getDigest(sourceUrl))
                                        dataset.setUrl(sourceUrl)
                                    }
                                    log.info("Switching to source url of " + sourceUrl);
                                }
                            } else if (metaVar.equals("time")) {
                                String metaName = metaRow.get(2).getAsString();
                                if (metaName.equals("calendar")) {
                                    timeAxis.setCalendar(metaRow.get(4).getAsString());
                                } else if (metaName.equals("units")) {
                                    timeAxis.setUnitsString(metaRow.get(4).getAsString());
                                }
                            } else if (metaVar.equals("longitude")) {
                                String metaName = metaRow.get(2).getAsString();
                                if (metaName.equals("units")) {
                                    geoAxisX.setUnits(metaRow.get(4).getAsString());
                                }
                            } else if (metaVar.equals("latitude")) {
                                String metaName = metaRow.get(2).getAsString();
                                if (metaName.equals("units")) {
                                    geoAxisY.setUnits(metaRow.get(4).getAsString());
                                }
                            } else if (metaVar.equals("depth")) {
                                String metaName = metaRow.get(2).getAsString();
                                if (metaName.equals("units")) {
                                    verticalAxis.setUnits(metaRow.get(4).getAsString());
                                } else if (metaName.equals("positive")) {
                                    verticalAxis.setPositive(metaRow.get(4).getAsString());
                                }
                            } else if (atvar) {
                                String metaName = metaRow.get(2).getAsString();
                                if (metaName.equals("units")) {
                                    atvar.setUnits(metaRow.get(4).getAsString());
                                } else if (metaName.equals("long_name")) {
                                    atvar.setTitle(metaRow.get(4).getAsString());
                                }
                            }
                        } else if (metaType.equals("variable")) {
                            Variable variable = new Variable();
                            variable.setGeometry(dataset.getGeometry())
                            variable.setHash(getDigest(url + "#" + metaRow.get(1).getAsString()));
                            variable.setName(metaRow.get(1).getAsString());
                            // Gets reset if there is a long_name attribute
                            variable.setTitle(metaRow.get(1).getAsString())
                            // Use if available
                            if ( sourceUrl != null ) {
                                variable.setUrl(sourceUrl);
                            } else {
                                variable.setUrl(url);
                            }
                            dataset.addToVariables(variable)
                        }

                    }
                }
            }
        } catch (Exception e ) {
            log.error('Error adding data set: ' + e.getLocalizedMessage())
        }

        if ( dataset.getVariables ( ) ) { // have at least one variable
            String intervals = ""
            if (geoAxisX.getMax()) {
                GeoAxisX vx = new GeoAxisX(geoAxisX.properties)
                dataset.setGeoAxisX(vx)
                vx.setDataset(dataset)
                intervals = intervals + "x"
            }
            if (geoAxisY.getMax()) {
                GeoAxisY vy = new GeoAxisY(geoAxisY.properties)
                dataset.setGeoAxisY(vy)
                vy.setDataset(dataset)
                intervals = intervals + "y"
            }
            if (verticalAxis.getZvalues()) {
                VerticalAxis vv = new VerticalAxis(verticalAxis.properties)
                dataset.setVerticalAxis(vv)
                vv.setDataset(dataset)
                intervals = intervals + "z"
            }
            if (timeAxis.getStart()) {
                TimeAxis vt = new TimeAxis(timeAxis.properties)
                CalendarDateUnit cdu = CalendarDateUnit.of(vt.getCalendar(), vt.getUnitsString())
                dateTimeService.init(vt.getCalendar())
                DateTime t0 = dateTimeService.dateTimeFromIso(vt.getStart())
                DateTime tN = dateTimeService.dateTimeFromIso(vt.getEnd())
                // Bob's times are always "seconds since" so divide milli's by 1000.
                Period pTotal = getPeriod(cdu, (t0.getMillis() / 1000.0d), (tN.getMillis() / 1000.0d))
                vt.setPeriod(pf.print(pTotal))
                vt.setDelta(pf.print(p0))
                dataset.setTimeAxis(vt)
                vt.setDataset(dataset)
                intervals = intervals + "t"
            }

            for (int vit = 0; vit < dataset.getVariables().size(); vit++) {
                Variable variable = (Variable) dataset.getVariables().get(vit)
                variable.setIntervals(intervals)
                variable.setGeometry(GeometryType.GRID)
                variable.setDataset(dataset)
                dataset.variableChildren = true
                if (variable.validate())
                    variable.save(failOnError: true)
            }

            if ( !dataset.getGeoAxisX()  ) {
                log.info("No X-axis for data set " + url)
                return null
            }
            if ( !dataset.getGeoAxisY() ) {
                log.info("No Y-axis for data set " + url)
                return null
            }
            if (dataset.validate()) {
                dataset.setStatus(Dataset.INGEST_FINISHED)
                dataset.save(failOnError: true)
            }
        }
        dataset
    }

    List<Dataset> createFromUAFDataset(thredds.client.catalog.Dataset invDataset, String erddap) {

        List<Dataset> rankDatasets = new ArrayList<Dataset>()
        // Either one with variables or one data sets holding the different rank variables?
        if ( erddap.endsWith("/") ) {
            erddap = erddap.substring(0, erddap.lastIndexOf("/"))
        }
        String searchUafErddap = erddap + "/search/index.json?page=1&itemsPerPage=1000&searchFor=";
        String metadataUafErddap = erddap + "/info/";  // + ID like "noaa_esrl_3ff0_1c43_88d7" + "/index.json"
        if ( invDataset.hasAccess() && invDataset.getAccess(ServiceType.OPENDAP) != null ) {
            String url = invDataset.getAccess(ServiceType.OPENDAP).getStandardUrlName();
            String term = url
            log.debug("Processing UAF THREDDS dataset: " + invDataset.getAccess(ServiceType.OPENDAP).getStandardUrlName() + " from " + invDataset.getParentCatalog().getUriString())



            LASProxy lasProxy = new LASProxy();
            JsonParser jsonParser = new JsonParser();
            String indexJSON = null;
            try {
                log.debug('Search URL is ' + searchUafErddap + term)
                indexJSON = lasProxy.executeGetMethodAndReturnResult(searchUafErddap + term);
            } catch (HttpException e) {
                log.debug("Failed on " + searchUafErddap + url);
            } catch (IOException e) {
                log.debug("Failed on " + searchUafErddap + url);
            }

            if (indexJSON != null) {
                JsonObject indexJO = jsonParser.parse(indexJSON).getAsJsonObject();
                JsonObject table = indexJO.get("table").getAsJsonObject();

                JsonArray names = table.getAsJsonArray("columnNames");
                int idIndex = 0;
                for (int i = 0; i < names.size(); i++) {
                    String name = names.get(i).getAsString();
                    if (name.equals("Dataset ID")) {
                        idIndex = i;
                    }
                }

                JsonArray rows = table.getAsJsonArray("rows");
                log.debug("ERDDAP Dataset from " + url + " has " + rows.size() + " rows.");
                for (int i = 0; i < rows.size(); i++) {
                    // Everything in a row is on the same grid (we'll make it one data set)
                    Dataset dataset = new Dataset()
                    dataset.setHash(getDigest(url+i))
                    TimeAxis timeAxis = new TimeAxis()
                    timeAxis.setName("time")
                    timeAxis.setTitle("Time")
                    Period p0;
                    // Will get set below if attribute exists
                    timeAxis.setCalendar("gregorian")
                    GeoAxisX geoAxisX = new GeoAxisX()
                    geoAxisX.setName("longitude")
                    geoAxisX.setTitle("Longitude")
                    geoAxisX.setType("x")
                    GeoAxisY geoAxisY = new GeoAxisY()
                    geoAxisY.setName("latitude")
                    geoAxisY.setTitle("Latitude")
                    geoAxisY.setType("y")
                    VerticalAxis verticalAxis = new VerticalAxis()
                    verticalAxis.setName("depth")
                    verticalAxis.setTitle("Depth")
                    verticalAxis.setType("z")
                    // May get changed below
                    verticalAxis.setPositive("down")

                    JsonArray first = rows.get(i).getAsJsonArray();
                    JsonElement idE = first.get(idIndex);
                    String erddapDatasetId = idE.getAsString();
                    log.debug("ERDDAP Dataset ID  " + erddapDatasetId + " from " + url + " processing row = " + i);
                    String metadataJSONString;

                    //EREDDAP splits a data source into separate data sets according to the variable's rank.
                    // XYT data sets in one, XYZT in another. And one with just the time axis and one with the time axis and other random time stuff like
                    // the calendar_components variable in http://ferret.pmel.noaa.gov/pmel/thredds/dodsC/ct_flux

                    // The only ones we're interested in have the WMS bit set. If WMS is a possibility then LAS is a possibility.

                    String wms = first.get(4).getAsString();
                    if (wms != null && !wms.isEmpty()) {

                        // TODO deal with z axis
                        try {
                            metadataJSONString = lasProxy.executeGetMethodAndReturnResult(metadataUafErddap + erddapDatasetId + "/index.json");
                            if (metadataJSONString != null) {

                                JsonObject metadata = jsonParser.parse(metadataJSONString).getAsJsonObject();
                                JsonObject metadata_table = metadata.get("table").getAsJsonObject();

                                // Assuming the positions in the array are always the same.
                                // Risky?
                                int typeIndex = 0;

                                // TODO use the title string, or combine data sets on different axes from the same data source.
                                JsonArray metadata_rows = metadata_table.getAsJsonArray("rows");

                                for (int mi = 0; mi < metadata_rows.size(); mi++) {
                                    JsonArray metaRow = metadata_rows.get(mi).getAsJsonArray();
                                    String metaType = metaRow.get(typeIndex).getAsString();
                                    if (metaType.equalsIgnoreCase("dimension")) {
                                        String dimName = metaRow.get(1).getAsString();
                                        // Time size
                                        if (dimName.equals("time")) {
                                            String info = metaRow.get(4).getAsString();
                                            String[] majorParts = info.split(",");
                                            String[] parts = majorParts[0].split("=");
                                            timeAxis.setSize(Long.valueOf(parts[1]).longValue());

                                            // Time step has to be derived from the average spacing which involves parsing out the values and deciding the units to use.
                                            /*
                                    Grab the first one.

                                    if it is days around 30 use it as monthsd
                                    if it is days less than 27 use days
                                    if it is hours use hours

                                     */


                                            int size = (int)(timeAxis.getSize())
                                            if (size > 1) {
                                                String[] deltaParts = majorParts[2].split("=");
                                                String[] timeParts = deltaParts[1].split(" ");
                                                if (timeParts[0].contains("infinity")) {
                                                    log.debug("Problem with the time axis.")
                                                    return null;
                                                } else if (timeParts[1].contains("year")) {
                                                    timeAxis.setUnits("year");
                                                    // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                    p0 = new Period(1, 0, 0, 0, 0, 0, 0, 0)
                                                } else if (timeParts[1].contains("days")) {
                                                    // Make a number out of the days;
                                                    int days = Integer.valueOf(timeParts[0]).intValue();
                                                    if (days < 27) {
                                                        timeAxis.setUnits("day");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(0, 0, 0, days, 0, 0, 0, 0)
                                                    } else if (days >= 27 && days < 33) {
                                                        timeAxis.setUnits("month");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(0, 1, 0, 0, 0, 0, 0, 0)
                                                    } else if (days >= 88 && days < 93) {
                                                        timeAxis.setUnits("month");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(0, 3, 0, 0, 0, 0, 0, 0)
                                                    } else if (days >= 175 && days < 188) {
                                                        timeAxis.setUnits("month");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(0, 6, 0, 0, 0, 0, 0, 0)
                                                    } else if (days > 357) {
                                                        timeAxis.setUnits("year");
                                                        // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                        p0 = new Period(1, 0, 0, 0, 0, 0, 0, 0)
                                                    }

                                                } else if (timeParts[0].contains("h")) {
                                                    String step = timeParts[0].replace("h", "");
                                                    timeAxis.setUnits("hour");
                                                    // Period(int years, int months, int weeks, int days, int hours, int minutes, int seconds, int millis)
                                                    p0 = new Period(0, 0, 0, 0, Integer.valueOf(step).intValue(), 0, 0, 0)
                                                }
                                            } else {
                                                String[] valueParts = majorParts[1].split("=");
                                                NameValuePair nv = new NameValuePair()
                                                nv.setName(valueParts[1])
                                                nv.setValue(valueParts[1])
                                                timeAxis.addToNameValuePairs(nv);
                                                timeAxis.setUnits("none")
                                                p0 = new Period(1, 0, 0, 0, 0, 0, 0, 0)
                                                timeAxis.setDelta(pf.print(p0))
                                            }
                                            // Lon size
                                        } else if (dimName.equals("longitude")) {
                                            // Found a lon axis
                                            String info = metaRow.get(4).getAsString();
                                            String[] majorParts = info.split(",");
                                            String[] parts = majorParts[0].split("=");
                                            geoAxisX.setSize(Long.valueOf(parts[1]).longValue());
                                            if (majorParts.length == 3) {
                                                parts = majorParts[2].split("=");
                                                geoAxisX.setDelta(Double.valueOf(parts[1]).doubleValue());
                                            }
                                            //
                                            // Lat size
                                        } else if (dimName.equals("latitude")) {
                                            String info = metaRow.get(4).getAsString();
                                            String[] majorParts = info.split(",");
                                            String[] parts = majorParts[0].split("=");
                                            geoAxisY.setSize(Long.valueOf(parts[1]).longValue());
                                            if (majorParts.length == 3) {
                                                parts = majorParts[2].split("=");
                                                geoAxisY.setDelta(Double.valueOf(parts[1]).doubleValue());
                                            }
                                        } else if (!dimName.equals("latitude") && dimName.equals("longitude") &&
                                                !dimName.equals("time")) {
                                            String zstring = lasProxy.executeGetMethodAndReturnResult(erddap + "/griddap/" + erddapDatasetId + ".json?" + dimName);
                                            JsonObject zobject = jsonParser.parse(zstring).getAsJsonObject();
                                            JsonObject ztable = zobject.get("table").getAsJsonObject();
                                            JsonArray zarray = ztable.getAsJsonArray("rows");

                                            for (int zi = 0; zi < zarray.size(); zi++) {
                                                // TODO positive, regular, delta, etc maybe we don't care
                                                Zvalue zValue = new Zvalue();
                                                zValue.setZ(zarray.get(zi).getAsDouble())
                                                verticalAxis.addToZvalues(zValue)
                                            }
                                        }
                                    } else if (metaType.equalsIgnoreCase("attribute")) {
                                        String metaVar = metaRow.get(1).getAsString();
                                        // See if it's a attribute for a variable. Guaranteed to have encountered the variable before its attributes.
                                        Variable atvar
                                        if (dataset.variables) {
                                            atvar = (Variable) dataset.variables.find { Variable variable ->
                                                variable.getName().equals(metaVar);
                                            }
                                        }
                                        if (metaVar.equals("NC_GLOBAL")) {
                                            String metaName = metaRow.get(2).getAsString();
                                            // Time start
                                            if (metaName.equals("time_coverage_start")) {
                                                timeAxis.setStart(metaRow.get(4).getAsString());
                                                if ( timeAxis.getStart().contains("0000") ) {
                                                    timeAxis.setClimatology(true)
                                                } else {
                                                    timeAxis.setClimatology(false)

                                                }
                                                // Time end
                                            } else if (metaName.equals("time_coverage_end")) {
                                                timeAxis.setEnd(metaRow.get(4).getAsString());
                                                // Lon start
                                            } else if (metaName.equals("Westernmost_Easting")) {
                                                geoAxisX.setMin(metaRow.get(4).getAsDouble());
                                                // Lon end
                                            } else if (metaName.equals("Easternmost_Easting")) {
                                                geoAxisX.setMax(metaRow.get(4).getAsDouble());
                                                // Lon step
                                            } else if (metaName.equals("geospatial_lon_resolution")) {
                                                geoAxisX.setDelta(metaRow.get(4).getAsDouble());
                                                // Lat start
                                            } else if (metaName.equals("geospatial_lat_min")) {
                                                geoAxisY.setMin(metaRow.get(4).getAsDouble());
                                                // Lat end
                                            } else if (metaName.equals("geospatial_lat_max")) {
                                                geoAxisY.setMax(metaRow.get(4).getAsDouble());
                                                // Lat step
                                            } else if (metaName.equals("geospatial_lat_resolution")) {
                                                geoAxisY.setDelta(metaRow.get(4).getAsDouble());
                                            } else if (metaName.equals("title")) {
                                                dataset.setTitle(metaRow.get(4).getAsString())
                                            }
                                        } else if ( metaVar.equals("time") ) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("calendar")) {
                                                timeAxis.setCalendar(metaRow.get(4).getAsString());
                                            } else if (metaName.equals("units")) {
                                                timeAxis.setUnitsString(metaRow.get(4).getAsString());
                                            }
                                        } else if (metaVar.equals("longitude")) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("units")) {
                                                geoAxisX.setUnits(metaRow.get(4).getAsString());
                                            }
                                        } else if (metaVar.equals("latitude")) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("units")) {
                                                geoAxisY.setUnits(metaRow.get(4).getAsString());
                                            }
                                        } else if (metaVar.equals("depth")) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("units")) {
                                                verticalAxis.setUnits(metaRow.get(4).getAsString());
                                            } else if ( metaName.equals("positive") ) {
                                                verticalAxis.setPositive(metaRow.get(4).getAsString());
                                            }
                                        } else if (atvar) {
                                            String metaName = metaRow.get(2).getAsString();
                                            if (metaName.equals("units")) {
                                                atvar.setUnits(metaRow.get(4).getAsString());
                                            } else if (metaName.equals("long_name")) {
                                                atvar.setTitle(metaRow.get(4).getAsString());
                                            }
                                        }
                                    } else if (metaType.equals("variable")) {
                                        Variable variable = new Variable();
                                        variable.setHash(getDigest(url + "#" + metaRow.get(1).getAsString()));
                                        variable.setName(metaRow.get(1).getAsString());
                                        // Gets reset if there is a long_name attribute
                                        variable.setTitle(metaRow.get(1).getAsString())
                                        variable.setUrl(url + "#" + metaRow.get(1).getAsString());
                                        dataset.addToVariables(variable)
                                    }

                                }

                            }
                        } catch (Exception e) {
                            log.error(e.getLocalizedMessage())
                        }
                    }
                    if ( dataset.getVariables() ) { // have at least one variable
                        String intervals = ""
                        if (geoAxisX.getMax()) {
                            GeoAxisX vx = new GeoAxisX(geoAxisX.properties)
                            dataset.setGeoAxisX(vx)
                            vx.setDataset(dataset)
                            intervals = intervals + "x"
                        }
                        if (geoAxisY.getMax()) {
                            GeoAxisY vy = new GeoAxisY(geoAxisY.properties)
                            dataset.setGeoAxisY(vy)
                            vy.setDataset(dataset)
                            intervals = intervals + "y"
                        }
                        if (verticalAxis.getZvalues()) {
                            VerticalAxis vv = new VerticalAxis(verticalAxis.properties)
                            dataset.setVerticalAxis(vv)
                            vv.setDataset(dataset)
                            intervals = intervals + "z"
                        }
                        if (timeAxis.getStart()) {
                            TimeAxis vt = new TimeAxis(timeAxis.properties)
                            CalendarDateUnit cdu = CalendarDateUnit.of(vt.getCalendar(), vt.getUnitsString())
                            DateTime t0 = dateTimeService.dateTimeFromIso(vt.getStart())
                            DateTime tN = dateTimeService.dateTimeFromIso(vt.getEnd())
                            // Bob's times are always "seconds since" so divide milli's by 1000.
                            Period pTotal = getPeriod(cdu, (t0.getMillis() / 1000.0d), (tN.getMillis() / 1000.0d))
                            vt.setPeriod(pf.print(pTotal))
                            vt.setDelta(pf.print(p0))
                            dataset.setTimeAxis(vt)
                            vt.setDataset(dataset)
                            intervals = intervals + "t"
                        }

                        for (int vit = 0; vit < dataset.getVariables().size(); vit++ ) {
                            Variable variable = dataset.getVariables().get(vit)
                            variable.setIntervals(intervals)
                            variable.setGeometry(GeometryType.GRID)
                            variable.setDataset(dataset)
                            dataset.variableChildren = true
                            if (variable.validate())
                                variable.save(failOnError: true)
                        }
                        if (dataset.validate()) {
                            dataset.setStatus(Dataset.INGEST_FINISHED)
                            dataset.save(failOnError: true)
                            rankDatasets.add(dataset)
                        }
                    }
                }
            }
        }
        rankDatasets
    }

    private String fixName(String url) {
        def main
        try {
            URIBuilder builder = new URIBuilder(url)
            String host = builder.getHost()
            def parts = host.tokenize(".")
            main = main + host
            if (parts.size() >= 3) {
                main = "TDS Data from " + parts.get(parts.size() - 3) + "." + parts.get(parts.size() - 2) + "." + parts.get(parts.size() - 1)
            }
        } catch (Exception e) {
            main = "TDS Data"
        }
        main
    }

    Dataset createDatasetFromCatalog(Catalog catalog, String parentHash, boolean full) {
        Dataset dataset = new Dataset()
        if (catalog.getName()) {
            if (catalog.getName().toLowerCase().contains("you must change")) {
                String name = fixName(catalog.getUriString())
                dataset.setTitle(name)
            } else {
                dataset.setTitle(catalog.getName())
            }
        } else {
            String name = fixName(catalog.getUriString())
            dataset.setTitle(name)
        }
        dataset.setUrl(catalog.getUriString())
        dataset.setHash(getDigest(catalog.getUriString()))

        List<thredds.client.catalog.Dataset> children = catalog.getDatasetsLogical();

        // TODO getFullName ???
        if (children.size() == 2 && !children.get(0).hasAccess() && children.get(1).getName().toLowerCase().contains("rubric")) {
            thredds.client.catalog.Dataset onlyChild = children.get(0);
            String name = onlyChild.getName();
            if (name == null) {
                name = "TDS Data"
            }
            if (!name.toLowerCase().contains("tds quality") && !name.contains("automated cleaning process") && !name.contains("An error occurred processing")) {
                dataset.setTitle(name)
                children = onlyChild.getDatasetsLogical()
            }
        }
        for (int i = 0; i < children.size(); i++) {

            thredds.client.catalog.Dataset nextChild = (thredds.client.catalog.Dataset) children.get(i)
            if (!nextChild.getName().toLowerCase().contains("tds quality") &&
                    !nextChild.getName().contains("automated cleaning process") &&
                    !nextChild.getName().contains("An error occurred processing")) {
                log.debug(nextChild.getName() + ' name approved. Processing.')
                Dataset child = processDataset(nextChild, parentHash, full, dataset)
                if (child && (child.variableChildren || (child.getDatasets() && child.getDatasets().size() > 0))) {
                    dataset.addToDatasets(child)
                }
            }
        }
        dataset
    }

    Dataset processDataset(thredds.client.catalog.Dataset invDataset, String parentHash, boolean full, Dataset parent) {

        List<thredds.client.catalog.Dataset> invDatasetList = new LinkedList<thredds.client.catalog.Dataset>(invDataset.getDatasetsLogical());
        List<thredds.client.catalog.Dataset> remove = new ArrayList<>()
        boolean access = false;
        if (invDataset.hasAccess()) {
            access = invDataset.getAccess(ServiceType.OPENDAP) != null;
        }
        for (int i = 0; i < invDatasetList.size(); i++) {
            thredds.client.catalog.Dataset child = invDatasetList.get(i)
            if (child.access) {
                Access a = child.getAccess(ServiceType.OPENDAP)
                if (a != null) {
                    access = true
                }
            }
            if (child.getName().contains("automated cleaning process") || child.getName().toLowerCase().contains("tds quality") || child.getName().contains("An error occurred processing")) {
                remove.add(child)
            }
        }
        for (int i = 0; i < remove.size(); i++) {
            invDatasetList.remove(remove.get(i))
        }

        // FIXME
        String title = invDataset.getName()
        if (title.toLowerCase().contains("you must change")) {
            title = fixName(invDataset.getCatalogUrl())
        }
        Dataset saveToDataset = new Dataset(title: "Failed reading data set", url: "http://", hash: getDigest(Math.random().toString()), variableChildren: false)
        Dataset d = saveToDataset
        try {
            d = new Dataset(title: title, url: invDataset.getCatalogUrl(), hash: getDigest(invDataset.getCatalogUrl()), variableChildren: false)
            saveToDataset = d
            if ((invDatasetList.size() == 1 && !access) ||
                    (invDatasetList.size() > 0 && invDataset.getName().equals(invDatasetList.get(0).getName())) ||
                    (invDatasetList.size() > 0 && invDataset.getName().toLowerCase().contains("top dataset"))) {
                // If skipping, save next level to parent
                saveToDataset = parent
                // And refuse the current
                d = null;
            } else {
                if (invDataset.hasAccess() && invDataset.getAccess(ServiceType.OPENDAP) != null) {
                    if (full) {
                        try {
                            d = ingest(parentHash, invDataset.getAccess(ServiceType.OPENDAP).getStandardUrlName())
                        } catch (Exception e) {
                            log.debug("We failed..." + e.getMessage())
                        }
                        d.variableChildren = true
                        d.geometry = GeometryType.GRID
                        if (d.getStatus() != Dataset.INGEST_FAILED)
                            d.setStatus(Dataset.INGEST_FINISHED)
                    } else {
                        d.variableChildren = true
                        d.geometry = GeometryType.GRID
                        d.setStatus(Dataset.INGEST_NOT_STARTED)
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Exception processing data set. Message = " + e.getMessage() + " Trying to go on with nested data ses.")
        }
        List<thredds.client.catalog.Dataset> kids = invDataset.getDatasetsLogical();
        for (int i = 0; i < kids.size(); i++) {
            thredds.client.catalog.Dataset kid = kids.get(i)
            if (!kid.getName().toLowerCase().contains("quality rubric") && !kid.getName().contains("automated cleaning process")) {
                Dataset dkid = processDataset(kid, parentHash, full, saveToDataset)
                if (dkid != null) {
                    if (dkid.variableChildren || (dkid.getDatasets() && dkid.getDatasets().size() > 0)) {
                        saveToDataset.addToDatasets(dkid)
                    }
                }
            }
        }
        d
    }

    Dataset ingestAllFromErddap(String url, List<AddProperty> ingestProperties) {


        def search = "?page=1&itemsPerPage=1000"
        url = url.replace(search, "");
        if (url.endsWith("index.html")) url = url.replace("index.html", "")
        if (!url.endsWith("tabledap") && !url.endsWith("tabledap/")) {
            log.warn("Expecting a url of the form https://host.gov/erddap/tabledap/ trying with /tabledap appended.")
            if (url.endsWith("erddap")) {
                url = url + "/tabledap/"
            } else if (url.endsWith("erddap/")) {
                url = url + "tabledap/"
            }
        }
        if (!url.endsWith("/")) {
            url = url + "/"
        }

        Dataset dsg = new Dataset([title: "Discrete Geometry Data", url: url, hash: getDigest(url)])
        def tsurl = url.substring(0, url.indexOf("erddap/")) + "erddap/categorize/cdm_data_type/timeseries/"
        Dataset timeseries = new Dataset([title: "Timeseries Data", url: tsurl, hash: getDigest(tsurl)])
        def trurl = url.substring(0, url.indexOf("erddap/")) + "erddap/categorize/cdm_data_type/trajectory/"
        Dataset trajectories = new Dataset([title: "Trajectory Data", url: trurl, hash: getDigest(trurl)])
        def trpurl = url.substring(0, url.indexOf("erddap/")) + "erddap/categorize/cdm_data_type/trajectoryprofile/"
        Dataset trajectorieprofiles = new Dataset([title: "Trajectory Profile Data", url: trurl, hash: getDigest(trurl)])
        def pturl = url.substring(0, url.indexOf("erddap/")) + "erddap/categorize/cdm_data_type/point/"
        Dataset point = new Dataset([title: "Point Data", url: pturl, hash: getDigest(pturl)])
        def prurl = url.substring(0, url.indexOf("erddap/")) + "erddap/categorize/cdm_data_type/profile/"
        Dataset profile = new Dataset([title: "Profile Data", url: prurl, hash: getDigest(prurl)])
        log.info("Processing table dap data sets from " + url)
        def tabledap = url;
        url = url + "index.json"
        InputStream stream = null;
        try {
            stream = lasProxy.executeGetMethodAndReturnStream(url, null);
        } catch (HttpException e) {
            System.err.println("DAS processing error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("DAS processing error: " + e.getMessage());
        }
        if (stream != null) {
            InputStreamReader reader = new InputStreamReader(stream);
            JsonStreamParser jp = new JsonStreamParser(reader);
            JsonObject tabledap_list = (JsonObject) jp.next();
            JsonArray rows = (JsonArray) ((JsonObject) (tabledap_list.get("table"))).get("rows");

            int total = 0;
            int limit = rows.size();
            int dscount = limit - 1
            log.debug(dscount + " datasets availble to be ingested.")

            // The first one is a listing of all data sets, not the first tabledap data set.
            for (int i = 1; i < limit; i++) {
                total++;
                JsonArray row = (JsonArray) rows.get(i);
                String fullurl = row.get(2).getAsString();
                String title = row.get(7).getAsString();

//                String u = fullurl.substring(0, fullurl.lastIndexOf("/"));
//                String uid = fullurl.substring(fullurl.lastIndexOf("/") + 1);

                Dataset tabledapItem = ingestFromErddap_using_json(fullurl, ingestProperties);
                def placed = null;
                if (tabledapItem) {
                    // First check if there is a container data set with the tabledap URL
                    Dataset d = Dataset.findByUrl(tabledap)
                    if (d) {
                        // The top level may contain other containers. One must travel down the hierarchy until the
                        // matching data set contains only data sets with variable children.
                        List<Dataset> children = d.getDatasets();
                        placed = placeDataset(null, tabledapItem, children)
                        if (placed) {
                            placed.save(flush: true)
                            d.save(flush: true)
                        } else {
                            if (tabledapItem.getGeometry() == GeometryType.TIMESERIES) {
                                timeseries.addToDatasets(tabledapItem)
                            } else if (tabledapItem.getGeometry() == GeometryType.TRAJECTORY) {
                                trajectories.addToDatasets(tabledapItem)
                            } else if (tabledapItem.getGeometry() == GeometryType.POINT) {
                                point.addToDatasets(tabledapItem)
                            } else if (tabledapItem.getGeometry() == GeometryType.PROFILE) {
                                profile.addToDatasets(tabledapItem)
                            } else if (tabledapItem.getGeometry() == GeometryType.TRAJECTORY_PROFILE) {
                                trajectorieprofiles.addToDatasets(tabledapItem)
                            }
                        }
                        // If not, just use the DSG type
                    } else {
                        if (tabledapItem.getGeometry() == GeometryType.TIMESERIES) {
                            timeseries.addToDatasets(tabledapItem)
                        } else if (tabledapItem.getGeometry() == GeometryType.TRAJECTORY) {
                            trajectories.addToDatasets(tabledapItem)
                        } else if (tabledapItem.getGeometry() == GeometryType.POINT) {
                            point.addToDatasets(tabledapItem)
                        } else if (tabledapItem.getGeometry() == GeometryType.PROFILE) {
                            profile.addToDatasets(tabledapItem)
                        } else if (tabledapItem.getGeometry() == GeometryType.TRAJECTORY_PROFILE) {
                            trajectorieprofiles.addToDatasets(tabledapItem)
                        }
                    }
                }
            }
            if (timeseries.datasets && timeseries.datasets.size() > 0) {
                dsg.addToDatasets(timeseries)
            }
            if (trajectories.datasets && trajectories.datasets.size() > 0) {
                dsg.addToDatasets(trajectories)
            }
            if (point.datasets && point.datasets.size() > 0) {
                dsg.addToDatasets(point)
            }
            if (profile.datasets && profile.datasets.size() > 0) {
                dsg.addToDatasets(profile)
            }
            if (trajectorieprofiles.datasets && trajectorieprofiles.datasets.size() > 0) {
                dsg.addToDatasets(trajectorieprofiles)
            }
        }
        dsg
    }

    Dataset placeDataset(Dataset incoming_parent, Dataset incoming, List<Dataset> containers) {
        for (int i = 0; i < containers.size(); i++) {
            Dataset parent = containers.get(i)
            if (incoming.getTitle().contains(parent.getTitle())) {
                // It's a match. It either goes here of in one of the children
                List<Dataset> children = parent.getDatasets()
                if (children.size() == 0) {
                    parent.addToDatasets(incoming)
                    incoming_parent = parent;
                } else {
                    Dataset child = children.get(0);
                    if (child.getVariableChildren()) {
                        parent.addToDatasets(incoming)
                        incoming_parent = parent
                    } else {
                        return placeDataset(incoming_parent, incoming, children)
                    }
                }
            }
        }
        return incoming_parent;
    }

    Dataset ingestFromErddap_using_json(String url, List<AddProperty> properties) {


        def dsg_url = url;

        // For now if the metadata is missing we're just going to punt for now and return a null.

        if (!url.endsWith("/")) {
            url = url + "/"
        }
        url = url + "index.json"
        url = url.replace("tabledap", "info")
        String json = lasProxy.executeGetMethodAndReturnResult(url)
        JsonMetadata metadata
        if (json) {
            metadata = new JsonMetadata(json)
        } else {
            return null;
        }

        List<String> subsetNames = new ArrayList<String>()

        boolean auto_display = false

        AddProperty auto = properties.find { it.name == "auto_display" }

        if (auto) auto_display = true

        AddProperty plots = properties.find { it.name == "mapandplot" }

        def default_supplied = false
        def default_value

        if (plots) {
            default_supplied = true
            default_value = plots.getValue()
        }

        AddProperty hour
        if (properties) {
            hour = properties.find { it.name == "hours" }
        } else {
            hour = new AddProperty([name: "hour", value: "1"])
        }
        def hours_value = null
        double hours_step
        if (hour) {
            hours_value = hour.getValue()
            hours_step = Double.valueOf(hours_value)
        }


        def axesToSkip = []

        def id = url.substring(url.lastIndexOf("/") + 1, url.length() - 1)

        int timeout = 400

        def hash = getDigest(dsg_url)

        Dataset dataset = new Dataset([url: dsg_url, hash: hash])

        DateTime date = new DateTime()
        log.info("Processing: " + url + " at " + date.toString())

        String cdm_trajectory_variables = metadata.getAttributeValue(NC_GLOBAL, TRAJECTORY)
        String cdm_profile_variables = metadata.getAttributeValue(NC_GLOBAL, PROFILE)
        String cdm_timeseries_variables = metadata.getAttributeValue(NC_GLOBAL, TIMESERIES)
        String cdm_data_type = metadata.getAttributeValue(NC_GLOBAL, "cdm_data_type")
        String altitude_proxy = metadata.getAttributeValue(NC_GLOBAL, "altitude_proxy")
        String grid_type = cdm_data_type.toLowerCase(Locale.ENGLISH)

        log.debug("This data set has cdm_data_type of " + cdm_data_type)
        if (cdm_trajectory_variables && !cdm_profile_variables) log.debug("This trajectory id is a " + cdm_trajectory_variables)
        if (!cdm_trajectory_variables && cdm_profile_variables) log.debug("The profile id is a " + cdm_profile_variables)
        if (cdm_trajectory_variables && cdm_profile_variables) log.debug("This trajectory and profile ids are " + cdm_trajectory_variables + " and " + cdm_profile_variables)
        if (cdm_timeseries_variables) log.debug("The timeseries id is  " + cdm_timeseries_variables)
        if (!cdm_trajectory_variables && cdm_profile_variables) log.debug("This is a " + GeometryType.PROFILE)

        String title = metadata.getAttributeValue(NC_GLOBAL, "title")
        if (!title) {
            title = metadata.getAttributeValue("dataset_title")
        }
        if (!title) {
            title = "Data set from " + url
        }

        dataset.setTitle(title)

        String subset_names = null
        if (cdm_trajectory_variables && cdm_profile_variables) {
            dataset.setGeometry(GeometryType.TRAJECTORY_PROFILE)
        } else if (cdm_timeseries_variables && cdm_profile_variables) {
            dataset.setGeometry(GeometryType.TIMESERIES_PROFILE)
        } else if (cdm_trajectory_variables) {
            subset_names = cdm_trajectory_variables
            dataset.setGeometry(GeometryType.TRAJECTORY)
        } else if (cdm_profile_variables) {
            subset_names = cdm_profile_variables
            dataset.setGeometry(GeometryType.PROFILE)
        } else if (cdm_timeseries_variables) {
            subset_names = cdm_timeseries_variables
            dataset.setGeometry(GeometryType.TIMESERIES)
        } else if (grid_type.equalsIgnoreCase(CdmDatatype.POINT)) {
            subset_names = null
            dataset.setGeometry(GeometryType.POINT)
        }

        if (subset_names) {
            subset_names.tokenize(",").each {
                subsetNames.add(it.trim())
            }
        }

        def trajectoryId
        def profileId
        def dsgid
        if (dataset.getGeometry().equals(GeometryType.TRAJECTORY_PROFILE)) {
            trajectoryId = metadata.getVariableWithCf_role("trajectory_id")
            profileId = metadata.getVariableWithCf_role("profile_id")
            log.debug("Setting trajectory id = " + trajectoryId + " and profile_id = " + profileId + " for trajectory profile data set.")
        } else {
            dsgid = metadata.getVariableWithCf_role()
            log.debug("id var = " + dsgid)
        }
        def id_var_list = []
        if (trajectoryId) {
            id_var_list.push(trajectoryId)
            dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "trajectory_id", value: trajectoryId]))

        }
        if (profileId) {
            id_var_list.push(profileId)
            dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "profile_id", value: profileId]))
        }
        if (dsgid) id_var_list.push(dsgid)

        String timeVar = metadata.getAssociatedVariable("_CoordinateAxisType", "time")
        String latVar = metadata.getAssociatedVariable("_CoordinateAxisType", "lat")
        String lonVar = metadata.getAssociatedVariable("_CoordinateAxisType", "lon")
        String zVar = metadata.getAssociatedVariable("_CoordinateAxisType", "height")

        log.debug("time var = " + timeVar + ", lat var = " + latVar + ", lon var = " + lonVar + ", z var = " + zVar)

        for (int i = 0; i < subsetNames.size(); i++) {
            log.debug("Subset variable " + subsetNames.get(i) + " found.")
        }
        List<String> dataVariables = metadata.getVariables()
        dataVariables.remove(timeVar)
        dataVariables.removeAll(subsetNames)
        dataVariables.remove(latVar)
        dataVariables.remove(lonVar)
        subsetNames.remove(lonVar)
        subsetNames.remove(latVar)
        if (zVar) dataVariables.remove(zVar)
        if ( dataVariables.size() == 0 ) {
            log.info("No data variables found in " + url)
            dataset.setStatus(Dataset.INGEST_FAILED)
            dataset.setMessage("No data variables found.")
            return dataset;
        }
        dataVariables.each {
            log.debug("Data variable found " + it)
        }

        TimeAxis timeAxis = new TimeAxis()
        GeoAxisX geoAxisX = new GeoAxisX()
        geoAxisX.setType("x")
        GeoAxisY geoAxisY = new GeoAxisY()
        geoAxisY.setType("y")
        VerticalAxis zAxis = new VerticalAxis()
        zAxis.setType("z")

        if (!axesToSkip.contains("t") && timeVar) {
            timeAxis.setName(timeVar)
            timeAxis.setTitle("Time")

            String deltaT = metadata.getAttributeValue(NC_GLOBAL, "time_coverage_resolution")
            if (deltaT) {
                timeAxis.setDelta(deltaT)
            } else {
                timeAxis.setDelta("P1D")
            }
            String start = metadata.getAttributeValue(NC_GLOBAL, "time_coverage_start")
            if (start) {
                timeAxis.setStart(start)
            } else {
                log.debug("Rejected " + ". Not time start.")
                return null
            }
            String end = metadata.getAttributeValue(NC_GLOBAL, "time_coverage_end")
            if (end) {
                timeAxis.setEnd(end)
            } else {
                log.debug("Rejected " + url + ". Not time end.")
                return null
            }

            DatasetProperty tp = new DatasetProperty([type: "tabledap_access", name: "time", value: timeVar])
            dataset.addToDatasetProperties(tp)

            String calendar = metadata.getAttributeValue(timeVar, "calendar")
            if (calendar) {
                timeAxis.setCalendar(calendar)
            } else {
                timeAxis.setCalendar("proleptic_gregorian")
            }


            // TODO pass in the whehter data are to be minutes
            def minutes = false
            def hours = false

            if (hours_value) {
                hours = true
            }

            if (minutes) {
                timeAxis.setUnits("minutes")
            } else if (hours) {
                timeAxis.setUnits("hours")
            } else {
                timeAxis.setUnits("days")
            }
            String units = metadata.getAttributeValue(timeVar, "units")
            if (units) {
                DatasetProperty tu = new DatasetProperty([type: "tabledap_access", name: "time_units", value: units])
                dataset.addToDatasetProperties(tu)
            }
            timeAxis.setUnitsString(units)


            Chronology chrono = GregorianChronology.getInstance(DateTimeZone.UTC)
            DateTimeFormatter iso = ISODateTimeFormat.dateTimeParser().withChronology(chrono).withZone(DateTimeZone.UTC)

            DateTime dtstart = iso.parseDateTime(start)
            DateTime dtend = iso.parseDateTime(end)

            int days = Days.daysBetween(dtstart.withTimeAtStartOfDay(), dtend.withTimeAtStartOfDay()).getDays()
            Period span = new Period(0, 0, 0, days, 0, 0, 0, 0)
            timeAxis.setPeriod(pf.print(span))
            timeAxis.setPosition("middle")

            if (hours || minutes) {
                timeAxis.setStart(hoursfmt.print(dtstart))
            } else {
                timeAxis.setStart(hoursfmt.print(dtstart.withTimeAtStartOfDay()))
            }

            AddProperty dlo = properties.find { it.name == "display_lo" }
            if (dlo) timeAxis.setDisplay_lo(dlo.getValue())
            AddProperty dhi = properties.find { it.name == "display_hi" }
            if (dhi) timeAxis.setDisplay_hi(dhi.getValue())

            // Fudge
            days = days + 1
            Period period;
            if (minutes) {
                // Days are now minutes :-)
                days = days * 24 * 60
                period = new Period(0, 0, 0, 0, 0, days, 0, 0)
            } else if (hours) {
                // Days are now hours :-)
                days = (int) (days * 24 * Math.rint(1.0d / hours_step))
                period = new Period(0, 0, 0, 0, days, 0, 0, 0)
            } else {
                period = new Period(0, 0, 0, days, 0, 0, 0, 0)
            }

            if (hours_value) {
                if (hours_step < 1) {
                    int deltamin = (int) (60 * hours_step)
                    Period delta = new Period(0, 0, 0, 0, 0, deltamin, 0, 0)
                    timeAxis.setDelta(pf.print(delta))
                } else {
                    Period delta = new Period(0, 0, 0, 0, (int) hours_step, 0, 0, 0)
                    timeAxis.setDelta(pf.print(delta))
                }

            } else {
                Period delta = new Period(0, 0, 0, 0, 1, 0, 0, 0)
                timeAxis.setDelta(pf.print(delta))
            }
            timeAxis.setPeriod(pf.print(period))
            timeAxis.setStart(start)
            timeAxis.setEnd(end)
            long size = (long) period.hours
            // TODO what to do. Number of obs really.
            timeAxis.setSize(size)

            // This should be set when scanning a catalog...
            if (auto_display) {
                timeAxis.setDisplay_lo(mediumFerretForm.print(dtstart))
                timeAxis.setDisplay_hi(mediumFerretForm.print(dtstart.plusDays(14)))
            }

        }


        if (!axesToSkip.contains("x") && lonVar) {

            geoAxisX.setName(lonVar)
            DatasetProperty xp = new DatasetProperty([type: "tabledap_access", name: "longitude", value: lonVar])
            dataset.addToDatasetProperties(xp)
            String units = metadata.getAttributeValue(lonVar, "units")
            if (units != null) {
                geoAxisX.setUnits(units)
            }
            String lon_long_name = metadata.getAttributeValue(lonVar, "long_name")
            if (lon_long_name != null) {
                geoAxisX.setTitle(lon_long_name)
            } else {
                geoAxisX.setTitle("Longitude")
            }


            String start = metadata.getAttributeValue(NC_GLOBAL, "geospatial_lon_min")
            String end = metadata.getAttributeValue(NC_GLOBAL, "geospatial_lon_max")

            if (!start || !end) {
                log.debug("Rejected " + url + " for no lon start/end metadata.")
                return null
            }
            double dmin = -180.0d
            double dmax = 180.0d
            DatasetProperty lonDomain
            if (Math.abs(Double.valueOf(start)) > 180.0d || Math.abs(Double.valueOf(end)) > 180.0d) {
                lonDomain = new DatasetProperty([type: "tabledap_access", name: "lon_domain", value: "0:360"])
                dmin = 0.0d
                dmax = 360.0d
            } else {
                lonDomain = new DatasetProperty([type: "tabledap_access", name: "lon_domain", value: "-180:180"])
            }
            dataset.addToDatasetProperties(lonDomain)
            double dstart = Double.valueOf(start)
            double dend = Double.valueOf(end)
            double size = dend - dstart

            // Fudge it up if the interval is really small...

            long fsize = 3l
            if (size < 355.0) {
                double fudge = size * 0.15
                if (size < 3.0d) {
                    fudge = 0.25
                }
                dstart = dstart - fudge

                if (dstart < dmin) {
                    dstart = dmin
                }
                dend = dend + fudge
                if (dend > dmax) {
                    dend = dmax
                }

                double c = Math.ceil(dend - dstart)
                fsize = (long) c + 1
            }
            double step = (dend - dstart) / (Double.valueOf(fsize) - 1.0d)

            // Maybe we prefer lon360 values because of the wrap...
            def range = metadata.getAttributeValue("lon360", "actual_range")
            if (range) {
                // I suspect if lon360 exists we should use it
                def parts = range.split(",")
                def min = Double.valueOf(parts[0].trim()).doubleValue()
                def max = Double.valueOf(parts[1].trim()).doubleValue()
                if ((min > 2.5 && min < 180 && max < 357.5 && max > 180) || (min >= 180.0d && max >= 180.0d)) {
                    dstart = min
                    dend = max
                }

                def r = dend - dstart
                def fudge = r * 0.15d

                if ((dstart - fudge) > 0.0d) {
                    dstart = dstart - fudge
                } else {
                    dstart = 0.0d
                }
                if ((dend + fudge) < 360.0d) {
                    dend = dend + fudge
                } else {
                    dend = 360.0d
                }
            }
            // Since there is no grid, there is no size.
            // and there is no delta, just just use some
            // values that will make the map happy.
            geoAxisX.setSize(fsize)
            geoAxisX.setMin(dstart)
            geoAxisX.setMax(dend)
            geoAxisX.setDelta(1.0d)

        }
        if (!axesToSkip.contains("y") && latVar) {

            geoAxisY.setName(latVar)
            DatasetProperty latName = new DatasetProperty([type: "tabledap_access", name: "latitude", value: latVar])
            dataset.addToDatasetProperties(latName)
            String units = metadata.getAttributeValue(latVar, "units")
            if (units) {
                geoAxisY.setUnits(units)
            } else {
                geoAxisX.setUnits("none")
            }
            String lat_long_name = metadata.getAttributeValue(latVar, "long_name")
            if (lat_long_name) {
                geoAxisY.setTitle(lat_long_name)
            } else {
                geoAxisY.setTitle("Latitude")
            }
            String start = metadata.getAttributeValue(NC_GLOBAL, "geospatial_lat_min")
            String end = metadata.getAttributeValue(NC_GLOBAL, "geospatial_lat_max")
            if (!start || !end) {
                log.debug("Rejected " + url + " for no lat start/end metadata.")
                return null
            }
            double dstart = Double.valueOf(start)
            double dend = Double.valueOf(end)
            double size = dend - dstart
            long fsize = 3l
            if (size < 85.0) {
                double fudge = size * 0.15
                if (size < 1.0d) {
                    fudge = 0.25
                }
                dstart = dstart - fudge
                if (dstart < -90.0d) {
                    dstart = -90.0d
                }
                dend = dend + fudge
                if (dend > 90.0d) {
                    dend = 90.0d
                }
                double c = Math.ceil(dend - dstart)
                fsize = (long) c + 1
            }
            double step = (dend - dstart) / (Double.valueOf(fsize) - 1.0d)
            def dr = dend - dstart
            def fudge = dr * 0.15d
            if (dstart - fudge > -85.0d) {
                dstart = dstart - fudge;
            } else {
                dstart = -90.0d
            }
            if (dend + fudge < 85.0d) {
                dend = dend + fudge
            } else {
                dend = 90.0d
            }
            geoAxisY.setMin(dstart)
            geoAxisY.setMax(dend)
            geoAxisY.setDelta(step)
            geoAxisY.setSize(fsize)

        }
        if (!axesToSkip.contains("z") && zVar) {
            DatasetProperty alt = new DatasetProperty([type: "tabledap_access", name: "altitude", value: zVar])
            dataset.addToDatasetProperties(alt)
            String units = metadata.getAttributeValue(zVar, "units")
            if (units) {
                zAxis.setUnits(units)
            }
            String longname = metadata.getAttributeValue(zVar, "long_name")
            if (longname) {
                zAxis.setTitle(longname)
            } else {
                zAxis.setTitle(zVar)
            }
            zAxis.setName(zVar)

            String pos = metadata.getAttributeValue(zVar, "positive")
            if (pos) {
                zAxis.setPositive(pos)
            } else {
                zAxis.setPositive("down")
            }
            // TODO this is old code. needs vertical axis object

            String start = metadata.getAttributeValue(NC_GLOBAL, "geospatial_vertical_min")
            String end = metadata.getAttributeValue(NC_GLOBAL, "geospatial_vertical_max")

            if (!start && !end) {
                // try this instead
                String range = metadata.getAttributeValue(zVar, "actual_range")
                // There are data sets with empty z variables. Just pertend there is no z and see if that works:
                // https://upwell.pfeg.noaa.gov/erddap/tabledap/cciea_OC_SL3.html
                if (range != null && range.contains((","))) {
                    String[] parts = range.split(",")
                    start = parts[0].trim()
                    end = parts[1].trim()
                } else if ( range == null ) {
                    zVar = null; // Pretend it's not there...
                }
            }

            if (start != null && end != null) {
                double min = Double.valueOf(start).doubleValue()
                double max = Double.valueOf(end).doubleValue()
                double size = max - min
                double step = size / 10.0d
                zAxis.setMax(max)
                zAxis.setMin(min)
                zAxis.setDelta(step)
                zAxis.setSize(size)

            } else {
                log.debug("Rejected " + url + " for no Z metadata.")
                return null
            }
        }


        dataset.setVariableChildren(true)
        id_var_list.each { dsgIDVariablename ->

            Variable idvb = new Variable()

            idvb.setName(dsgIDVariablename)
            idvb.setUrl(url + "#" + dsgIDVariablename)
            idvb.setHash(getDigest(idvb.getUrl()))
            idvb.setDsgId(true)
            String longname = metadata.getAttributeValue(dsgIDVariablename, "long_name")
            if (longname) {
                idvb.setTitle(longname)
            } else {
                idvb.setTitle(dsgIDVariablename)
            }

            VariableAttribute cby = new VariableAttribute([name: "color_by", value: "true"])
            VariableAttribute cid = new VariableAttribute([name: grid_type.toLowerCase(Locale.ENGLISH) + "_id", value: "true"])
            idvb.addToVariableAttributes(cby)
            idvb.addToVariableAttributes(cid)
            idvb.setGeometry(grid_type)
            def intervals = "xy"
            if (zVar) {
                intervals = intervals + "z"
            }
            intervals = intervals + "t"
            idvb.setIntervals(intervals)

            dataset.addToVariables(idvb)
            subsetNames.remove(dsgIDVariablename)
            dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "metadata", value: dsgIDVariablename]))
        }
        // Axis and intervals
        GeoAxisX gx = new GeoAxisX(geoAxisX.properties)
        gx.setDataset(dataset)
        dataset.setGeoAxisX(gx)

        GeoAxisY gy = new GeoAxisY(geoAxisY.properties)
        gy.setDataset(dataset)
        dataset.setGeoAxisY(gy)

        if (zVar) {
            VerticalAxis za = new VerticalAxis(zAxis.properties)
            za.setDataset(dataset)
            dataset.setVerticalAxis(za)
        }

        TimeAxis ta = new TimeAxis(timeAxis.properties)
        ta.setDataset(dataset)
        dataset.setTimeAxis(ta)

        // Add any subset variables that are not the id and are not a XYZT variable
        subsetNames.each { subsetVariable ->

            Variable vb = new Variable()
            vb.setName(subsetVariable)
            vb.setGeometry(grid_type)
            String longname = metadata.getAttributeValue(subsetVariable, "long_name")
            if (longname) {
                vb.setTitle(longname)
            } else {
                vb.setTitle(subsetVariable)
            }
            vb.setUrl(url + "#" + subsetVariable)
            vb.setHash(getDigest(vb.getUrl()))
            vb.setUnits("text")
            vb.setSubset(true)

            vb.addToVariableAttributes(new VariableAttribute([name: "subset_variable", value: "true"]))
            vb.addToVariableAttributes(new VariableAttribute([name: "geometry", value: grid_type.toLowerCase(Locale.ENGLISH)]))

            def intervals = "xy"
            if (zVar) {
                VerticalAxis za = new VerticalAxis(zAxis.properties)
                intervals = intervals + "z"
            }

            intervals = intervals + "t"
            vb.setIntervals(intervals)
            dataset.addToVariables(vb)
        }

        int i = 0
        // Make the prop-prop list before adding in lat,lon and time.
        StringBuilder allv = new StringBuilder()
        for (Iterator subIt = dataVariables.iterator(); subIt.hasNext();) {
            String key = (String) subIt.next()
            allv.append(key)
            if (subIt.hasNext()) allv.append(",")
        }


        dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "all_variables", value: allv.toString()]))

        StringBuilder pairs = new StringBuilder()
        StringBuilder vnames = new StringBuilder()
        pairs.append("\n")
        pairs.append(lonVar + "-" + id + "," + latVar + "-" + id + "\n")


        def data_variable_ids = []

        dataVariables.each { dataVar ->
            vnames.append(dataVar)
            if (dataVar != dataVariables.last()) {
                vnames.append(",")
            }
            if (CdmDatatype.TRAJECTORY.contains(grid_type)) {
                pairs.append(timeVar + "-" + id + "," + dataVar + "-" + id + "\n")
                pairs.append(dataVar + "-" + id + "," + latVar + "-" + id + "\n")
                pairs.append(lonVar + "-" + id + "," + dataVar + "-" + id + "\n")
            } else if (CdmDatatype.PROFILE.contains(grid_type) && zVar != null) {
                pairs.append(dataVar + "-" + id + "," + zVar + "-" + id + "\n")
            } else if (CdmDatatype.TIMESERIES.contains(grid_type)) {
                pairs.append(timeVar + "-" + id + "," + dataVar + "-" + id + "\n")
            } else if (CdmDatatype.POINT.contains(grid_type)) {
                if (zVar != null && !zVar.equals("")) {
                    pairs.append(dataVar + "-" + id + "," + zVar + "-" + id + "\n")
                }
                pairs.append(dataVar + "-" + id + "," + latVar + "-" + id + "\n")
                pairs.append(lonVar + "-" + id + "," + dataVar + "-" + id + "\n")
            }
            data_variable_ids.add(dataVar + "-" + id)
        }

        // Pair up every data variable with every other.
        // Filter these in the UI to only use variables paired with current selection.
        StringBuilder data_pairs = new StringBuilder()

        for (int index = 0; index < data_variable_ids.size(); index++) {
            for (int jindex = index; jindex < data_variable_ids.size(); jindex++) {
                if (index != jindex) {
                    data_pairs.append(data_variable_ids.get(index) + "," + data_variable_ids.get(jindex) + "\n")
                }
            }
        }


        pairs.append("\n")
        dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "coordinate_pairs", value: pairs.toString()]))
        if (data_pairs.length() > 0) {
            dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "variable_pairs", value: data_pairs.toString()]))
        }
        dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "variable_names", value: vnames.toString()]))

        // Use the first data variable as the dummy variable.
        dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "dummy", value: dataVariables.get(0)]))

        dataVariables.add(lonVar)
        dataVariables.add(latVar)
        dataVariables.add(timeVar)
        if (zVar) dataVariables.add(zVar)

        dataVariables.each { dataVar ->

            Variable vb = new Variable()
            vb.setName(dataVar)
            vb.setUrl(dsg_url + "#" + dataVar)
            vb.setHash(getDigest(vb.getUrl()))
            vb.setGeometry(grid_type)
            String units = metadata.getAttributeValue(dataVar, "units")
            if (units) {
                vb.setUnits(units)
            } else {
                vb.setUnits("none")
            }
            String longname = metadata.getAttributeValue(dataVar, "long_name")
            if (longname) {
                vb.setTitle(longname)
            } else {
                vb.setTitle(dataVar)
            }
            def intervals = "xy"
            if (zVar) {
                VerticalAxis za = new VerticalAxis(zAxis.properties)
                intervals = intervals + "z"
            }
            intervals = intervals + "t"
            vb.setIntervals(intervals)
            vb.addToVariableAttributes(new VariableAttribute([name: "grid_type", value: grid_type.toLowerCase(Locale.ENGLISH)]))
            dataset.addToVariables(vb)

        }


        dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "server", value: "TableDAP " + grid_type.toLowerCase(Locale.ENGLISH)]))
        dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "title", value: title]))
        dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "id", value: id]))

        if (!default_supplied) {
            dataset.addToDatasetProperties(new DatasetProperty([type: "ui", name: "default", value: "file:ui.xml#" + grid_type]))
        } else {
            def dv
            if (default_value.contains("only")) {
                dv = grid_type + "_only"
            } else {
                dv = grid_type
            }
            dataset.addToDatasetProperties(new DatasetProperty([type: "ui", name: "default", value: "file:ui.xml#" + dv]))
        }
        dataset.setStatus(Dataset.INGEST_FINISHED)
        dataset
    }

/*
@Deprecated
 */

    Dataset ingestFromErddap(String url, List<AddProperty> properties) {

        AddProperty plots = properties.find { it.name == "mapandplot" }

        def default_supplied = false
        def default_value

        if (plots) {
            default_supplied = true
            default_value = plots.getValue()
        }

        AddProperty hour
        if (properties) {
            hour = properties.find { it.name == "hours" }
        } else {
            hour = new AddProperty([name: "hour", value: "1"])
        }
        def hours_value = null
        double hours_step
        if (hour) {
            hours_value = hour.getValue()
            hours_step = Double.valueOf(hours_value)
        }


        def id = url.substring(url.lastIndexOf("/") + 1, url.length() - 1)

        int timeout = 400  // units of seconds

        DAS das = new DAS()
        InputStream input
        List<String> subsetNames = new ArrayList<String>()
        Map<String, AttributeTable> idVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> timeVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> latVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> lonVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> zVar = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> data = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> subsets = new HashMap<String, AttributeTable>()
        Map<String, AttributeTable> monthOfYear = new HashMap<String, AttributeTable>()


        InputStream stream = null
        JsonStreamParser jp = null


        boolean isTrajectoryProfile = false
        boolean isProfile = false
        boolean isPoint = false
        boolean isTrajectory = false
        boolean isTimeseries = false

        def display = null

        def axesToSkip = []

        try {


            def skip = null

            def hash = getDigest(url)

            Dataset dataset = new Dataset([url: url, hash: hash])

            DateTime date = new DateTime()
            log.info("Processing: " + url + " at " + date.toString())


            input = lasProxy.executeGetMethodAndReturnStream(url + ".das", null, timeout)
            das.parse(input)
            AttributeTable global = das.getAttributeTable("NC_GLOBAL")
            opendap.dap.Attribute cdm_trajectory_variables_attribute = global.getAttribute(TRAJECTORY)
            opendap.dap.Attribute cdm_profile_variables_attribute = global.getAttribute(PROFILE)
            opendap.dap.Attribute cdm_timeseries_variables_attribute = global.getAttribute(TIMESERIES)
            opendap.dap.Attribute cdm_data_type = global.getAttribute("cdm_data_type")
            opendap.dap.Attribute altitude_proxy = global.getAttribute("altitude_proxy")
            String grid_type = cdm_data_type.getValueAt(0).toLowerCase(Locale.ENGLISH)
            opendap.dap.Attribute subset_names = null
            opendap.dap.Attribute title_attribute = global.getAttribute("title")
            if (title_attribute == null) {
                title_attribute = global.getAttribute("dataset_title")
            }
            String title = "No title global attribute"
            if (title_attribute != null) {
                Iterator<String> titleIt = title_attribute.getValuesIterator()
                title = titleIt.next()
            }
            AttributeTable variableAttributes = das.getAttributeTable("s")
            if (((cdm_data_type != null && grid_type.equalsIgnoreCase(CdmDatatype.POINT)) || cdm_profile_variables_attribute != null || cdm_trajectory_variables_attribute != null || cdm_timeseries_variables_attribute != null) && variableAttributes != null) {
                if (grid_type.equals(CdmDatatype.TRAJECTORYPROFILE)) {
                    isTrajectoryProfile = true
                } else {
                    if (cdm_trajectory_variables_attribute != null && cdm_profile_variables_attribute != null) {
                        isTrajectoryProfile = true
                        dataset.setGeometry(GeometryType.TRAJECTORY_PROFILE)
                    } else if (cdm_trajectory_variables_attribute != null) {
                        subset_names = cdm_trajectory_variables_attribute
                        isTrajectory = true
                        dataset.setGeometry(GeometryType.TRAJECTORY)
                    } else if (cdm_profile_variables_attribute != null) {
                        subset_names = cdm_profile_variables_attribute
                        isProfile = true
                        dataset.setGeometry(GeometryType.PROFILE)
                    } else if (cdm_timeseries_variables_attribute != null) {
                        subset_names = cdm_timeseries_variables_attribute
                        isTimeseries = true
                        dataset.setGeometry(GeometryType.TIMESERIES)
                    } else if (grid_type.equalsIgnoreCase(CdmDatatype.POINT)) {
                        subset_names = null
                        isPoint = true
                        dataset.setGeometry(GeometryType.POINT)
                    }
                }
                if (subset_names != null) {
                    Iterator<String> subset_variables_attribute_values = subset_names.getValuesIterator()
                    if (subset_variables_attribute_values.hasNext()) {
                        // Work with the first value...  Attributes like ranges can have multiple values...
                        String subset_variable_value = subset_variables_attribute_values.next()
                        String[] subset_variables = subset_variable_value.split(",")
                        for (int i = 0; i < subset_variables.length; i++) {
                            String tv = subset_variables[i].trim()
                            if (!tv.equals("")) {
                                subsetNames.add(tv)
                            }
                        }
                    } else {
                        System.err.println("No CDM trajectory, profile or timeseries variables found in the cdm_trajectory_variables, cdm_profile_variables or cdm_timeseries_variables global attribute.")
                    }
                }
                // Collect the subset names...

                // Classify all of the variables...

                Enumeration names = variableAttributes.getNames()
                if (!names.hasMoreElements()) {
                    log.debug("No variables found in this data collection.")
                } else {
                    // We found some variables, so set the flag
                    dataset.setVariableChildren(true);
                }
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement()
                    AttributeTable var = variableAttributes.getAttribute(name).getContainer()
                    if (subsetNames.contains(name)) {
                        if (var.hasAttribute("cf_role") && (var.getAttribute("cf_role").getValueAt(0).equals("trajectory_id") ||
                                var.getAttribute("cf_role").getValueAt(0).equals("profile_id") ||
                                var.getAttribute("cf_role").getValueAt(0).equals("timeseries_id"))) {
                            idVar.put(name, var);
                        } else {
                            if (!subsets.containsKey(name)) {
                                subsets.put(name, var)
                            }
                        }
                    } else if (var.hasAttribute("cf_role") && (var.getAttribute("cf_role").getValueAt(0).equals("trajectory_id") ||
                            var.getAttribute("cf_role").getValueAt(0).equals("profile_id") ||
                            var.getAttribute("cf_role").getValueAt(0).equals("timeseries_id"))) {
                        idVar.put(name, var);
                        if (!subsets.containsKey(name)) {
                            subsets.put(name, var)
                        }
                    }
                    // Look at the attributes and classify any variable as either time, lat, lon, z or a data variable.
                    if (var.hasAttribute("_CoordinateAxisType")) {
                        String type = var.getAttribute("_CoordinateAxisType").getValueAt(0)
                        if (type.toLowerCase(Locale.ENGLISH).equals("time")) {
                            timeVar.put(name, var)
                        } else if (type.toLowerCase(Locale.ENGLISH).equals("lon")) {
                            lonVar.put(name, var)
                        } else if (type.toLowerCase(Locale.ENGLISH).equals("lat")) {
                            latVar.put(name, var)
                        } else if (type.toLowerCase(Locale.ENGLISH).equals("height")) {
                            zVar.put(name, var)
                        }
                    } else {
                        if (name.toLowerCase(Locale.ENGLISH).contains("tmonth")) {
                            monthOfYear.put(name, var)
                        }
                        boolean skipCheck = false
                        if (skip != null) {
                            skipCheck = Arrays.asList(skip).contains(name)
                        }
                        if (!data.containsKey(name) && !subsets.containsKey(name) && !idVar.containsKey(name) && !skipCheck) {
                            data.put(name, var)
                        }
                    }


                }
                // DEBUG what we've got so far:
                if (!idVar.keySet().isEmpty()) {
                    String name = idVar.keySet().iterator().next()
                    log.debug(grid_type + " ID variable:")
                    log.debug("\t " + name)
                }
                log.debug("Subset variables:")

                for (Iterator subIt = subsets.keySet().iterator(); subIt.hasNext();) {
                    String key = (String) subIt.next()
                    log.debug("\t " + key)
                }
                if (!timeVar.keySet().isEmpty()) {
                    String name = timeVar.keySet().iterator().next()
                    log.debug("Time variable:")
                    log.debug("\t " + name)
                }
                if (!lonVar.keySet().isEmpty()) {
                    String name = lonVar.keySet().iterator().next()
                    log.debug("Lon variable:")
                    log.debug("\t " + name)
                }
                if (!latVar.keySet().isEmpty()) {
                    String name = latVar.keySet().iterator().next()
                    log.debug("Lat variable:")
                    log.debug("\t " + name)
                }
                if (!zVar.keySet().isEmpty()) {
                    String name = zVar.keySet().iterator().next()
                    log.debug("Z variable:")
                    log.debug("\t " + name)
                }
                if (!monthOfYear.keySet().isEmpty()) {
                    String name = monthOfYear.keySet().iterator().next()
                    log.debug("Month of year variable:")
                    log.debug("\t " + name)
                }

                log.debug("Data variables:")

                for (Iterator subIt = data.keySet().iterator(); subIt.hasNext();) {
                    String key = (String) subIt.next()
                    log.debug("\t " + key)
                }


                dataset.setTitle(title)
                DatasetProperty property = new DatasetProperty([type: "ferret", name: "data_format", value: "csv"])
                dataset.addToDatasetProperties(property)


                String dsgIDVariablename = null
                if (!idVar.keySet().isEmpty()) {
                    dsgIDVariablename = idVar.keySet().iterator().next()
                }

                // Get the ISO Metadata
                String isourl = url + ".iso19115"
                stream = null

                IsoMetadata meta = new IsoMetadata()
                stream = lasProxy.executeGetMethodAndReturnStream(isourl, null, timeout)
                if (stream != null) {
                    JDOMUtils.XML2JDOM(new InputStreamReader(stream), meta)
                    meta.init()
                }

                /*
                With an ERDDAP DSG data set, the "grid" which is just the maximum lon/lat/time/depth extents is the
                same for every variable. Though it's wasteful for storage, we're going to build the axes and save
                a copy in each varible.
                 */

                TimeAxis timeAxis = new TimeAxis()
                GeoAxisX geoAxisX = new GeoAxisX()
                geoAxisX.setType("x")
                GeoAxisY geoAxisY = new GeoAxisY()
                geoAxisY.setType("y")
                VerticalAxis zAxis = new VerticalAxis()
                zAxis.setType("z")

                if (!timeVar.keySet().isEmpty() && timeVar) {
                    String name = timeVar.keySet().iterator().next()
                    timeAxis.setName(name)
                    AttributeTable var = timeVar.get(name)
                    DatasetProperty tp = new DatasetProperty([type: "tabledap_access", name: "time", value: name])
                    dataset.addToDatasetProperties(tp)

                    opendap.dap.Attribute cala = var.getAttribute("calendar")
                    String calendar = "standard"
                    if (cala != null) {
                        calendar = cala.getValueAt(0)
                    }
                    timeAxis.setCalendar(calendar)
                    if (display != null && !display[0].equals("minimal")) {
                        if (display.length == 1) {
                            timeAxis.setDisplay_lo(display[0])
                        } else if (display.length == 2) {
                            String t0text = display[0]
                            String t1text = display[1]
                            DateTime dt0
                            DateTime dt1
                            try {
                                dt0 = shortFerretForm.parseDateTime(t0text)
                            } catch (Exception e) {
                                try {
                                    dt0 = mediumFerretForm.parseDateTime(t0text)
                                } catch (Exception e1) {
                                    dt0 = null
                                }
                            }
                            try {
                                dt1 = shortFerretForm.parseDateTime(t1text)
                            } catch (Exception e) {
                                try {
                                    dt1 = mediumFerretForm.parseDateTime(t1text)
                                } catch (Exception e1) {
                                    dt1 = null
                                }
                            }
                            if (dt1 != null && dt0 != null) {
                                if (dt0.isBefore(dt1)) {
                                    timeAxis.setDisplay_lo(t0text)
                                    timeAxis.setDisplay_hi(t1text)
                                } else {
                                    timeAxis.setDisplay_lo(t1text)
                                    timeAxis.setDisplay_hi(t0text)
                                }
                            }
                        }
                    }
                    // TODO pass in the whehter data are to be minutes
                    def minutes = false
                    def hours = false

                    if (hours_value) {
                        hours = true
                    }

                    if (minutes) {
                        timeAxis.setUnits("minutes")
                    } else if (hours) {
                        timeAxis.setUnits("hours")
                    } else {
                        timeAxis.setUnits("days")
                    }
                    opendap.dap.Attribute ua = var.getAttribute("units")
                    if (ua != null) {
                        String units = ua.getValueAt(0)
                        DatasetProperty tu = new DatasetProperty([type: "tabledap_access", name: "time_units", value: units])
                        dataset.addToDatasetProperties(tu)
                    }

                    if (!axesToSkip.contains("t")) {

                        String start = meta.getTlo()
                        String end = meta.getThi()


                        if (start == null || end == null) {
                            // TODO debugging for OSMC
//                            throw new Exception("Time metadata not found.")
                            DateTime now = new DateTime().withTimeAtStartOfDay()
                            DateTime earlier = now.minusDays(30)
                            start = dateTimeService.isoFromDateTime(earlier, "proleptic_gregorian")
                            end = dateTimeService.isoFromDateTime(now, "proleptic_gregorian")
                        }

                        // This should be time strings in ISO Format

                        Chronology chrono = GregorianChronology.getInstance(DateTimeZone.UTC)
                        DateTimeFormatter iso = ISODateTimeFormat.dateTimeParser().withChronology(chrono).withZone(DateTimeZone.UTC)

                        DateTime dtstart = iso.parseDateTime(start)
                        DateTime dtend = iso.parseDateTime(end)

                        int days = Days.daysBetween(dtstart.withTimeAtStartOfDay(), dtend.withTimeAtStartOfDay()).getDays()
                        Period span = new Period(0, 0, 0, days, 0, 0, 0, 0)
                        timeAxis.setPeriod(pf.print(span))
                        timeAxis.setPosition("middle")

                        if (hours || minutes) {
                            timeAxis.setStart(hoursfmt.print(dtstart))
                        } else {
                            timeAxis.setStart(hoursfmt.print(dtstart.withTimeAtStartOfDay()))
                        }

                        AddProperty dlo = properties.find { it.name == "display_lo" }
                        if (dlo) timeAxis.setDisplay_lo(dlo.getValue())
                        AddProperty dhi = properties.find { it.name == "display_hi" }
                        if (dhi) timeAxis.setDisplay_hi(dhi.getValue())

                        // Fudge
                        days = days + 1
                        Period period;
                        if (minutes) {
                            // Days are now minutes :-)
                            days = days * 24 * 60
                            period = new Period(0, 0, 0, 0, 0, days, 0, 0)
                        } else if (hours) {
                            // Days are now hours :-)
                            days = (int) (days * 24 * Math.rint(1.0d / hours_step))
                            period = new Period(0, 0, 0, 0, days, 0, 0, 0)
                        } else {
                            period = new Period(0, 0, 0, days, 0, 0, 0, 0)
                        }

                        if (hours_value) {
                            if (hours_step < 1) {
                                int deltamin = (int) (60 * hours_step)
                                Period delta = new Period(0, 0, 0, 0, 0, deltamin, 0, 0)
                                timeAxis.setDelta(pf.print(delta))
                            } else {
                                Period delta = new Period(0, 0, 0, 0, (int) hours_step, 0, 0, 0)
                                timeAxis.setDelta(pf.print(delta))
                            }

                        } else {
                            Period delta = new Period(0, 0, 0, 0, 1, 0, 0, 0)
                            timeAxis.setDelta(pf.print(delta))
                        }
                        timeAxis.setPeriod(pf.print(period))
                        timeAxis.setStart(start)
                        timeAxis.setEnd(end)
                        long size = (long) period.hours
                        // TODO what to do. Number of obs really.
                        timeAxis.setSize(size)
                        //TODO should be long_name, yeah ?-)
                        timeAxis.setTitle("Time")

                        // If we're scanning a catalog set the display dates so the entire data set is not requested with the first plot.
                        //TODO You'll need to fix this
                        def auto_display = false
                        if (auto_display) {
                            timeAxis.setDisplay_lo(mediumFerretForm.print(dtstart))
                            timeAxis.setDisplay_hi(mediumFerretForm.print(dtstart.plusDays(1)))
                        }
                        //TODO big  ton more stuff needs to be set in the TimeAxis object.

                    } else {
                        // Beats me
                    }

                }
                if (!lonVar.keySet().isEmpty()) {
                    String name = lonVar.keySet().iterator().next()
                    geoAxisX.setName(name)
                    geoAxisX.setType("x")
                    DatasetProperty xp = new DatasetProperty([type: "tabledap_access", name: "longitude", value: name])
                    dataset.addToDatasetProperties(xp)
                    AttributeTable var = lonVar.get(name)
                    opendap.dap.Attribute ua = var.getAttribute("units")
                    if (ua != null) {
                        String units = ua.getValueAt(0)
                        geoAxisX.setUnits(units)
                    }
                    opendap.dap.Attribute ln = var.getAttribute("long_name")
                    if (ln != null) {
                        String long_name = ln.getValueAt(0)
                        geoAxisX.setTitle(long_name)
                    } else {
                        geoAxisX.setTitle("Longitude")
                    }
                    if (!axesToSkip.contains("x")) {

                        String start = meta.getXlo()
                        String end = meta.getXhi()
                        double dmin = -180.0d
                        double dmax = 180.0d
                        DatasetProperty lonDomain
                        if (Math.abs(Double.valueOf(start)) > 180.0d || Math.abs(Double.valueOf(end)) > 180.0d) {
                            lonDomain = new DatasetProperty([type: "tabledap_access", name: "lon_domain", value: "0:360"])
                            dmin = 0.0d
                            dmax = 360.0d
                        } else {
                            lonDomain = new DatasetProperty([type: "tabledap_access", name: "lon_domain", value: "-180:180"])
                        }
                        dataset.addToDatasetProperties(lonDomain)
                        double dstart = Double.valueOf(start)
                        double dend = Double.valueOf(end)
                        double size = dend - dstart

                        // Fudge it up if the interval is really small...

                        long fsize = 3l
                        if (size < 355.0) {
                            double fudge = size * 0.15
                            if (size < 1.0d) {
                                fudge = 0.25
                            }
                            dstart = dstart - fudge

                            if (dstart < dmin) {
                                dstart = dmin
                            }
                            dend = dend + fudge
                            if (dend > dmax) {
                                dend = dmax
                            }

                            double c = Math.ceil(dend - dstart)
                            fsize = (long) c + 1
                        }
                        double step = (dend - dstart) / (Double.valueOf(fsize) - 1.0d)
                        geoAxisX.setSize(fsize)
                        geoAxisX.setMin(dstart)
                        geoAxisX.setMax(dend)
                        geoAxisX.setDelta(step)


                    } else {
                        // Don't know about this.
                    }

                }
                if (!latVar.keySet().isEmpty()) {

                    String name = latVar.keySet().iterator().next()
                    geoAxisY.setName(name)
                    geoAxisY.setType("y")
                    DatasetProperty latName = new DatasetProperty([type: "tabledap_access", name: "latitude", value: name])
                    dataset.addToDatasetProperties(latName)
                    AttributeTable var = latVar.get(name)
                    opendap.dap.Attribute ua = var.getAttribute("units")
                    if (ua != null) {
                        String units = ua.getValueAt(0)
                        geoAxisY.setUnits(units)
                    }
                    opendap.dap.Attribute ln = var.getAttribute("long_name")
                    if (ln != null) {
                        String long_name = ln.getValueAt(0)
                        geoAxisY.setTitle(long_name)
                    } else {
                        geoAxisY.setTitle("Latitude")
                    }
                    if (!axesToSkip.contains("y")) {

                        String start = meta.getYlo()
                        String end = meta.getYhi()
                        double dstart = Double.valueOf(start)
                        double dend = Double.valueOf(end)
                        double size = dend - dstart
                        long fsize = 3l
                        if (size < 85.0) {
                            double fudge = size * 0.15
                            if (size < 1.0d) {
                                fudge = 0.25
                            }
                            dstart = dstart - fudge
                            if (dstart < -90.0d) {
                                dstart = -90.0d
                            }
                            dend = dend + fudge
                            if (dend > 90.0d) {
                                dend = 90.0d
                            }
                            double c = Math.ceil(dend - dstart)
                            fsize = (long) c + 1
                        }
                        double step = (dend - dstart) / (Double.valueOf(fsize) - 1.0d)
                        geoAxisY.setMin(dstart)
                        geoAxisY.setMax(dend)
                        geoAxisY.setDelta(step)
                        geoAxisY.setSize(fsize)
                    } else {
                        //TODO what to do when we can't fix the axis?
                    }

                }
                /*
                 * For profiles, grab the depth and make 10 equal levels.
                 *
                 *
                 */
                // TODO look for the cdm_alititude_proxy attribute do a query since there won't be metadata.
                if (!zVar.keySet().isEmpty()) {
                    String name = zVar.keySet().iterator().next()
                    DatasetProperty alt = new DatasetProperty([type: "tabledap_access", name: "altitude", value: name])
                    dataset.addToDatasetProperties(alt)
                    AttributeTable var = zVar.get(name)
                    opendap.dap.Attribute ua = var.getAttribute("units")
                    if (ua != null) {
                        String units = ua.getValueAt(0)
                        zAxis.setUnits(units)
                    }
                    zAxis.setTitle(name)
                    zAxis.setName(name)
                    // TODO is this always true? I think so.
                    zAxis.setPositive("down")
                    // TODO this is old code. needs vertical axis object
                    if (!axesToSkip.contains("z")) {

                        String start = meta.getZlo()
                        String end = meta.getZhi()
                        if (start == null || end == null || altitude_proxy != null) {
                            // If it was a proxy, there's no metadata.
                            // Pull the range from the data.
                            stream = null
                            jp = null
                            String zquery = ""

                            String nanDistinct = "&" + name + "!=NaN&distinct()"
                            if (zquery.length() > 0) {
                                zquery = zquery + ","
                            }
                            zquery = zquery + name + "&orderByMinMax(\"" + name + "\")"
                            String zurl = url + ".json?" + URLEncoder.encode(zquery, "UTF-8")
                            stream = null

                            stream = lasProxy.executeGetMethodAndReturnStream(zurl, null, timeout)


                            if (stream != null) {
                                jp = new JsonStreamParser(new InputStreamReader(stream))
                                JsonObject bounds = (JsonObject) jp.next()
                                String[] zminmax = getMinMax(bounds, name)
                                stream.close()

                                start = zminmax[0]
                                end = zminmax[1]
                            }
                        }
                        if (start != null && end != null) {
                            double min = Double.valueOf(start).doubleValue()
                            double max = Double.valueOf(end).doubleValue()
                            double size = max - min
                            double step = size / 10.0d
                            zAxis.setMax(max)
                            zAxis.setMin(min)
                            zAxis.setDelta(step)
                            if (size < 0.00001d) size = 1.0d
                            zAxis.setSize(size)

                        } else {
                            //TODO something needed here:?
                        }
                    } else {

                    }

                }

                Variable idvb = new Variable()

                if (dsgIDVariablename != null) {

                    AttributeTable idvar = idVar.get(dsgIDVariablename)
                    idvb.setName(dsgIDVariablename)
                    idvb.setUrl(url + "#" + dsgIDVariablename)
                    idvb.setHash(getDigest(idvb.getUrl()))
                    idvb.setDsgId(true)
                    opendap.dap.Attribute ln = idvar.getAttribute("long_name")
                    if (ln != null) {
                        String longname = ln.getValueAt(0)
                        idvb.setTitle(longname)
                    } else {
                        idvb.setTitle(dsgIDVariablename)
                    }

                    VariableAttribute cby = new VariableAttribute([name: "color_by", value: "true"])
                    VariableAttribute cid = new VariableAttribute([name: grid_type.toLowerCase(Locale.ENGLISH) + "_id", value: "true"])
                    idvb.addToVariableAttributes(cby)
                    idvb.addToVariableAttributes(cid)
                    idvb.setGeometry(grid_type)
                    // Axis and intervals
                    GeoAxisX gx = new GeoAxisX(geoAxisX.properties)
                    gx.setVariable(idvb)
                    idvb.setGeoAxisX(gx)

                    GeoAxisY gy = new GeoAxisY(geoAxisY.properties)
                    gy.setVariable(idvb)
                    idvb.setGeoAxisY(gy)
                    def intervals = "xy"
                    if (!zVar.keySet().isEmpty()) {
                        VerticalAxis za = new VerticalAxis(zAxis.properties)
                        za.setVariable(idvb)
                        idvb.setVerticalAxis(za)
                        intervals = intervals + "z"
                    }

                    TimeAxis ta = new TimeAxis(timeAxis.properties)
                    ta.setVariable(idvb)
                    idvb.setTimeAxis(ta)

                    intervals = intervals + "t"
                    idvb.setIntervals(intervals)

                    dataset.addToVariables(idvb)
                }
//TODO this shas to do with the constaintt objec wich we mujst define and populate
//                if ( isTrajectory ) {
//                    idcg.setAttribute("name", "Individual Trajectory(ies)")
//                }
//                if ( isTrajectoryProfile ) {
//                    idcg.setAttribute("name", "Trajectory Profiles(s)")
//                }
//                if ( isProfile ) {
//                    idcg.setAttribute("name", "Individual Profile(s)")
//                }
//                if ( isTimeseries ) {
//                    idcg.setAttribute("name", "Individual Station(s)")
//                }
//                if ( isPoint ) {
//                    idcg.setAttribute("name", "Points")
//                }
//                idcg.setAttribute("type", "selection")

//                Element idc = new Element("constraint")
//                idc.setAttribute("name","Select By")
//                if ( dsgIDVariablename != null ) {
//                    Element idv = new Element("variable")
//                    idv.setAttribute("IDREF", dsgIDVariablename+"-"+id)
//                    Element idkey = new Element("key")
//                    idkey.setText(dsgIDVariablename)
//                    idc.addContent(idv)
//                    idc.addContent(idkey)
//                    idcg.addContent(idc)
//                    cons.addContent(idcg)
//                }
//
//                Element subsetcg = new Element("constraint_group")
//                subsetcg.setAttribute("type", "subset")
//                subsetcg.setAttribute("name", "by Metadata")


                String lonn = lonVar.keySet().iterator().next()
                String latn = latVar.keySet().iterator().next()

                // Before using them, remove latn and lonn
                subsets.remove(latn)
                subsets.remove(lonn)

                if (subsets.keySet().size() > 0) {
                    for (Iterator subsetIt = subsets.keySet().iterator(); subsetIt.hasNext();) {

                        String name = (String) subsetIt.next()
                        AttributeTable var = subsets.get(name)

                        Variable vb = new Variable()
                        vb.setName(name)
                        vb.setGeometry(grid_type)
                        opendap.dap.Attribute ln = var.getAttribute("long_name")
                        if (ln != null) {
                            String longname = ln.getValueAt(0)
                            vb.setTitle(longname)
                        } else {
                            vb.setTitle(name)
                        }
                        vb.setUrl(url + "#" + name)
                        vb.setHash(getDigest(vb.getUrl()))
                        vb.setUnits("text")
                        vb.setSubset(true)

                        vb.addToVariableAttributes(new VariableAttribute([name: "subset_variable", value: "true"]))
                        vb.addToVariableAttributes(new VariableAttribute([name: "geometry", value: grid_type.toLowerCase(Locale.ENGLISH)]))

                        GeoAxisX gx = new GeoAxisX(geoAxisX.properties)
                        gx.setVariable(vb)
                        vb.setGeoAxisX(gx)

                        GeoAxisY gy = new GeoAxisY(geoAxisY.properties)
                        gy.setVariable(vb)
                        vb.setGeoAxisY(gy)

                        def intervals = "xy"
                        if (!zVar.keySet().isEmpty()) {
                            VerticalAxis za = new VerticalAxis(zAxis.properties)
                            za.setVariable(vb)
                            vb.setVerticalAxis(za)
                            intervals = intervals + "z"
                        }

                        TimeAxis ta = new TimeAxis(timeAxis.properties)
                        ta.setVariable(vb)
                        vb.setTimeAxis(ta)

                        intervals = intervals + "t"
                        vb.setIntervals(intervals)
                        dataset.addToVariables(vb)
//TODO this is the subset variable constraint
//                        Element c = new Element("constraint")
//                        c.setAttribute("type", "subset")
//                        c.setAttribute("widget", "list")
//                        Element v = new Element("variable")
//                        v.setAttribute("IDREF", name+"-"+id)
//                        Element key = new Element("key")
//                        key.setText(name)
//                        c.addContent(v)
//                        c.addContent(key)
//                        subsetcg.addContent(c)
                    }
//                    cons.addContent(subsetcg)
                }

                int i = 0
                // Make the prop-prop list before adding in lat,lon and time.
                StringBuilder allv = new StringBuilder()
                for (Iterator subIt = data.keySet().iterator(); subIt.hasNext();) {
                    String key = (String) subIt.next()
                    allv.append(key)
                    if (subIt.hasNext()) allv.append(",")
                }

                // Z name zn is used below as well...

                String zn = null
                if (!zVar.keySet().isEmpty()) {
                    zn = zVar.keySet().iterator().next()
                }

                dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "all_variables", value: allv.toString()]))

                /*
                 * There is a page that will show thumbnails of property-property plots.
                 *
                 * It takes 3 pieces of metadata. First is the list of variables that will show up in the banner for a particular ID.
                 *
                     <thumbnails>
                 *
                 * THESE are ERDDAP variable names.
                          <metadata>expocode,vessel_name,investigators,qc_flag</metadata>
                 *
                 * Next is the list of plot paris:
                 *
                 *          <variable_pairs>
                               <!-- NO WHITESPACE AROUND THE COMMA -->
                               <!-- x-axis followed by y-axis variable. -->
                               <!-- LAS IDs -->

                               longitude-socatV3_c6c1_d431_8194,latitude-socatV3_c6c1_d431_8194
                               time-socatV3_c6c1_d431_8194,day_of_year-socatV3_c6c1_d431_8194
                               time-socatV3_c6c1_d431_8194,temp-socatV3_c6c1_d431_8194
                               time-socatV3_c6c1_d431_8194,Temperature_equi-socatV3_c6c1_d431_8194

                           </variable_paris>

                 * Finally, is just a flat list of every variable needed to make all the plots so there can be one data pull from ERDDAP.

                           <!-- The names of the variables needed to make all of the thumbnail plots so the netcdf file can be as minimal as possible.
                                Do not list latitude,longitude,depth,time,expocode
                                as these are handled by LAS internally
                           -->
                           <variable_names>day_of_year,temp,Temperature_equi</variable_names>

                 *
                 * The default set that we will construct will be lat vs lon and time vs all other varaibles.
                 */

                if (dsgIDVariablename != null) {
                    dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "metadata", value: dsgIDVariablename]))
                }

                StringBuilder pairs = new StringBuilder()
                StringBuilder vnames = new StringBuilder()
                pairs.append("\n")
                pairs.append(lonn + "-" + id + "," + latn + "-" + id + "\n")
                String timen = timeVar.keySet().iterator().next()


                List<String> data_variable_ids = new ArrayList()
                for (Iterator subIt = data.keySet().iterator(); subIt.hasNext();) {
                    String key = (String) subIt.next()
                    vnames.append(key)
                    if (subIt.hasNext()) {
                        vnames.append(",")
                    }
                    if (CdmDatatype.TRAJECTORY.contains(grid_type)) {
                        pairs.append(timen + "-" + id + "," + key + "-" + id + "\n")
                        pairs.append(key + "-" + id + "," + latn + "-" + id + "\n")
                        pairs.append(lonn + "-" + id + "," + key + "-" + id + "\n")
                    } else if (CdmDatatype.PROFILE.contains(grid_type) && zn != null) {
                        pairs.append(key + "-" + id + "," + zn + "-" + id + "\n")
                    } else if (CdmDatatype.TIMESERIES.contains(grid_type)) {
                        pairs.append(timen + "-" + id + "," + key + "-" + id + "\n")
                    } else if (CdmDatatype.POINT.contains(grid_type)) {
                        if (zn != null && !zn.equals("")) {
                            pairs.append(key + "-" + id + "," + zn + "-" + id + "\n")
                        }
                        pairs.append(key + "-" + id + "," + latn + "-" + id + "\n")
                        pairs.append(lonn + "-" + id + "," + key + "-" + id + "\n")
                    }
                    data_variable_ids.add(key + "-" + id)
                }

                // Pair up every data variable with every other.
                // Filter these in the UI to only use variables paired with current selection.
                StringBuilder data_pairs = new StringBuilder()

                for (int index = 0; index < data_variable_ids.size(); index++) {
                    for (int jindex = index; jindex < data_variable_ids.size(); jindex++) {
                        if (index != jindex) {
                            data_pairs.append(data_variable_ids.get(index) + "," + data_variable_ids.get(jindex) + "\n")
                        }
                    }
                }


                pairs.append("\n")
                dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "coordinate_pairs", value: pairs.toString()]))
                if (data_pairs.length() > 0) {
                    dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "variable_pairs", value: data_pairs.toString()]))
                }
                dataset.addToDatasetProperties(new DatasetProperty([type: "thumbnails", name: "variable_names", value: vnames.toString()]))
                // Add lat, lon and time to the data variable for output to the dataset

                String vn = lonVar.keySet().iterator().next()
                if (!data.containsKey(vn)) {
                    data.put(vn, lonVar.get(vn))
                }
                vn = latVar.keySet().iterator().next()
                if (!data.containsKey(vn)) {
                    data.put(vn, latVar.get(vn))
                }
                vn = timeVar.keySet().iterator().next()
                if (!data.containsKey(vn)) {
                    data.put(vn, timeVar.get(vn))
                }
                if (zn != null && !data.containsKey(zn)) {
                    data.put(zn, zVar.get(zn))
                }
                // May already be done because it's a sub set variable??
                boolean dummy = false
                for (Iterator dataIt = data.keySet().iterator(); dataIt.hasNext();) {
                    String name = (String) dataIt.next()
                    if (!subsets.containsKey(name)) {
                        if (!dummy && !name.toLowerCase(Locale.ENGLISH).contains("time") && !name.toLowerCase(Locale.ENGLISH).contains("lat") && !name.toLowerCase(Locale.ENGLISH).contains("lon") && !name.toLowerCase(Locale.ENGLISH).contains("depth")) {
                            dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "dummy", value: name]))
                            dummy = true
                        }
                        i++
                        AttributeTable var = data.get(name)
                        Variable vb = new Variable()
                        vb.setName(name)
                        vb.setUrl(url + "#" + name)
                        vb.setHash(getDigest(vb.getUrl()))
                        vb.setGeometry(grid_type)
                        opendap.dap.Attribute ua = var.getAttribute("units")
                        if (ua != null) {
                            String units = ua.getValueAt(0)
                            vb.setUnits(units)
                        } else {
                            vb.setUnits("none")
                        }
                        opendap.dap.Attribute ln = var.getAttribute("long_name")
                        if (ln != null) {
                            String longname = ln.getValueAt(0)
                            vb.setTitle(longname)
                        } else {
                            vb.setTitle(name)
                        }
                        GeoAxisX gx = new GeoAxisX(geoAxisX.properties)
                        gx.setVariable(vb)
                        vb.setGeoAxisX(gx)
                        GeoAxisY gy = new GeoAxisY(geoAxisY.properties)
                        gy.setVariable(vb)
                        vb.setGeoAxisY(gy)
                        def intervals = "xy"
                        if (!zVar.keySet().isEmpty()) {
                            VerticalAxis za = new VerticalAxis(zAxis.properties)
                            za.setVariable(vb)
                            vb.setVerticalAxis(za)
                            intervals = intervals + "z"
                        }
                        intervals = intervals + "t"
                        TimeAxis ta = new TimeAxis(timeAxis.properties)
                        ta.setVariable(vb)
                        vb.setTimeAxis(ta)
                        vb.setIntervals(intervals)
                        vb.addToVariableAttributes(new VariableAttribute([name: "grid_type", value: grid_type.toLowerCase(Locale.ENGLISH)]))
                        dataset.addToVariables(vb)
                    }

                }

                if (dsgIDVariablename) {
                    dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "table_variables", value: dsgIDVariablename]))
                }

                // add any variable properties.
                for (Iterator varid = dataset.variables.iterator(); varid.hasNext();) {
                    Variable variableb = (Variable) varid.next()
                    // TODO Some variable properties nee do be pass in
//                    if ( varproperties != null && varproperties.length > 0 ) {
//                        for (int p = 0 p < varproperties.length p++) {
//                            // Split n-1 times so any ":" after the third remain
//                            String[] parts = varproperties[p].split(":", 4)
//                            if ( variableb.getUrl().endsWith(parts[0]) ) {
//                                variableb.setProperty(parts[1], parts[2], parts[3])
//                            }
//                        }
//                    }
                }

                // Add all the tabledap_access properties

                //TODO "Profile"
                if (dsgIDVariablename != null) {
                    dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: grid_type.toLowerCase(Locale.ENGLISH) + "_id", value: dsgIDVariablename]))
                }
                dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "server", value: "TableDAP " + grid_type.toLowerCase(Locale.ENGLISH)]))
                dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "title", value: title]))
                dataset.addToDatasetProperties(new DatasetProperty([type: "tabledap_access", name: "id", value: id]))


                if (!default_supplied) {
                    dataset.addToDatasetProperties(new DatasetProperty([type: "ui", name: "default", value: "file:ui.xml#" + grid_type]))
                } else {
                    def dv
                    if (default_value.contains("only")) {
                        dv = grid_type + "_only"
                    } else {
                        dv = grid_type
                    }
                    dataset.addToDatasetProperties(new DatasetProperty([type: "ui", name: "default", value: "file:ui.xml#" + dv]))
                }

                //TODO the rest of the contrains logic
//                if ( !monthOfYear.keySet().isEmpty() ) {
//                    String name = monthOfYear.keySet().iterator().next()
//                    String mid = name+"-"+id
//                    Element season = new Element("constraint_group")
//                    season.setAttribute("type", "season")
//
//                    season.setAttribute("name", "by Season")
//                    Element con = new Element("constraint")
//                    con.setAttribute("widget", "month")
//                    Element variable = new Element("variable")
//                    variable.setAttribute("IDREF", mid)
//                    Element key = new Element("key")
//                    key.setText(name)
//                    con.addContent(key)
//                    con.addContent(variable)
//                    season.addContent(con)
//                    cons.addContent(season)
//                }

//                Element vrcg = new Element("constraint_group")
//                vrcg.setAttribute("type", "variable")
//                vrcg.setAttribute("name", "by Variable")
//                cons.addContent(vrcg)
//
//                Element valcg = new Element("constraint_group")
//                valcg.setAttribute("type", "valid")
//                valcg.setAttribute("name", "by Valid Data")
//                cons.addContent(valcg)
//
//
//                Element d = db.toXml()
//                d.addContent(cons)
//                datasetsE.addContent(d)


            }
            dataset.setStatus(Dataset.INGEST_FINISHED)
            dataset
        } catch (Exception e) {
            log.error("Exception adding data set. " + e.getMessage())
            //TODO return an error
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (IOException e) {
                    System.err.println("Error closing stream.  " + e.getMessage())
                }
            }
        }


    }
// Sometimes hierarchies from THREDDS servers end up with several levels
// of children with only one child at each level. This makes for a bunch
// of miserable clicking.
// This method will remove any intermediate data sets with only one child
    def cleanup() {
        List<Dataset> datasets = Dataset.findAllVariableChildren()
        for (int i = 0; i < datasets.size(); i++) {
            Dataset dataset = datasets.get(i)
            collapse(dataset)
        }

    }

    def collapse(Dataset dataset) {
        Dataset parent = dataset.getParent()
        if (parent) {
            Dataset grandparent = parent.getParent()
            if (grandparent) {
                if (grandparent.getDatasets().size() == 1 && parent.getDatasets().size() == 1) {
                    // Parent is superfluous
                    parent.removeFromDatasets(dataset)
                    grandparent.removeFromDatasets(parent)
                    dataset.setParent(grandparent)
                    grandparent.addToDatasets(dataset)
                    log.debug("Removing: " + parent.getTitle() + " with id " + parent.getId())
                    parent.setParent(null);
                    // Are we at the top yet?
                    if (grandparent.getParent()) {
                        if (grandparent.getParent().getParent()) {
                            // The parent is dead, continue from the grandparent
                            collapse(grandparent)
                        }
                    }
                } else {
                    // If there is more hierarchy above continue
                    if (parent.getParent()) {
                        if (parent.getParent().getParent()) {
                            collapse(parent)
                        }
                    }
                }
            }
        }
    }

    public void addVariablesToAll() {

        List<Dataset> needIngest = Dataset.withCriteria {
            eq("variableChildren", true)
            eq("status", Dataset.INGEST_NOT_STARTED)
            isEmpty("variables")
        }

        // When something goes wrong the transaction rolls back all of the datasets.
        // Do only a few at a time, but the service will run every 15 minutes.
        int limit = 25
        if (needIngest.size() < 25) limit = needIngest.size();
        log.debug("STARTED adding variables to " + limit + " of " + needIngest.size() + " OPeNDAP endpoints from THREDDS catalogs.")
        Collections.shuffle(needIngest)
        for (int i = 0; i < limit; i++) {

            Dataset d = needIngest.get(i)
            log.debug(String.format("%03d", i) + ". Adding variables to " + d.getUrl() + " which has variableChildren = " + d.variableChildren)
            addVariablesAndSaveFromThredds(d.getUrl(), d.getHash(), null, true)
            // keep the database from being locked constantly
            try {
                java.util.concurrent.TimeUnit.SECONDS.sleep(10)
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt()
            }
        }

        log.debug("FINISHED adding variables to " + limit + " of " + needIngest.size() + " OPeNDAP endpoints from THREDDS catalogs.")

    }

    def addVectors(Dataset dataset) {

        if (!dataset.getVariableChildren()) {
            return;
        }

        List<List<Variable>> vectors = new ArrayList<List<String>>();

        def variables = dataset.getVariables();

        Map<String, Variable> x_names = new HashMap<>()
        Map<String, Variable> y_names = new HashMap<>()
//        Map<String, Variable> z_names = new HashMap<>()

        Map<String, Variable> X_names = new HashMap<>()
        Map<String, Variable> Y_names = new HashMap<>()
//        Map<String, Variable> Z_names = new HashMap<>()

        Map<String, Variable> u_names = new HashMap<>()
        Map<String, Variable> v_names = new HashMap<>()
//        Map<String, Variable> w_names = new HashMap<>()

        Map<String, Variable> U_names = new HashMap<>()
        Map<String, Variable> V_names = new HashMap<>()
//        Map<String, Variable> W_names = new HashMap<>()

        for (int i = 0; i < variables.size(); i++) {
            Variable v = variables.get(i)
            if (v.getName().contains("x")) {
                String xname = v.getName();
                xname = xname.replace("x", "");
                x_names.put(xname, v)
            } else if (v.getName().contains("y")) {
                String yname = v.getName();
                yname = yname.replace("y", "");
                y_names.put(yname, v)
            } else if (v.getName().contains("X")) {
                String xname = v.getName();
                xname = xname.replace("X", "");
                X_names.put(xname, v)
            } else if (v.getName().contains("Y")) {
                String yname = v.getName()
                yname = yname.replace("Y", "")
                Y_names.put(yname, v)
            } else if (v.getName().contains("u")) {
                String uname = v.getName()
                uname = uname.replace("u", "")
                u_names.put(uname, v)
            } else if (v.getName().contains("v")) {
                String vname = v.getName()
                vname = vname.replace("v", "")
                v_names.put(vname, v)
            } else if (v.getName().contains("U")) {
                String uname = v.getName()
                uname = uname.replace("U", "")
                U_names.put(uname, v)
            } else if (v.getName().contains("V")) {
                String vname = v.getName()
                vname = vname.replace("V", "")
                V_names.put(vname, v)
            }
        }

        List<Vector> vees_x = vectorPairs(x_names, y_names)
        if (vees_x.size() > 0) {
            for (int i = 0; i < vees_x.size(); i++) {
                Vector vector = vees_x.get(i)
                Vector found = dataset.getVectors().find { Vector iv -> iv.getTitle() == vector.getTitle() }
                if (!found) {
                    dataset.addToVectors(vector)
                }
            }
        }
        List<Vector> vees_X = vectorPairs(X_names, Y_names)
        if (vees_X.size() > 0) {
            for (int i = 0; i < vees_X.size(); i++) {
                Vector vector = vees_X.get(i)
                Vector found = dataset.getVectors().find { Vector iv -> iv.getTitle() == vector.getTitle() }
                if (!found) {
                    dataset.addToVectors(vector)
                }
            }
        }
        List<Vector> vees_u = vectorPairs(u_names, v_names)
        if (vees_u.size() > 0) {
            for (int i = 0; i < vees_u.size(); i++) {
                Vector vector = vees_u.get(i)
                Vector found = dataset.getVectors().find { Vector iv -> iv.getTitle() == vector.getTitle() }
                if (!found) {
                    dataset.addToVectors(vector)
                }
            }
        }
        List<Vector> vees_U = vectorPairs(U_names, V_names)
        if (vees_U.size() > 0) {
            for (int i = 0; i < vees_U.size(); i++) {
                Vector vector = vees_U.get(i)
                Vector found = dataset.getVectors().find { Vector iv -> iv.getTitle() == vector.getTitle() }
                if (!found) {
                    dataset.addToVectors(vector)
                }
            }
        }

        if (dataset.getVectors() && dataset.getVectors().size() > 0)
            dataset.save(flush: true)

    }

    def List<Vector> vectorPairs(Map<String, Variable> u, Map<String, Variable> v) {
        List<Vector> vectors = new ArrayList<>()
        if (u.size() > 0) {
            Iterator<String> keys = u.keySet().iterator()
            while (keys.hasNext()) {
                String key = keys.next()
                Variable u_var = u.get(key)
                Variable v_var = v.get(key)
                if (u_var && v_var) {
                    Vector vector = new Vector()
                    vector.setHash(u_var.getHash() + "_" + v_var.getHash())
                    vector.setName(u_var.getName() + "_" + v_var.getName() + " vector")
                    vector.setTitle("Vector of " + u_var.getTitle() + " and " + v_var.getTitle())
                    vector.setGeometry(GeometryType.VECTOR)
                    vector.setU(u_var)
                    vector.setV(v_var)
                    vectors.add(vector)
                }
            }
        }
        vectors
    }

    def makeVectors() {
        def datasets = Dataset.findAllByGeometry(GeometryType.GRID);
        for (int i = 0; i < datasets.size(); i++) {
            Dataset dataset = datasets.get(i)
            addVectors(dataset)
        }
    }

    private static Period getPeriod(CalendarDateUnit cdu, double t0, double t1) {
        CalendarDate cdt0 = cdu.makeCalendarDate(t0)
        CalendarDate cdt1 = cdu.makeCalendarDate(t1)
        DateTime dt0 = new DateTime(cdt0.getMillis()).withZone(DateTimeZone.UTC)
        DateTime dt1 = new DateTime(cdt1.getMillis()).withZone(DateTimeZone.UTC)

        return new Period(dt0, dt1)
    }

    private static String getPosition(double t, double tb1, double tb2) {
        String position = null
        double c1 = tb1 - t
        double ca1 = Math.abs(c1)

        double delta = 0.00001d

        if (c1 < delta) {
            position = "beginning"
        }

        double c2 = t - tb2
        double ca2 = Math.abs(c2)

        if (ca2 < delta) {
            position = "end"
        }
        if (Math.abs((tb1 + ((tb2 - tb1) / 2.0d)) - t) < delta) {
            position = "middle"
        }
        return position
    }

    public static String getDigest(String url) {
        MessageDigest md
        StringBuffer sb = new StringBuffer()
        try {
            md = MessageDigest.getInstance("MD5")
            md.update(url.getBytes())
            byte[] digest = md.digest()
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff))
            }
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            System.err.println(e.getMessage())
        }
        return sb.toString()


    }

    boolean isMonotonic(double[] times) {
        for (int i = 0; i < times.length - 1; i++) {
            if (times[i + 1] <= times[i]) {
                log.debug("Time axis is not monotonic at " + i + " time[" + i + "] = " + times[i] + " time[" + i + 1 + "] = " + times[i + 1])
                return false;
            }
        }
        return true;
    }

}
