package pmel.sdig.las

import grails.gorm.transactions.Transactional

@Transactional
class OptionsService {



    /*
    Each method returns a fully populated, but unsaved verison of the named option. You can modify it before attaching it to an Operation with .addToMenuOptions, .addToTextOptions, .addToYesNoOptions
     */


    /*

<optiondef name="use_ref_map">
<option>
  <help>
Draw a map showing the currently selected geographical region If <b>Default</b> is
selected, the server will decide whether it is appropriate to draw the map. If <B>No</B> is selected, the map is never drawn.
  </help>
  <title>Show reference map</title>
  <menu type="options" name="use_ref_map">
    <item values="default">Default</item>
    <item values="false">No</item>
    <item values="true">Yes</item>
  </menu>
</option>
</optiondef>

with the mini-map we don't really need this anymore...

     */

    // Yes No Options ...

    def YesNoOption getInterpolate_data() {

        YesNoOption interpolate = new YesNoOption([name        : "interpolate_data",
                                                   title       : "Interpolate Data Normal to the Plot?",
                                                   help        : "<p&gt;This interpolation affects the interpretation of coordinates\n" +
                                                           "that lie normal to the current view.\n" +
                                                           "For example, in a lat-long view (a traditional map) the time and\n" +
                                                           "depth axes are normal to the view.  If This interpolation is\n" +
                                                           "on LAS performs an interpolation to the exact specified normal\n" +
                                                           "coordinate(s) --  time and depth for a map view.  If off, LAS\n" +
                                                           "instead uses the data at the nearest grid point.\n" +
                                                           "(To be more precise, it uses the data at the grid point of the\n" +
                                                           "cell that contains the specified coordinate).\n" +
                                                           "</p&gt;\n" +
                                                           "<p&gt;For example:</p&gt;\n" +
                                                           "\n" +
                                                           "<p&gt;If the grid underlying the variable has points defined at Z=5\n" +
                                                           "and at Z=15 (with the grid box boundary at Z=10) and data is\n" +
                                                           "requested at Z=12 then with View interpolation set to &#8217;On&#8217; the\n" +
                                                           "data in the X-Y plane will be obtained by calculating the\n" +
                                                           "interpolated value of data at Z=12 between the Z=5 and Z=15 planes.\n" +
                                                           "With View interpolation set to &#8217;Off&#8217;, the data will be obtained\n" +
                                                           "from the data at Z=15.</p&gt;",
                                                   defaultValue: "no"])

        interpolate

    }

    // Options that require a menu of choices ...


    /*

        <optiondef name="stride_quality_factor">
          <option>
            <help>When visualizing variables that have a great many data points (high resolution) the system can respond faster by thinning (subsampling) the number of data points.   Setting the Quality to draft will use fewer points, thereby increasing speed but losing details in the images. Setting Quality to best will be slower but will reveal more detail.</help>
            <title>Quality</title>
            <menu type="options" name="stride_quality_factor">
              <item values="1.0">draft(fast)</item>
              <item values="0.5">medium</item>
              <item values="0.0">best(slow)</item>
            </menu>
          </option>
        </optiondef>

 */

    def MenuOption getStride_quality_factor() {
        MenuOption stride_quality_factor = new MenuOption(help: "When visualizing variables that have a great many data points (high resolution) the system can " +
                "respond faster by thinning (subsampling) the number of data points.   Setting the Quality to draft " +
                "will use fewer points, thereby increasing speed but losing details in the images. Setting Quality to " +
                "best will be slower but will reveal more detail.",
                name: "stride_quality_factor",
                title: "Quality")



        MenuItem sf001 = new MenuItem([value: "1.0", title: "draft(fast)"])
        MenuItem sf002 = new MenuItem([value: "0.5", title: "medium"])
        MenuItem sf003 = new MenuItem([value: "0.0", title: "best(slow)"])

        stride_quality_factor.addToMenuItems(sf001)
        stride_quality_factor.addToMenuItems(sf002)
        stride_quality_factor.addToMenuItems(sf003)

        stride_quality_factor


    }

    def MenuOption getUse_graticules() {
        /*

  <optiondef name="use_graticules">
    <option>
      <help>Turn on and off graticule lines on the plot, and set their color. None/No tics turns off both graticules and tic marks along the axes.</help>
      <title>Show graticule</title>
      <menu type="options" name="use_graticules">
        <item values="default">Default</item>
        <item values="black">Black</item>
        <item values="gray">Gray</item>
        <item values="white">White</item>
        <item values="none">None</item>
        <item values="notic">None/No tics</item>
      </menu>
    </option>
  </optiondef>

         */
        MenuOption use_graticules = new MenuOption([name        : "use_graticules",
                                                    title       : "Show graticule",
                                                    help        : "Turn on and off graticule lines on the plot, and set their color. None/No tics turns off both graticules and tic marks along the axes.</help>\n",
                                                    defaultValue: "gray"])

        MenuItem sf001 = new MenuItem([value:"black", title:"Black"])
        MenuItem sf002 = new MenuItem([value:"gray", title:"Gray"])
        MenuItem sf003 = new MenuItem([value:"white", title:"White"])
        MenuItem sf004 = new MenuItem([value:"none", title:"None"])
        MenuItem sf005 = new MenuItem([value:"notic", title:"None/No tics"])

        use_graticules.addToMenuItems(sf001)
        use_graticules.addToMenuItems(sf002)
        use_graticules.addToMenuItems(sf003)
        use_graticules.addToMenuItems(sf004)
        use_graticules.addToMenuItems(sf005)

        use_graticules

    }


