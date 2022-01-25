package pmel.sdig.las

/**
 * Holds all the necessary information to run Ferret (or PyFerret)
 * @author rhs
 *
 */
//class Ferret implements Serializable {
class Ferret {

        // Path to the executable (python in the case of PyFerret)
        String path

        // A place to write script files
        String tempDir

        // Must be a list to keep them ordered
        List arguments

        // Whether the configuration has been tested and produced a plot.
        boolean tested

        // External public URL that is the base F-TDS URL, Getting it from tomcat gives the internal URL that is
        // behind the firewall. Relative URL not possible since external application like PyFerret has to access.
        String base_url

        // Time limit, the maximum number of milliseconds a Ferret process will be allowed to run, default value 5 minutes
        long productServerTimeout = 1000 * 60 * 5;

        // All FER_ and other necessary environment variables
        static hasOne = [ferretEnvironment: FerretEnvironment]

        // The list of command-line arguments necessary to run Ferret or PyFerret
        static hasMany = [arguments: Argument]

        static constraints = {
            tested (defaultValue: false)
        }

        // Convenience method to get the environment as needed for the java Runtime
        String[] getRuntimeEnvironment() {
            ferretEnvironment.getRuntimeEnvironment()
        }

        // The  ordered list of arguments, including the script
        String[] getCommandArguments(String scriptPath) {
            int offset = 1

            def a = Argument.all
            // The executable and arguments, and the script to be run
            // E.g. /usr/bin/python2.7 -cimport sys; import pyferret; (errval, errmsg) = pyferret.init(sys.argv[1:], True) -gif -script blah
            String[] cmd = new String[a.size()+offset+1]

            cmd[0] = path

            a.eachWithIndex { arg, i ->
                cmd[i + offset] = arg.value
            }
            cmd[cmd.length-1] = scriptPath
            cmd

        }

}
