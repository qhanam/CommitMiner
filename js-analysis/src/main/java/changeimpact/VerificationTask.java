package changeimpact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ReturnStatement;

import changeimpact.DefinitionVisitor.Definition;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.transfer.ExpEval;
import problem.Constraint;
import problem.InitFunction;
import problem.InitVariable;
import problem.InitVariable.Type;
import symgen.CVC4Runner;
import problem.VerificationProblem;

/**
 * Fixes false positive change impacts by verifying equivalence between program
 * slices.
 */
public class VerificationTask {
	
	ExpEval evaluator;
	
	private CFGNode srcCFGNode;
	private CFGNode dstCFGNode;
	
	/** The maximum number of statements in the backwards slice. **/
	private int maxDepth;
	
	private VerificationTask(CFGNode srcNode, CFGNode dstNode, int maxDepth) {
		this.evaluator = new ExpEval((State)dstNode.getAfterState());
		this.srcCFGNode = srcNode;
		this.dstCFGNode = dstNode;
		this.maxDepth = maxDepth;
	}
	
	/**
	 * Verifies equivalence for some change impacts.
	 * @return {@code true} if both versions of the assignee are guaranteed
	 * semantically equivalent, {@code false} if they may not be.
	 */
	public boolean verify() {
		
		AstNode dstNode = (AstNode)dstCFGNode.getStatement();
		
		/* We only need to verify values that may be changed according to data
		* flow analysis.  */
		Definition def = DefinitionVisitor.getDependencies(
				evaluator, dstNode);
		if(!def.hasDef()) return true;
		if(!def.change.isChanged()) return true;
		
		/* We need to verify this value. */
		VerificationProblem problem = buildVerificationProblem(srcCFGNode, dstCFGNode);
		if(problem == null) return true; // Something went wrong.
		System.out.println(problem);

		/* Look for equivalence with SMT. */
		CVC4Runner runner = new CVC4Runner("/opt/local/bin/cvc4");
		boolean equivalent = false;
		try {
			equivalent = runner.verify(problem);
		} catch (NullPointerException e) {
			// Ignore
		}

		/* If the values are equivalent, update the BValue. */
		if(equivalent) {
			
			switch(problem.type) {

			case RETURN:
				BValue retValue = evaluator.resolveReturn();
				retValue.change = Change.u();
				break;

			case ASSIGN:
				for(Address address: def.addresses) {
					BValue newValue = evaluator.resolveAddress(address);
					
					newValue.change = Change.u();
					evaluator.updateAddress(address, newValue);
				}
				break;
			case UNKNOWN:

			}
			
			return true;
		}
		
		return false;
		
	}
	
	/**
	 * @return The verification problem.
	 */
	private VerificationProblem buildVerificationProblem(
			CFGNode oldCriterion, CFGNode newCriterion) {
		
		Set<Constraint> constraints = new HashSet<Constraint>();
		
		/* Create backwards slices for both versions. */
		SubProblem oldSubProblem = buildSubProblem(oldCriterion);
		SubProblem newSubProblem = buildSubProblem(newCriterion);
		
		/* Make sure the statements match. */
		if(oldSubProblem.type != newSubProblem.type) return null;
		
		/* Sync sets of initial variables so the constraints make sense. */
		oldSubProblem.initVariables.addAll(newSubProblem.initVariables);
		newSubProblem.initVariables.addAll(oldSubProblem.initVariables);

		/* Merge constraint sets. */
		constraints.addAll(oldSubProblem.constraints);
		constraints.addAll(newSubProblem.constraints);
		
		Set<InitFunction> oldInitFunctions = new HashSet<InitFunction>();
		Set<InitFunction> newInitFunctions = new HashSet<InitFunction>();
		return new VerificationProblem(
				newSubProblem.type,
				oldSubProblem.program, newSubProblem.program,
				oldSubProblem.initVariables, newSubProblem.initVariables,
				oldInitFunctions, newInitFunctions,
				constraints, 
				oldSubProblem.queryVariable, newSubProblem.queryVariable);

	}