    def MenuOption getData_format() {
        /*
    <optiondef name="data_format">
        <option>
            <help>Choose a file format</help>
            <title>ASCII file format</title>
            <menu type="options" name="data_format">
                <item values="tsv">Tab separated</item>
                <item values="csv">Comma separated</item>
                <item values="asc">FORTRAN formatted</item>
            </menu>
        </option>
    </optiondef>

 */

        MenuOption data_format = new MenuOption(help: "Choose a file format.", name: "data_format", title: "ASCII file format")



        MenuItem df001 = new MenuItem([value: "tsv", title: "Tab Separated"])
        MenuItem df002 = new MenuItem([value: "csv", title: "Comma Separated"])
        MenuItem df003 = new MenuItem([value: "asc", title: "FORTRAN Formatted"])

        data_format.addToMenuItems(df001)
        data_format.addToMenuItems(df002)
        data_format.addToMenuItems(df003)

        data_format

    }

    def MenuOption getVector_style() {

/*

<optiondef name="vector_style">
 <option>
   <help> This option sets a choice of standard vector arrows, or a "flowline" style, which draws a pathline integration of a 2-dimensional instantaneous flow field (it is not a streamline calculation). The default is vector arrows.
   </help>
   <title>Vector style</title>
   <menu type="options" name="vector_style">
     <item values="default">Default</item>
     <item values="1">Flowlines</item>
     <item values="0">Arrows</item>
   </menu>
 </option>
</optiondef>

*/

        MenuOption vector_style = new MenuOption([name     : "vector_style",
                                                  title       : "Vector style",
                                                  help        : "This option sets a choice of standard vector arrows, or a \"flowline\" style, which draws a pathline integration of a 2-dimensional instantaneous flow field (it is not a streamline calculation). The default is vector arrows.",
                                                  defaultValue: "arrows"])



        MenuItem sf001 = new MenuItem([value:"1", title:"Flowlines"])
        MenuItem sf002 = new MenuItem([value:"0", title:"Arrows"])

        vector_style.addToMenuItems(sf001)
        vector_style.addToMenuItems(sf002)

        vector_style

    }

