package multidiffplus.factories;

import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.SourceCodeFileChange;

/**
 * Builds new instances of a domain analysis.
 */
public interface ICFGVisitorFactory {
	ICFGVisitor newInstance(SourceCodeFileChange sourceCodeFileChange);
}