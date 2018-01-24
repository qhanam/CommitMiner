package multidiffplus.factories;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.commit.SourceCodeFileChange;

/**
 * Builds new instances of a domain analysis.
 */
public interface IASTVisitorFactory {
	NodeVisitor newInstance(SourceCodeFileChange sourceCodeFileChange, AstNode root);
}