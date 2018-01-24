package multidiffplus.mining.factories;

import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.mining.analysis.AjaxDataASTAnalysis;

public class AjaxDataASTAnalysisFactory implements IASTVisitorFactory {

	@Override
	public NodeVisitor newInstance(SourceCodeFileChange sourceCodeFileChange) {
		return new AjaxDataASTAnalysis(sourceCodeFileChange);
	}

}
