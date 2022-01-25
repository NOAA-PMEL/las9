package pmel.sdig.las

/**
 *
 */
class Site {
    List datasets
    String title
    long total
    long grids
    long trajectory
    long timeseries
    long trajectoryProfile
    long point
    long profile
    long discrete

    List<FooterLink> footerLinks;

    Map<String, List<String>> attributes

    boolean toast // show the toast message (mostly for debugging)
    boolean dashboard // limit the map interaction to region and point selection and show "unrolled" graphs of selected points

    String infoUrl;

    static hasMany = [datasets: Dataset, siteProperties: SiteProperty, footerLinks: FooterLink]
    static constraints = {
        siteProperties nullable: true
        infoUrl nullable: true
        trajectory(nullable: true)
        timeseries(nullable: true)
        trajectoryProfile(nullable: true)
        point(nullable: true)
        profile(nullable: true)
        footerLinks(nullable: true)
        attributes(nullable: true)
    }
    static mapping = {
        title type: "text"
    }
}