    def MenuOption getPalettes() {

        /*


  <optiondef name="interpolate_data">
  <optiondef name="expression">
  <optiondef name="expression2">
  <optiondef name="data_format">
  <optiondef name="stride_quality_factor">

  <optiondef name="time_step">

  <optiondef name="use_graticules">
  <optiondef name="use_ref_map">
  <optiondef name="margins">
  <optiondef name="deg_min_sec">
  <optiondef name="line_or_sym">
  <optiondef name="trend_line">
  <optiondef name="line_color">
  <optiondef name="line_thickness">

  <optiondef name="dep_axis_scale">
  <optiondef name="palette">
  <optiondef name="contour_style">
  <optiondef name="fill_levels">
  <optiondef name="contour_levels">
  <optiondef name="mark_grid">
  <optiondef name="set_aspect">
  <optiondef name="land_type">
  <optiondef name="orientation">
  <optiondef name="do_contour2">
  <optiondef name="contour_levels2">
  <optiondef name="fill_levels2">
  <optiondef name="palette2">
  <optiondef name="vector_subsampling">
  <optiondef name="vector_length">
  <optiondef name="vector_style">
  <optiondef name="gen_script_option">
  <optiondef name="tline_range">

  Later...
  <optiondef name="ge_overlay_style">
  <optiondef name="show_all_ensembles">
  <optiondef name="show_stddev_band">

  Don't know what to do with this since pyFerret allows more specific sizing
  <optiondef name="size">

  All png all the time
  <optiondef name="image_format">

         */
        MenuOption palettes = new MenuOption(help: "Set the color scale of the plot. Only applies to shaded plots.", name: "palette",
                title: "Color palettes", defaultValue: "rainbow")



        MenuItem p001 = new MenuItem([value: "rainbow", title: "Rainbow"])
        MenuItem p002 = new MenuItem([value: "rnb2", title: "Rainbow alternative"])
        MenuItem p003 = new MenuItem([value: "light_rainbow", title: "Rainbow pastel"])
        MenuItem p004 = new MenuItem([value: "rainbow_by_levels", title: "Rainbow (repeating by-level)"])
        MenuItem p005 = new MenuItem([value: "light_bottom", title: "Rainbow light bottom"])

        MenuItem p006 = new MenuItem([value: "ocean_temp", title: "Ocean temperature (consistent by-value)"])

        MenuItem p007 = new MenuItem([value: "land_sea", title: "topo: land and sea"])
        MenuItem p008 = new MenuItem([value: "dark_land_sea", title: "topo: land and sea, dark "])
        MenuItem p009 = new MenuItem([value: "land_sea_values", title: "topo: (consistent by value)"])
        MenuItem p010 = new MenuItem([value: "etop_values", title: "topo: etopo land and sea (consistent by value)"])
        MenuItem p011 = new MenuItem([value: "ocean_blue", title: "topo: blue bathymetry"])
        MenuItem p012 = new MenuItem([value: "terrestrial", title: "topo: land only"])
        MenuItem p013 = new MenuItem([value: "dark_terrestrial", title: "topo: land only, dark"])

        MenuItem p014 = new MenuItem([value: "inferno", title: "CM inferno (purple to orange to yellow)"])
        MenuItem p015 = new MenuItem([value: "magma", title: "CM magma (purple to yellow)"])
        MenuItem p016 = new MenuItem([value: "plasma", title: "CM plasma (lighter purple to yellow)"])
        MenuItem p017 = new MenuItem([value: "viridis", title: "CM viridis (blue to green)"])


        MenuItem p018 = new MenuItem([value: "cmocean_algae", title: "CMocean algae (light to dark greens)"])
        MenuItem p019 = new MenuItem([value: "cmocean_amp", title: "CMocean amp (light to dark browns)"])
        MenuItem p020 = new MenuItem([value: "cmocean_balance", title: "CMocean balance (centered blue and brown)"])
        MenuItem p021 = new MenuItem([value: "cmocean_curl", title: "CMocean curl (centered green and brown)"])
        MenuItem p022 = new MenuItem([value: "cmocean_deep", title: "CMocean deep (yellow to blue)"])
        MenuItem p023 = new MenuItem([value: "cmocean_delta", title: "CMocean delta (centered green and blue)"])
        MenuItem p024 = new MenuItem([value: "cmocean_dense", title: "CMocean dense (blues and purples)"])
        MenuItem p025 = new MenuItem([value: "cmocean_gray", title: "CMocean gray (dark to light grays)"])
        MenuItem p026 = new MenuItem([value: "cmocean_haline", title: "CMocean haline (blue to green)"])
        MenuItem p027 = new MenuItem([value: "cmocean_ice", title: "CMocean ice (dark to light blue)"])
        MenuItem p028 = new MenuItem([value: "cmocean_matter", title: "CMocean matter (yellow to brown)"])
        MenuItem p029 = new MenuItem([value: "cmocean_oxy", title: "CMocean oxy (red/ gray/ yellow)"])
        MenuItem p030 = new MenuItem([value: "cmocean_phase", title: "CMocean phase (smoothly varying)"])
        MenuItem p031 = new MenuItem([value: "cmocean_solar", title: "CMocean solar (brown to yellow)"])
        MenuItem p032 = new MenuItem([value: "cmocean_speed", title: "CMocean speed (yellow to green)  "])
        MenuItem p033 = new MenuItem([value: "cmocean_tempo", title: "CMocean tempo (light to dark green)"])
        MenuItem p034 = new MenuItem([value: "cmocean_thermal", title: "CMocean thermal (purple to yellow)"])
        MenuItem p035 = new MenuItem([value: "cmocean_turbid", title: "CMocean turbid (yellow to brown)"])

        MenuItem p036 = new MenuItem([value: "light_centered", title: "centered anomaly"])
        MenuItem p037 = new MenuItem([value: "white_centered", title: "centered w/white at center"])

        MenuItem p038 = new MenuItem([value: "no_blue_centered", title: "centered no-blue"])
        MenuItem p039 = new MenuItem([value: "no_green_centered", title: "centered no-green"])
        MenuItem p040 = new MenuItem([value: "no_red_centered", title: "centered no-red"])

        MenuItem p041 = new MenuItem([value: "bluescale", title: "scale of blues"])
        MenuItem p042 = new MenuItem([value: "bluescale", title: "scale of blues reversed"])
        MenuItem p043 = new MenuItem([value: "redscale", title: "scale of reds"])
        MenuItem p044 = new MenuItem([value: "redscale", title: "scale of blues reversed"])
        MenuItem p045 = new MenuItem([value: "greenscale", title: "scale of greens"])
        MenuItem p046 = new MenuItem([value: "greenscale", title: "scale of greens reversed"])
        MenuItem p047 = new MenuItem([value: "grayscale", title: "scale of grays"])
        MenuItem p048 = new MenuItem([value: "grayscale", title: "scale of grays reversed"])



        palettes.addToMenuItems(p001)
        palettes.addToMenuItems(p002)
        palettes.addToMenuItems(p003)
        palettes.addToMenuItems(p004)
        palettes.addToMenuItems(p005)
        palettes.addToMenuItems(p006)
        palettes.addToMenuItems(p007)
        palettes.addToMenuItems(p008)
        palettes.addToMenuItems(p009)

        palettes.addToMenuItems(p010)
        palettes.addToMenuItems(p011)
        palettes.addToMenuItems(p012)
        palettes.addToMenuItems(p013)
        palettes.addToMenuItems(p014)
        palettes.addToMenuItems(p015)
        palettes.addToMenuItems(p016)
        palettes.addToMenuItems(p017)
        palettes.addToMenuItems(p018)
        palettes.addToMenuItems(p019)

        palettes.addToMenuItems(p020)
        palettes.addToMenuItems(p021)
        palettes.addToMenuItems(p022)
        palettes.addToMenuItems(p023)
        palettes.addToMenuItems(p024)
        palettes.addToMenuItems(p025)
        palettes.addToMenuItems(p026)
        palettes.addToMenuItems(p027)
        palettes.addToMenuItems(p028)
        palettes.addToMenuItems(p029)

        palettes.addToMenuItems(p030)
        palettes.addToMenuItems(p031)
        palettes.addToMenuItems(p032)
        palettes.addToMenuItems(p033)
        palettes.addToMenuItems(p034)
        palettes.addToMenuItems(p035)
        palettes.addToMenuItems(p036)
        palettes.addToMenuItems(p037)
        palettes.addToMenuItems(p038)
        palettes.addToMenuItems(p039)

        palettes.addToMenuItems(p040)
        palettes.addToMenuItems(p041)
        palettes.addToMenuItems(p042)
        palettes.addToMenuItems(p043)
        palettes.addToMenuItems(p044)
        palettes.addToMenuItems(p045)
        palettes.addToMenuItems(p046)
        palettes.addToMenuItems(p047)
        palettes.addToMenuItems(p048)

        palettes
    }

