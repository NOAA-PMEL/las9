package pmel.sdig.las

import grails.gorm.transactions.Transactional
import grails.util.Holders
import org.hibernate.SessionFactory
import pmel.sdig.las.type.GeometryType


@Transactional
class FerretService {

    DateTimeService dateTimeService
    ResultsService resultsService
    ProductService productService
    SessionFactory sessionFactory

    def ferretColorsMap = [
            red:"(69.7, 7.58, 22.73)",
            green:"(19.05, 57.14, 23.81)",
            yellow:"(50.50, 44.55, 4.95)",
            blue:"(17.54, 25.92, 56.54)",
            orange:"(57.78, 30.66, 11.56)",
            purple:"(40.85, 8.45, 50.70)",
            cyan: "(12.64,40.61,46.74)",
            magenta:"(46.15,9.62,44.23)",
            lime:"(38.28,47.90,13.83)",
            pink:"(39.68,30.16,30.16)",
            teal:"(19.07,41.69,39.24)",
            lavendar:"(34.07,28.15,37.78)",
            brown:"(53.29,34.26,12.46)",
            beige:"(36.17,35.46,28.37)",
            maroon:"(50, 0, 0)",
            mint: "(27.42, 41.13,31.45)",
            olive: "(50, 50, 0)",
            apricot: "(39.35, 33.33, 27.31)",
            navy: "(0, 0, 45)"
    ]
    def ferretColorsNames=[
            "red",
            "green",
            "yellow",
            "blue",
            "orange",
            "purple",
            "cyan",
            "magenta",
            "lime",
            "pink",
            "teal",
            "lavendar",
            "brown",
            "beige",
            "maroon",
            "mint",
            "olive",
            "apricot",
            "navy"
    ]
    def ferretColorsValues=[
            "(69.7, 7.58, 22.73)",
            "(19.05, 57.14, 23.81)",
            "(50.50, 44.55, 4.95)",
            "(17.54, 25.92, 56.54)",
            "(57.78, 30.66, 11.56)",
            "(40.85, 8.45, 50.70)",
            "(12.64,40.61,46.74)",
            "(46.15,9.62,44.23)",
            "(38.28,47.90,13.83)",
            "(39.68,30.16,30.16)",
            "(19.07,41.69,39.24)",
            "(34.07,28.15,37.78)",
            "(53.29,34.26,12.46)",
            "(36.17,35.46,28.37)",
            "(50, 0, 0)",
            "(27.42, 41.13,31.45)",
            "(50, 50, 0)",
            "(39.35, 33.33, 27.31)",
            "(0, 0, 45)"
    ]
    Map runScript (StringBuffer script, File sp) {

        def result = [:]

        def ferret = Ferret.first()

        sp.withWriter { out ->
            out.writeLine(script.toString().stripIndent())
        }

        Task task = new Task(ferret, sp.getAbsolutePath() )
        try {
            task.run()
        } catch (Exception e ) {
            task.appendError("ERROR: Exception running task.  ")
            task.appendError(e.getMessage())
        }


        if ( task.hasErrors() ) {
            result["error"] = true
            result["message"] = task.getErrors().toString();
        } else {
            result["error"] = false
        }

        return result

    }

    String getFerretColor(String key) {
        ferretColorsMap.get(key)
    }
    String getFerretColorName(int index) {
        index = index % ferretColorsNames.size()
        ferretColorsNames.get(index)
    }
    String getFerretColorValue(int index) {
        index = index % ferretColorsValues.size()
        ferretColorsValues.get(index)
    }

