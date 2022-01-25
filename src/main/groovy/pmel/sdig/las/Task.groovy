package pmel.sdig.las;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
public class Task {

	/** An array of strings that indicate there is an error in command output */
	// Right now we are not scanning for errors... protected String[] ERROR_INDICATOR;

	/** Standard output buffer */
	protected StringBuffer output;

	/** Standard error output buffer */
	protected StringBuffer stderr;

	/** Default time limit in sec for this external process */
	protected long timeLimit = 600l;

	protected String[] cmd;

	protected String[] env;

	protected String workDirPath;

	public boolean hasErrors() {
		if ( stderr.length() > 0 ) {
			String error_messages = stderr.toString();
			boolean error = error_messages.contains("ERR");
			error = error || error_messages.contains("netCDF error")
			error = error || error_messages.contains("PPL+ error");
			error = error || error_messages.contains("NOTE: unrepairable repeated axis coords");
			error = error || error_messages.contains("NOTE: Coordinates out of order or missing");
			error = error || error_messages.contains("NOTE: calendar attribute on axis");
			error = error || error_messages.contains("NOTE: A dummy value of 1");
			error = error || error_messages.contains("**Error");
			error = error || error_messages.contains("STOP");
			error = error || error_messages.contains("Segmentation");
			error = error || error_messages.contains("No such");
			error = error || error_messages.contains("Internet data error");
			error = error || error_messages.contains("netCDF error");
			error = error || error_messages.contains("Internet Data error");
			return error;
		} else {
			return false;
		}
	}
	public StringBuffer getErrors() {
		return stderr;
	}
	public StringBuffer getOutput() {
		return output;
	}
	
	public Task(Ferret ferret, String scriptPath) {
		this.output = new StringBuffer();
		this.stderr = new StringBuffer();


		this.cmd = ferret.getCommandArguments(scriptPath);
		this.env = ferret.getRuntimeEnvironment();
		this.workDirPath = ferret.getTempDir();


	}
	/**
	 * Executes the external process, returning when it is finished or when it
	 * exceeds the time limit specified in the constructor.
	 * 
	 * @throws Exception
	 *             If the process fails, or if the output parser finds an error
	 *             message in the output.
	 */
	public void run() throws Exception {


		File workDir = new File(workDirPath);

		long startTime = System.currentTimeMillis();

		Process process = Runtime.getRuntime().exec(cmd, env, workDir);

		if (process == null) {
			throw new Exception("creation of child process "
					+ "failed for unknown reasons\n" + "command: "
					+ cmd);
		}

		finish(process, startTime);


	}

	/**
	 * Monitors the running process, puts the process's standard output to
	 * <code>output</code> and errors output to <code>stderr</code>. Also
	 * monitors the process' time limit and output limit, checks if there is any
	 * error generated.
	 * <p>
	 * 
	 * @param process
	 *            the running process
	 * @param startTime
	 *            the start time of the process
	 * @throws Exception
	 *             if anything goes wrong
	 */
	protected void finish(Process process, long startTime) throws Exception {

		BufferedReader outstream = new BufferedReader(new InputStreamReader(
				process.getInputStream()));
		BufferedReader errstream = new BufferedReader(new InputStreamReader(
				process.getErrorStream()));

		char[] buffer = new char[1024];

		// wait in 10ms increments for the script to complete
		while (true) {
			try {
				process.exitValue();
				break;
			} catch (IllegalThreadStateException itse) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException ie) {
					System.out.println("interupted.");
				}
				try {
					if (outstream.ready()) {
						int charsRead = outstream.read(buffer);
						output.append(buffer, 0, charsRead);
					}
				} catch (IOException ioe) {
					System.out.println("io problem.");

				}
				try {
					if (errstream.ready()) {
						int charsRead = errstream.read(buffer);
						stderr.append(buffer, 0, charsRead);
					}
				} catch (IOException ioe) {
					System.out.println("io problem.");

				}

				long endTime = System.currentTimeMillis();
				if (timeLimit > 0 && endTime - startTime > timeLimit * 1000) {
					try {
						outstream.close();
					} catch (IOException ioe) {
						// Well, we tried...
						System.out.println("io problem.");

					}
					try {
						errstream.close();
					} catch (IOException ioe) {
						// Well, we tried...
						System.out.println("io problem.");

					} finally {
						if ( errstream != null ) {
							errstream.close();
						}
						if ( outstream != null ) {
							outstream.close();
						}
					}
					process.destroy();
					throw new Exception("process exceeded time limit of "+ timeLimit + " sec");
				}
			}
		}

		try {
			while (outstream.ready()) {
				int charsRead = outstream.read(buffer);
				output.append(buffer, 0, charsRead);
			}
		} catch (IOException ioe) {
			System.out.println("ioe get output is ready "+ioe);

		}
		try {
			while (errstream.ready()) {
				int charsRead = errstream.read(buffer);
				stderr.append(buffer, 0, charsRead);
			}
		} catch (IOException ioe) {
			System.out.println("late ioe");
		}

		try {
			outstream.close();
		} catch (IOException ioe) {
			System.out.println("late ioe");

		}
		try {
			errstream.close();
		} catch (IOException ioe) {
			System.out.println("late ioe");

		}

	}

	public void appendError(String error) {
		stderr.append(error);
	}
}
