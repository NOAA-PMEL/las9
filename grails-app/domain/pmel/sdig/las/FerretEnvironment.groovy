package pmel.sdig.las


// This was an attempt ot use RPC. Using JSON will work just fine. :-)

//class FerretEnvironment implements Serializable {

class FerretEnvironment {

	String fer_data
	String fer_go
	String fer_grids
	String fer_external_functions
	String fer_libs
	String fer_palette
	String fer_dsets
	String fer_fonts
	String fer_descr
	String fer_dir
	String plotfonts
	String pythonpath
	String ld_library_path
	static belongsTo = [ferret: Ferret]

    static mapping = {
        fer_go(type: 'text')
        fer_grids (type: 'text')
        fer_external_functions (type: 'text')
        fer_palette (type: 'text')
        fer_libs (type: 'text')
        fer_dsets (type: 'text')
        fer_data (type: 'text')
        fer_fonts(type: 'text')
        fer_descr(type: 'text')
        fer_dir(type: 'text')
        plotfonts(type: 'text')
        pythonpath(type: 'text')
        ld_library_path(type: 'text')
    }

	// The NAME() syntax is required because of the upper case name.
	static constraints = {
		fer_go(nullable: true)
		fer_grids (nullable: true)
		fer_external_functions (nullable: true)
		fer_palette (nullable: true)
		fer_libs (nullable: true)
		fer_dsets (nullable: true)
		fer_data (nullable: true)
		fer_fonts(nullable: true)
		fer_descr(nullable: true)
		fer_dir(nullable: true)
		plotfonts(nullable: true)
		pythonpath(nullable: true)
		ld_library_path(nullable: true)
        ferret(nullable: true)
	}
	String[] getRuntimeEnvironment() {
		
		def runtimeEnvironment = []
		if ( fer_data ) runtimeEnvironment.push("FER_DATA="+fer_data)
		if ( fer_go ) runtimeEnvironment.push("FER_GO="+fer_go)
		if ( fer_grids ) runtimeEnvironment.push("FER_GRIDS="+fer_grids)
		if ( fer_external_functions ) runtimeEnvironment.push("FER_EXTERNAL_FUNCTIONS="+fer_external_functions)
		if ( fer_libs) runtimeEnvironment.push("FER_LIBS="+fer_libs)
		if ( fer_palette) runtimeEnvironment.push("FER_PALETTE="+fer_palette)
		if ( fer_dsets) runtimeEnvironment.push("FER_DSETS="+fer_dsets)
		if ( fer_fonts) runtimeEnvironment.push("FER_FONTS="+fer_fonts)
		if( fer_descr) runtimeEnvironment.push("FER_DESCR="+fer_descr)
		if ( fer_dir) runtimeEnvironment.push("FER_DIR="+fer_dir)
		if ( plotfonts ) runtimeEnvironment.push("PLOTFONTS="+plotfonts)
		if ( pythonpath ) runtimeEnvironment.push("PYTHONPATH="+pythonpath)
		if ( ld_library_path) runtimeEnvironment.push("LD_LIBRARY_PATH="+ld_library_path)
		runtimeEnvironment
	}
}
