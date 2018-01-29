package multidiffplus.mining.test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.commit.Annotation;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.mining.factories.MiningCommitAnalysisFactory;

public class AjaxDataTests {
	
	private static final List<DependencyIdentifier> EMPTY = new LinkedList<DependencyIdentifier>();

	/**
	 * Tests data mining data set construction.
	 * @param args The command line arguments (i.e., old and new file names).
	 * @throws Exception
	 */
	protected void runTest(String src, String dst, Annotation expected) throws Exception {

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
	
		/* Check that the correct annotations were generated. */
		SortedSet<Annotation> actualAnnotations = factBase.getAnnotations();
		if(expected != null)
			Assert.assertTrue(actualAnnotations.contains(expected));
		else
			Assert.assertTrue(actualAnnotations.isEmpty());

	}

	@Test
	public void testContrived() throws Exception {
		String src = "src/test/resources/ajax_stringify/contrived_old.js";
		String dst = "src/test/resources/ajax_stringify/contrived_new.js";
		Annotation expected = new Annotation("AJAX_STRINGIFY_MUTATE", EMPTY, 2, 16, 37);
		this.runTest(src, dst, expected);
	}

	@Test
	public void testStrider() throws Exception {
		String src = "src/test/resources/ajax_stringify/strider_old.js";
		String dst = "src/test/resources/ajax_stringify/strider_new.js";
		Annotation expected = new Annotation("AJAX_STRINGIFY_REPAIR", EMPTY, 70, 2103, 34);
		this.runTest(src, dst, expected);
	}

	@Test
	public void testRecline() throws Exception {
		String src = "src/test/resources/ajax_stringify/recline_old.js";
		String dst = "src/test/resources/ajax_stringify/recline_new.js";
		Annotation expected = new Annotation("AJAX_STRINGIFY_REPAIR", EMPTY, 126, 3996, 20);
		this.runTest(src, dst, expected);
	}

	@Test
	public void testAnnotator() throws Exception {
		String src = "src/test/resources/ajax_stringify/annotator_old.js";
		String dst = "src/test/resources/ajax_stringify/annotator_new.js";
		Annotation expected = null;
		this.runTest(src, dst, expected);
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