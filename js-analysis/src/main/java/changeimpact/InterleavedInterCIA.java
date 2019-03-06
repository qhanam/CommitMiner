package changeimpact;

import org.mozilla.javascript.ast.AstRoot;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.diff.DiffContext;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.jsanalysis.annotate.DependencyASTVisitor;
import multidiffplus.jsanalysis.flow.Analysis;

/**
 * Performs an inter-procedural analysis of a script (file). Synchronizes the
 * change impact analysis of the original and new files.
 */
public class InterleavedInterCIA {

    SourceCodeFileChange sourceCodeFileChange;
    DiffContext diffContext;
    Analysis dstAnalysis;

    public InterleavedInterCIA(SourceCodeFileChange sourceCodeFileChange, DiffContext diffContext) {
	this.sourceCodeFileChange = sourceCodeFileChange;
	this.diffContext = diffContext;
	this.dstAnalysis = Analysis.build(diffContext.dstScript, diffContext.dstCFGs);
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