    // Options that require user to enter text ...

    def TextOption getFill_levels() {

        TextOption fill_levels = new TextOption([name : "fill_levels",
                                                 hint: "Either number of levels or (lo, hi, delta)",
                                                 title: "Color Fill Levels",
                                                 help : "Set the color levels of the plot. Levels are described using Ferret syntax. The" +
                                                         "number of levels is approximate, and may be changed as the algorithm rounds off the values. " +
                                                         "Examples:" +
                                                         "<ul class=\"LAS-helplist LAS-helplist-hover\">" +
                                                         "<li class=\"LAS-helplist LAS-helplist-hover\"><b>60V</b> Draw 60 levels based on the variance of the data with open-ended extrema\n" +
                                                         "<li class=\"LAS-helplist LAS-helplist-hover\"><b>30H</b> Draw 30 levels based on a histogram\n" +
                                                         "<li class=\"LAS-helplist LAS-helplist-hover\"><b>25</b> Draw 25 levels spanning the range of the data\n" +
                                                         "<li class=\"LAS-helplist LAS-helplist-hover\"><b>30C</b> Draw 30 levels centered at 0\n" +
                                                         "<li class=\"LAS-helplist LAS-helplist-hover\"><b>(0,100,10)</b>  Bands of color starting at 0, ending at 100, with an interval of 10\n" +
                                                         "<li class=\"LAS-helplist LAS-helplist-hover\"><b>(-inf)(-10,10,0.25)(inf)</b> Bands of color between -10 and 10 with an additional color at each end of the spectrum representing all values below (-inf) or above (inf)\n" +
                                                         "<li class=\"LAS-helplist LAS-helplist-hover\"><b>(-100)(-10,10,0.25)(100)</b> Bands of color between -10 and 10 with a additional bands for all outlying values up to +/- 100.\n" +
                                                         "</ul>\n" +
                                                         "Detailed info is available in the Ferret User\\'s Guide., see Levels at\n" +
                                                         "http://ferret.pmel.noaa.gov/Ferret/documentation/users-guide/customizing-plots/CONTOURING#_VPINDEXENTRY_853"])
        fill_levels

    }
    def TextOption getVector_subsampling() {
        /*
           <!-- Vector plot Options -->

           <optiondef name="vector_subsampling">
           <option>
           <help>Enter two numbers: m,n. Ferret draws subsampled vectors along two coordinate directions beginning with the first vector requested. By default, Ferret automatically thins vectors to achieve a clear plot; this option gives you control over the sampling; every m-th vector in the horizontal direction, every n-th in the vertical. For FLOWline-style plots, enter one number which will be the "density" parameter. Lower numbers of density result in fewer lines.
                   </help>
         <title>Vector xskip,yskip</title>
           <textfield name="vector_subsampling"/>
       </option>
           </optiondef>
     */

        TextOption vector_subsampling = new TextOption(help: "Enter two numbers: m,n. Ferret draws subsampled vectors along two coordinate directions beginning with the first vector requested. By default, Ferret automatically thins vectors to achieve a clear plot; this option gives you control over the sampling; every m-th vector in the horizontal direction, every n-th in the vertical. For FLOWline-style plots, enter one number which will be the \"density\" parameter. Lower numbers of density result in fewer lines.\n" +
                "Detailed info is available in the Ferret User\\'s Guide., see Levels at\n" +
                "http://ferret.pmel.noaa.gov/Ferret/documentation/users-guide/customizing-plots/CONTOURING#_VPINDEXENTRY_853",
                name: "vector_subsampling",
                hint: " xskip, yskip",
                title: "Vector xskip,yskip")
        vector_subsampling
    }
    def TextOption getVector_length() {
        /*

        <optiondef name="vector_length">
          <option>
            <help> This associates the value with the standard vector length, normally one half inch. By default this is computed automatically based on the length of the vectors shown. On FLOWline-style plots, this number controls the length of the arrow-heads.
            </help>
            <title>Vector length scale</title>
            <textfield name="vector_length"/>
          </option>
        </optiondef>

        */
        TextOption vector_length = new TextOption(help: "This associates the value with the standard vector length, normally one half inch. By default this is computed automatically based on the length of the vectors shown. On FLOWline-style plots, this number controls the length of the arrow-heads.",
                name: "vector_length",
                hint: "Float value of length in inches",
                title: "Vector length scale")
        vector_length
    }

