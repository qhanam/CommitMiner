package multidiffplus.mining.cfgvisitor.ajax;

import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.facts.AnnotationFactBase;

public class AjaxCFGVisitorFactory implements ICFGVisitorFactory {

	@Override
	public ICFGVisitor newInstance(SourceCodeFileChange sourceCodeFileChange) {
		return new AjaxCFGVisitor(AnnotationFactBase.getInstance(sourceCodeFileChange));
	}

}