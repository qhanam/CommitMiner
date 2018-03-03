package multidiffplus.mining.astvisitor.ajax;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.IASTVisitorFactory;

public class AjaxDataASTAnalysisFactory implements IASTVisitorFactory {

	@Override
	public NodeVisitor newInstance(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		return new AjaxDataASTAnalysis(sourceCodeFileChange, root);
	}

}
