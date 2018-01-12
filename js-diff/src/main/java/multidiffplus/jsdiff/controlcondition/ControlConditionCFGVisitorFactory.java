package multidiffplus.jsdiff.controlcondition;

import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICFGVisitorFactory;

public class ControlConditionCFGVisitorFactory implements ICFGVisitorFactory {

	@Override
	public ICFGVisitor newInstance(SourceCodeFileChange sourceCodeFileChange) {
		return new ControlConditionCFGVisitor(AnnotationFactBase.getInstance(sourceCodeFileChange));
	}

}