package pmel.sdig.las

import grails.gorm.transactions.Transactional

@Transactional
class ResultsService {

    def getNetcdfFile() {
        ResultSet results_netcdf = new ResultSet([name: "results_netcdf"])
        Result ncfile = new Result([name: "netcdf", mime_type: "application/netcdf", type: "file", file_type: "netcdf", suffix: ".nc"])
        results_netcdf.addToResults(ncfile)
        results_netcdf
    }
    def getPlotResults() {

        ResultSet results_debug_image_mapscale_annotations = new ResultSet([name: "results_debug_image_mapscale_annotations"])

        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "debug", file_type: "text", suffix: ".txt"])
        Result plot_image = new Result([name: "plot_image", mime_type: "image/png", type: "image", file_type: "png", suffix: ".png"])
        Result map_scale = new Result([name: "map_scale", mime_type: "text/xml", type: "map_scale", file_type: "txt", suffix: ".xml"])
        Result annotations = new Result([name: "annotations", mime_type: "text/xml", type: "annotations", file_type: "xml", suffix: ".xml"])

        results_debug_image_mapscale_annotations.addToResults(debug)
        results_debug_image_mapscale_annotations.addToResults(plot_image)
        results_debug_image_mapscale_annotations.addToResults(map_scale)
        results_debug_image_mapscale_annotations.addToResults(annotations)

        results_debug_image_mapscale_annotations

    }
    def getThumbnailResults() {
        ResultSet xy_thumb = new ResultSet([name: "results_xy_thumbnail"])
        Result plot_image = new Result([name: "plot_image", mime_type: "image/png", type: "image", file_type: "png", suffix: ".png"])
        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "debug", file_type: "text", suffix: ".txt"])
        Result map_scale = new Result([name: "map_scale", mime_type: "text/xml", type: "map_scale", file_type: "txt", suffix: ".xml"])
        Result annotations = new Result([name: "annotations", mime_type: "text/xml", type: "annotations", file_type: "xml", suffix: ".xml"])
        xy_thumb.addToResults(plot_image)
        xy_thumb.addToResults(debug)
        xy_thumb.addToResults(map_scale)
        xy_thumb.addToResults(annotations)
        xy_thumb
    }
    def getAnimateSetupResults() {

        ResultSet results = new ResultSet([name: "results_debug_frames_list"])
        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "debug", file_type: "text", suffix: ".txt"])
        Result frame_list = new Result([name: "ferret_listing", mime_type: "text/xml", type: "frame_list", file_type: "xml", suffix: ".xml"])
        results.addToResults(debug)
        results.addToResults(frame_list)
        results
    }
    def getDataExtractResults() {
        ResultSet results = new ResultSet([name: "results_debug_data_listing_show"])
        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "debug", file_type: "text", suffix: ".txt"])
        Result frame_list = new Result([name: "ferret_listing", mime_type: "text", type: "ferret_listing", file_type: "text", suffix: ".txt"])
        results.addToResults(debug)
        results.addToResults(frame_list)
        results
    }
    def getDataExtractResultsCDF() {
        ResultSet results = new ResultSet([name: "results_debug_data_listing_netcdf"])
        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "debug", file_type: "text", suffix: ".txt"])
        Result frame_list = new Result([name: "ferret_listing", mime_type: "application/x-netcdf", type: "netcdf", file_type: "netcdf", suffix: ".nc"])
        results.addToResults(debug)
        results.addToResults(frame_list)
        results
    }
    def getDataExtractResultsFile() {
        ResultSet results = new ResultSet([name: "results_debug_data_listing_file"])
        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "debug", file_type: "text", suffix: ".txt"])
        Result frame_list = new Result([name: "ferret_listing", mime_type: "text", type: "ferret_listing", file_type: "text", suffix: ".txt"])
        results.addToResults(debug)
        results.addToResults(frame_list)
        results
    }
    def getDataExtractResultsCSV() {
        ResultSet results = new ResultSet([name: "results_debug_data_listing_file"])
        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "debug", file_type: "text", suffix: ".txt"])
        Result frame_list = new Result([name: "ferret_listing", mime_type: "text/csv", type: "ferret_listing", file_type: "csv", suffix: ".csv"])
        results.addToResults(debug)
        results.addToResults(frame_list)
        results
    }
    def getVectorResults() {

        ResultSet vecResults = new ResultSet([name: "results_debug_image_mapscale_annotations"])

        Result debug = new Result([name: "debug", mime_type: "text/plain", type: "debug", file_type: "text", suffix: ".txt"])
        Result plot_image = new Result([name: "plot_image", mime_type: "image/png", type: "image", file_type: "png", suffix: ".png"])
        Result map_scale = new Result([name: "map_scale", mime_type: "text/xml", type: "map_scale", file_type: "txt", suffix: ".xml"])
        Result annotations = new Result([name: "annotations", mime_type: "text/xml", type: "annotations", file_type: "xml", suffix: ".xml"])

        vecResults.addToResults(debug)
        vecResults.addToResults(plot_image)
        vecResults.addToResults(map_scale)
        vecResults.addToResults(annotations)

        vecResults

    }
}
