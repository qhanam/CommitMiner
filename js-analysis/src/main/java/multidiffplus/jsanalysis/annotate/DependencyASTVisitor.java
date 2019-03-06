package multidiffplus.jsanalysis.annotate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.facts.Annotation;
import multidiffplus.facts.AnnotationFactBase;

/**
 * A visitor for extracting criterion/dependency annotations from an
 * {@code AstRoot} (script).
 */
public class DependencyASTVisitor implements NodeVisitor {

    /**
     * The set of changed variable annotations found in the statement.
     */
    public Set<Annotation> annotations;

    /**
     * Registers all criterion/dependency labels on AST nodes as code annotations.
     */
    public static void registerAnnotations(AstRoot script, AnnotationFactBase factBase) {
	DependencyASTVisitor visitor = new DependencyASTVisitor();
	script.visit(visitor);
	factBase.registerAnnotationFacts(visitor.annotations);
    }

    public DependencyASTVisitor() {
	this.annotations = new HashSet<Annotation>();
    }

    @Override
    public boolean visit(AstNode node) {

	Map<String, Integer> criteria = node.getCriteria();
	for (Map.Entry<String, Integer> entry : criteria.entrySet()) {
	    this.annotations.add(new Annotation(entry.getKey() + "_CRIT",
		    Collections.singleton(entry.getValue()), node.getLineno(),
		    node.getAbsolutePosition(), node.getLength()));
	}

	Map<String, Set<Integer>> dependencies = node.getDependencies();
	for (Map.Entry<String, Set<Integer>> entry : dependencies.entrySet()) {
	    this.annotations.add(new Annotation(entry.getKey() + "_DEP", entry.getValue(),
		    node.getLineno(), node.getAbsolutePosition(), node.getLength()));
	}

	return true;

    }

}