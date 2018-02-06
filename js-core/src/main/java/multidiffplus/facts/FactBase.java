package multidiffplus.facts;

import multidiffplus.commit.SourceCodeFileChange;

/**
 * Facts should be registered through a FactBase. The goal of a FactBase is to
 * make the structure of facts more consistent and make the registering 
 * process more maintainable by providing a central location for working with
 * the fact database.
 */
public abstract class FactBase {
	
	/* The source file pair. */
	protected SourceCodeFileChange sourceCodeFileChange;
	
	protected FactBase(SourceCodeFileChange sourceCodeFileChange) {
		this.sourceCodeFileChange = sourceCodeFileChange;
	}
	
}