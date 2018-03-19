package multidiffplus.mining.ast.factories;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.mining.ast.analysis.ModifiedStatementASTAnalysis;

public class ModifiedStatementASTAnalysisFactory implements IASTVisitorFactory {

	@Override
	public NodeVisitor newInstance(SourceCodeFileChange sourceCodeFileChange, AstNode root) {
		return new ModifiedStatementASTAnalysis(sourceCodeFileChange, root);
	}

}