    def TextOption getTime_step() {
        /*

          <!-- animation time step i.e., delta T -->
          <optiondef name="time_step">
            <option>
              <help>Set the time step for animation. It is between 1 and the number of frames being selected.</help>
              <title>Time Step</title>
              <textfield name="time_step"/>
            </option>
          </optiondef>

     */

        TextOption time_step = new TextOption(help: "Set the time step for animation. It is between 1 and the number of frames being selected.",
                name: "time_step",
                hint: "10 would show every 10th time step.",
                title: "Animation Stride in Time")
        time_step

    }
    def YesNoOption getMargins() {

        /*
 <optiondef name="margins">
    <option>
      <help>
Make the plot with or without margins: when no margins is chosen, the axes are
at the edges of the plot (WMS-style plots). By default margins are shown.
      </help>
      <title>Margins</title>
      <menu type="options" name="margins">
        <item values="default">Default</item>
        <item values="false">No</item>
        <item values="true">Yes</item>
      </menu>
    </option>
  </optiondef>

         */

        YesNoOption margins = new YesNoOption([name        : "margins",
                                               title       : "Use Margins",
                                               help        : "Make the plot with or without margins: when no margins is chosen, the axes are\n" +
                                                       "at the edges of the plot (WMS-style plots). By default margins are shown.",
                                               defaultValue: "yes"])

        margins
    }
    def YesNoOption getDeg_min_sec() {

        /*
    <optiondef name="deg_min_sec">
    <option>
      <help>Format the labels on plot axes that are in units of degrees longitude or latitude as degrees,minutes rather than degrees and decimal fractions of degrees.  For axes with other units, this setting will be ignored.
      </help>
      <title>Degrees/Minutes axis labels</title>
      <menu type="options" name="deg_min_sec">
        <item values="default">Default</item>
        <item values="false">No</item>
        <item values="true">Yes</item>
      </menu>
    </option>
  </optiondef>

         */
        YesNoOption deg_min_sec = new YesNoOption([name        : "deg_min_sec",
                                                   title       : "Use degrees/minutes axis labels",
                                                   help        : "Format the labels on plot axes that are in units of degrees longitude or latitude as degrees,minutes rather than degrees and decimal fractions of degrees.  For axes with other units, this setting will be ignored.",
                                                   defaultValue: "no"])

        deg_min_sec
    }

    def MenuOption getLine_or_sym() {
        /*

  <optiondef name="line_or_sym">
    <option>
      <help>Draw a line or a symbol or both.</help>
      <title>Line Style</title>
      <menu type="options" name="line_or_sym">
        <item values="default">Default</item>
        <item values="sym">Symbol only</item>
        <item values="line">Line only</item>
        <item values="both">Both Symbol and Line</item>
      </menu>
    </option>
  </optiondef>

         */

        MenuOption line_or_sym = new MenuOption([name        : "line_or_sym",
                                                 title       : "Line Style",
                                                 help        : "Draw a line or a symbol or both.",
                                                 defaultValue: "line"])



        MenuItem sf001 = new MenuItem([value: "sym", title: "Symbol only"])
        MenuItem sf002 = new MenuItem([value: "line", title: "Line only"])
        MenuItem sf003 = new MenuItem([value: "both", title: "Both Symbol and Line"])

        line_or_sym.addToMenuItems(sf001)
        line_or_sym.addToMenuItems(sf002)
        line_or_sym.addToMenuItems(sf003)

        line_or_sym
    }


