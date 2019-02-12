package multidiffplus.mining.ast.analysis.criterion.trycallsite;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.IASTVisitorFactory;

public class TryCallsiteAstAnalysisFactory implements IASTVisitorFactory {

    @Override
    public NodeVisitor newInstance(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
	return new TryCallsiteAnalysis(sourceCodeFileChange, root);
    }

}