package multidiff.analysis.flow;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.diff.DiffContext;
import multidiffplus.facts.AnnotationFactBase;

/**
 * An analysis which performs interleaved, inter-procedural change impact
 * analysis of a script.
 */
public class InterleavedInterCIA {

    SourceCodeFileChange sourceCodeFileChange;
    DiffContext diffContext;
    Analysis dstAnalysis;

    public InterleavedInterCIA(SourceCodeFileChange sourceCodeFileChange, DiffContext diffContext,
	    Analysis dstAnalysis) {
	this.sourceCodeFileChange = sourceCodeFileChange;
	this.diffContext = diffContext;
	this.dstAnalysis = dstAnalysis;
    }

    /**
     * Run the analysis.
     */
    public void analyze() throws Exception {

	/* Run the dst analysis. */
	dstAnalysis.run();

	/* Run reachable but un-analyzed functions. */
	while (dstAnalysis.pushReachableFunction())
	    dstAnalysis.run();

	/* Create criterion/dependency annotations for GUI output. */
	dstAnalysis.registerAnnotations(diffContext.dstScript,
		AnnotationFactBase.getInstance(sourceCodeFileChange));

    }

}
