package pmel.sdig.las.client.util;

import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.GWT;
import gwt.material.design.client.constants.Color;

/**
 * Created by rhs on 5/31/16.
 */
public class Constants {
    public static final String base = GWT.getHostPageBaseURL();
    public static final String siteJson = base + "site/show";        // with {id}.json
    public static final String datasetJson = base + "dataset/show";  // requests look like dataset/show/{id}.json
    public static final String thumbinfo = base + "dataset/browse"; // Search for the first data set with variable children
    public static final String configJson = base + "config/json";
    public static final String productCreate = base + "product/make";
    public static final String cancelProduct = base + "product/cancel";
    public static final String erddapDataRequest = base + "product/erddapDataRequest";
    public static final String datatable = base + "product/datatable";
    public static final String productsByInterval = base + "config/productsByInterval";
    public static final String search = base + "search/search";
    public static final String regions = base + "config/regions";
    public static final String datasetSuggestSearch = base + "dataset/oracle";
    public static final String variableSuggestSearch = base + "variable/oracle";
    public static final String stream = base + "product/stream";

    public final static int navWidth = 400;
    public final static int imageBorderFactor = 200;

    public final static int PAGE = 10;


    public static final int rndRedColor = 244;
    public static final int rndGreenColor = 154;
    public static final int rndBlueColor = 0;
    public static final double rndAlpha = .45;

    public final static CssColor randomColor = CssColor.make("rgba(" + rndRedColor + ", " + rndGreenColor + "," + rndBlueColor + ", " + rndAlpha + ")");

    public static final String PANEL01 = "panel01"; // Main panel, upper left
    public static final String PANEL02 = "panel02"; // Second panel; upper right
    public static final String PANEL03 = "panel03"; // lower left
    public static final String PANEL04 = "panel04"; // lower right
    public static final String PANEL05 = "panel05"; // animation
    public static final String PANEL06 = "panel06"; // Show values... kinda pointless
    public static final String PANEL07 = "panel07"; // Save as... but it has no actual panel

    public static final String PANEL08 = "panel08"; // correlation viewer

    public static Color UPDATE_NEEDED = Color.PINK_DARKEN_4;
    public static Color UPDATE_NOT_NEEDED;

    public static String GRID = "grid";
    public static String TRAJECTORY = "trajectory";
    public static String PROFILE = "profile";
    public static String TIMESERIES = "timeseries";

    public static String ANIMATE_CANCEL = "Push the cancel button to stop making new frames and animate the ones already downloaded.";
    public static String ANIMATE_SUBMIT = "Select a time range, the number of time steps to skip between frames, and submit";

    public static String STARTING_INGEST = "LAS is downloading the metadata for the variable in this data set. It may take a moment.";
    public static String CHECKING_INGEST_STATUS = "LAS is still downloading and processing the metadata for the variables in this data set.";
    public static String INGEST_FAILED = "LAS was unable to load the metadata for this data set. Use the back arrow and home button above to try a different data set. Contact the site administrator for help.";

    public static String NO_MIN_MAX = "Unable to compute a global min and max from the available information. Enther your own coutour levels.";

}