    def MenuOption getTrend_line() {

        /*
  <optiondef name="trend_line">
    <option>
      <help>Overlay a trend line computed by least-squares. For the option "Trend Line and Detrended", a second panel is added, showing the variable minus mean and variable minus mean and trend. Note that the slope of the trend is computed using the units of the independent axis. A monthly axis may have underlying units of days, so in such a case the slope will be data_units/days. Line color choices are ignored in this style. The plots may be zoomed - for 2-panel plots zoom on the upper or left panel.</help>
      <title>Trend Line</title>
      <menu type="options" name="trend_line">
        <item values="0">Default</item>
        <item values="0">none</item>
        <item values="1">With Trend Line</item>
        <item values="2">Trend Line and Detrended</item>
      </menu>
    </option>
  </optiondef>         */

        MenuOption trend_line = new MenuOption([name        : "trend_line",
                                                title       : "Trend Line",
                                                help        : "Overlay a trend line computed by least-squares. For the option \"Trend Line and Detrended\", a second panel is added, showing the variable minus mean and variable minus mean and trend. Note that the slope of the trend is computed using the units of the independent axis. A monthly axis may have underlying units of days, so in such a case the slope will be data_units/days. Line color choices are ignored in this style. The plots may be zoomed - for 2-panel plots zoom on the upper or left panel.",
                                                defaultValue: "line"])



        MenuItem sf001 = new MenuItem([value:"0", title:"None"])
        MenuItem sf002 = new MenuItem([value:"1", title:"With Trend Line"])
        MenuItem sf003 = new MenuItem([value:"2", title:"Trend Line and Detrended"])

        trend_line.addToMenuItems(sf001)
        trend_line.addToMenuItems(sf002)
        trend_line.addToMenuItems(sf003)

        trend_line

    }
    def MenuOption getLine_color() {
        /*
  <optiondef name="line_color">
    <option>
      <help>Set the color of the plot symbols and/or line.</help>
      <title>Line color (single-var plots)</title>
      <menu type="options" name="line_color">
        <item values="default">Default</item>
        <item values="black">Black</item>
        <item values="red">Red</item>
        <item values="green">Green</item>
        <item values="blue">Blue</item>
        <item values="lightblue">Light Blue</item>
        <item values="purple">Purple</item>
      </menu>
    </option>
  </optiondef>
         */

        MenuOption line_color = new MenuOption([name        : "line_color",
                                                title       : "Line color (single-var plots)",
                                                help        : "Set the color of the plot symbols and/or line.",
                                                defaultValue: "black"])



        MenuItem sf001 = new MenuItem([value:"black", title:"Black"])
        MenuItem sf002 = new MenuItem([value:"red", title:"Red"])
        MenuItem sf003 = new MenuItem([value:"green", title:"Green"])
        MenuItem sf004 = new MenuItem([value:"blue", title:"Blue"])
        MenuItem sf005 = new MenuItem([value:"lightblue", title:"Light Blue"])
        MenuItem sf006 = new MenuItem([value:"purple", title:"Purple"])

        line_color.addToMenuItems(sf001)
        line_color.addToMenuItems(sf002)
        line_color.addToMenuItems(sf003)
        line_color.addToMenuItems(sf004)
        line_color.addToMenuItems(sf005)
        line_color.addToMenuItems(sf006)

        line_color

    }
    def MenuOption getLine_thickness() {
        /*
  <optiondef name="line_thickness">
    <option>
      <help>Set the thickness of the plot symbols and/or line.</help>
      <title>Line thickness</title>
      <menu type="options" name="line_thickness">
        <item values="default">Default</item>
        <item values="1">Thin</item>
        <item values="2">Medium</item>
        <item values="3">Thick</item>
      </menu>
    </option>
  </optiondef>

         */
        MenuOption line_thickness = new MenuOption([name        : "line_thickness",
                                                    title       : "Line thickness",
                                                    help        : "Set the thickness of the plot symbols and/or line.",
                                                    defaultValue: "1"])



        MenuItem sf001 = new MenuItem([value:"1", title:"Thin"])
        MenuItem sf002 = new MenuItem([value:"2", title:"Medium"])
        MenuItem sf003 = new MenuItem([value:"3", title:"Thick"])


        line_thickness.addToMenuItems(sf001)
        line_thickness.addToMenuItems(sf002)
        line_thickness.addToMenuItems(sf003)

        line_thickness

    }

    def TextOption getExpression() {
        /*
 <optiondef name="expression">
    <option>
      <help>
Evaluate an algebraic expression. &lt;b&gt;$&lt;/b&gt; is used to represent
the current variable; you must have at least one &lt;b&gt;$&lt;/b&gt; in your expression. Example:&lt;p&gt;
&lt;b&gt;9/5 * $ + 32&lt;/b&gt;   Convert degrees C to Fahrenheit
      </help>
      <title>Evaluate expression</title>
      <textfield name="expression"/>
    </option>
  </optiondef>

         */
        TextOption expression = new TextOption([help:"Evaluate an algebraic expression. &lt;b&gt;\$&lt;/b&gt; is used to represent\n" +
                                                      "the current variable; you must have at least one &lt;b&gt;\$&lt;/b&gt; in your expression. Example:&lt;p&gt;\n" +
                                                      "&lt;b&gt;9/5 * \$ + 32&lt;/b&gt;   Convert degrees C to Fahrenheit",
                                                title: "Evaluate Expression",
                                                hint: '9/5*$+32',
                                                name: "expression"])
        expression
    }

