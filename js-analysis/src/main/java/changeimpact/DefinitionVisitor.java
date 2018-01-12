package changeimpact;

import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ReturnStatement;

import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.transfer.ExpEval;
import problem.VerificationProblem;
import problem.VerificationProblem.Type;

/**
 * Extract a definition and its dependencies from the statement.
 */
public class DefinitionVisitor implements NodeVisitor {
	
	private ExpEval evaluator;
	private VerificationProblem.Type type;
	private String definition;
	private Set<Address> addresses;
	private Change change;
	private Map<String, Change> dependencies;

	/**
	 * @param node The return value or the RHS of an assignment.
	 * @return the data dependencies
	 */
	public static Definition getDependencies(ExpEval evaluator, AstNode node) {
		DefinitionVisitor visitor = new DefinitionVisitor(evaluator);
		node.visit(visitor);
		return visitor.new Definition(visitor.type, visitor.definition, 
				visitor.change, visitor.addresses, visitor.dependencies);
	}
	
	public DefinitionVisitor(ExpEval evaluator) {
		this.type = Type.UNKNOWN;
		this.evaluator = evaluator;
	}

	@Override
	public boolean visit(AstNode node) {

		switch(node.getType()) {
		case Token.RETURN:
			ReturnStatement ret = (ReturnStatement) node;
			type = VerificationProblem.Type.RETURN;
			definition = "retval";
			change = evaluator.resolveReturn().change;
			dependencies = DependencyVisitor.getDependencies(evaluator, ret.getReturnValue());
			return false;
		case Token.ASSIGN:
			Assignment assignment = (Assignment)node;
			type = VerificationProblem.Type.ASSIGN;
			definition = assignment.getLeft().toSource();
			change = evaluator.eval(assignment.getLeft()).change;
			addresses = evaluator.resolveOrCreate(assignment.getLeft());
			dependencies = DependencyVisitor.getDependencies(evaluator, assignment.getRight());
			return false;
		}

		return true;

	}
	
	public class Definition {

		public VerificationProblem.Type type;
		public String definition;
		public Change change;
		public Set<Address> addresses;
		public Map<String, Change> dependencies;

		public Definition(VerificationProblem.Type type, String definition, Change change, 
				Set<Address> addresses,
				Map<String, Change> dependencies) {
			this.type = type;
			this.definition = definition;
			this.change = change;
			this.addresses = addresses;
			this.dependencies = dependencies;
		}

		/**
		 * @return {@code true} if this contains a (non-null) definition.
		 */
		public boolean hasDef() {
			return type != Type.UNKNOWN;
		}

	}

}
