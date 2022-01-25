package pmel.sdig.las

class Product {

    String name
    String title
    String geometry
    String view
    String data_view
    String product_order // sort order
    int minArgs = 1 // Minimum number of data variables needed
    int maxArgs = 1 // Maximum number of data variables needed
    boolean hidden

    /*
      Use ui_group to put related operations together (like line plots, or 2D slices)
      Currently we group things like:
         Maps
         Line Plots
         Vertical Secion Plots
         Hofmuller Plots

  */
    String ui_group

    List operations
    static hasMany = [operations: Operation]

    static mapping = {
        operations (cascade: 'all-delete-orphan')
        operations lazy: false
    }
    static constraints = {

    }


}
