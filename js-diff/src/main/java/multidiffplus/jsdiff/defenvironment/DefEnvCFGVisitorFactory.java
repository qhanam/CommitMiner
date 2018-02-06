package multidiffplus.jsdiff.defenvironment;

import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.facts.AnnotationFactBase;

public class DefEnvCFGVisitorFactory implements ICFGVisitorFactory {

	@Override
	public ICFGVisitor newInstance(SourceCodeFileChange sourceCodeFileChange) {
		return new DefEnvCFGVisitor(AnnotationFactBase.getInstance(sourceCodeFileChange));
	}

}