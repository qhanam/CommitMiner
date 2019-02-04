package multidiffplus.mining.flow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.analysis.Options;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.mining.flow.analysis.CommitAnalysisFactory;
import multidiffplus.mining.flow.analysis.CommitAnalysisFactory.Sensitivity;
import multidiffplus.mining.flow.facts.SliceFactBase;

/**
 * Analyzes a candidate file change (thread safe).
 */
public class CandidateAnalysis {
	
	private Candidate candidate;
	private File jsonFile;
	private Sensitivity sensitivity;
	
	public CandidateAnalysis(Candidate candidate, File jsonFile, Sensitivity sensitivity) {
		this.candidate = candidate;
		this.jsonFile = jsonFile;
		this.sensitivity = sensitivity;
	}

	public void analyze() throws Exception {

		/* Read the source files. */
		SourceCodeFileChange sourceCodeFileChange = getSourceCodeFileChange(
				candidate.getFile(), candidate.getOldFile(), candidate.getNewFile());

		/* Build the dummy commit. */
		Commit commit = getCommit(candidate.getProject(), 
				candidate.getURI(), 
				candidate.getCommitID());
		commit.addSourceCodeFileChange(sourceCodeFileChange);

		/* Builds the data set with our custom queries. */
		SliceFactBase factBase = SliceFactBase.getInstance(sourceCodeFileChange);

		/* Set up the analysis. */
		ICommitAnalysisFactory commitFactory = new CommitAnalysisFactory(sensitivity);
		CommitAnalysis commitAnalysis = commitFactory.newInstance();

		/* Run the analysis. */
		commitAnalysis.analyze(commit);

        /* Print the data set. */
		factBase.printDataSet();
		
		/* Add the commit information to the JSON object. */
		JsonObject json = factBase.getJsonObject();
		json.addProperty("url", commit.url);
		json.addProperty("projectID", commit.projectID);
		json.addProperty("commitID", commit.repairedCommitID);

		/* Write the data set to the json output file. */

		Options options = Options.getInstance();
		if(options.labels() == Options.Labels.MUTABLE) {
			if(!factBase.isEmpty() && factBase.hasLabels()) 
				flushToFile(json, jsonFile); // Only repair or mutable function changes
		}
		else if (options.labels() == Options.Labels.NOMINAL){
			if(!factBase.isEmpty() && !factBase.hasLabels()) 
				flushToFile(json, jsonFile); // Only nominal function changes
		}

		/* We are done with the factbase and can recover the memory. */
		SliceFactBase.removeInstance(sourceCodeFileChange);

	}

	/**
	 * @return Create a dummy commit
	 */
	private static Commit getCommit(String project, String url, String commitID) {
		return new Commit(project, url, "unk", commitID, Type.OTHER);
	}

	/**;
	 * @return Create a dummy source code file change.
	 */
	private static SourceCodeFileChange getSourceCodeFileChange(String file, File srcFile, File dstFile) throws IOException {
		String buggyCode = FileUtils.readFileToString(srcFile);
		String repairedCode = FileUtils.readFileToString(dstFile);
		return new SourceCodeFileChange(file, file, buggyCode, repairedCode);
	}

	/**
	 * Appends JSON to a file for persistent storage.
	 * @throws IOException if a path does not exist.
	 */
	private static synchronized void flushToFile(
			JsonObject json, File jsonFile) throws IOException {

		if(jsonFile == null) return;

		jsonFile.getParentFile().mkdirs();
		jsonFile.createNewFile();

		try (PrintStream stream = new PrintStream(new FileOutputStream(jsonFile, true))) {
			stream.println(new GsonBuilder().serializeNulls().create().toJson(json));
		}

	}

}
