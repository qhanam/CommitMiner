package multidiffplus.mining.ast.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.google.gson.GsonBuilder;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.mining.flow.analysis.CommitAnalysisFactory;
import multidiffplus.mining.flow.analysis.CommitAnalysisFactory.Sensitivity;
import multidiffplus.mining.flow.facts.SliceFactBase;

public class TryDataTests {
	
	/**
	 * Tests data mining data set construction.
	 */
	protected void runTest(String src, String dst) throws Exception {

		/* Read the source files. */
		SourceCodeFileChange sourceCodeFileChange = getSourceCodeFileChange(src, dst);

		/* Build the dummy commit. */
		Commit commit = getCommit();
		commit.addSourceCodeFileChange(sourceCodeFileChange);

		/* Builds the data set with our custom queries. */
		SliceFactBase factBase = SliceFactBase.getInstance(sourceCodeFileChange);

		/* Set up the analysis. */
		ICommitAnalysisFactory commitFactory = new CommitAnalysisFactory(Sensitivity.AST);
		CommitAnalysis commitAnalysis = commitFactory.newInstance();

		/* Run the analysis. */
		commitAnalysis.analyze(commit);
		
        /* Print the data set. */
		System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(factBase.getJsonObject()));
	
	}

	@Test
	public void testContrived() throws Exception {
		String src = "src/test/resources/try/contrived_old.js";
		String dst = "src/test/resources/try/contrived_new.js";
		this.runTest(src, dst);
	}

	@Test
	public void testShellJS() throws Exception {
		String src = "src/test/resources/try/shelljs_old.js";
		String dst = "src/test/resources/try/shelljs_new.js";
		this.runTest(src, dst);
	}

	/**
	 * @return A dummy commit for testing.
	 */
	private static Commit getCommit() {
		return new Commit("test", "http://github.com/saltlab/Pangor", "c0", "c1", Type.BUG_FIX);
	}

	/**;
	 * @return A dummy source code file change for testing.
	 * @throws IOException
	 */
	private static SourceCodeFileChange getSourceCodeFileChange(String srcFile, String dstFile) throws IOException {
		String buggyCode = readFile(srcFile);
		String repairedCode = readFile(dstFile);
		return new SourceCodeFileChange(srcFile, dstFile, buggyCode, repairedCode);
	}

	/**
	 * Reads the contents of a source code file into a string.
	 * @param path The path to the source code file.
	 * @return A string containing the source code.
	 * @throws IOException
	 */
	private static String readFile(String path) throws IOException {
		return FileUtils.readFileToString(new File(path));
	}

}