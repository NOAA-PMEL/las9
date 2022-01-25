package pmel.sdig.las

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.util.Environment
import grails.util.Holders
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import pmel.sdig.las.type.GeometryType

import javax.servlet.ServletContext

@Transactional
class InitializationService {

    IngestService ingestService
    OptionsService optionsService
    ResultsService resultsService
    GrailsApplication grailsApplication
    ServletContext servletContext

    /**
     * Initialize the Ferret environment. If the values of the environment are available, then they are set. If not, the user will be led to a page where they can be filled in on startup.
     * @return
     */
    @Transactional
    def initEnvironment() {

        def v = grailsApplication.getMetadata().getApplicationVersion()
        def n = grailsApplication.getMetadata().getApplicationName()

        log.info("This is ${n} verison ${v}")
        log.debug("Setting up Ferret environment from the runtime environment.")

        // We are remaking this every time we start up...
        def old_ferret = Ferret.first()
        if ( old_ferret ) {
            // Delete old so we always read the config on startup
            old_ferret.delete(flush: true, failOnError: true)
        }

        // Get their values from the system environment
        def env = System.getenv()

        // Turns out the map constructor does not work when one of the keys is "_"
        // This code clean out that bad value

        def cleanenv = [:]

        // grails and gorm HATE upper case variables names. I don't know why.

        env.each() { key, value ->
            if ( !key.startsWith("_") ) {
                cleanenv.putAt(key.toLowerCase(), value)
            }
        }

        // Use the FER_DIR to set the path to the executable

        def fer_dir = env['FER_DIR']

        if ( !fer_dir ) {
            fer_dir = ""
        }

        def fer_go = cleanenv.get("fer_go")

        URL ferret_go_dir = this.class.classLoader.getResource("ferret/scripts")

        def resource_fer_go = ferret_go_dir.getPath().replace("file:", "")

        fer_go = fer_go + " " + resource_fer_go;

        cleanenv.put("fer_go", fer_go)

        // Override the system environment with values from the external config application.yml file
        fer_go = grailsApplication.config.getProperty('ferret.FER_GO')
        def fer_grids = grailsApplication.config.getProperty('ferret.FER_GRIDS')
        def fer_palette = grailsApplication.config.getProperty('ferret.FER_PALETTE')
        def fer_dsets = grailsApplication.config.getProperty('ferret.FER_DSETS')
        def fer_data = grailsApplication.config.getProperty('ferret.FER_DATA')
        def fer_fonts = grailsApplication.config.getProperty('ferret. FER_FONTS')
        def fer_descr = grailsApplication.config.getProperty('ferret.FER_DESCR')
        fer_dir = grailsApplication.config.getProperty('ferret.FER_DIR')
        def pyfer_external_functions = grailsApplication.config.getProperty('ferret.PYFER_EXTERNAL_FUNCTIONS')
        def pythonpath = grailsApplication.config.getProperty('ferret.PYTHONPATH')
        def tmp = grailsApplication.config.getProperty('ferret.TMP')
        def python = grailsApplication.config.getProperty('ferret.PYTHON')
        def ld_library_path = grailsApplication.config.getProperty('ferret.LD_LIBRARY_PATH')
        def ftds_url = grailsApplication.config.getProperty('ferret.EXTERNAL_BASE_URL')

        if ( !python ) python = 'python'

        if ( fer_go )
            cleanenv['fer_go'] = fer_go + " " + resource_fer_go; // Add the webapp resource directory back in each time
        if ( fer_grids )
            cleanenv['fer_grids'] = fer_grids
        if ( fer_palette )
            cleanenv['fer_palette'] = fer_palette
        if ( fer_dsets )
            cleanenv['fer_dsets'] = fer_dsets
        if ( fer_data )
            cleanenv['fer_data'] = fer_data
        if ( fer_fonts )
            cleanenv['fer_fonds'] = fer_fonts
        if ( fer_descr )
            cleanenv['fer_descr'] = fer_descr
        if (fer_dir )
            cleanenv['fer_dir'] = fer_dir
        if ( pyfer_external_functions )
            cleanenv['pyfer_external_functions'] = pyfer_external_functions
        if ( tmp )
            cleanenv['tmp'] = tmp
        if ( ld_library_path )
            cleanenv['ld_library_path'] = ld_library_path
        if ( pythonpath )
            cleanenv['pythonpath'] = pythonpath


        def ferretEnvironment = FerretEnvironment.first()
        if ( !ferretEnvironment ) {
            ferretEnvironment = new FerretEnvironment(cleanenv)
        } else {
            ferretEnvironment.setProperties(cleanenv)
        }

        // This is an attempt to automate the choice of the full path to the python executable by taking it from the pyferret script
        def ferret = Ferret.first();
        if ( !ferret ) {
            ferret = new Ferret()
        }

        ferret.setBase_url(ftds_url)
        ferret.setPath(python)

        if ( tmp ) {
            ferret.setTempDir(tmp)
        } else {
            ferret.setTempDir("/tmp/las")
        }

        ferret.setFerretEnvironment(ferretEnvironment)

        // These shouldn't change.
        // If they aren't there, add them.
        if ( !ferret.arguments ) {
            ferret.addToArguments(new Argument([value: "-cimport sys; import pyferret; (errval, errmsg) = pyferret.init(sys.argv[1:], True)"]))
            ferret.addToArguments(new Argument([value: "-nodisplay"]))
            ferret.addToArguments(new Argument([value: "-script"]))
        }


        if ( !ferret.validate() ) {
            ferret.errors.each {
                log.debug(it.toString())
            }
        }

        def tempFile = new File(ferret.tempDir);
        if ( !tempFile.exists() ) {
            tempFile.mkdirs()
        }

        ferret.save(failOnError: true)

        // This is an attempt to automate the configuration for Ferret as used by F-TDS by writing the config based on the environment/
        // TODO if changes to the Ferret or FerretEnvironment happen in the admin UI, rewrite this file
        writeFerretXml(ferret, ferretEnvironment)

        // Write the F-TDS base catalog
        writeFTDSCatalog(ferret)
        /*

 I could write this with JDOM, but since I have the text here already...

 <catalog name="F-TDS for LAS"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0
         http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.6.xsd">

  <service name="dap" base="" serviceType="compound">
    <service name="odap" serviceType="OpenDAP" base="/las/thredds/dodsC/" />
    <service name="dap4" serviceType="DAP4" base="/las/thredds/dap4/" />
  </service>

  <datasetScan name="Data From LAS" path="las" location="/tmp/las/dynamic" serviceName="dap">
    <filter>
      <include wildcard="*.nc"/>
      <include wildcard="*.fds"/>
      <include wildcard="*.jnl"/>
    </filter>
  </datasetScan>

</catalog>

         */

    }
    def writeFTDSCatalog(Ferret ferret) {

        def dynamicTempDir = ferret.getTempDir()+File.separator+"dynamic"
        File dynam = new File(dynamicTempDir)
        if ( !dynam.exists() ) {
            dynam.mkdirs()
        }
        def iospTemp = ferret.getTempDir()+File.separator+"temp"
        File iospDir = new File(iospTemp)
        if ( !iospDir.exists() ) {
            iospDir.mkdirs()
        }

        def catalog = """
 <catalog name="F-TDS for LAS"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0
         http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.6.xsd">

 <!-- 
      This catalog is automatically written by the LAS start up process.
      Changes made here will be lost.
      This TDS is reserved for LAS functions. You can set up your own
      for whatever purpose you need on the same tomcat using the content/thredds directory.
 -->

  <service name="dap" base="" serviceType="compound">
    <service name="odap" serviceType="OpenDAP" base="/las/thredds/dodsC/" />
    <service name="dap4" serviceType="DAP4" base="/las/thredds/dap4/" />
  </service>

  <datasetScan name="Data From LAS" path="las" location="$dynamicTempDir" serviceName="dap">
    <filter>
      <include wildcard="*.nc"/>
      <include wildcard="*.fds"/>
      <include wildcard="*.jnl"/>
    </filter>
  </datasetScan>

</catalog>
"""

        FileWriter configFileWriter
        if (Environment.current == Environment.DEVELOPMENT) {
            configFileWriter = new FileWriter(new File("/home/rhs/tomcat/content/ftds/catalog.xml"))
        } else {
            File outputFile = Holders.grailsApplication.mainContext.getResource("output").file
            File contextPath = outputFile.getParentFile();
            File webapp = contextPath.getParentFile();
            String tomcat = webapp.getParent();
            configFileWriter = new FileWriter(new File("${tomcat}/content/ftds/catalog.xml"))
        }
        catalog.writeTo(configFileWriter).close()
    }
    def writeFerretXml(Ferret ferret, FerretEnvironment ferretEnvironment) {
        Document doc = new Document()
        Element root = new Element("application")
        doc.setRootElement(root)
        Element invoker = new Element("invoker")
        invoker.setAttribute("executable", ferret.getPath())
        invoker.setAttribute("iosp_temp_dir", ferret.getTempDir()+File.separator+"temp")
        for (int i = 0; i < ferret.getArguments().size(); i++) {
            Argument a = ferret.getArguments().get(i)
            Element arg = new Element("arg")
            arg.setText(a.getValue())
            invoker.addContent(arg)
        }
        root.addContent(invoker)
        Element environment = new Element("environment")
        root.addContent(environment)
        if ( ferretEnvironment.getFer_data())
            environment.addContent(makeEnvVariable("FER_DATA", ferretEnvironment.getFer_data()))
        if ( ferretEnvironment.getFer_descr() )
            environment.addContent(makeEnvVariable("FER_DESCRS", ferretEnvironment.getFer_descr()))
        if ( ferretEnvironment.getFer_dir() )
            environment.addContent(makeEnvVariable("FER_DIR", ferretEnvironment.getFer_dir()))
        if ( ferretEnvironment.getFer_dsets() )
            environment.addContent(makeEnvVariable("FER_DSETS", ferretEnvironment.getFer_dsets()))
        if ( ferretEnvironment.getFer_external_functions() )
            environment.addContent(makeEnvVariable("FER_EXTERNAL_FUNCTIONS", ferretEnvironment.getFer_external_functions()))
        if ( ferretEnvironment.getFer_fonts() )
            environment.addContent(makeEnvVariable("FER_FONTS", ferretEnvironment.getFer_fonts()))
        // Add FER_GO below with path to FER_GO for las#thredds
        if ( ferretEnvironment.getFer_grids() )
            environment.addContent(makeEnvVariable("FER_GRIDS", ferretEnvironment.getFer_grids()))
        if ( ferretEnvironment.getFer_libs() )
            environment.addContent(makeEnvVariable("FER_LIBS", ferretEnvironment.getFer_libs()))
        if ( ferretEnvironment.getFer_palette() )
            environment.addContent(makeEnvVariable("FER_PALETTE", ferretEnvironment.getFer_palette()))
        if ( ferretEnvironment.getLd_library_path() )
            environment.addContent(makeEnvVariable("LD_LIBRARY_PATH", ferretEnvironment.getLd_library_path()))
        if ( ferretEnvironment.getPythonpath() )
            environment.addContent(makeEnvVariable("PYTHONPATH", ferretEnvironment.getPythonpath()))
        if ( ferretEnvironment.getPlotfonts() )
            environment.addContent(makeEnvVariable("PLOTFONTS", ferretEnvironment.getPlotfonts()))
        // Write
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat())
        FileWriter configFileWriter
        if (Environment.current == Environment.DEVELOPMENT) {
            configFileWriter = new FileWriter(new File("/home/rhs/tomcat/webapps/las#thredds/WEB-INF/classes/FerretConfig.xml"))
            if ( ferretEnvironment.getFer_go() )
                environment.addContent(makeEnvVariable("FER_GO", ferretEnvironment.getFer_go()))
        } else {
            File outputFile = Holders.grailsApplication.mainContext.getResource("output").file
            File contextPath = outputFile.getParentFile();
            String webapp = contextPath.getParent();
            if ( ferretEnvironment.getFer_go() )
                environment.addContent(makeEnvVariable("FER_GO", ferretEnvironment.getFer_go()+" ${webapp}/las#thredds/WEB-INF/classes/resources/iosp/scripts"))
            configFileWriter = new FileWriter(new File("${webapp}/las#thredds/WEB-INF/classes/resources/iosp/FerretConfig.xml"))
        }
        out.output(doc, configFileWriter)
    }

    def updateFromExternalConfig() {

    }
    def Element makeEnvVariable(String name, String value) {
        Element fer_variable = new Element("variable")
        Element fer_name = new Element("name")
        fer_name.setText(name)
        fer_variable.addContent(fer_name)
        String[] v = value.split(" ")
        for (int i = 0; i < v.length; i++) {
            Element fer_value = new Element("value")
            fer_value.setText(v[i])
            fer_variable.addContent(fer_value)
        }
        fer_variable
    }

    def createDefaultRegions(boolean reinit) {

        if ( reinit ) {
            def regions = Region.findAll()
            regions.each {
                it.delete(flush:it==regions.last(), failOnError:true)
            }
        }
        // These are s, n, w, e

        // For the OSMC Dashboard, don't care about land masses
//        Region africa = new Region([name: "africa", title: "Africa", southLat: -40.0d, northLat: 40.0d, westLon: -20.0d, eastLon: 60.0d])
//        africa.save(failOnError: true)
//
//        Region asia = new Region([name: "asia", title: "Asian", southLat: 0.0d, northLat: 80.0d, westLon: 40.0d, eastLon: 180.0d])
//        asia.save(failOnError: true)
//
//        Region australia = new Region([title: "Australia",name: "australia", southLat: -50.0d, northLat:  0.0d, westLon:  110.0d, eastLon:  180.0d])
//        australia.save(failOnError: true)
//
//        Region europe = new Region ([title: "Europe", name:  "europe", southLat: 30.0d, northLat:  75.0d, westLon: -10.0d, eastLon:  40.0d])
//        europe.save(failOnError: true)
//
//        Region north_america = new Region([title: "North America", name: "north america", southLat:  10.0d, northLat:  75.0d, westLon: -170.0d, eastLon: -50.0d])
//        north_america.save(failOnError: true)
//
//        Region south_america = new Region([title: "South America", name: "south america", southLat: -60.0d, northLat: 15.0d, westLon: -90.0d, eastLon:-30.0d])
//        south_america.save(failOnError: true)

        Region global_0 = new Region([title: "Global (0)", name: "global 0", southLat: -90.0d, northLat: 90.0d, westLon: -180.0d, eastLon: 180.0d])
        global_0.save(failOnError: true)

        Region global_180 = new Region([title: "Global (180)", name: "global 180", southLat: -90.0d, northLat: 90.0d, westLon: 0.0d, eastLon: 180.0d])
        global_180.save(failOnError: true)

        Region indian_ocean = new Region ([title: "Indian Ocean", name: "indian ocean", southLat: -75.0d, northLat:  30.0d, westLon:  20.0d, eastLon:  120.0d])
        indian_ocean.save(failOnError: true)

        Region north_atlantic = new Region([title: "North Atlantic",name: "north atlantic", southLat: 0.0d, northLat:  70.0d, westLon: -80.0d, eastLon: 20.0])
        north_atlantic.save(failOnError: true)

        Region equatorial_atlantic = new Region([title: "Equatorial Atlantic", name: "equatorial atlantic", southLat: -30.0d, northLat: 30.0d, westLon: -80.0, eastLon: 20.0])
        equatorial_atlantic.save(failOnError: true)

        Region south_atlantic = new Region([title: "South Atlantic", name: "south atlantic", southLat: -75.0d, northLat:  10.0d, westLon: -70.0d, eastLon:  25.0d])
        south_atlantic.save(failOnError: true)

        Region north_pacific = new Region([title: "North Pacific",name: "north pacific", southLat:  0.0d, northLat:  70.0d, westLon:  110.0d,eastLon:  260.0d])
        north_pacific.save(failOnError: true)

        Region equatorial_pacific = new Region([title: "Equatorial Pacific", name: "equatorial pacific", southLat: -30.0d, northLat: 30.0d, westLon:  135.0d, eastLon:  285.0d])
        equatorial_pacific.save(failOnError: true)

        Region south_pacific = new Region([title: "South Pacific", name: "south pacific", southLat: -75.0d, northLat:  0.0d, westLon:  150.0d, eastLon:  290.0])
        south_pacific.save(failOnError: true)

        Region gulf_of_mexico = new Region([title: "Gulf of Mexico", name: "gulf of mexico", southLat: 15.0d, northLat: 31.0d, westLon: -100.0d, eastLon: -82.0d])
        gulf_of_mexico.save(failOnError: true)

        Region gulf_and_caribbean = new Region([title: "Gulf of Mexico and Caribbean", name: "gulf and caribbean", southLat: 6.0d, northLat: 35.0d, westLon: -100.0d, eastLon: -56.0d])
        gulf_and_caribbean.save(failOnError: true)

        Region eastern_seaboard = new Region([title: "Eastern Seaboard", name: "eastern seaboard", southLat: 22.0d, northLat: 55.0d, westLon: -84.0d, eastLon: -41.0d])
        eastern_seaboard.save(failOnError: true)
    }

    /**
     * Create the options and save them...
     */

    def createProducts(boolean reinit) {

        if ( reinit ) {
            def products = Product.findAll()
            products.each {
                it.delete(flush:it==products.last(), failOnError:true)
            }
        }
        /*

  <operation name="Point Location value Plot" ID="Point_location_value_plot" output_template="zoom" default="true" category="visualization">
    <operation name="Database Extraction" ID="DBExtract" output_template="" service_action="Timeseries_interactive_plot">
      <response ID="DBExtractResponse">
        <result type="debug" ID="db_debug" file_suffix=".txt"/>
        <result type="netCDF" ID="netcdf" file_suffix=".nc"/>
        <result type="cancel" ID="cancel"/>
      </response>
      <service>tabledap</service>
    </operation>
    <operation chained="true" name="In-situ location and value plot" ID="Plot_insitu_XY_locations_and_values" output_template="zoom" service_action="Plot_insitu_XY_locations_and_values">
      <args>
        <arg chained="true" type="variable" index="1" operation="DBExtract" result="netcdf" file_suffix=".nc"/>
        <arg type="region" index="1" ID="in-situ-Region"/>
      </args>
      <response ID="PlotResp" type="HTML" index="1">
        <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
        <result type="svg" ID="plot_svg" file_suffix=".svg"/>
        <result type="ps" ID="plot_ps" file_suffix=".ps"/>
        <result type="pdf" ID="plot_pdf" file_suffix=".pdf"/>
        <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
        <result type="js_total" ID="js_total" file_suffix=".js"/>
        <result type="map_data" ID="map_data" file_suffix=".txt"/>
        <result type="image" ID="ref_map" file_suffix=".png"/>
        <result type="debug" ID="debug" file_suffix=".txt"/>
        <result type="cancel" ID="cancel"/>
        <result type="xml" ID="webrowset" file_suffix=".xml"/>
      </response>
      <service>ferret</service>
    </operation>
    <optiondef IDREF="Timeseries_Options"/>
    <region>
      <intervals name="xyt" type="Maps" title="Latitude-Longitude"/>
    </region>
  </operation>

         */

        Product point_location = Product.findByName("Point_location_value_plot")
        if ( !point_location ) {
            point_location = new Product([name:"Point_location_value_plot", title: "Location Plot", view: "xyz", data_view: "xyzt", ui_group: "Maps", geometry: GeometryType.POINT, product_order: "100001"])

            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            Operation operation_location_plot = new Operation([name: "Location_plot", service_action: "Plot_insitu_XY_locations_and_values", type: "ferret", output_template: "zoom"])
            operation_location_plot.setResultSet(resultsService.getPlotResults())
            // #Timeseries_palette,#size,#fill_levels,#deg_min_sec,#set_aspect,#use_graticules,#full_data,#bathymetry_style
            operation_location_plot.addToMenuOptions(optionsService.getPalettes())
            operation_location_plot.addToTextOptions(optionsService.getFill_levels())
            operation_location_plot.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_location_plot.addToYesNoOptions(optionsService.getSet_aspect())
            operation_location_plot.addToMenuOptions(optionsService.getUse_graticules())

            point_location.addToOperations(operation_extract_data)
            point_location.addToOperations(operation_location_plot)

            point_location.save(failOnError: true)
        }

        /*

 <operation chained="true" ID="Profile_Plot_2D" service_action="Plot_2D_Profile" order="0101" category="visualization">
    <args>
        <arg chained="true" type="variable" index="1" operation="DBExtract" result="netcdf" file_suffix=".nc"/>
        <arg type="region" index="1" ID="in-situ-Region"/>
    </args>
    <service>ferret</service>
    <response ID="PlotResp">
      <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
      <!-- <result type="ps" ID="plot_postscript" streamable="true" mime_type="application/postscript"
                                /> -->
      <result type="image" ID="ref_map" file_suffix=".png"/>
      <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
      <result type="debug" ID="debug" file_suffix=".txt"/>
      <result type="cancel" ID="cancel" file_suffix=".txt"/>
    </response>
    </operation>
     <region>
      <intervals name="xzt" type="Depth Profiles" title="Longitude-Depth"/>
      <intervals name="yzt" type="Depth Profiles" title="Latitude-Depth"/>
      <intervals name="zt" type="Hovmoller Plots" title="Latitude-Depth"/>
    </region>
    <grid_types>
      <grid_type name="profile"/>
    </grid_types>
    <optiondef IDREF="Options_2D_image_contour_7"/>
  </operation>


         */
        Product profile = Product.findByName("Profile_Plot_2D")
        if ( !profile ) {
            profile = new Product([name:"Profile_Plot_2D", title: "Profile Plot", view: "zt", data_view: "xyzt", ui_group: "Profile Plots", geometry: GeometryType.PROFILE, product_order: "100001"])
            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            Operation operation_plot_profile = new Operation([name: "Proflie_plot", service_action: "Plot_2D_Profile", type: "ferret", output_template: "zoom"])
            operation_plot_profile.setResultSet(resultsService.getPlotResults())

            //
            operation_plot_profile.addToTextOptions(optionsService.getExpression())
            operation_plot_profile.addToTextOptions(optionsService.getDep_axis_scale())
            operation_plot_profile.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_plot_profile.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_profile.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_plot_profile.addToMenuOptions(optionsService.getLine_or_sym())
            operation_plot_profile.addToMenuOptions(optionsService.getLine_color())
            operation_plot_profile.addToMenuOptions(optionsService.getLine_thickness())

            profile.addToOperations(operation_extract_data)
            profile.addToOperations(operation_plot_profile)

            profile.save(failOnError: true)
        }
        // Make a profile from a trajectory profile data set
        Product traj_profile = Product.findByName("Trajectory_Profile_Plot_2D")
        if ( !traj_profile ) {
            traj_profile = new Product([name:"Trajectory_Profile_Plot_2D", title: "Profile Plot", view: "zt", data_view: "xyzt", ui_group: "Profile Plots", geometry: GeometryType.TRAJECTORY_PROFILE, product_order: "100001"])
            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            Operation operation_plot_profile = new Operation([name: "Proflie_plot", service_action: "Plot_2D_Profile", type: "ferret", output_template: "zoom"])
            operation_plot_profile.setResultSet(resultsService.getPlotResults())

            //
            operation_plot_profile.addToTextOptions(optionsService.getExpression())
            operation_plot_profile.addToTextOptions(optionsService.getDep_axis_scale())
            operation_plot_profile.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_plot_profile.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_profile.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_plot_profile.addToMenuOptions(optionsService.getLine_or_sym())
            operation_plot_profile.addToMenuOptions(optionsService.getLine_color())
            operation_plot_profile.addToMenuOptions(optionsService.getLine_thickness())

            traj_profile.addToOperations(operation_extract_data)
            traj_profile.addToOperations(operation_plot_profile)

            traj_profile.save(failOnError: true)
        }
        /*

  <!-- This is the default XY plot. -->
  <operation name="Trajectory Plot" ID="Trajectory_interactive_plot" output_template="zoom" default="true" category="visualization">
    <operation name="Database Extraction" ID="DBExtract" output_template="" service_action="Trajectory_interactive_plot">
      <response ID="DBExtractResponse">
        <result type="debug" ID="db_debug" file_suffix=".txt"/>
        <result type="netCDF" ID="netcdf" file_suffix=".nc"/>
        <result type="cancel" ID="cancel"/>
      </response>
      <service>tabledap</service>
    </operation>
    <operation chained="true" name="Trajectory Map" ID="Trajectory_2D_poly" output_template="output" service_action="Trajectory_2D_poly">
      <args>
        <arg chained="true" type="variable" index="1" operation="DBExtract" result="netcdf" file_suffix=".nc"/>
        <arg type="region" index="1" ID="in-situ-Region"/>
      </args>
      <response ID="PlotResp" type="HTML" index="1">
        <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
        <result type="svg" ID="plot_svg" file_suffix=".svg"/>
        <result type="ps" ID="plot_ps" file_suffix=".ps"/>
        <result type="pdf" ID="plot_pdf" file_suffix=".pdf"/>
        <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
        <result type="js_total" ID="js_total" file_suffix=".js"/>
        <result type="map_data" ID="map_data" file_suffix=".txt"/>
        <result type="image" ID="ref_map" file_suffix=".png"/>
        <result type="debug" ID="debug" file_suffix=".txt"/>
        <result type="cancel" ID="cancel"/>
        <result type="xml" ID="webrowset" file_suffix=".xml"/>
      </response>
      <service>ferret</service>
    </operation>
    <optiondef IDREF="Trajectory_Options"/>
    <region>
      <intervals name="xyt" type="Maps" title="Latitude-Longitude"/>
    </region>
  </operation>


         */

        Product trajectory = Product.findByName("Trajectory_interactive_plot")
        if ( !trajectory ) {
            trajectory = new Product([name:"Trajectory_interactive_plot", title: "Trajectory Plot", view: "xy", data_view: "xyzt", ui_group: "Maps", geometry: GeometryType.TRAJECTORY, product_order: "100001"])
            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            Operation operation_plot_traj = new Operation([name: "Trajectory_plot", service_action: "Trajectory_2D_poly", type: "ferret", output_template: "zoom"])
            operation_plot_traj.setResultSet(resultsService.getPlotResults())

            //
            operation_plot_traj.addToTextOptions(optionsService.getExpression())
            operation_plot_traj.addToTextOptions(optionsService.getDep_axis_scale())
            operation_plot_traj.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_plot_traj.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_traj.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_plot_traj.addToMenuOptions(optionsService.getLine_or_sym())
            operation_plot_traj.addToMenuOptions(optionsService.getLine_color())
            operation_plot_traj.addToYesNoOptions(optionsService.getSet_aspect())
            operation_plot_traj.addToMenuOptions(optionsService.getLine_thickness())

            trajectory.addToOperations(operation_extract_data)
            trajectory.addToOperations(operation_plot_traj)

            trajectory.save(failOnError: true)
        }

        // Make a trajectory plot from a trajectoryprofile data set
        Product trajectory_profile = Product.findByName("Trajectory_profile_interactive_plot")
        if ( !trajectory_profile ) {
            trajectory_profile = new Product([name:"Trajectory_profile_interactive_plot", title: "Trajectory Plot", view: "xy", data_view: "xyzt", ui_group: "Maps", geometry: GeometryType.TRAJECTORY_PROFILE, product_order: "100001"])
            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            Operation operation_plot_traj = new Operation([name: "Trajectory_plot", service_action: "Trajectory_2D_poly", type: "ferret", output_template: "zoom"])
            operation_plot_traj.setResultSet(resultsService.getPlotResults())

            //
            operation_plot_traj.addToTextOptions(optionsService.getExpression())
            operation_plot_traj.addToTextOptions(optionsService.getDep_axis_scale())
            operation_plot_traj.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_plot_traj.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_traj.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_plot_traj.addToMenuOptions(optionsService.getLine_or_sym())
            operation_plot_traj.addToMenuOptions(optionsService.getLine_color())
            operation_plot_traj.addToYesNoOptions(optionsService.getSet_aspect())
            operation_plot_traj.addToMenuOptions(optionsService.getLine_thickness())

            trajectory_profile.addToOperations(operation_extract_data)
            trajectory_profile.addToOperations(operation_plot_traj)

            trajectory_profile.save(failOnError: true)
        }

/*
  <operation name="Correlation Plot" ID="Trajectory_correlation_extract_and_plot" default="true" category="file" output_template="zoom">
    <backend_service>
      <exclude>ferret,propprop</exclude>
    </backend_service>
    <operation name="Database Extraction" ID="DBExtract" service_action="Trajectory_correlation_plot">
      <response ID="DBExtractResponse">
        <result type="debug" ID="db_debug" file_suffix=".txt"/>
        <result type="netCDF" ID="netcdf" file_suffix=".nc"/>
        <result type="cancel" ID="cancel"/>
      </response>
      <service>tabledap</service>
      <properties>
        <property_group type="backend_request">
          <property>
            <name>exclude</name>
            <value>ferret,prop_hist</value>
          </property>
        </property_group>
      </properties>
    </operation>
    <operation chained="true" name="Trajectory Correlation Plot" ID="Trajectgory_correlation" service_action="Trajectory_correlation">
      <args>
        <arg chained="true" type="variable" index="1" operation="DBExtract" result="netcdf" file_suffix=".nc"/>
        <arg type="region" index="1" ID="in-situ-Region"/>
      </args>
      <response ID="PlotResp" type="HTML" index="1">
        <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
        <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
        <result type="debug" ID="debug" file_suffix=".txt"/>
        <result type="xml" ID="webrowset" file_suffix=".xml"/>
        <result type="icon_webrowset" ID="icon_webrowset" file_suffix=".xml"/>
        <result type="cancel" ID="cancel"/>
      </response>
      <service>ferret</service>
    </operation>
    <region>
      <intervals name="xyt"/>
    </region>
    <grid_types>
      <grid_type name="trajectory"/>
    </grid_types>
  </operation>
 */
        Product prop_prop_profile = Product.findByName("profile_prop_prop_plot")
        if (!prop_prop_profile) {
            prop_prop_profile = new Product([name: "profile_prop_prop_plot", title: "Property-Property Plot", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.PROFILE, product_order: "99999", hidden: "true"])

            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            Operation operation_plot_profile = new Operation([name: "Trajectory_correlation_plot", service_action: "Trajectory_correlation", type: "ferret", output_template: "zoom"])
            operation_plot_profile.setResultSet(resultsService.getPlotResults())

            prop_prop_profile.addToOperations(operation_extract_data)
            prop_prop_profile.addToOperations(operation_plot_profile)
            prop_prop_profile.save(failOnError: true)
        }

        Product prop_prop_traj = Product.findByName("trajectory_prop_prop_plot")
        if (!prop_prop_traj) {
            prop_prop_traj = new Product([name: "trajectory_prop_prop_plot", title: "Property-Property Plot", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.TRAJECTORY, product_order: "99999", hidden: "true"])

            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            Operation operation_plot_traj = new Operation([name: "Trajectory_correlation_plot", service_action: "Trajectory_correlation", type: "ferret", output_template: "zoom"])
            operation_plot_traj.setResultSet(resultsService.getPlotResults())

            prop_prop_traj.addToOperations(operation_extract_data)
            prop_prop_traj.addToOperations(operation_plot_traj)
            prop_prop_traj.save(failOnError: true)
        }

        Product prop_prop_ts = Product.findByName("time_series_prop_prop_plot")
        if (!prop_prop_ts) {

            prop_prop_ts = new Product([name: "time_series_prop_prop_plot", title: "Property-Property Plot", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.TIMESERIES, product_order: "99999", hidden: "true"])

            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            // TODO figure out what this is supposed to be
            Operation operation_plot_traj = new Operation([name: "Timeseries_correlation_plot", service_action: "Trajectory_correlation", type: "ferret", output_template: "zoom"])
            operation_plot_traj.setResultSet(resultsService.getPlotResults())

            prop_prop_ts.addToOperations(operation_extract_data)
            prop_prop_ts.addToOperations(operation_plot_traj)
            prop_prop_ts.save(failOnError: true)
        }
/*
    <response ID="PlotResp" type="HTML" index="1">
      <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
      <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
      <result type="debug" ID="debug" file_suffix=".txt"/>
      <result type="xml" ID="webrowset" file_suffix=".xml"/>
      <result type="cancel" ID="cancel"/>
    </response>

 */

        Product prop_prop = Product.findByName("prop_prop_plot")
        if (!prop_prop) {
            prop_prop = new Product([name: "prop_prop_plot", title: "Property-Property Plot", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation prop_prop_operation = new Operation([output_template: "zoom", service_action: "prop_prop_plot", type: "ferret"])
            prop_prop_operation.setResultSet(resultsService.getPlotResults())
            prop_prop.addToOperations(prop_prop_operation)
            prop_prop.save(failOnError: true)
        }


        /* data for display as a block in a window (not for download) but I think we only need save as and not show values

    <operation ID="Data_Extract" default="true" name="Table of values (text)" output_template="table" service_action="Data_Extract" order="0300" category="table" minvars="1" maxvars="9999">
    <service>ferret</service>
    <response ID="Data_Extract_Response">
      <result type="text" ID="ferret_listing" streamable="true" mime_type="text/plain"/>
      <result type="debug" ID="debug" file_suffix=".txt"/>
    </response>
         */
        // Hidden for now.  All the button products need a ui_group
        Product data_extract = Product.findByName("Data_Extract")
        if (!data_extract) {
            data_extract = new Product([name: "Data_Extract", title: "Show Values", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation data_extract_operation = new Operation([output_template: "table", service_action: "Data_Extract", type: "ferret"])
            data_extract_operation.setResultSet(resultsService.getDataExtractResults())
            data_extract.addToOperations(data_extract_operation)
            data_extract.save(failOnError: true)
        }

        // Hidden for now.  All the button products need a ui_group
        Product data_extract_netcdf = Product.findByName("Data_Extract_netCDF")
        if (!data_extract_netcdf) {
            data_extract_netcdf = new Product([name: "Data_Extract_netCDF", title: "Save as...", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation data_extract_netcdf_operation = new Operation([output_template: "table", service_action: "Data_Extract_netCDF", type: "ferret"])
            data_extract_netcdf_operation.setResultSet(resultsService.getDataExtractResultsCDF())
            data_extract_netcdf.addToOperations(data_extract_netcdf_operation)
            data_extract_netcdf.save(failOnError: true)
        }

        Product data_extract_file = Product.findByName("Data_Extract_File")
        if (!data_extract_file) {
            data_extract_file = new Product([name: "Data_Extract_File", title: "Save as...", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation data_extract_file_operation = new Operation([output_template: "table", service_action: "Data_Extract_File", type: "ferret"])
            data_extract_file_operation.setResultSet(resultsService.getDataExtractResultsFile())
            data_extract_file.addToOperations(data_extract_file_operation)
            data_extract_file.save(failOnError: true)
        }

        Product data_extract_csv = Product.findByName("Data_Extract_CSV")
        if (!data_extract_csv) {
            data_extract_csv = new Product([name: "Data_Extract_CSV", title: "Save as...", ui_group: "button", data_view: "xyzt", view: "xyzt", geometry: GeometryType.GRID, product_order: "99999", hidden: "true"])
            Operation data_extract_csv_operation = new Operation([output_template: "table", service_action: "Data_Extract_File", type: "ferret"])
            data_extract_csv_operation.setResultSet(resultsService.getDataExtractResultsCSV())
            data_extract_csv.addToOperations(data_extract_csv_operation)
            data_extract_csv.save(failOnError: true)
        }
        /*

        This are all the line plots, xyzt

         */

        // Sort order set by the product_order string. I use first three digits to sort the groups, and the next three digits for the operation within the group.
        Product t_line_plot = Product.findByName("Time")
        if (!t_line_plot) {

            t_line_plot = new Product([name: "Time", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200001", minArgs: 1, maxArgs: 10])
            Operation operation_t_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D", type: "ferret"])

            // inherit="#expression,#interpolate_data,#image_format,#size,#use_graticules,#margins,#deg_min_sec"/
            // inherit="#Options_Default_7,#line_or_sym,#trend_line,#line_color,#line_thickness,#dep_axis_scale"

            operation_t_line_plot.setResultSet(resultsService.getPlotResults())

            operation_t_line_plot.addToTextOptions(optionsService.getExpression())
            operation_t_line_plot.addToTextOptions(optionsService.getDep_axis_scale())
            operation_t_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_t_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_t_line_plot.addToMenuOptions(optionsService.getUse_graticules())
            operation_t_line_plot.addToMenuOptions(optionsService.getLine_or_sym())
            operation_t_line_plot.addToMenuOptions(optionsService.getLine_color())
            operation_t_line_plot.addToMenuOptions(optionsService.getLine_thickness())

            t_line_plot.addToOperations(operation_t_line_plot)
            t_line_plot.save(failOnError: true)
        }

        // DSG time series plot

        /*

  <operation chained="true" name="Timeseries Plot" ID="Timeseries_station_plot" output_template="output" service_action="Timeseries_station_plot">
      <args>
        <arg chained="true" type="variable" index="1" operation="DBExtract" result="netcdf" file_suffix=".nc"/>
        <arg type="region" index="1" ID="in-situ-Region"/>
      </args>
      <response ID="PlotResp" type="HTML" index="1">
        <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
        <result type="svg" ID="plot_svg" file_suffix=".svg"/>
        <result type="ps" ID="plot_ps" file_suffix=".ps"/>
        <result type="pdf" ID="plot_pdf" file_suffix=".pdf"/>
        <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
        <result type="js_total" ID="js_total" file_suffix=".js"/>
        <result type="map_data" ID="map_data" file_suffix=".txt"/>
        <result type="image" ID="ref_map" file_suffix=".png"/>
        <result type="debug" ID="debug" file_suffix=".txt"/>
        <result type="cancel" ID="cancel"/>
        <result type="xml" ID="webrowset" file_suffix=".xml"/>
      </response>
      <service>ferret</service>
    </operation>
    <optiondef IDREF="Options_1D_7"/>
    <region>
      <intervals name="xyt" type="Line Plots" title="Time Series"/>
    </region>
  </operation>

         */

        Product traj_time_series = Product.findByName("Trajectory_timeseries_plot")
        if ( !traj_time_series ) {
            traj_time_series = new Product([name:"Trajectory_timeseries_plot", title: "Timeseries Plot", view: "t", data_view: "xyzt", ui_group: "Line Plots", geometry: GeometryType.TRAJECTORY, product_order: "100002", minArgs: 1, maxArgs: 10])
            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            Operation operation_plot_ts = new Operation([name: "Trajectory_timeseries_plot", service_action: "Timeseries_station_plot", type: "ferret", output_template: "zoom"])
            operation_plot_ts.setResultSet(resultsService.getPlotResults())
            operation_plot_ts.addToTextOptions(optionsService.getExpression())
            operation_plot_ts.addToTextOptions(optionsService.getDep_axis_scale())
            operation_plot_ts.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_plot_ts.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_ts.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_plot_ts.addToMenuOptions(optionsService.getLine_or_sym())
            operation_plot_ts.addToMenuOptions(optionsService.getLine_color())
            operation_plot_ts.addToMenuOptions(optionsService.getLine_thickness())

            traj_time_series.addToOperations(operation_extract_data)
            traj_time_series.addToOperations(operation_plot_ts)

            traj_time_series.save(failOnError: true)
        }
/*
<!-- Time series (station) data operations -->
  <!-- This is the default XY plot. -->
  <operation name="Timeseries Location Plot" ID="Timeseries_interactive_plot" output_template="zoom" default="true" category="visualization">
    <operation name="Database Extraction" ID="DBExtract" output_template="" service_action="Timeseries_interactive_plot">
      <response ID="DBExtractResponse">
        <result type="debug" ID="db_debug" file_suffix=".txt"/>
        <result type="netCDF" ID="netcdf" file_suffix=".nc"/>
        <result type="cancel" ID="cancel"/>
      </response>
      <service>tabledap</service>
    </operation>
    <operation chained="true" name="Timeseries Plot" ID="Time_Series_Location_Plot" output_template="output" service_action="Profile_2D_poly">
      <args>
        <arg chained="true" type="variable" index="1" operation="DBExtract" result="netcdf" file_suffix=".nc"/>
        <arg type="region" index="1" ID="in-situ-Region"/>
      </args>
      <response ID="PlotResp" type="HTML" index="1">
        <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
        <result type="svg" ID="plot_svg" file_suffix=".svg"/>
        <result type="ps" ID="plot_ps" file_suffix=".ps"/>
        <result type="pdf" ID="plot_pdf" file_suffix=".pdf"/>
        <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
        <result type="js_total" ID="js_total" file_suffix=".js"/>
        <result type="map_data" ID="map_data" file_suffix=".txt"/>
        <result type="image" ID="ref_map" file_suffix=".png"/>
        <result type="debug" ID="debug" file_suffix=".txt"/>
        <result type="cancel" ID="cancel"/>
        <result type="xml" ID="webrowset" file_suffix=".xml"/>
      </response>
      <service>ferret</service>
    </operation>
    <optiondef IDREF="Timeseries_Options"/>
    <region>
      <intervals name="xyt" type="Maps" title="Latitude-Longitude"/>
    </region>
  </operation>

 */
        Product dsg_time_series_location = Product.findByName("Timeseries_station_location_plot")
        if ( !dsg_time_series_location ) {
            dsg_time_series_location = new Product([name: "Timeseries_station_location_plot", title: "Timeseries Locations", view: "xy", data_view: "xyzt", ui_group: "Maps", geometry: GeometryType.TIMESERIES, product_order: "100002"])
            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())
            Operation operation_plot_ts_loc = new Operation([name: "Timeseries_station_plot", service_action: "Profile_2D_poly", type: "ferret", output_template: "zoom"])
            operation_plot_ts_loc.setResultSet(resultsService.getPlotResults())
            operation_plot_ts_loc.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_ts_loc.addToYesNoOptions(optionsService.getDeg_min_sec())
            dsg_time_series_location.addToOperations(operation_extract_data)
            dsg_time_series_location.addToOperations(operation_plot_ts_loc)
            dsg_time_series_location.save(failOnError: true)
        }
        Product dsg_profile_location = Product.findByName("Profile_location_plot")
        if ( !dsg_profile_location ) {
            dsg_profile_location = new Product([name: "Timeseries_station_location_plot", title: "Profile Locations", view: "xy", data_view: "xyzt", ui_group: "Maps", geometry: GeometryType.PROFILE, product_order: "100002"])
            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())
            Operation operation_plot_ts_loc = new Operation([name: "Profile_station_plot", service_action: "Profile_2D_poly", type: "ferret", output_template: "zoom"])
            operation_plot_ts_loc.setResultSet(resultsService.getPlotResults())
            operation_plot_ts_loc.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_ts_loc.addToYesNoOptions(optionsService.getDeg_min_sec())
            dsg_profile_location.addToOperations(operation_extract_data)
            dsg_profile_location.addToOperations(operation_plot_ts_loc)
            dsg_profile_location.save(failOnError: true)
        }
        Product dsg_time_series = Product.findByName("Timeseries_station_plot")
        if ( !dsg_time_series ) {
            dsg_time_series = new Product([name:"Timeseries_station_plot", title: "Timeseries Plot", view: "t", data_view: "xyzt", ui_group: "Line Plots", geometry: GeometryType.TIMESERIES, product_order: "100001", maxArgs: 10])
            Operation operation_extract_data = new Operation([name: "ERDDAPExtract", type: "erddap", service_action: "erddap"])
            operation_extract_data.setResultSet(resultsService.getNetcdfFile())

            Operation operation_plot_ts = new Operation([name: "Timeseries_station_plot", service_action: "Timeseries_station_plot", type: "ferret", output_template: "zoom"])
            operation_plot_ts.setResultSet(resultsService.getPlotResults())
            operation_plot_ts.addToTextOptions(optionsService.getExpression())
            operation_plot_ts.addToTextOptions(optionsService.getDep_axis_scale())
            operation_plot_ts.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_plot_ts.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_ts.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_plot_ts.addToMenuOptions(optionsService.getLine_or_sym())
            operation_plot_ts.addToMenuOptions(optionsService.getLine_color())
            operation_plot_ts.addToMenuOptions(optionsService.getLine_thickness())

            dsg_time_series.addToOperations(operation_extract_data)
            dsg_time_series.addToOperations(operation_plot_ts)

            dsg_time_series.save(failOnError: true)
        }

        Product z_line_plot = Product.findByName("Longitude")
        if (!z_line_plot) {

            z_line_plot = new Product([name: "Z", title: "Z", view: "z", data_view: "z", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200004"])
            Operation operation_z_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D", type: "ferret"])

            operation_z_line_plot.setResultSet(resultsService.getPlotResults())

            operation_z_line_plot.addToTextOptions(optionsService.getExpression())
            operation_z_line_plot.addToTextOptions(optionsService.getDep_axis_scale())
            operation_z_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_z_line_plot.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_z_line_plot.addToMenuOptions(optionsService.getUse_graticules())
            operation_z_line_plot.addToMenuOptions(optionsService.getLine_or_sym())
            operation_z_line_plot.addToMenuOptions(optionsService.getLine_color())
            operation_z_line_plot.addToMenuOptions(optionsService.getLine_thickness())


            z_line_plot.addToOperations(operation_z_line_plot)
            z_line_plot.save(failOnError: true)
        }


        Product y_line_plot = Product.findByName("Longitude")
        if (!y_line_plot) {

            y_line_plot = new Product([name: "Latitude", title: "Latitude", view: "y", data_view: "y", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200003", minArgs: 1, maxArgs: 10])
            Operation operation_y_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D", type: "ferret"])

            operation_y_line_plot.setResultSet(resultsService.getPlotResults())

            operation_y_line_plot.addToTextOptions(optionsService.getExpression())
            operation_y_line_plot.addToTextOptions(optionsService.getDep_axis_scale())
            operation_y_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_y_line_plot.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_y_line_plot.addToMenuOptions(optionsService.getUse_graticules())
            operation_y_line_plot.addToMenuOptions(optionsService.getLine_or_sym())
            operation_y_line_plot.addToMenuOptions(optionsService.getLine_color())
            operation_y_line_plot.addToMenuOptions(optionsService.getLine_thickness())

            y_line_plot.addToOperations(operation_y_line_plot)
            y_line_plot.save(failOnError: true)
        }

        Product x_line_plot = Product.findByName("Longitude")
        if (!x_line_plot) {

            x_line_plot = new Product([name: "Longitude", title: "Longitude", view: "x", data_view: "x", ui_group: "Line Plots", geometry: GeometryType.GRID, product_order: "200002", minArgs: 1, maxArgs: 10])
            Operation operation_x_line_plot = new Operation([output_template: "zoom", service_action: "Plot_1D", type: "ferret"])

            operation_x_line_plot.setResultSet(resultsService.getPlotResults())

            operation_x_line_plot.addToTextOptions(optionsService.getExpression())
            operation_x_line_plot.addToTextOptions(optionsService.getDep_axis_scale())
            operation_x_line_plot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_x_line_plot.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_x_line_plot.addToMenuOptions(optionsService.getUse_graticules())
            operation_x_line_plot.addToMenuOptions(optionsService.getLine_or_sym())
            operation_x_line_plot.addToMenuOptions(optionsService.getLine_color())
            operation_x_line_plot.addToMenuOptions(optionsService.getLine_thickness())

            x_line_plot.addToOperations(operation_x_line_plot)
            x_line_plot.save(failOnError: true)
        }

        /*

        Difference plot, this is "hidden" from the UI and called when an XY grid is to be differenced.
        All the decorations about the title and ui_group are superfluous

         */
        Product compare_plot = Product.findByName("Compare_Plot")
        if (!compare_plot) {

            // #expression,#interpolate_data,#image_format,#size,#use_ref_map,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            compare_plot = new Product([name: "Compare_Plot", title: "Latitude-Longitude", view: "xy", data_view: "xy", ui_group: "Maps", geometry: GeometryType.GRID, hidden: true, product_order: "999999"]) // Not in ui, order unnecessary
            Operation operation_comparePlot = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot", type: "ferret"])

            operation_comparePlot.setResultSet(resultsService.getPlotResults())

            operation_comparePlot.addToMenuOptions(optionsService.getPalettes())
            operation_comparePlot.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_comparePlot.addToTextOptions(optionsService.getFill_levels())

            compare_plot.addToOperations(operation_comparePlot)
            compare_plot.save(failOnError: true)
        }

        /*
        Difference plot, this is "hidden" from the UI and called when an XY grid is to be differenced.
                All the decorations about the title and ui_group are superfluous
                This is going to be for 1D in T

        */
        Product compare_plot_t = Product.findByName("Compare_Plot_T")
        if (!compare_plot_t) {

            // #expression,#interpolate_data,#image_format,#size,#use_ref_map,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            compare_plot_t = new Product([name: "Compare_Plot_T", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, hidden: true, product_order: "999999"]) // Not in ui, order unnecessary
            Operation operation_comparePlot_t = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot", type: "ferret"])

            operation_comparePlot_t.setResultSet(resultsService.getPlotResults())

            operation_comparePlot_t.addToMenuOptions(optionsService.getPalettes())
            operation_comparePlot_t.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_comparePlot_t.addToTextOptions(optionsService.getFill_levels())

            compare_plot_t.addToOperations(operation_comparePlot_t)
            compare_plot_t.save(failOnError: true)
        }
        /*
        Difference plot, this is "hidden" from the UI and called when an XY grid is to be differenced.
                All the decorations about the title and ui_group are superfluous
                This is going to be for 1D in X

        */
        Product compare_plot_x = Product.findByName("Compare_Plot_X")
        if (!compare_plot_x) {

            // #expression,#interpolate_data,#image_format,#size,#use_ref_map,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            compare_plot_x = new Product([name: "Compare_Plot_X", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, hidden: true, product_order: "999999"]) // Not in ui, order unnecessary
            Operation operation_comparePlot_x = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot", type: "ferret"])

            operation_comparePlot_x.setResultSet(resultsService.getPlotResults())

            operation_comparePlot_x.addToMenuOptions(optionsService.getPalettes())
            operation_comparePlot_x.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_comparePlot_x.addToTextOptions(optionsService.getFill_levels())

            compare_plot_x.addToOperations(operation_comparePlot_x)
            compare_plot_x.save(failOnError: true)
        }

        /*
        Difference plot, this is "hidden" from the UI and called when an XY grid is to be differenced.
                All the decorations about the title and ui_group are superfluous
                This is going to be for 1D in Y

        */
        Product compare_plot_y = Product.findByName("Compare_Plot_Y")
        if (!compare_plot_y) {

            // #expression,#interpolate_data,#image_format,#size,#use_ref_map,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            compare_plot_y = new Product([name: "Compare_Plot_Y", title: "Time", view: "t", data_view: "t", ui_group: "Line Plots", geometry: GeometryType.GRID, hidden: true, product_order: "999999"]) // Not in ui, order unnecessary
            Operation operation_comparePlot_y = new Operation([output_template: "xy_zoom", service_action: "Compare_Plot", type: "ferret"])

            operation_comparePlot_y.setResultSet(resultsService.getPlotResults())

            operation_comparePlot_y.addToMenuOptions(optionsService.getPalettes())
            operation_comparePlot_y.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_comparePlot_y.addToTextOptions(optionsService.getFill_levels())

            compare_plot_y.addToOperations(operation_comparePlot_y)
            compare_plot_y.save(failOnError: true)
        }

        /*

        Regular old, lat/lon map xy

        */
        Product plot_2d_xy = Product.findByGeometryAndViewAndData_viewAndHidden(GeometryType.GRID, "xy", "xy", false)

        if (!plot_2d_xy) {

            // #expression,#interpolate_data,#use_graticules,#margins,#deg_min_sec"
            // #palette
            // #contour_style,#fill_levels,#contour_levels,#mark_grid"
            // #set_aspect,#land_type

            // Later:
            // #image_format,#size,#use_ref_map,

            plot_2d_xy = new Product([name: "Plot_2D_XY", title: "Latitude-Longitude", view: "xy", data_view: "xy", ui_group: "Maps", geometry: GeometryType.GRID, product_order: "100001"])

            Operation operation_plot_2d_xy = new Operation([output_template: "xy_zoom", service_action: "Plot_2D_XY", type: "ferret"])

            operation_plot_2d_xy.setResultSet(resultsService.getPlotResults())

            operation_plot_2d_xy.addToTextOptions(optionsService.getExpression())
            operation_plot_2d_xy.addToYesNoOptions(optionsService.getInterpolate_data())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getUse_graticules())
            operation_plot_2d_xy.addToYesNoOptions(optionsService.getMargins())
            operation_plot_2d_xy.addToYesNoOptions(optionsService.getDeg_min_sec())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getPalettes())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getContour_style())
            operation_plot_2d_xy.addToTextOptions(optionsService.getFill_levels())
            operation_plot_2d_xy.addToTextOptions(optionsService.getContour_levels())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getMark_grid())
            operation_plot_2d_xy.addToYesNoOptions(optionsService.getSet_aspect())
            operation_plot_2d_xy.addToMenuOptions(optionsService.getLand_type())

            plot_2d_xy.addToOperations(operation_plot_2d_xy)
            plot_2d_xy.save(failOnError: true)

        }

        /*
        
        Vertical cross sections xz, yz
        
         */
        Product plot_2d_xz = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "xz", "xz")

        if ( !plot_2d_xz) {
            plot_2d_xz = new Product([name: "Plot_2D_xz", title: "Longitude-z", ui_group: "Vertical Cross Sections", view: "xz", data_view: "xz", geometry: GeometryType.GRID, product_order: "300001"])
            Operation operation_plot_2d_xz = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])

            operation_plot_2d_xz.setResultSet(resultsService.getPlotResults())

            operation_plot_2d_xz.addToMenuOptions(optionsService.getPalettes())

            plot_2d_xz.addToOperations(operation_plot_2d_xz)
            plot_2d_xz.save(failOnError: true)

        }

        Product plot_2d_yz = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "yz", "yz")

        if ( !plot_2d_yz) {
            plot_2d_yz = new Product([name: "Plot_2D_yz", title: "Latitude-z", ui_group: "Vertical Cross Sections", view: "yz", data_view: "yz", geometry: GeometryType.GRID, product_order: "300002"])
            Operation operation_plot_2d_yz = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])

            operation_plot_2d_yz.setResultSet(resultsService.getPlotResults())


            operation_plot_2d_yz.addToMenuOptions(optionsService.getPalettes())

            plot_2d_yz.addToOperations(operation_plot_2d_yz)
            plot_2d_yz.save(failOnError: true)

        }

        /*
        
        Hovmöller Diagrams, xt, yt and zt
        
         */
        Product plot_2d_xt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "xt", "xt")

        if ( !plot_2d_xt) {
            plot_2d_xt = new Product([name: "Plot_2D_xt", title: "Longitude-time", ui_group: "Hovmöller Diagram", view: "xt", data_view: "xt", geometry: GeometryType.GRID, product_order: "400001"])
            Operation operation_plot_2d_xt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])

            operation_plot_2d_xt.setResultSet(resultsService.getPlotResults())

            operation_plot_2d_xt.addToMenuOptions(optionsService.getPalettes())

            plot_2d_xt.addToOperations(operation_plot_2d_xt)
            plot_2d_xt.save(failOnError: true)

        }

        Product plot_2d_yt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "yt", "yt")

        if ( !plot_2d_yt) {
            plot_2d_yt = new Product([name: "Plot_2D_yt", title: "Latitude-time", ui_group: "Hovmöller Diagram", view: "yt", data_view: "yt", geometry: GeometryType.GRID, product_order: "400002"])
            Operation operation_plot_2d_yt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])

            operation_plot_2d_yt.setResultSet(resultsService.getPlotResults())

            operation_plot_2d_yt.addToMenuOptions(optionsService.getPalettes())

            plot_2d_yt.addToOperations(operation_plot_2d_yt)
            plot_2d_yt.save(failOnError: true)

        }

        Product plot_2d_zt = Product.findByGeometryAndViewAndData_view(GeometryType.GRID, "zt", "zt")

        if ( !plot_2d_zt) {
            plot_2d_zt = new Product([name: "Plot_2D_zt", title: "Z-time", ui_group: "Hovmöller Diagram", view: "zt", data_view: "zt", geometry: GeometryType.GRID, product_order: "400003"])
            Operation operation_plot_2d_zt = new Operation([output_template:"plot_zoom", service_action: "Plot_2D", type: "ferret"])
            ResultSet results_plot_2d = ResultSet.findByName("results_debug_image_mapscale_annotations")
            if ( results_plot_2d ) {
                ResultSet rs = new ResultSet(results_plot_2d.properties)
                operation_plot_2d_zt.setResultSet(rs)
            } else {
                log.error("Results sets not available. Did you use the results service method createReults before calling createOperations?")
            }
            MenuOption palettes = MenuOption.findByName("palette")
            if (palettes) {
                operation_plot_2d_zt.addToMenuOptions(palettes.properties)
            } else {
                log.error("Results sets not available. Did you use the results service method createOptions before calling createOperations?")
            }
            plot_2d_zt.addToOperations(operation_plot_2d_zt)
            plot_2d_zt.save(failOnError: true)

        }


        // See if the product already exists by name and title.  This implies name and title combinations should be unique.
