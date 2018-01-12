package multidiffplus.jsdiff.env;

import java.util.Set;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.Annotation;
import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.jsanalysis.abstractdomain.State;

/**
 * Extracts facts from a flow analysis.
 */
public class EnvCFGVisitor implements ICFGVisitor {

	/* The fact database we will populate. */
	private AnnotationFactBase factBase;

	public EnvCFGVisitor(AnnotationFactBase factBase) {
		this.factBase = factBase;
	}

	@Override
	public void visit(CFGNode node) {
		visit((AstNode) node.getStatement(), (State)node.getAfterState());
	}

	@Override
	public void visit(CFGEdge edge) {
		visit((AstNode) edge.getCondition(), (State)edge.getBeforeState());
	}

	/**
	 * Visit an AstNode (a statement or condition) and extract facts about
	 * identifier protection.
	 */
	private void visit(AstNode node, State state) {
		if(state != null) {
			Set<Annotation> annotations = EnvASTVisitor.getAnnotations(state.env, node);
			factBase.registerAnnotationFacts(annotations);
		}
	}

}