package multidiffplus.cfg;

import java.util.List;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;

public class CallSiteNode {

    private ClassifiedASTNode callSite;

    private Predecessor predecessor;

    private AnalysisState beforeState;
    private AnalysisState afterState;

    /**
     * @param callSite
     *            The AST node containing the function call.
     * @param predecessorCallSite
     *            The call site that is evaluated immediately before this call site.
     */
    public CallSiteNode(ClassifiedASTNode callSite, CallSiteNode predecessorCallSite) {
	this.callSite = callSite;
	this.predecessor = new Predecessor(predecessorCallSite);
	this.beforeState = null;
	this.afterState = null;
    }

    /**
     * @param callSite
     *            The AST node containing the function call.
     * @param predecessorCfgNode
     *            The CFG node that contains this call site.
     */
    public CallSiteNode(ClassifiedASTNode callSite, CFGNode predecessorCfgNode) {
	this.callSite = callSite;
	this.predecessor = new Predecessor(predecessorCfgNode);
	this.beforeState = null;
	this.afterState = null;
    }

    /**
     * @param callSite
     *            The AST node containing the function call.
     * @param predecessorCfgEdge
     *            The CFG edge that contains this call site.
     */
    public CallSiteNode(ClassifiedASTNode callSite, List<CFGEdge> predecessorCfgEdges) {
	this.callSite = callSite;
	this.predecessor = new Predecessor(predecessorCfgEdges);
	this.beforeState = null;
	this.afterState = null;
    }

    /**
     * Returns the call site.
     */
    public ClassifiedASTNode getCallSite() {
	return callSite;
    }

    /**
     * Returns the abstract state before the call is evaluated.
     */
    public AnalysisState getBeforeState() {
	return beforeState;
    }

    /**
     * Set the abstract state before the call site is evaluated.
     */
    public void setBeforeState(AnalysisState state) {
	beforeState = state;
    }

    /**
     * Returns the abstract state after the call is evaluated.
     */
    public AnalysisState getAfterState() {
	return afterState;
    }

    /**
     * Set the abstract state after the call site is evaluated.
     */
    public void setAfterState(AnalysisState state) {
	beforeState = state;
    }

    /**
     * Returns the abstract state following the interpretation of the call site's
     * predecessor.
     */
    public AnalysisState getPredecessorAfterState() {
	return predecessor.getPredecessorAfterState();
    }

    /**
     * A statement, branch condition or call site, which is the predecessor of this
     * call site.
     */
    private class Predecessor {
	CallSiteNode callSite = null;
	CFGNode node = null;
	List<CFGEdge> edges = null;

	public Predecessor(CallSiteNode callSite) {
	    // Predecessor is the previous callsite in topological order.
	    this.callSite = callSite;
	}

	public Predecessor(CFGNode node) {
	    // Predecessor is the before state of the node that contains this call site.
	    this.node = node;
	}

	public Predecessor(List<CFGEdge> edges) {
	    // Predecessor is the before state of the edge that contains this call site.
	    this.edges = edges;
	}

	public AnalysisState getPredecessorAfterState() {
	    if (callSite != null)
		return callSite.afterState;
	    if (node != null)
		return node.getBeforeState();
	    else {
		AnalysisState beforeState = null;
		for (CFGEdge edge : edges) {
		    if (beforeState == null)
			beforeState = edge.getAfterState();
		    else
			beforeState = beforeState.join(edge.getAfterState());
		}
		return beforeState;
	    }
	}
    }

}
