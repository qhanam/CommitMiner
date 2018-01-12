package multidiffplus.cfg;

/**
 * Visits a CFGNode or CFGEdge.
 */
public interface ICFGVisitor {

	/** Visits a node. **/
	void visit(CFGNode node);

	/** Visits an edge. **/
	void visit(CFGEdge edge);

}