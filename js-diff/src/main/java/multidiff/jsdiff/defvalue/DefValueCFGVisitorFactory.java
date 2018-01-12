package multidiff.jsdiff.defvalue;

import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICFGVisitorFactory;

public class DefValueCFGVisitorFactory implements ICFGVisitorFactory {

	@Override
	public ICFGVisitor newInstance(SourceCodeFileChange sourceCodeFileChange) {
		return new DefValueCFGVisitor(AnnotationFactBase.getInstance(sourceCodeFileChange));
	}

}