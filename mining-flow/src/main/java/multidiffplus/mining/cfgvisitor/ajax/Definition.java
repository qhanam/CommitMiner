package multidiffplus.mining.cfgvisitor.ajax;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.commit.DependencyIdentifier;

/**
 * Stores a definition's {@code DependencyIdentifier} and {@code AstNode}.
 */
@SuppressWarnings("unused")
public class Definition {
	
	private DependencyIdentifier identifier;
	private AstNode node;
	private Integer line;
	private Integer absolutePosition;
	private Integer length;
	
	public Definition(DependencyIdentifier identifier, AstNode node, 
			int line, int absolutePosition, int length) {
		this.identifier = identifier;
		this.node = node;
		this.line = line;
		this.absolutePosition = absolutePosition;
		this.length = length;
	}
	
	public DependencyIdentifier getDependencyIdentifier() { return identifier; }
	public AstNode getAstNode() { return node; }
	
	@Override
	public int hashCode() {
		return identifier.getAddress().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof DependencyIdentifier)) return false;
		DependencyIdentifier x = (DependencyIdentifier)o;
		return identifier.getAddress().equals(x.getAddress());
	}

}