    def TextOption getDep_axis_scale() {
        /*
  <optiondef name="dep_axis_scale">
    <option>
      <help>Set scale on the dependent axis lo,hi[,delta] where [,delta] is optional, units are data units. If a delta is given, it will determine the tic mark intervals. The dependent axis is the vertical axis for most plots; for plots of a variable vs height or depth it is the horizontal axis. If the scale is not set, Ferret determines this from the data.</help>
      <title>Dependent axis scale</title>
      <textfield name="dep_axis_scale"/>
    </option>
  </optiondef>

         */

        TextOption dep_axis_scale = new TextOption(help: "Set scale on the dependent axis lo,hi[,delta] where [,delta] is optional, units are data units. If a delta is given, it will determine the tic mark intervals. The dependent axis is the vertical axis for most plots; for plots of a variable vs height or depth it is the horizontal axis. If the scale is not set, Ferret determines this from the data.",
                name: "dep_axis_scale",
                hint: "lo, hi [,delta]",
                title: "Dependent axis scale")
        dep_axis_scale


    }

    def MenuOption getContour_style() {
        /*
<optiondef name="contour_style">
    <option>
      <help>What style of contours to draw
Choices are:
<ul>
<li><b>Default</b> -- let LAS decide
<li><b>Raster</b> -- Fill each grid cell with the appropriate color
<li><b>Color filled</b> -- Fill in between contour lines with color
<li><b>Lines</b> -- Just draw lines
<li><b>Raster and lines</b> -- Fill in each grid cell and draw lines on top
<li><b>Color filled and lines</b> -- Fill in between contour lines with color and draw lines on top
</ul>
      </help>
      <title>Contour style</title>
      <menu type="options" name="contour_style">
        <item values="default">Default</item>
        <item values="raster">Raster</item>
        <item values="color_filled_contours">Color filled</item>
        <item values="contour_lines">Lines</item>
        <item values="raster_plus_lines">Raster and lines</item>
        <item values="color_filled_plus_lines">Color filled and lines</item>
      </menu>
    </option>
  </optiondef>

         */

        MenuOption contour_style = new MenuOption([name        : "contour_style",
                                                   title       : "Contour Style",
                                                   help        : "What style of contours to draw\n" +
                                                           "Choices are:\n" +
                                                           "<ul class=\"LAS-helplist LAS-helplist-hover\">\n" +
                                                           "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Raster</b> -- Fill each grid cell with the appropriate color\n" +
                                                           "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Color filled (default)</b> -- Fill in between contour lines with color\n" +
                                                           "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Lines</b> -- Just draw lines\n" +
                                                           "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Raster and lines</b> -- Fill in each grid cell and draw lines on top\n" +
                                                           "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Color filled and lines</b> -- Fill in between contour lines with color and draw lines on top\n" +
                                                           "</ul>.",
                                                   defaultValue: "color_filled_contours"])



        MenuItem sf001 = new MenuItem([value:"raster", title:"Raster"])
        MenuItem sf002 = new MenuItem([value:"color_filled_contours", title:"Color filled"])
        MenuItem sf003 = new MenuItem([value:"raster_plus_lines", title:"Raster and lines"])
        MenuItem sf004 = new MenuItem([value:"color_filled_plus_lines", title:"Color filled and lines"])

        contour_style.addToMenuItems(sf001)
        contour_style.addToMenuItems(sf002)
        contour_style.addToMenuItems(sf003)
        contour_style.addToMenuItems(sf004)

        contour_style
    }
    def TextOption getContour_levels() {

        /*
 <optiondef name="contour_levels">
    <option>
      <help>Set the contour levels of the plot. Contour levels are described using Ferret syntax. Examples:
<ul><li><b>(0,100,10)</b>  Draw lines starting at 0, ending at 100, with an interval of 10
<li><b>25</b> Draw 25 lines
<li><b>10C</b> Draw 10 lines centered at 0
</ul>
Detailed info is available in the Ferret User\'s Guide., see Levels at
http://ferret.pmel.noaa.gov/Ferret/documentation/users-guide/customizing-plots/CONTOURING#_VPINDEXENTRY_853
</help>
      <title>Contour levels</title>
      <textfield name="contour_levels"/>
    </option>
  </optiondef>



         */

        TextOption contour_levels = new TextOption(help: "<p>Set the contour levels of the plot. Contour levels are described using Ferret syntax. Examples:</p>\n" +
                "<ul class=\"LAS-helplist LAS-helplist-hover\">" +
                "<li class=\"LAS-helplist LAS-helplist-hover\"><b>(0,100,10)</b>  Draw lines starting at 0, ending at 100, with an interval of 10\n" +
                "<li class=\"LAS-helplist LAS-helplist-hover\"><b>25</b> Draw 25 lines\n" +
                "<li class=\"LAS-helplist LAS-helplist-hover\"><b>10C</b> Draw 10 lines centered at 0\n" +
                "</ul>\n" +
                "Detailed info is available in the Ferret User\\'s Guide., see Levels at\n" +
                "http://ferret.pmel.noaa.gov/Ferret/documentation/users-guide/customizing-plots/CONTOURING#_VPINDEXENTRY_853",
                hint: "Number of contours or (lo, hi, delta)",
                name: "contour_levels", title: "Dependent axis scale")
        contour_levels

    }

