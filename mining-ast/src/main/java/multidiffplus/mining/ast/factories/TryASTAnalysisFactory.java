package multidiffplus.mining.ast.factories;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.mining.ast.analysis.TryASTAnalysis;

public class TryASTAnalysisFactory implements IASTVisitorFactory {

	@Override
	public NodeVisitor newInstance(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		return new TryASTAnalysis(sourceCodeFileChange, root);
	}

}