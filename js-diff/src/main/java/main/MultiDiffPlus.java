package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import multidiff.js.factories.ChangeImpactCommitAnalysisFactory;
import multidiffplus.analysis.CommitAnalysis;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.facts.JsonFactBase;
import multidiffplus.jsdiff.view.HTMLMultiDiffViewer;
import multidiffplus.jsdiff.view.HTMLUnixDiffViewer;

public class MultiDiffPlus {

    public static void main(String[] args) {

	/* The test files. */
	MultiDiffPlusOptions options = new MultiDiffPlusOptions();
	CmdLineParser parser = new CmdLineParser(options);

	try {
	    parser.parseArgument(args);
	} catch (CmdLineException e) {
	    MultiDiffPlus.printUsage(e.getMessage(), parser);
	    return;
	}

	/* Run the analysis. */
	MultiDiffPlus main = new MultiDiffPlus(options);
	try {
	    main.diff();
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }

    /** The analysis options. **/
    private MultiDiffPlusOptions options;

    public MultiDiffPlus(MultiDiffPlusOptions options) {
	this.options = options;
    }

    protected void diff() throws Exception {

	/* Read the source files. */
	String srcCode = new String(Files.readAllBytes(Paths.get(options.getOriginal())));
	String dstCode = new String(Files.readAllBytes(Paths.get(options.getModified())));

	/* Read the source files. */
	SourceCodeFileChange sourceCodeFileChange = getSourceCodeFileChange(options.getOriginal(),
		options.getModified());

	/* Build the dummy commit. */
	Commit commit = getCommit();
	commit.addSourceCodeFileChange(sourceCodeFileChange);

	/* Builds the data set with our custom queries. */
	AnnotationFactBase factBase = AnnotationFactBase.getInstance(sourceCodeFileChange);

	/* Set up the analysis. */
	ICommitAnalysisFactory commitFactory = new ChangeImpactCommitAnalysisFactory();
	CommitAnalysis commitAnalysis = commitFactory.newInstance();

	/* Run the analysis. */
	commitAnalysis.analyze(commit);

	/*
	 * Only annotate the destination file. The source file isn't especially useful.
	 */
	String annotatedDst = HTMLMultiDiffViewer.annotate(dstCode, factBase);

	/* Combine the annotated file with the UnixDiff. */
	String annotatedCombined = HTMLUnixDiffViewer.annotate(srcCode, dstCode, annotatedDst);
	Files.write(Paths.get(options.getOutputFile()), annotatedCombined.getBytes(),
		StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

	/* Write the json file. */
	JsonFactBase jsonFactBase = JsonFactBase.getInstance(sourceCodeFileChange);
	Files.write(Paths.get(options.getJsonFile()), jsonFactBase.getJson().toString().getBytes(),
		StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * @return A dummy commit for testing.
     */
    private static Commit getCommit() {
	return new Commit("test", "http://github.com/saltlab/Pangor", "c0", "c1", Type.BUG_FIX);
    }

    /**
     * ;
     * 
     * @return A dummy source code file change for testing.
     * @throws IOException
     */
    private static SourceCodeFileChange getSourceCodeFileChange(String srcFile, String dstFile)
	    throws IOException {
	String buggyCode = readFile(srcFile);
	String repairedCode = readFile(dstFile);
	return new SourceCodeFileChange(srcFile, dstFile, buggyCode, repairedCode);
    }

    /**
     * Reads the contents of a source code file into a string.
     * 
     * @param path
     *            The path to the source code file.
     * @return A string containing the source code.
     * @throws IOException
     */
    private static String readFile(String path) throws IOException {
	return FileUtils.readFileToString(new File(path));
    }

    /**
     * Prints the usage of main.
     * 
     * @param error
     *            The error message that triggered the usage message.
     * @param parser
     *            The args4j parser.
     */
    private static void printUsage(String error, CmdLineParser parser) {
	System.out.println(error);
	System.out.print("Usage: MultiDiffPlus ");
	parser.printSingleLineUsage(System.out);
	System.out.println("");
    }

}
