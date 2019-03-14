package multidiff.analysis.flow;

import org.mozilla.javascript.ast.AstRoot;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.diff.DiffContext;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.jsanalysis.annotate.DependencyASTVisitor;

/**
 * An analysis which performs inter-procedural analysis of a script (file).
 */
public class InterleavedInterCIA {

    SourceCodeFileChange sourceCodeFileChange;
    DiffContext diffContext;
    Analysis dstAnalysis;

    public InterleavedInterCIA(SourceCodeFileChange sourceCodeFileChange, Analysis dstAnalysis) {
	this.sourceCodeFileChange = sourceCodeFileChange;
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
	DependencyASTVisitor.registerAnnotations((AstRoot) diffContext.dstScript,
		AnnotationFactBase.getInstance(sourceCodeFileChange));

    }

}
