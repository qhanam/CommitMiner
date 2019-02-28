package multidiffplus.jsdiff.annotations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.NodeVisitor;

import multidiffplus.facts.Annotation;

public class ValueASTVisitor implements NodeVisitor {

    /**
     * The set of changed variable annotations found in the statement.
     */
    public Set<Annotation> annotations;

    /**
     * Detects uses of the identifier.
     * 
     * @return the set of nodes where the identifier is used.
     */
    public static Set<Annotation> getAnnotations(AstNode statement) {
	ValueASTVisitor visitor = new ValueASTVisitor();

	if (statement instanceof AstRoot) {
	    // We only need to visit the AST once,
	    statement.visit(visitor);
	    return visitor.annotations;
	}

	return visitor.annotations;
    }

    public ValueASTVisitor() {
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