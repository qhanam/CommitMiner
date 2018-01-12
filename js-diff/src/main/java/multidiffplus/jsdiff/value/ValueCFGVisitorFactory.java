package multidiffplus.jsdiff.value;

import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICFGVisitorFactory;

public class ValueCFGVisitorFactory implements ICFGVisitorFactory {

	@Override
	public ICFGVisitor newInstance(SourceCodeFileChange sourceCodeFileChange) {
		return new ValueCFGVisitor(AnnotationFactBase.getInstance(sourceCodeFileChange));
	}

}