package multidiffplus.mining.flow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.io.FileUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import multidiff.js.factories.ChangeImpactCommitAnalysisFactory;
import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.facts.JsonFactBase;

/**
 * Analyzes a candidate file change (thread safe).
 */
public class CandidateAnalysis {

    private Candidate candidate;
    private File jsonFile;

    public CandidateAnalysis(Candidate candidate, File jsonFile) {
	this.candidate = candidate;
	this.jsonFile = jsonFile;
    }

    public void analyze() throws Exception {

	System.out.println(candidate.getNewFile().getAbsolutePath());

	/* Read the source files. */
	SourceCodeFileChange sourceCodeFileChange = getSourceCodeFileChange(candidate.getFile(),
		candidate.getOldFile(), candidate.getNewFile());

	/* Build the dummy commit. */
	Commit commit = getCommit(candidate.getProject(), candidate.getURI(),
		candidate.getCommitID());
	commit.addSourceCodeFileChange(sourceCodeFileChange);

	/* Set up the analysis. */
	ICommitAnalysisFactory commitFactory = new ChangeImpactCommitAnalysisFactory();
	CommitAnalysis commitAnalysis = commitFactory.newInstance();

	/* Run the analysis. */
	commitAnalysis.analyze(commit);

	/* Get the Esprima JSON object. */
	JsonFactBase jsonFactBase = JsonFactBase.getInstance(sourceCodeFileChange);
	JsonObject json = jsonFactBase.getJson();

	/* Add the commit information to the JSON object. */
	json.addProperty("url", commit.url);
	json.addProperty("projectID", commit.projectID);
	json.addProperty("commitID", commit.repairedCommitID);
	json.addProperty("fileName", sourceCodeFileChange.getFileName());

	/* Write the data set to the json output file. */
	flushToFile(json, jsonFile);

	/* We are done with the factbases and can recover the memory. */
	AnnotationFactBase.removeInstance(sourceCodeFileChange);
	JsonFactBase.removeInstance(sourceCodeFileChange);

    }

    /**
     * @return The candidate being analyzed.
     */
    public Candidate getCandidate() {
	return candidate;
    }

    /**
     * @return Create a dummy commit
     */
    private static Commit getCommit(String project, String url, String commitID) {
	return new Commit(project, url, "unk", commitID, Type.OTHER, 0);
    }

    /**
     * ;
     * 
     * @return Create a dummy source code file change.
     */
    private static SourceCodeFileChange getSourceCodeFileChange(String file, File srcFile,
	    File dstFile) throws IOException {
	String buggyCode = FileUtils.readFileToString(srcFile);
	String repairedCode = FileUtils.readFileToString(dstFile);
	return new SourceCodeFileChange(file, file, buggyCode, repairedCode);
    }

    /**
     * Appends JSON to a file for persistent storage.
     * 
     * @throws IOException
     *             if a path does not exist.
     */
    private static synchronized void flushToFile(JsonObject json, File jsonFile)
	    throws IOException {

	if (jsonFile == null)
	    return;

	jsonFile.getParentFile().mkdirs();
	jsonFile.createNewFile();

	try (PrintStream stream = new PrintStream(new FileOutputStream(jsonFile, true))) {
	    stream.println(new GsonBuilder().serializeNulls().create().toJson(json));
	}

    }

}
