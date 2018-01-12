package symgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.IRFactory;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.NodeVisitor;

import problem.Constraint;
import problem.Constraint.Operator;
import problem.InitVariable;
import problem.VerificationProblem;

public class CVC4Translator implements NodeVisitor {

	private static final String OLD = "o";
	private static final String NEW = "n";

	public Map<String, Integer> varCtr = new HashMap<String, Integer>();
	public Map<String, String> varTypes = new HashMap<String, String>();

	public String postfix; // distinguish between vars in the old version and new version

	public String cvc4;

	/**
	 * Produce CVC4 code for the verification problem.
	 */
	public static String translateProblemToCVC4(VerificationProblem verificationProblem) {
		
		/* Build the variable maps for creating unique identifiers. */
		Map<String, Integer> oldCtrs = initVarCtr(verificationProblem.oldInitVariables);
		Map<String, String> oldTypes = initVarTypes(verificationProblem.oldInitVariables);
		Map<String, Integer> newCtrs = initVarCtr(verificationProblem.newInitVariables);
		Map<String, String> newTypes = initVarTypes(verificationProblem.newInitVariables);

		/* Add variable initializations. */
		String oldInit = getInitializations(verificationProblem.oldInitVariables, OLD);
		String newInit = getInitializations(verificationProblem.oldInitVariables, NEW);
		
		/* Add constraints. */
		String constraints = "";
		for(Constraint constraint : verificationProblem.constraints) {
			if(constraint.operator == Operator.EQ) {
				String old_var  =  constraint.oldVariable + OLD;
				String new_var = constraint.newVariable + NEW;
				constraints += "ASSERT (" + old_var +  "0" + " = " + new_var + "0);\n";
			}
		}

		/* Translate the source code into CVC4. */
		String oldCode = translateCode(verificationProblem.oldProgram, oldCtrs, oldTypes, OLD);
		String newCode = translateCode(verificationProblem.newProgram, newCtrs, newTypes, NEW);

		/* Add assertions. */
		String assertions = getAssertions(verificationProblem.oldQueryVariable, verificationProblem.newQueryVariable, oldCtrs, newCtrs);

		return oldInit + newInit + constraints + oldCode + newCode + assertions;

	}
	
	private static String getAssertions(String oldVar, String newVar, Map<String, Integer> oldVars, Map<String, Integer> newVars) {
		String condition = oldVar + OLD + oldVars.get(oldVar) + " = " + newVar + NEW + newVars.get(newVar);
		String cvc4 = "PUSH;\n";
		cvc4 += "QUERY (" + condition + ");\n";
		cvc4 += "POP;\n";
		return cvc4;
	}
	
	/**
	 * @return CVC4 that initializes the given variables.
	 */
	private static String getInitializations(Set<InitVariable> variables, String postfix) {
		String cvc4 = "";
		for(InitVariable variable : variables)
			cvc4 += variable.name + postfix + "0 : " + variable.type.toString() + ";\n";
		return cvc4;
	}
	
	/**
	 * @return the source code component translated to CVC4
	 */
	private static String translateCode(String code, Map<String, Integer> varCtr, Map<String, String> varTypes, String postfix) {
		IRFactory factory = new IRFactory (new CompilerEnvirons());
		AstRoot root = factory.parse(code, null, 0);
		CVC4Translator translator = new CVC4Translator(varCtr, varTypes, 1, postfix);
		root.visit(translator);
		return translator.cvc4;
	}

	/**
	 * @return a map of variables with counters initialized to zero.
	 */
	private static Map<String, Integer> initVarCtr(Set<InitVariable> initVariables) {
		Map<String, Integer> variables = new HashMap<String, Integer>();
		for(InitVariable variable : initVariables) variables.put(variable.name, 0);
		return variables;
	}

	/**
	 * @return a map of variables with counters initialized to zero.
	 */
	private static Map<String, String> initVarTypes(Set<InitVariable> initVariables) {
		Map<String, String> variables = new HashMap<String, String>();
		for(InitVariable variable : initVariables) variables.put(variable.name, variable.type.toString());
		return variables;
	}

	public CVC4Translator(Map<String, Integer> varCtr, Map<String, String> varTypes, int arraySize, String postfix) {
		this.varCtr = varCtr;
		this.varTypes = varTypes;
		this.postfix = postfix;
		this.cvc4 = new String();
	}

	public boolean visit(AstNode node) {
		
		switch(node.getType()) {
		case Token.ASSIGN:
			return visitAssignment((Assignment) node);
		}
		
		return true;

	}
	
	/**
	 * Create CVC4 script from an assignment statement. It has the form: "x[n]:
	 * INT = y[n]".
	 */
	private boolean visitAssignment(Assignment a) {
		
		/* Get the variable being assigned to and update its counter. */

		String lhs = "";
		String left;
		int leftCnt;

		switch(a.getLeft().getType()) {
	
		case Token.NAME:
			left = a.getLeft().toSource();
			leftCnt = varCtr.get(left) + 1;
			lhs = left + postfix + leftCnt + " : " + varTypes.get(left) + " = ";
			break;
		default:
			return false;
		}

		String leftType = varTypes.get(left);

		/* Get the value to assign. */
		cvc4 += lhs + ExpressionBuilder.generateCVC4(a.getRight(), varCtr, postfix, leftType) + ";\n";

		/* Update the variable counter. */
		varCtr.put(left,  leftCnt);

		return false;

	}
	
}
