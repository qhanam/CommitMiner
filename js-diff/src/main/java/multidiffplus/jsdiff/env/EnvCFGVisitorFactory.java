package multidiffplus.jsdiff.env;

import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.facts.AnnotationFactBase;

public class EnvCFGVisitorFactory implements ICFGVisitorFactory {

	@Override
	public ICFGVisitor newInstance(SourceCodeFileChange sourceCodeFileChange) {
		return new EnvCFGVisitor(AnnotationFactBase.getInstance(sourceCodeFileChange));
	}

}