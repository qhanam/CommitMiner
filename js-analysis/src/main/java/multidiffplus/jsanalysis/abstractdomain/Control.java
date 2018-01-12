package multidiffplus.jsanalysis.abstractdomain;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;

/**
 * Stores the state of control flow changes.
 */
public class Control {
	
	private ControlCall call;
	private ControlCondition condition;

	public Control() {
		call = new ControlCall();
		condition = new ControlCondition();
	}

	public Control(ControlCall call, ControlCondition condition) {
		this.call = call;
		this.condition = condition;
	}

	@Override
	public Control clone() {
		return new Control(call, condition);
	}

	/**
	 * Updates the state for the branch conditions exiting the CFGNode.
	 * @return The new control state after update.
	 */
	public Control update(CFGEdge edge, CFGNode node) {
		return new Control(call, condition.update(edge, node));
	}
	
	/**
	 * Updates the state for a function call.
	 * @return The new control state after updates.
	 */
	public Control update(AstNode fc) {

		/* If this is a new function call, we interpret the control of
		 * the callee as changed. */
		if(Change.convU(fc).le == Change.LatticeElement.CHANGED)
			return new Control(call.update(fc.getID()), new ControlCondition());

		/* If this is not a new function call,  the control-call lattice is set to
		* bottom. */
		return new Control(new ControlCall(), new ControlCondition());

	}
	
	/**
	 * Joins two Control abstract domains.
	 * @return The new state (ControlFlowChange) after join.
	 */
	public Control join(Control right) {

		return new Control(call.join(right.call),
						   condition.join(right.condition));

	}
	
	public ControlCall getCall() {
		return call;
	}
	
	public ControlCondition getCondition() {
		return condition;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Control)) return false;
		Control cc = (Control)o;
		return call.equals(cc.call);
	}

}