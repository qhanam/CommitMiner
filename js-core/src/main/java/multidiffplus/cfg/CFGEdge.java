package multidiffplus.cfg;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;

/**
 * A labeled, directed edge to another node.
 */
public class CFGEdge {

    /** The unique id for this node. **/
    private int id;

    /**
     * The condition in which this edge is traversed. If null then the edge is
     * always traversed.
     **/
    private ClassifiedASTNode condition;

    /** The node that this edge exits. */
    private CFGNode from;

    /** The node that this edge points to. */
    private CFGNode to;

    /** The change operation applied to the edge from source to destination. **/
    public ChangeType changeType;

    /** True if this edge is the first edge of a loop. **/
    public boolean isLoopEdge;

    /**
     * The state of the environment and store before transferring over the term
     * (statement). The state is language dependent.
     */
    private AnalysisState beforeState;

    /**
     * The state of the environment and store after transferring over the term
     * (statement). The state is language dependent.
     */
    private AnalysisState afterState;

    public CFGEdge(ClassifiedASTNode condition, CFGNode from, CFGNode to, int id) {
	this.condition = condition;
	this.to = to;
	this.from = from;
	this.changeType = ChangeType.UNKNOWN;
	this.id = id;
    }

    public CFGEdge(ClassifiedASTNode condition, CFGNode from, CFGNode to, boolean loopEdge,
	    int id) {
	this.condition = condition;
	this.to = to;
	this.from = from;
	this.changeType = ChangeType.UNKNOWN;
	this.id = id;
    }

    /**
     * Accepts and runs a visitor.
     */
    public void accept(ICFGVisitor visitor) {
	visitor.visit(this);
    }

    /**
     * Set the lattice element at this point in the program.
     * 
     * @param as
     *            The abstract state.
     */
    public void setBeforeState(AnalysisState state) {
	this.beforeState = state;
    }

    /**
     * @return the abstract state at this point in the program.
     */
    public AnalysisState getBeforeState() {
	return this.beforeState;
    }

    /**
     * Set the lattice element at this point in the program.
     * 
     * @param as
     *            The abstract state.
     */
    public void setAfterState(AnalysisState state) {
	this.afterState = state;
    }

    /**
     * @return the abstract state at this point in the program.
     */
    public AnalysisState getAfterState() {
	return this.afterState;
    }

    /**
     * @return a shallow copy of the edge.
     */
    public CFGEdge copy() {
	return copy(this.id);
    }

    /**
     * @return a shallow copy of the edge.
     */
    public CFGEdge copy(int id) {
	return new CFGEdge(this.condition, this.from, this.to, id);
    }

    /**
     * @param to
     *            the node this edge enters.
     */
    public void setTo(CFGNode to) {
	this.to = to;
    }

    /**
     * @return the node this edge enters.
     */
    public CFGNode getTo() {
	return to;
    }

    /**
     * @param from
     *            the node this edge exits.
     */
    public void setFrom(CFGNode from) {
	this.from = from;
    }

    /**
     * @return the node this edge exits.
     */
    public CFGNode getFrom() {
	return from;
    }

    /**
     * @param condition
     *            the condition for which this edge is traversed.
     */
    public void setCondition(ClassifiedASTNode condition) {
	this.condition = condition;
    }

    /**
     * @return the condition for which this edge is traversed.
     */
    public ClassifiedASTNode getCondition() {
	return condition;
    }

    /**
     * @return the unique ID for the edge.
     */
    public int getId() {
	return id;
    }

    @Override
    public String toString() {
	return this.from.toString() + "-[" + this.condition + "]->" + this.to.toString();
    }

}
