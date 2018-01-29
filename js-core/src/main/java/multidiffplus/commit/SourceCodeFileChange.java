package multidiffplus.commit;

import java.util.regex.Matcher;

/**
 * Stores the information that represents a change to a source code file.
 *
 * This is used by {@code CommitAnalysid} to initiate a
 * {@code SourceCodeFileAnalysis}.
 */
public class SourceCodeFileChange {

	/** The path to the source file before the commit. **/
	public String buggyFile;

	/** The path to the source file after the commit. **/
	public String repairedFile;

	/** The code before the commit. **/
	public String buggyCode;

	/** The code after the commit. **/
	public String repairedCode;
	
	/** The the time spent analyzing the file. **/
	public long analysisRuntime;

	/**
	 * @param buggyFile The path to the source file before the commit.
	 * @param repairedFile The path to the source file after the commit.
	 * @param buggyCode The code before the commit.
	 * @param repairedCode The code after the commit.
	 */
	public SourceCodeFileChange(String buggyFile, String repairedFile,
								String buggyCode, String repairedCode) {
		this.buggyFile = buggyFile;
		this.repairedFile = repairedFile;
		this.buggyCode = buggyCode;
		this.repairedCode = repairedCode;
		this.analysisRuntime = -1;
	}

	/**
	 * @return the file name (without the full path).
	 */
	public String getFileName() {
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/([A-za-z]+)\\.java");
		Matcher m = pattern.matcher(this.repairedFile);

		if(m.find()) {
			return m.group(1);
		}

		return "[unknown]";
	}

	@Override
	public boolean equals(Object o) {

		if(o instanceof SourceCodeFileChange) {

			SourceCodeFileChange a = (SourceCodeFileChange) o;

			if(this.buggyFile.equals(a.buggyFile)
				&& this.repairedFile.equals(a.repairedFile)) {

				return true;

			}

		}
		return false;
	}

	@Override
	public int hashCode() {
		return (this.buggyFile + this.repairedCode).hashCode();
	}
	
	@Override
	public String toString() {
		return repairedFile;
	}

}