package multidiffplus.jsdiff.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import multidiff.js.factories.ChangeImpactCommitAnalysisFactory;
import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.analysis.Options;
import multidiffplus.analysis.Options.SymEx;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.jsdiff.view.HTMLMultiDiffViewer;
import multidiffplus.jsdiff.view.HTMLUnixDiffViewer;

public class ChangeImpactAnalysisTests {

	/**
	 * Tests data mining data set construction.
	 * @param args The command line arguments (i.e., old and new file names).
	 * @throws Exception
	 */
	protected void runTest(
			String src, String dst, String out) throws Exception {

		/* Read the source files. */
		String srcCode = new String(Files.readAllBytes(Paths.get(src)));
		String dstCode = new String(Files.readAllBytes(Paths.get(dst)));

		/* Read the source files. */
		SourceCodeFileChange sourceCodeFileChange = getSourceCodeFileChange(src, dst);

		/* Build the dummy commit. */
		Commit commit = getCommit();
		commit.addSourceCodeFileChange(getSourceCodeFileChange(src, dst));

		/* Builds the data set with our custom queries. */
		AnnotationFactBase factBase = AnnotationFactBase.getInstance(sourceCodeFileChange);

		/* Set up the analysis. */
		ICommitAnalysisFactory commitFactory = new ChangeImpactCommitAnalysisFactory();
		CommitAnalysis commitAnalysis = commitFactory.newInstance();

		/* Run the analysis. */
		commitAnalysis.analyze(commit);

        /* Print the data set. */
//		factBase.printDataSet();

		/* Only annotate the destination file. The source file isn't especially useful. */
		String annotatedDst = HTMLMultiDiffViewer.annotate(dstCode, factBase);

		/* Combine the annotated file with the UnixDiff. */
		String annotatedCombined = HTMLUnixDiffViewer.annotate(srcCode, dstCode, annotatedDst);
		Files.write(Paths.get(out), annotatedCombined.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

	}

	@Test
	public void testSimple() throws Exception {
		String src = "src/test/resources/input/simple_old.js";
		String dst = "src/test/resources/input/simple_new.js";
		String out = "out/simple.html";
		this.runTest(src, dst, out);
	}

	@Test
	public void testOne() throws Exception {
		String src = "src/test/resources/input/one_old.js";
		String dst = "src/test/resources/input/one_new.js";
		String out = "out/one.html";
		this.runTest(src, dst, out);
	}
	
	@Test
	public void testPM2_SymEx_Off() throws Exception {
		String src = "src/test/resources/input/pm2_old.js";
		String dst = "src/test/resources/input/pm2_new.js";
		String out = "out/pm2.html";
		Options.createInstance(SymEx.OFF);
		this.runTest(src, dst, out);
	}

	@Test
	public void testPM2_SymEx_On() throws Exception {
		String src = "src/test/resources/input/pm2_old.js";
		String dst = "src/test/resources/input/pm2_new.js";
		String out = "out/pm2.html";
		Options.createInstance(SymEx.ON);
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