//
//        Product timeseries_plot = Product.findByNameAndTitle("Timeseries Plot", "Timeseries")
//        if ( !timeseries_plot ) {
//            timeseries_plot = new Product([name: "Timeseries Plot", title: "Timeseries", ui_group: "Line Plots", view: "t", data_view: "xyt", geometry: GeometryType.TIMESERIES])
//            Operation operation_timeseries_plot = new Operation([service_action: "client_plot"])
//            timeseries_plot.addToOperations(operation_timeseries_plot)
//            timeseries_plot.save(failOnError: true)
//        }

        Product charts_traj_timeseries_plot = Product.findByNameAndTitle("Charts Traj Timeseries Plot", "Timeseries Plot");
        if ( !charts_traj_timeseries_plot ) {
            charts_traj_timeseries_plot = new Product([name: "Charts Traj Timeseries Plot", title: "Timeseries Plot", ui_group: "Interactive Line Plots", view: "t", data_view: "xyt", geometry: GeometryType.TRAJECTORY, minArgs: 1, maxArgs: 20,  product_order: "100001"])
            Operation operation_timeseries_plot = new Operation([service_action: "client_plot", type: "client"])
            charts_traj_timeseries_plot.addToOperations(operation_timeseries_plot)
            charts_traj_timeseries_plot.save(failOnError: true)
        }
        Product charts_timeseries_plot = Product.findByNameAndTitle("Charts Timeseries Plot", "Timeseries Plot");
        if ( !charts_timeseries_plot ) {
            charts_timeseries_plot = new Product([name: "Charts Timeseries Plot", title: "Timeseries Plot", ui_group: "Interactive Line Plots", view: "t", data_view: "xyt", geometry: GeometryType.TIMESERIES, product_order: "100001", maxArgs: 10])
            Operation operation_timeseries_plot = new Operation([service_action: "client_plot", type: "client"])
            charts_timeseries_plot.addToOperations(operation_timeseries_plot)
            charts_timeseries_plot.save(failOnError: true)
        }