	private SubProblem buildSubProblem(CFGNode criterion) {
		
		int depth = 0;
		
		/* The verification sub-problem. */
		VerificationProblem.Type type;
		String program = "";
		Set<InitVariable> initVariables = new HashSet<InitVariable>();
		Set<Constraint> constraints = new HashSet<Constraint>();
		String queryVariable;
		
		/* For keeping track of data dependencies. */
		Definition def;
		Map<String, Change> dependencies;

		/* For traversing the CFG. */
		Set<CFGEdge> visited = new HashSet<CFGEdge>();
		Stack<CFGNode> stack = new Stack<CFGNode>();

		/* Initialize. */
		def = DefinitionVisitor.getDependencies(
				new ExpEval((State)criterion.getAfterState()), 
				(AstNode)criterion.getStatement());
		type = def.type;
		queryVariable = def.definition;
		initVariables.add(new InitVariable(queryVariable, Type.INT));
		dependencies = new HashMap<String, Change>();
		dependencies.put(def.definition, def.change);
		stack.push(criterion);

		/* Traverse the CFG backwards. */
		while(!stack.isEmpty()) {

			CFGNode node = stack.pop();
			
			/* Get the definition and dependencies for this node. */
			def = DefinitionVisitor.getDependencies(
					new ExpEval((State)node.getAfterState()),
					(AstNode)node.getStatement());

			/* If the definition is one of our dependencies, we need to include
			* this statement in our slice and update the list of dependencies. */
			if(def.hasDef() && dependencies.containsKey(def.definition)) {
				initVariables.add(new InitVariable(def.definition, Type.INT));
				dependencies.remove(def.definition);
				for(Entry<String, Change> entry : def.dependencies.entrySet()) {
					dependencies.put(entry.getKey(), entry.getValue());
				}
				switch(((AstNode)node.getStatement()).getType()) {
				case Token.RETURN:
					ReturnStatement ret = (ReturnStatement)node.getStatement();
					Name name = new Name(ret.getPosition(), "retval");
					AstNode val = ret.getReturnValue();
					Assignment assign = new Assignment(Token.ASSIGN, name, val, 3);
					program = new ExpressionStatement(assign).toSource() + "\n" + program;
					break;
				default:
					program  = ((AstNode)node.getStatement()).toSource() + "\n" + program;
					break;
				}
				depth++;
				if(depth >= this.maxDepth) break;
			}

			/* Visit add the incoming edges to the stack. */
			for(CFGEdge edge : node.getIncommingEdges()) {
				if(!visited.contains(edge) && edge.getFrom().getIncommingEdgeCount() > 0) {
					visited.add(edge);
					stack.push(edge.getFrom());
				}
			}
			
		}
		
		/* Set the initial variables and constraints from the dependency list. */
		for(Entry<String, Change> entry : dependencies.entrySet()) {
			initVariables.add(new InitVariable(entry.getKey(), Type.INT));
			if(entry.getValue().isChanged())
				constraints.add(
						new Constraint(entry.getKey(), entry.getKey(), 
								Constraint.Operator.NEQ));
			else
				constraints.add(
						new Constraint(entry.getKey(), entry.getKey(),
								Constraint.Operator.EQ));
		}
		
		return new SubProblem(type, program, initVariables, constraints, queryVariable);
		
	}
	
	public static  VerificationTask build(CFGNode srcNode, CFGNode dstNode, int maxDepth) {
		return new VerificationTask(srcNode, dstNode, maxDepth);
	}
	
	private class SubProblem {

		public VerificationProblem.Type type;
		public String program;
		public Set<InitVariable> initVariables;
		public Set<Constraint> constraints;
		public String queryVariable;

		public SubProblem(VerificationProblem.Type type, String program, 
				Set<InitVariable> initVariables,
				Set<Constraint> constraints, String queryVariable) {
			this.type = type;
			this.program = program;
			this.initVariables = initVariables;
			this.constraints = constraints;
			this.queryVariable = queryVariable;
		}

	}
	
}
