package multidiffplus.jsdiff.value;

import java.util.Set;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.jsanalysis.abstractdomain.State;

/**
 * Extracts facts from a flow analysis.
 */
public class ValueCFGVisitor implements ICFGVisitor {

    /* The fact database we will populate. */
    private AnnotationFactBase factBase;

    public ValueCFGVisitor(AnnotationFactBase factBase) {
	this.factBase = factBase;
    }

    @Override
    public void visit(CFGNode node) {
	visit((AstNode) node.getStatement(), (State) node.getAfterState());
    }

    @Override
    public void visit(CFGEdge edge) {
	visit((AstNode) edge.getCondition(), (State) edge.getAfterState());
    }

    /**
     * Visit an AstNode (a statement or condition) and extract facts about value
     * changes.
     */
    private void visit(AstNode node, State state) {
	if (state != null) {
	    Set<Annotation> annotations = ValueASTVisitor.getAnnotations(state, node);
	    factBase.registerAnnotationFacts(annotations);
	}
    }

}