/*
<!-- animate XY plots -->
<operation ID="Animation_2D_XY" default="true" name="Animation" output_template="output_animation" service_action="Data_Extract_Frames" order="9999" category="animation">
  <service>ferret</service>
  <response ID="Data_Extract_Frames_Response">
    <result type="xml" ID="ferret_listing" streamable="true" mime_type="text/xml" file_suffix=".xml"/>
    <result type="debug" ID="debug" file_suffix=".txt"/>
  </response>
  <region>
    <intervals name="xy"/>
  </region>
  <grid_types>
    <grid_type name="regular"/>
  </grid_types>
  <optiondef IDREF="Options_2D_image_contour_animation_xy"/>
</operation>
*/
        Product animateSetup = Product.findByName("Animation_2D_XY")
        if ( !animateSetup ) {
            animateSetup = new Product([name: "Animation_2D_XY", title: "Setup Animate", ui_group: "NONE", view: "xyt", data_view: "xyt", geometry: GeometryType.GRID, hidden: true, product_order: "999999"])
            Operation animateSetup_op = new Operation([output_template:"none", service_action: "Data_Extract_Frames", type: "ferret"])

            animateSetup_op.setResultSet(resultsService.getAnimateSetupResults())

            animateSetup_op.addToMenuOptions(optionsService.getPalettes())

            animateSetup.addToOperations(animateSetup_op)
            animateSetup.save(failOnError: true)
        }
