package multidiffplus.jsanalysis.abstractdomain;

import org.mozilla.javascript.ast.AstNode;

public class Condition {

    private AstNode condition;
    private Change conditionChange;

    public Condition(AstNode condition, Change conditionChange) {
	this.condition = condition;
	this.conditionChange = conditionChange;
    }

    public AstNode getCondition() {
	return condition;
    }

    public Change getChange() {
	return conditionChange;
    }

    @Override
    public int hashCode() {
	return condition.hashCode();
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof Condition))
	    return false;
	Condition that = (Condition) o;
	return this.condition.equals(that.condition);
    }

}
