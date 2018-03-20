package multidiffplus.mining.astvisitor.unhandledex;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.IASTVisitorFactory;

public class TryASTAnalysisFactory implements IASTVisitorFactory {

	@Override
	public NodeVisitor newInstance(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		return new TryASTAnalysis(sourceCodeFileChange, root);
	}

}
