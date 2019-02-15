package multidiffplus.jsdiff.analysis;

import java.util.List;
import java.util.regex.Matcher;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ast.AstRoot;

import changeimpact.InterleavedInterCIA;
import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.diff.Diff;
import multidiffplus.diff.DiffContext;
import multidiffplus.factories.ICFGFactory;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.facts.JsonFactBase;

/**
 * Gathers change impact facts about one source code file.
 */
public class ChangeImpactDomainAnalysis extends DomainAnalysis {

    public List<ICFGVisitorFactory> cfgVisitorFactories;

    public ChangeImpactDomainAnalysis(List<ICFGVisitorFactory> cfgVisitorFactories,
	    ICFGFactory cfgFactory, boolean preProcess, boolean measureRuntime) {
	super(null, null, cfgFactory, preProcess, measureRuntime);
	this.cfgVisitorFactories = cfgVisitorFactories;
    }

    /**
     * Performs AST-differencing and launches the analysis of the
     * pre-commit/post-commit source code file pair.
     *
     * @param sourceCodeFileChange
     *            The source code file change information.
     * @param preProcess
     *            Set to true to enable AST pre-processing.
     * @param srcAnalysisFactory
     *            The analysis to run on the buggy file.
     * @param dstAnalysisClass
     *            The analysis to run on the repaired file.
     */
    protected void analyzeFile(SourceCodeFileChange sourceCodeFileChange) throws Exception {

	/* Get the file extension. */
	String fileExtension = getSourceCodeFileExtension(sourceCodeFileChange.buggyFile,
		sourceCodeFileChange.repairedFile);

	/* Difference the files and analyze if they are an extension we handle. */
	if (fileExtension != null && cfgFactory.acceptsExtension(fileExtension)) {

	    /* AST-diff the files. */
	    Diff diff = null;
	    try {
		String[] args = preProcess
			? new String[] { sourceCodeFileChange.buggyFile,
				sourceCodeFileChange.repairedFile, "-pp" }
			: new String[] { sourceCodeFileChange.buggyFile,
				sourceCodeFileChange.repairedFile };
		diff = new Diff(cfgFactory, args, sourceCodeFileChange.buggyCode,
			sourceCodeFileChange.repairedCode);
	    } catch (ArrayIndexOutOfBoundsException e) {
		System.err
			.println("ArrayIndexOutOfBoundsException: possibly caused by empty file.");
		e.printStackTrace();
		return;
	    } catch (EvaluatorException e) {
		System.err.println("Evaluator exception: " + e.getMessage());
		return;
	    } catch (Exception e) {
		throw e;
	    }

	    /*
	     * Get the results of the control flow differencing. The results include an
	     * analysis context: the source and destination ASTs and CFGs.
	     */
	    DiffContext diffContext = diff.getContext();

	    /*
	     * To co-ordinate interleaving, we need to setup an analysis one level higher.
	     */
	    InterleavedInterCIA interleavedAnalysis = new InterleavedInterCIA(cfgVisitorFactories,
		    sourceCodeFileChange, diffContext);

	    /* Run the analysis. */
	    interleavedAnalysis.analyze();

	    /* Add the Json. */
	    AstRoot dstScript = (AstRoot) diffContext.dstScript;
	    JsonFactBase.getInstance(sourceCodeFileChange)
		    .registerJsonFact(dstScript.getJsonObject());
	}

    }

    /**
     * @param preCommitPath
     *            The path of the file before the commit.
     * @param postCommitPath
     *            The path of the file after the commit.
     * @return The extension of the source code file or null if none is found or the
     *         extensions of the pre and post paths do not match.
     */
    protected static String getSourceCodeFileExtension(String preCommitPath,
	    String postCommitPath) {

	java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\.([a-z]+)$");
	Matcher preMatcher = pattern.matcher(preCommitPath);
	Matcher postMatcher = pattern.matcher(postCommitPath);

	String preExtension = null;
	String postExtension = null;

	if (preMatcher.find() && postMatcher.find()) {
	    preExtension = preMatcher.group(1);
	    postExtension = postMatcher.group(1);
	    if (preExtension.equals(postExtension))
		return preExtension;
	}

	return null;

    }

}