    String makeThumbnail(String dhash, String vhash) {

        Dataset dataset = Dataset.findByHash(dhash)
        if ( dataset ) {
            Variable variable = dataset.variables.find{it.hash == vhash}
            if ( variable ) {

                if ( variable.getThumbnail() && !variable.getThumbnail().isEmpty() ) {
                    return variable.getThumbnail();
                }

                String variable_url = variable.getUrl()
                String variable_name = variable.getName()
                String variable_title = variable.getTitle()

                def x = dataset.getGeoAxisX()
                def y = dataset.getGeoAxisY()
                def z = dataset.getVerticalAxis()
                def t = dataset.getTimeAxis()

                def hash = "${dhash}_${vhash}"

                def opname;
                def opview;
                if (dataset.getGeometry().equals(GeometryType.GRID ) ) {
                    opname = "Plot_2D_XY"
                    opview = "xy"
                } else if (dataset.getGeometry().equals(GeometryType.TIMESERIES ) ) {
                    opname = "Timeseries_station_plot"
                    opview = "t"
                    /*
{
	"targetPanel": 1,
	"datasetHashes": ["46bc3b7d4ae261841352c1ae695b2402"],
	"dataQualifiers": null,
	"variableHashes": ["87b437774dad2b5a5857f6e7ba3e7c3d"],
	"axesSets": [{
		"thi": "19-Sep-2015 00:00",
		"tlo": "18-Sep-2015 22:14",
		"xhi": "-125.44958496094",
		"xlo": "-125.94960021973",
		"zhi": null,
		"yhi": "45.065101623535",
		"zlo": null,
		"ylo": "44.565101623535"
	}],
	"requestProperties": [{
		"name": "interpolate_data",
		"id": 0,
		"type": "ferret",
		"value": "0"
	}, {
		"name": "deg_min_sec",
		"id": 0,
		"type": "ferret",
		"value": "0"
	}, {
		"name": "data_count",
		"id": 0,
		"type": "ferret",
		"value": "1"
	}],
	"id": 0,
	"analysis": [null],
	"operation": "Timeseries_station_plot",
	"dataConstraints": []
}
                     */
                } else if ( dataset.getGeometry().equals((GeometryType.TRAJECTORY) ) ) {

                    /*
{
	"targetPanel": 1,
	"datasetHashes": ["e37d787028837593c04bbe65a81b042d"],
	"dataQualifiers": null,
	"variableHashes": ["2f456c189b51babdea3fd924722bcb30"],
	"axesSets": [{
		"thi": "15-Mar-2018 00:00",
		"tlo": "14-Mar-2018 18:00",
		"xhi": "-120.28875904",
		"xlo": "-137.75806336",
		"zhi": null,
		"yhi": "40.51020488",
		"zlo": null,
		"ylo": "17.29451992"
	}],
	"requestProperties": [{
		"name": "interpolate_data",
		"id": 0,
		"type": "ferret",
		"value": "0"
	}, {
		"name": "deg_min_sec",
		"id": 0,
		"type": "ferret",
		"value": "0"
	}, {
		"name": "data_count",
		"id": 0,
		"type": "ferret",
		"value": "1"
	}],
	"id": 0,
	"analysis": [null],
	"operation": "Trajectory_interactive_plot",
	"dataConstraints": []
}
                     */
                } else if ( dataset.getGeometry().equals(GeometryType.PROFILE) ) {
                    opname = "Plot_2D_Profile"
                    opview = "zt"
                    /*
{
	"targetPanel": 1,
	"datasetHashes": ["9d0a32ac05ac185609f3f8f4e760cc09"],
	"dataQualifiers": null,
	"variableHashes": ["b7b39960ad5015c9b3c1d14559b7bb3f"],
	"axesSets": [{
		"thi": "10-Dec-2016 00:00",
		"tlo": "16-Sep-2016 09:47",
		"xhi": "360",
		"xlo": "0",
		"zhi": "45.77",
		"yhi": "90",
		"zlo": "-0.23",
		"ylo": "-90"
	}],
	"requestProperties": [{
		"name": "interpolate_data",
		"id": 0,
		"type": "ferret",
		"value": "0"
	}, {
		"name": "deg_min_sec",
		"id": 0,
		"type": "ferret",
		"value": "0"
	}, {
		"name": "data_count",
		"id": 0,
		"type": "ferret",
		"value": "1"
	}],
	"id": 0,
	"analysis": [null],
	"operation": "Plot_2D_Profile",
	"dataConstraints": []
}
                     */


                } else if ( dataset.getGeometry().equals(GeometryType.POINT) ) {
                    opname = "Point_location_value_plot"
                    opview = "xy"
                    /*
                    {
	"targetPanel": 1,
	"datasetHashes": ["de56a95e8d201e04217fb5cf4b51e6ee"],
	"dataQualifiers": null,
	"variableHashes": ["410bf1993e81cf95215c4475a6fb1dc0"],
	"axesSets": [{
		"thi": "10-Jan-1951 00:00",
		"tlo": "09-Jan-1951 18:16",
		"xhi": "-110.19176470321",
		"xlo": "-122.20677671562",
		"zhi": null,
		"yhi": "36.207115942573",
		"zlo": null,
		"ylo": "24.927643127871"
	}],
	"requestProperties": [{
		"name": "deg_min_sec",
		"id": 0,
		"type": "ferret",
		"value": "0"
	}, {
		"name": "set_aspect",
		"id": 0,
		"type": "ferret",
		"value": "1"
	}, {
		"name": "data_count",
		"id": 0,
		"type": "ferret",
		"value": "1"
	}],
	"id": 0,
	"analysis": [null],
	"operation": "Point_location_value_plot",
	"dataConstraints": []
}
                     */
                }

                LASRequest lasRequest = new LASRequest();
                lasRequest.setDatasetHashes([dhash])
                lasRequest.setVariableHashes([vhash])
                lasRequest.setOperation(opname)

                AxesSet axesSet = new AxesSet()
                if (t) {

                    String sd = dateTimeService.ferretFromIso(t.getStart(), t.getCalendar())
                    String fd = dateTimeService.ferretFromIso(t.getEnd(), t.getCalendar())

                    // Full range if on T view, last only otherwise
                    if (opview.contains("t")) {
                        axesSet.setTlo(sd)
                    } else {
                        axesSet.setTlo(fd)
                    }
                    axesSet.setThi(fd)

                }
                if (x) {
                    axesSet.setXhi(String.format("%.4f", x.getMax()))
                    axesSet.setXlo(String.format("%.4f", x.getMin()))
                }
                if (y) {
                    axesSet.setYhi(String.format("%.4f",y.getMax()))
                    axesSet.setYlo(String.format("%.4f",y.getMin()))
                }
                if (z) {
                    if ( opview.contains("z") ) {
                        axesSet.setZlo(String.format("%.4f", z.getMin()))
                        axesSet.setZhi(String.format("%.4f", z.getMax()))
                    } else {
                        axesSet.setZlo(String.format("%.4f", z.getMin()))
                        axesSet.setZhi(String.format("%.4f", z.getMin()))
                    }
                }

                lasRequest.setAxesSets([axesSet])

                ResultSet allResults = productService.doRequest(lasRequest, hash)
                def error = allResults.getError()
                if ( !error ) {
                    Result r = allResults.results.find { it.name == "plot_image" }
                    return r.getUrl()
                }
            }
        }
        return null;
    }
    boolean makeAndSaveThumbnail(Dataset dataset, Variable variable) {
        log.debug("Attempting to make thumbnail for " + variable.title + " with id " + variable.id)
        def url = makeThumbnail(dataset.hash, variable.hash)
        if ( url ) {
            variable.setUrl(url)
            variable.save(flush:true)
            log.debug("Succeeded with thumbnail for " + variable.title + " with id " + variable.id)
        }
        true
    }
    boolean makeThumbnails(Dataset dataset) {
        dataset.variables.each{
            makeAndSaveThumbnail(dataset, it)
        }
        true
    }
    private def addResults(ResultSet resultSet, ResultSet allResults, String product) {
        // Loop through the results and treat them according to their name and type
        // Only some results require post-processing special treatment
        def results = resultSet.results
        for (int i = 0; i < results.size(); i++) {
            def result = results[i]
            def name = result.name
            if (name == "map_scale") {
                File mapscaleFile = new File(result.filename)
                if (mapscaleFile.exists()) {
                    def mapScale = productService.makeMapScale(mapscaleFile)
                    allResults.setMapScale(mapScale)
                } else {
                    allResults.setError("Unknown error running PyFerret while making product. Mapscale file missing.")
                }
            } else if (name == "annotations") {
                File ann = new File(result.filename)
                if ( ann.exists() ) {
                    def annotationGroups = productService.makeAnnotations(ann);
                    allResults.setAnnotationGroups(annotationGroups)
                } else {
                    allResults.setError("Unknown error running PyFerret while making product. Annotations file missing.")
                }
                // TODO this name has to be more specific to the animation product
            } else if (name == "ferret_listing" && (product == "Animation_2D_XY" || product == "Animation_2D_XY_vector") ) {
                File anim = new File(result.filename)
                if ( anim.exists() ) {
                    def animation = productService.makeAnimationList(anim)
                    allResults.setAnimation(animation)
                } else {
                    allResults.setError("Unknown error running PyFerret while making product. Animation list file missing.")
                }
            }
            allResults.addToResults(result)
        }
    }
    boolean makeThumbnails() {
        log.debug("STARTED making thumbnails for all existing variables.")
        List<Variable> allVariables = Variable.getAll();
        Collections.shuffle(allVariables);
        allVariables.each{Variable variable ->
            def dhash = variable.getDataset().getHash();
            def vhash = variable.getHash()
            log.debug("Attempting to make thumbnail for " + variable.title + " with id " + variable.id)
            String url = makeThumbnail(dhash, vhash)
            if ( url ) {
                log.debug("Succeeded with thumbnail for " + variable.title + " with id " + variable.id)
                variable.setThumbnail(url)
                if ( !variable.save(flush: true) ) {
                    log.debug("Failed to save thumbnail " + variable.errors.allErrors )
                }
            }
        }
        log.debug("FINISHED making thumbnails for all existing variables.")
        true
    }
}

