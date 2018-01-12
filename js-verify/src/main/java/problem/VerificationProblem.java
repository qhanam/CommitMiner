package problem;

import java.util.Set;

public class VerificationProblem {
	
	public Type type;
	public String oldProgram;
	public String newProgram;
	public Set<InitVariable> oldInitVariables;
	public Set<InitVariable> newInitVariables;
	public Set<InitFunction> oldInitFunctions;
	public Set<InitFunction> newInitFunctions;
	public Set<Constraint> constraints;
	public String oldQueryVariable;
	public String newQueryVariable;
	
	public VerificationProblem(Type type,
								String oldProgram,
								String newProgram,
								Set<InitVariable> oldInitVariables, 
								Set<InitVariable> newInitVariables, 
								Set<InitFunction> oldInitFunctions, Set<InitFunction> newInitFunctions, Set<Constraint> constraints, 
								String oldQueryVariable,
								String newQueryVariable) {
		this.type = type;
		this.oldProgram = oldProgram;
		this.newProgram = newProgram;
		this.oldInitVariables = oldInitVariables;
		this.newInitVariables = newInitVariables;
		this.oldInitFunctions = oldInitFunctions;
		this.newInitFunctions = newInitFunctions;
		this.constraints = constraints;
		this.oldQueryVariable = oldQueryVariable;
		this.newQueryVariable = newQueryVariable;
	}
	
	@Override
	public String toString() {
		String s = "";
		for(InitVariable var : oldInitVariables)
			s += var.type + " " + var.name + "o\n";
		for(InitVariable var : newInitVariables)
			s += var.type + " " + var.name + "n\n";
		for(InitFunction func : oldInitFunctions)
			s += func.name+ "o : " + func.getTypeString() + "\n";  
		for(InitFunction func : newInitFunctions)
			s += func.name+ "n : " + func.getTypeString() + "\n";  
		for(Constraint c : constraints)
			s += c.oldVariable + "o " + c.operator + " " + c.newVariable + "n\n";
		s += "OLD: " + oldProgram + "\n";
		s += "NEW: " + newProgram + "\n";
		s += "ASSERT(" + oldQueryVariable + "o" + " = " + newQueryVariable + "n" + ")";
		return s;
	}

	public enum Type {
		UNKNOWN,
		ASSIGN,
		RETURN
	}


}
