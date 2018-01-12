package multidiffplus.jsdiff.controlcondition;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.commit.Annotation;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.GenericDependencyIdentifier;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.State;

public class ControlConditionASTVisitor implements NodeVisitor {

	/**
	 * The set of changed variable annotations found in the statement.
	 **/
	public Set<Annotation> annotations;
	
	/** The abstract state. **/
	@SuppressWarnings("unused")
	private State state;

	/**
	 * Detects defs of changes to branch conditions.
	 * @return the set of nodes that are impacted by a branch condition change 
	 */
	public static Set<Annotation> getDefAnnotations(State state, AstNode statement) {
		ControlConditionASTVisitor visitor = new ControlConditionASTVisitor(state);
		
		if(statement instanceof AstRoot) {
			/* This is the root. Nothing should be flagged. */
			return visitor.annotations;
		}
		else if(statement instanceof FunctionNode) {
			/* This is a function declaration. Nothing should be flagged. */
			return visitor.annotations;
		}
		else if(statement != null){

			/* Register CON-USE annotations for statements at depth 1 from an
			 * inserted or updated branch condition. */
			if(state.control.getCondition().isChanged() && statement.getLineno() > 0) {
				List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
				ids.add(state.control.getCondition());
				visitor.annotations.add(new Annotation("CON-USE", ids, statement.getLineno(), statement.getFixedPosition(), statement.getLength()));
			}

		}
		return visitor.annotations;
	}

	/**
	 * Detects uses of changes to branch conditions.
	 * @return the set of nodes that are impacted by a branch condition change 
	 */
	public static Set<Annotation> getUseAnnotations(State state, AstNode statement) {

		ControlConditionASTVisitor visitor = new ControlConditionASTVisitor(state);
		if(statement == null) return visitor.annotations;
		statement.visit(visitor);
		return visitor.annotations;

	}

	public ControlConditionASTVisitor(State state) {
		this.annotations = new HashSet<Annotation>();
		this.state = state;
	}

	@Override
	public boolean visit(AstNode node) {

		/* Register CON-DEF annotations for branch conditions. */
		if(Change.convU(node).le == Change.LatticeElement.CHANGED && node.getID() != null) {
			List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
			ids.add(new GenericDependencyIdentifier(node.getID()));
			if(node.getLength() > 0) // Don't register def for the negated condition
				annotations.add(new Annotation("CON-DEF", ids, node.getLineno(), node.getFixedPosition(), node.getLength()));
		}

		return false;

	}

}