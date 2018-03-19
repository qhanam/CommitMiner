package multidiffplus.mining.ast.factories;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.mining.ast.analysis.WrapperASTAnalysis;

public class JQueryASTAnalysisFactory implements IASTVisitorFactory {

	@Override
	public NodeVisitor newInstance(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		return new WrapperASTAnalysis(sourceCodeFileChange, root);
	}

}
