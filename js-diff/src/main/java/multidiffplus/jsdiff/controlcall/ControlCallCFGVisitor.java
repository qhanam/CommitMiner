package multidiffplus.jsdiff.controlcall;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.jsanalysis.abstractdomain.State;

/**
 * Extracts facts from a flow analysis.
 */
public class ControlCallCFGVisitor implements ICFGVisitor {

	/* The fact database we will populate. */
	private AnnotationFactBase factBase;

	public ControlCallCFGVisitor(AnnotationFactBase factBase) {
		this.factBase = factBase;
	}

	@Override
	public void visit(CFGNode node) {
		visit((AstNode) node.getStatement(), (State)node.getBeforeState());
	}

	@Override
	public void visit(CFGEdge edge) {
		visit((AstNode) edge.getCondition(), (State)edge.getBeforeState());
	}

	/**
	 * Visit an AstNode (a statement or condition) and extract facts about
	 * control-call changes.
	 */
	private void visit(AstNode node, State state) {
		if(state != null && node instanceof FunctionNode) {
			/* Register CALL-USE annotations for statements at depth 1 from an
			 * inserted or updated callsite. */
			if(state.control.getCall().isChanged() && node.isStatement() && node.getLineno() > 0) {
				List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
				ids.add(state.control.getCall());
				factBase.registerAnnotationFact(new Annotation("CALL-USE", ids, node.getLineno(), node.getFixedPosition(), node.getLength()));
			}
		}
		if(state != null) {
			Set<Annotation> annotations = ControlCallASTVisitor.getAnnotations(state, node);
			factBase.registerAnnotationFacts(annotations);
		}
	}

}