/*
  <operation name="Vector plot" ID="Plot_vector" default="true" output_template="zoom" service_action="Plot_vector" order="0103" private="false" category="visualization" isZoomable="true">
    <service>ferret</service>
    <response ID="PlotResp">
      <result type="image" ID="plot_image" streamable="true" mime_type="image/png" file_suffix=".png"/>
      <!-- <result type="ps" ID="plot_postscript" streamable="true" mime_type="application/postscript"
                                file_suffix=".ps"/> -->
      <result type="image" ID="ref_map" file_suffix=".png"/>
      <result type="map_scale" ID="map_scale" file_suffix=".xml"/>
      <result type="debug" ID="debug" file_suffix=".txt"/>
      <result type="cancel" ID="cancel" file_suffix=".txt"/>
    </response>
    <region>
      <intervals name="xy" type="Maps" title="Latitude-Longitude"/>

      <intervals name="xt" type="Hovmoller Plots" title="Longitude-Time"/>
      <intervals name="yt" type="Hovmoller Plots" title="Latitude-Time"/>

      <intervals name="yz" type="Depth Profiles" title="Latitude-Depth"/>
      <intervals name="xz" type="Depth Profiles" title="Longitude-Depth"/>

      <intervals name="zt" type="Hovmoller Plots" title="Depth-Time"/>
    </region>
    <grid_types>
      <grid_type name="vector"/>
    </grid_types>
    <optiondef IDREF="Options_Vector_7"/>
  </operation>


    Repeat this for all the other views as shown in the intervals section above


 */
        Product vector = Product.findByName("Plot_vector")
        if ( !vector ) {
            vector = new Product([name: "Plot_vector", title: "Vector Plot", ui_group: "Maps", view: "xy", data_view: "xy", geometry: GeometryType.VECTOR, hidden: false, product_order: "000001"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector.addToOperations(vector_op)
            vector.save(failOnError: true)
        }

        Product vector_xt = Product.findByName("Plot_vector_xt")
        if ( !vector_xt ) {
            vector_xt = new Product([name: "Plot_vector_xt", title: "Vector Plot, Longitude Time", ui_group: "Hovmöller Diagram", view: "xt", data_view: "xt", geometry: GeometryType.VECTOR, hidden: false, product_order: "000001"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_xt.addToOperations(vector_op)
            vector_xt.save(failOnError: true)
        }

        Product vector_yt = Product.findByName("Plot_vector_yt")
        if ( !vector_yt ) {
            vector_yt = new Product([name: "Plot_vector_yt", title: "Vector Plot, Latitude Time", ui_group: "Hovmöller Diagram", view: "yt", data_view: "yt", geometry: GeometryType.VECTOR, hidden: false, product_order: "000002"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_yt.addToOperations(vector_op)
            vector_yt.save(failOnError: true)
        }

        Product vector_yz = Product.findByName("Plot_vector_yz")
        if ( !vector_yz ) {
            vector_yz = new Product([name: "Plot_vector_yz", title: "Vector Plot, Latitude Depth", ui_group: "Depth Profile", view: "yz", data_view: "yz", geometry: GeometryType.VECTOR, hidden: false, product_order: "000002"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_yz.addToOperations(vector_op)
            vector_yz.save(failOnError: true)
        }

        Product vector_xz = Product.findByName("Plot_vector_xz")
        if ( !vector_xz ) {
            vector_xz = new Product([name: "Plot_vector_xz", title: "Vector Plot, Longitude Depth", ui_group: "Depth Profile", view: "xz", data_view: "xz", geometry: GeometryType.VECTOR, hidden: false, product_order: "000001"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_xz.addToOperations(vector_op)
            vector_xz.save(failOnError: true)
        }

        Product vector_zt = Product.findByName("Plot_vector_zt")
        if ( !vector_zt ) {
            vector_zt = new Product([name: "Plot_vector_zt", title: "Vector Plot, Depth Time", ui_group: "Depth Time", view: "zt", data_view: "zt", geometry: GeometryType.VECTOR, hidden: false, product_order: "000001"])
            Operation vector_op = new Operation([output_template: "plot_zoom", service_action: "Plot_vector", type: "ferret"])
            vector_op.setResultSet(resultsService.getVectorResults())
            vector_op.addToTextOptions(optionsService.getVector_length())
            vector_op.addToTextOptions(optionsService.getVector_subsampling())
            vector_op.addToMenuOptions(optionsService.getVector_style())
            vector_zt.addToOperations(vector_op)
            vector_zt.save(failOnError: true)
        }
        /*
  <operation ID="Animation_2D_XY_vector" default="true" name="Animation" output_template="output_animation" service_action="Data_Extract_Frames" order="9999" category="animation">
    <service>ferret</service>
    <response ID="Data_Extract_Frames_Response">
      <result type="xml" ID="ferret_listing" streamable="true" mime_type="text/xml" file_suffix=".xml"/>
      <result type="debug" ID="debug" file_suffix=".txt"/>
    </response>
    <region>
      <intervals name="xyt"/>
      <intervals name="xy"/>
    </region>
    <grid_types>
      <grid_type name="vector"/>
    </grid_types>
    <optiondef IDREF="Options_Vector"/>
  </operation>

         */

        Product vectorAnim = Product.findByName("Animation_2D_XY_vector")
        if ( !vectorAnim ) {
            vectorAnim = new Product([name: "Animation_2D_XY_vector", title: "Animate Vector", ui_group: "none", view: "xy", data_view: "xy", geometry: GeometryType.VECTOR, hidden: true, product_order: "999999"])
            Operation vecAnimOp = new Operation([output_template: "plot_zoom", service_action: "Data_Extract_Frames", type: "ferret"])
            vecAnimOp.setResultSet(resultsService.getAnimateSetupResults())
            vecAnimOp.addToTextOptions(optionsService.getVector_length())
            vecAnimOp.addToTextOptions(optionsService.getVector_subsampling())
            vecAnimOp.addToMenuOptions(optionsService.getVector_style())
            vectorAnim.addToOperations(vecAnimOp)
            vectorAnim.save(failOnError: true)
        }
    }
    def loadDefaultLasDatasets() {

        // Only load default datasets if none exist...
        def count = Dataset.count()

        log.debug("Data set count = "+count);


        if ( count  == 0 ) {

            log.debug("No data sets found. Setting up default data sets.  Entered method...");

            // Set up the default LAS
            // We're going to use fer_data to find them, so Ferret must be configured first.
            def sites = Site.withCriteria{ne('title', 'Private Data')}
            Site site
            if ( sites && sites.size() > 0)
                site = sites[0]
            if (!site) {

                site = new Site([title: "Example LAS Site from Initial Installation"])
                // No site configured, so build the default site.

                // Add default footer links
                FooterLink f1 = new FooterLink([url: "https://www.noaa.gov/", linktext: "NOAA", linkindex: 1])
                FooterLink f2 = new FooterLink([url: "https://www.pmel.noaa.gov/", linktext: "PMEL", linkindex: 2])
                FooterLink f3 = new FooterLink([url: "https://www.noaa.gov/protecting-your-privacy", linktext: "Privacy", linkindex: 3])
                FooterLink f4 = new FooterLink([url: "mailto:roland.schweitzer@noaa.gov", linktext: "Contact Administrator", linkindex: 4])

                site.addToFooterLinks(f1)
                site.addToFooterLinks(f2)
                site.addToFooterLinks(f3)
                site.addToFooterLinks(f4)

                // Turn off toast message by default.
                site.setToast(false)

                // Default link to to LAS documentation
                site.setInfoUrl("https://ferret.pmel.noaa.gov/LAS/")

                FerretEnvironment ferretEnvironment = FerretEnvironment.first()
                if (!ferretEnvironment) {
                    return
                }

                def dsets = ferretEnvironment.fer_dsets

                def coads
                def levitus
                def ocean_atlas
                def leetmaaSurface
                def leetmaaDepth


                if (dsets) {
                    if (dsets.contains(" ")) {
                        def lookin = dsets.split("\\s")
                        lookin.each { datadir ->
                            if (new File("$datadir" + File.separator + "data" + File.separator + "coads_climatology.cdf").exists()) {
                                coads = "$datadir" + File.separator + "data" + File.separator + "coads_climatology.cdf"
                                ocean_atlas = "$datadir" + File.separator + "data" + File.separator + "ocean_atlas_subset.nc"
                                levitus = "$datadir" + File.separator + "data" + File.separator + "levitus_climatology.cdf"
                            }
                        }
                    } else {
                        coads = "$dsets" + File.separator + "data" + File.separator + "coads_climatology.cdf"
                        ocean_atlas = "$dsets" + File.separator + "data" + File.separator + "ocean_atlas_subset.nc"
                        levitus = "$dsets" + File.separator + "data" + File.separator + "levitus_climatology.cdf"
                    }
                }
                if (coads) {
                    log.debug("Ingesting COADS")
                    // Use the ferret shorthand for the default data set
                    def coadshash = IngestService.getDigest("coads_climatology")
                    Dataset coadsDS = Dataset.findByHash(coadshash)
                    if (!coadsDS) {
                        coadsDS = ingestService.ingest(coadshash, coads)
                        if (coadsDS) {
                            coadsDS.setTitle("COADS")
                            coadsDS.setStatus(Dataset.INGEST_FINISHED)
//                            Variable v = coadsDS.getVariables().get(0);
//                            v.addToVariableProperties(new VariableProperty([type: "ferret", name: "time_step", value: "3"]))
//                            coadsDS.addToDatasetProperties(new DatasetProperty([type: "ferret", name: "time_step", value: "4"]))
//                            Vector vector = new Vector();
//                            vector.setGeometry(GeometryType.VECTOR)
//                            vector.setTitle("Ocean Currents")
//                            Variable ucomp = coadsDS.getVariables().find { it.name == "UWND" };
//                            Variable vcomp = coadsDS.getVariables().find { it.name == "VWND" }
//                            vector.setHash(ucomp.getHash() + "_" + vcomp.getHash())
//                            vector.setName(ucomp.getName() + " and " + vcomp.getName())
//                            vector.setU(ucomp)
//                            vector.setV(vcomp)
//                            coadsDS.addToVectors(vector)
//                            coadsDS.save(flush: true)
                            site.addToDatasets(coadsDS)
                        }
                    }
                }
                if (ocean_atlas) {
                    log.debug("Ingesting the ocean atlas subset.")
                    def oahash = IngestService.getDigest("ocean_atlas_subset")
                    Dataset ocean_atlasDS = Dataset.findByHash(oahash)
                    if (!ocean_atlasDS) {
                        ocean_atlasDS = ingestService.ingest(oahash, ocean_atlas)
                        if (ocean_atlasDS) {
                            ocean_atlasDS.setTitle("Ocean Atlas Subset")
                            ocean_atlasDS.setStatus(Dataset.INGEST_FINISHED)
                            ocean_atlasDS.save(flush: true)
                            site.addToDatasets(ocean_atlasDS)
                        }
                    }
                }
                if (levitus) {
                    log.debug("Ingesting Levitus climatology")
                    def levhash = IngestService.getDigest("levitus_climatology.cdf")
                    Dataset levitusDS = Dataset.findByHash(levhash)
                    if (!levitusDS) {
                        levitusDS = ingestService.ingest(levhash, levitus)
                        if (levitusDS) {
                            levitusDS.setTitle("Levitus Ocean Climatology")
                            levitusDS.setStatus(Dataset.INGEST_FINISHED)
                            levitusDS.save(flush: true)
                            site.addToDatasets(levitusDS)
                        }
                    }
                }

//                log.debug("Ingesting carbon tracker THREDDS catalog.")
//                def carbonThredds = "http://ferret.pmel.noaa.gov/pmel/thredds/carbontracker.xml"
//                def carbon = Dataset.findByHash(IngestService.getDigest(carbonThredds))
//                if ( !carbon ) {
//                    carbon = ingestService.ingestFromThredds(carbonThredds, null)
//                    carbon.setStatus(Dataset.INGEST_FINISHED)
//                    carbon.save(flush: true)
//                    site.addToDatasets(carbon)
//                }


                // TODO GOOD EXAMPLES OF DSG DATA SETS FOR RELEASE??
//                log.debug("Ingesting example Timeseries DSG from ERDDAP")
//                def ts = "http://ferret.pmel.noaa.gov/engineering/erddap/tabledap/15min_w20_fdd7_a060"
//                List<AddProperty> properties = new ArrayList<>()
//                AddProperty hours = new AddProperty([name: "hours", value: ".25"])
//                properties.add(hours)
//                AddProperty display_hi = new AddProperty([name: "display_hi", value: "2018-02-20T00:00:00.000Z"])
//                properties.add(display_hi)
//                AddProperty display_lo = new AddProperty(([name: "display_lo", value: "2018-02-05T00:00:00.000Z"]))
//                properties.add(display_lo)
//                def dsgDataset = Dataset.findByHash(IngestService.getDigest(ts))
//                if (!dsgDataset) {
//                    dsgDataset = ingestService.ingestFromErddap(ts, properties)
//                    if (dsgDataset) {
//                        dsgDataset.setStatus(Dataset.INGEST_FINISHED)
//                        dsgDataset.save(flush: true)
//                        site.addToDatasets(dsgDataset)
//                    }
//                }

//                log.debug("Ingesting example trajectory")
//                def traj = "https://upwell.pfeg.noaa.gov/erddap/tabledap/LiquidR_HBG3_2015_weather"
//                def trajDataset = Dataset.findByHash(IngestService.getDigest(traj))
//                if (!trajDataset) {
//                    trajDataset = ingestService.ingestFromErddap(traj, null)
//                    if (trajDataset) {
//                        trajDataset.setStatus(Dataset.INGEST_FINISHED)
//                        trajDataset.save(flush: true)
//                        site.addToDatasets(trajDataset)
//                    }
//                }

//                log.debug("Ingesting example points")
//                def ais = "https://upwell.pfeg.noaa.gov/erddap/tabledap/hawaii_soest_01af_e372_5bb6"
//                def aisDataset = Dataset.findByHash(IngestService.getDigest(ais))
//                if (!aisDataset) {
//                    aisDataset = ingestService.ingestFromErddap(ais, null)
//                    if (aisDataset) {
//                        aisDataset.setStatus(Dataset.INGEST_FINISHED)
//                        aisDataset.save(flush: true)
//                        site.addToDatasets(aisDataset)
//                    }
//                }
                // TODO END OF --- GOOD EXMAPLES OF DSG DATA SETS FOR RELEASE??

//
//                log.debug("Ingesting example profile dataset")
//                def prof = "https://ferret.pmel.noaa.gov/alamo/erddap/tabledap/arctic_heat_alamo_profiles_9076"
//                def profDataset = Dataset.findByHash(IngestService.getDigest(prof))
//                if (!profDataset) {
//                    List<AddProperty> profileProps = new ArrayList<>()
//                    AddProperty add1 = new AddProperty([name: "hours", value: ".25"])
//                    profileProps.add(add1)
//                    profDataset = ingestService.ingestFromErddap(prof, profileProps)
//                    if (profDataset) {
//                        profDataset.setStatus(Dataset.INGEST_FINISHED)
//                        profDataset.save(flush: true)
//                        site.addToDatasets(profDataset)
//                    }
//                }

//                log.debug("Ingesting UAF THREDDS server")
//                //def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalog.xml"
//                def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/ecowatch.ncddc.noaa.gov/thredds/oceanNomads/aggs/catalog_g_ncom_aggs.xml"
//                // def uaf = "http://ferret.pmel.noaa.gov/uaf/thredds/CleanCatalogs/data.nodc.noaa.gov/thredds/catalog/ncml/woa13/catalog.xml"
//                def erddap = "http://upwell.pfeg.noaa.gov/erddap/"
//                def uafDataset = Dataset.findByHash(IngestService.getDigest(uaf))
//                if ( ! uafDataset ) {
//                    uafDataset = ingestService.ingestFromThredds(uaf, erddap)
//                    uafDataset.setStatus(Dataset.INGEST_FINISHED)
//                    uafDataset.save(flush: true)
//                    site.addToDatasets(uafDataset)
//                }

//                ingestService.cleanup(site)

//                def n = "https://oceanwatch.pfeg.noaa.gov/thredds/dodsC/satellite/QA/vekm/3day"
//                Dataset wind = ingestService.ingest(null, n)
//                if (wind) {
//                    wind.setStatus(Dataset.INGEST_FINISHED)
//                    wind.save()
//                    site.addToDatasets(wind)
//                }

                site.save(failOnError: true)

                ingestService.makeVectors()

            }
        }
    }
}
