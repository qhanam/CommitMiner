package multidiffplus.mining.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.mining.factories.MiningCommitAnalysisFactory;

public class ASTMiningTests {

	/**
	 * Tests data mining data set construction.
	 * @param args The command line arguments (i.e., old and new file names).
	 * @throws Exception
	 */
	protected void runTest(
			String src, String dst, String out) throws Exception {

		/* Read the source files. */
//		String srcCode = new String(Files.readAllBytes(Paths.get(src)));
//		String dstCode = new String(Files.readAllBytes(Paths.get(dst)));

		/* Read the source files. */
		SourceCodeFileChange sourceCodeFileChange = getSourceCodeFileChange(src, dst);

		/* Build the dummy commit. */
		Commit commit = getCommit();
		commit.addSourceCodeFileChange(getSourceCodeFileChange(src, dst));

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

	@Test
	public void testAjaxStringify() throws Exception {
		String src = "src/test/resources/ajax_stringify_old.js";
		String dst = "src/test/resources/ajax_stringify_new.js";
		String out = "out/simple.html";
		this.runTest(src, dst, out);
	}

	/**
	 * @return A dummy commit for testing.
	 */
	public static Commit getCommit() {
		return new Commit("test", "http://github.com/saltlab/Pangor", "c0", "c1", Type.BUG_FIX);
	}

	/**;
	 * @return A dummy source code file change for testing.
	 * @throws IOException
	 */
	public static SourceCodeFileChange getSourceCodeFileChange(String srcFile, String dstFile) throws IOException {
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