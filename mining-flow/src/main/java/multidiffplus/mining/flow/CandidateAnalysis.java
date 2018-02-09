package multidiffplus.mining.flow;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.mining.flow.factories.MiningCommitAnalysisFactory;

/**
 * Analyzes a candidate file change (thread safe).
 */
public class CandidateAnalysis {
	
	private Candidate candidate;
	
	public CandidateAnalysis(Candidate candidate) {
		this.candidate = candidate;
	}

	public void analyze() throws Exception {

		/* Read the source files. */
		SourceCodeFileChange sourceCodeFileChange = getSourceCodeFileChange(
				candidate.getFile(), candidate.getOldFile(), candidate.getNewFile());

		/* Build the dummy commit. */
		Commit commit = getCommit(candidate.getProject(), 
				candidate.getURI(), 
				candidate.getCommitID());
		commit.addSourceCodeFileChange(getSourceCodeFileChange(candidate.getFile(), 
				candidate.getOldFile(), 
				candidate.getNewFile()));

		/* Builds the data set with our custom queries. */
		AnnotationFactBase factBase = AnnotationFactBase.getInstance(sourceCodeFileChange);

		/* Set up the analysis. */
		ICommitAnalysisFactory commitFactory = new MiningCommitAnalysisFactory();
		CommitAnalysis commitAnalysis = commitFactory.newInstance();

		/* Run the analysis. */
		commitAnalysis.analyze(commit);

        /* Print the data set. */
		factBase.printDataSet();

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

}
