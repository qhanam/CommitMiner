package symgen;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;
import problem.Constraint;
import problem.InitVariable;
import problem.VerificationProblem;
import problem.Constraint.Operator;
import problem.InitFunction;
import problem.VerificationProblem.Type;

/**
 * Unit tests for the symbolic execution engine.
 */
public class ValidateTest 
    extends TestCase
{
	
	private void run(VerificationProblem problem) {
    		CVC4Runner runner = new CVC4Runner("./lib/cvc4-1.5-x86_64-macos-10.12-opt");
		Assert.assertTrue(runner.verify(problem));
	}

    public void testUninterpretedFunctions() {

	    String oldProgram = "y = x;";
	    String newProgram = "z = x + 0;";
	    
	    	Set<InitVariable> oldInitVariables = new HashSet<InitVariable>();
	    	oldInitVariables.add( new InitVariable("x", InitVariable.Type.INT));
	    oldInitVariables.add(new InitVariable("y", InitVariable.Type.INT));
		oldInitVariables.add(new InitVariable("z", InitVariable.Type.INT));

		Set<InitVariable> newInitVariables = new HashSet<InitVariable>();
		newInitVariables.add( new InitVariable("x", InitVariable.Type.INT));
	    newInitVariables.add(new InitVariable("y", InitVariable.Type.INT));
		newInitVariables.add(new InitVariable("z", InitVariable.Type.INT));

		Set<InitFunction> oldInitFunctions = new HashSet<InitFunction>();
		oldInitFunctions.add(new InitFunction("f", InitVariable.Type.INT, new InitVariable.Type[] {InitVariable.Type.INT}));

		Set<InitFunction> newInitFunctions = new HashSet<InitFunction>();
		newInitFunctions.add(new InitFunction("f", InitVariable.Type.INT, new InitVariable.Type[] {InitVariable.Type.INT}));

		Set<Constraint> constraints = new HashSet<Constraint>();
		constraints.add(new Constraint("x", "x", Operator.EQ));

		String oldQueryVariable = "y";
		String newQueryVariable = "z";

		run(new VerificationProblem(Type.ASSIGN, oldProgram, newProgram, 
				oldInitVariables, newInitVariables, 
				oldInitFunctions, newInitFunctions, 
				constraints, 
				oldQueryVariable, newQueryVariable));

    }

}
