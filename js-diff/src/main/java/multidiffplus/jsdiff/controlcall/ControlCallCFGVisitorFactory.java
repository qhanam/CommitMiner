package multidiffplus.jsdiff.controlcall;

import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICFGVisitorFactory;

public class ControlCallCFGVisitorFactory implements ICFGVisitorFactory {

	@Override
	public ICFGVisitor newInstance(SourceCodeFileChange sourceCodeFileChange) {
		return new ControlCallCFGVisitor(AnnotationFactBase.getInstance(sourceCodeFileChange));
	}

}