    def MenuOption getMark_grid() {
        /*
  <!-- Default is no -->
  <optiondef name="mark_grid">
    <option>
      <help>Draw a mark at the middle of each grid cell on the plot.</help>
      <title>Mark grid points</title>
      <menu type="options" name="mark_grid">
        <item values="no">No</item>
        <item values="all">All Points</item>
        <item values="subsample">Subsampled</item>
      </menu>
    </option>
  </optiondef>
         */

        MenuOption mark_grid = new MenuOption([name        : "mark_grid",
                                               title       : "Mark grid points",
                                               help        : "Draw a mark at the middle of each grid cell on the plot.",
                                               defaultValue: "no"])



        MenuItem sf001 = new MenuItem([value:"no", title:"No"])
        MenuItem sf002 = new MenuItem([value:"all", title:"All Points"])
        MenuItem sf003 = new MenuItem([value:"subsample", title:"Subsampled"])

        mark_grid.addToMenuItems(sf001)
        mark_grid.addToMenuItems(sf002)
        mark_grid.addToMenuItems(sf003)

        mark_grid
    }
    def YesNoOption getSet_aspect() {
        /*
          <optiondef name="set_aspect">
    <option>
      <help>Have LAS calculate a suitable aspect ratio
Choices are:
<ul><li><b>Default</b> -- let LAS decide the aspect ratio
<li><b>Yes</b> -- Force LAS to calculate the aspect ratio of the plot based on the aspect ratio of the geographic region
<li><b>No</b> -- Do not change the aspect ratio based on the region.
</ul>
      </help>
      <title>Keep aspect ratio of region</title>
      <menu type="options" name="set_aspect">
        <item values="default">Default</item>
        <item values="1">Yes</item>
        <item values="0">No</item>
      </menu>
    </option>
  </optiondef>
         */

        YesNoOption set_aspect = new YesNoOption([name        : "set_aspect",
                                                  title       : "Keep aspect ratio of region",
                                                  help        : "Have LAS calculate a suitable aspect ratio\n" +
                                                          "Choices are:\n" +
                                                          "<ul class=\"LAS-helplist LAS-helplist-hover\">" +
                                                          "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Default</b> -- let LAS decide the aspect ratio\n" +
                                                          "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Yes</b> -- Force LAS to calculate the aspect ratio of the plot based on the aspect ratio of the geographic region\n" +
                                                          "<li class=\"LAS-helplist LAS-helplist-hover\"><b>No</b> -- Do not change the aspect ratio based on the region.\n" +
                                                          "</ul>",
                                                  defaultValue: "yes"])

        set_aspect
    }

    def MenuOption getLand_type() {
        /*
  <optiondef name="land_type">
    <option>
      <help>Style for drawing continents. Only applies to XY plots.
Choices are:
<ul><li><b>Default</b> -- let LAS decide
<li><b>None</b> -- don\'t draw continents
<li><b>Outline</b> -- draw continent outlines
<li><b>Filled</b> -- draw filled continents
</ul>
      </help>
      <title>Land fill style</title>
      <menu type="options" name="land_type">
        <item values="default">Default</item>
        <item values="none">None</item>
        <item values="contour">Outline</item>
        <item values="filled">Filled</item>
      </menu>
    </option>
  </optiondef>
         */
        MenuOption land_type = new MenuOption([name        : "land_type",
                                               title       : "Land fill style",
                                               help        : "Style for drawing continents. Only applies to XY plots.\n" +
                                                       "Choices are:\n" +
                                                       "<ul class=\"LAS-helplist LAS-helplist-hover\">" +
                                                       "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Default</b> -- let LAS decide\n" +
                                                       "<li class=\"LAS-helplist LAS-helplist-hover\"><b>None</b> -- don\\'t draw continents\n" +
                                                       "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Outline</b> -- draw continent outlines\n" +
                                                       "<li class=\"LAS-helplist LAS-helplist-hover\"><b>Filled</b> -- draw filled continents\n" +
                                                       "</ul>",
                                               defaultValue: "contour"])



        MenuItem sf001 = new MenuItem([value:"none", title:"None"])
        MenuItem sf002 = new MenuItem([value:"contour", title:"Outline"])
        MenuItem sf003 = new MenuItem([value:"filled", title:"Filled"])

        land_type.addToMenuItems(sf001)
        land_type.addToMenuItems(sf002)
        land_type.addToMenuItems(sf003)

        land_type
    }


}
