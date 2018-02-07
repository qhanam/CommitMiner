package multidiffplus.mining.ast.test;

import java.io.File;
import java.io.IOException;
import java.util.SortedSet;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.mining.flow.factories.MiningCommitAnalysisFactory;

public class AjaxDataTests {
	
	/**
	 * Tests data mining data set construction.
	 */
	protected void runTest(String src, String dst, String expected) throws Exception {

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
		SortedSet<Annotation> annotations = factBase.getAnnotations();
		if(expected != null)
			Assert.assertTrue(contains(annotations, expected));
		else
			Assert.assertTrue(annotations.isEmpty());

	}

	@Test
	public void testContrived() throws Exception {
		String src = "src/test/resources/ajax_stringify/contrived_old.js";
		String dst = "src/test/resources/ajax_stringify/contrived_new.js";
		String expected = "AJAX_STRINGIFY_MUTATE_DEL";
		this.runTest(src, dst, expected);
	}

	@Test
	public void testStrider() throws Exception {
		String src = "src/test/resources/ajax_stringify/strider_old.js";
		String dst = "src/test/resources/ajax_stringify/strider_new.js";
		String expected = "AJAX_STRINGIFY_REPAIR";
		this.runTest(src, dst, expected);
	}

	@Test
	public void testStrider2() throws Exception {
		String src = "src/test/resources/ajax_stringify/strider2_old.js";
		String dst = "src/test/resources/ajax_stringify/strider2_new.js";
		String expected = null;
		this.runTest(src, dst, expected);
	}

	@Test
	public void testRecline() throws Exception {
		String src = "src/test/resources/ajax_stringify/recline_old.js";
		String dst = "src/test/resources/ajax_stringify/recline_new.js";
		String expected = "AJAX_STRINGIFY_REPAIR";
		this.runTest(src, dst, expected);
	}

	@Test
	public void testAnnotator() throws Exception {
		String src = "src/test/resources/ajax_stringify/annotator_old.js";
		String dst = "src/test/resources/ajax_stringify/annotator_new.js";
		String expected = "AJAX_STRINGIFY_MUTATE_ADD";
		this.runTest(src, dst, expected);
	}

	@Test
	public void testNodeRed1() throws Exception {
		String src = "src/test/resources/ajax_stringify/node-red_old.js";
		String dst = "src/test/resources/ajax_stringify/node-red_new.js";
		String expected = "AJAX_STRINGIFY_MUTATE_DEL";
		this.runTest(src, dst, expected);
	}

	@Test
	public void testNodeRed2() throws Exception {
		String src = "src/test/resources/ajax_stringify/node-red_old.js";
		String dst = "src/test/resources/ajax_stringify/node-red_new.js";
		String expected = "AJAX_OTHER";
		this.runTest(src, dst, expected);
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
	
	/**
	 * @return {@cod true} if the annotations contain the label.
	 */
	private static boolean contains(SortedSet<Annotation> annotations, String label) {
		for(Annotation annotation : annotations)
			if(annotation.label.equals(label)) return true;
		return false;
	}

}