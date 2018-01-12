package problem;

public class Constraint {
	
	public String oldVariable;
	public String newVariable;
	public Operator operator;
	
	public Constraint(String oldVariable, String newVariable, Operator operator) {
		this.oldVariable = oldVariable;
		this.newVariable = newVariable;
		this.operator = operator;
	}
	
	public enum Operator {
		EQ,
		NEQ
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof Constraint)) return false;
		Constraint c = (Constraint) o;
		if(!oldVariable.equals(c.oldVariable)) return false;
		if(!newVariable.equals(c.newVariable)) return false;
		if(!operator.equals(c.operator)) return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		return (oldVariable + "~" + newVariable + "~" + operator).hashCode();
	}

}
