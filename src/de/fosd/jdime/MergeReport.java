/**
 * 
 */
package de.fosd.jdime;

import java.io.File;
import java.io.StringWriter;
import java.util.List;

/**
 * @author lessenic
 *
 */
public class MergeReport {
	private StringWriter stdIn;
	private StringWriter stdErr;
	private MergeType mergeType;
	private List<File> inputFiles;

	/**
	 * 
	 */
	public MergeReport(MergeType mergeType, List<File> inputFiles) {
		stdIn = new StringWriter();
		stdErr = new StringWriter();
		this.mergeType = mergeType;
		this.inputFiles = inputFiles;
	}
	
	
	public void write(String s) {
		if (stdIn != null) {
			stdIn.write(s);
			stdIn.flush();
		}
	}
	
	public void appendLine(String s) {
		if (stdIn != null) {
			stdIn.append(s);
			stdIn.append(System.getProperty("line.separator"));
		}
	}
	
	public void appendErrorLine(String s) {
		if (stdErr != null) {
			stdErr.append(s);
			stdErr.append(System.getProperty("line.separator"));
		}
	}
	
	public final String getStdIn() {
		return stdIn.toString();
	}
	
	public final String getStdErr() {
		return stdErr.toString();
	}
	
	public void flushStreams() {
		stdIn.flush();
		stdErr.flush();
	}
	
	public final boolean hasErrors() {
		return stdErr.toString().length() != 0;
	}

	public final MergeType getMergeType() {
		return mergeType;
	}
	
	public final List<File> getInputFiles() {
		return inputFiles;
	}
	
}
