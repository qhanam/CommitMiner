package multidiffplus.jsanalysis.abstractdomain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.UnaryExpression;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.commit.DependencyIdentifier;

/**
 * Stores the state of control flow changes due to changes in branch conditions.
 */
public class ControlCondition implements DependencyIdentifier {

    /**
     * Tracks condition changes for each branch. When the negated branch condition
     * is encountered, the condition is removed from the set.
     */
    public Set<Condition> conditions;

    /**
     * Tracks control flow changes that have been merged and no longer apply.
     */
    public Set<Condition> negConditions;

    private ControlCondition() {
	conditions = new HashSet<Condition>();
	negConditions = new HashSet<Condition>();
    }

    private ControlCondition(Set<Condition> conditions, Set<Condition> negConditions) {
	this.conditions = conditions;
	this.negConditions = negConditions;
    }

    /**
     * Updates the state for the branch conditions exiting the CFGNode.
     * 
     * @return The new state (ControlFlowChange) after update.
     */
    public ControlCondition update(CFGEdge edge) {

	Set<Condition> conditions = new HashSet<Condition>(this.conditions);
	Set<Condition> negConditions = new HashSet<Condition>(this.negConditions);

	// Put the current branch condition in the 'conditions' set and all other
	// conditions in the 'neg' set since they must be false.

	if (edge.getCondition() != null) {

	    Change change = Change.convU((AstNode) edge.getCondition(),
		    (n) -> Dependencies.injectConditionChange(n));

	    if (change.isChanged()) {
		conditions.add(new Condition((AstNode) edge.getCondition(), change));

		for (CFGEdge child : edge.getFrom().getOutgoingEdges()) {
		    if (child != edge && child.getCondition() != null) {
			negConditions.add(new Condition((AstNode) child.getCondition(), change));
		    }
		}
	    }

	}

	/* Check the siblings for neg conditions. */
	for (CFGEdge child : edge.getFrom().getOutgoingEdges()) {
	    if (child != edge && child.getCondition() != null) {

		Change change = Change.convU((AstNode) child.getCondition(),
			(n) -> Dependencies.injectConditionChange(n));

		if (change.isChanged()) {
		    negConditions.add(new Condition((AstNode) child.getCondition(), change));
		}
	    }
	}

	return new ControlCondition(conditions, negConditions);

    }

    /**
     * Joins two ControlFlowChanges.
     * 
     * @return The new state (ControlFlowChange) after join.
     */
    public ControlCondition join(ControlCondition right) {

	/* Join the sets. */
	Set<Condition> conditions = new HashSet<Condition>(this.conditions);
	Set<Condition> negConditions = new HashSet<Condition>(this.negConditions);

	conditions.addAll(right.conditions);
	negConditions.addAll(right.negConditions);

	/* conditions = conditions - negConditions */
	conditions.removeAll(negConditions);

	return new ControlCondition(conditions, negConditions);

    }

    public boolean isChanged() {
	return !conditions.isEmpty();
    }

    public static ControlCondition bottom() {
	return new ControlCondition();
    }

    public static ControlCondition inject(Condition condition, Set<Condition> negConditions) {
	Set<Condition> conditions = new HashSet<Condition>();
	conditions.add(condition);
	return new ControlCondition(conditions, negConditions);
    }

    @Override
    public String getAddress() {
	String id = "";
	if (conditions.isEmpty())
	    return "";
	for (Condition condition : conditions) {
	    /* Get the ID of the non-negated condition. */
	    if (condition.getCondition().getID() == null
		    && condition.getCondition() instanceof UnaryExpression) {
		UnaryExpression ue = (UnaryExpression) condition.getCondition();
		if (ue.getOperator() == Token.NOT)
		    id += ((ParenthesizedExpression) ue.getOperand()).getExpression().getID() + ",";
	    } else {
		id += condition.getCondition().getID() + ",";
	    }
	}
	return id.substring(0, id.length() - 1);
    }

    @Override
    public List<Integer> getAddresses() {
	List<Integer> addresses = new ArrayList<Integer>();
	if (conditions.isEmpty())
	    return addresses;
	for (Condition condition : conditions) {
	    /* Get the ID of the non-negated condition. */
	    if (condition.getCondition().getID() == null
		    && condition.getCondition() instanceof UnaryExpression) {
		UnaryExpression ue = (UnaryExpression) condition.getCondition();
		if (ue.getOperator() == Token.NOT)
		    addresses.add(
			    ((ParenthesizedExpression) ue.getOperand()).getExpression().getID());
	    } else {
		addresses.add(condition.getCondition().getID());
	    }
	}
	return addresses;
    }

}
