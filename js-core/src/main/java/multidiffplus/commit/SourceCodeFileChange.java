package multidiffplus.commit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.regex.Matcher;

/**
 * Stores the information that represents a change to a source code file.
 *
 * This is used by {@code CommitAnalysid} to initiate a
 * {@code SourceCodeFileAnalysis}.
 */
public class SourceCodeFileChange {
	
	/** Used for generating unique SourceCodeFileChange IDs. **/
	private static long unique = initUniqueID();
	
	/** The unique id for the {@code SourceCodeFileChange}. **/
	private long id;

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
		this.id = getUniqueID();
		this.buggyFile = buggyFile;
		this.repairedFile = repairedFile;
		this.buggyCode = buggyCode;
		this.repairedCode = repairedCode;
		this.analysisRuntime = -1;
	}
	
	/**
	 * @return the unique ID for the {@code SourceCodeFileChange}
	 */
	public long getID() {
		return id;
	}

	/**
	 * @return the file name (without the full path).
	 */
	public String getFileName() {
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/(?<name>[^/]+\\.js)");
		Matcher m = pattern.matcher(this.repairedFile);

		if(m.find()) {
			return m.group("name");
		}

		return "[unknown]";
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof SourceCodeFileChange)) return false;
		SourceCodeFileChange a = (SourceCodeFileChange) o;
		return this.id == a.id;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(id).hashCode();
	}
	
	@Override
	public String toString() {
		return repairedFile;
	}
	
	private static synchronized long getUniqueID() {

		long unique = SourceCodeFileChange.unique;
		SourceCodeFileChange.unique++;

		File file = new File("./", ".scfcid");
		try(PrintStream stream = new PrintStream(new FileOutputStream(file, false))) {
			stream.print(String.valueOf(unique));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return unique;

	}
	
	/**
	 * @return the lowest ID guaranteed to be unique (based on the last stored ID)
	 */
	private static long initUniqueID() {

		File file = new File("./", ".scfcid");
		long unique = 1;
		
		if(file.exists()) {

			try(BufferedReader br = new BufferedReader(new FileReader(file))) {
				
				try {
					unique = Long.parseLong(br.readLine()) + 1;
				}
				catch(NumberFormatException e) {
					e.printStackTrace();
				}
				
			}
			catch(Exception e) {
				System.err.println("Error while reading URI file: " + e.getMessage());
			}
			
		}
		
		return unique;
		
	}

}