package multidiffplus.jsanalysis.abstractdomain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;

/**
 * Dependencies of AST subtrees.
 * 
 * In the case of change impact, the AST subtrees have associated edit
 * operations, and these dependencies reflect the changes to the program state
 * that result from those edit operations.
 * 
 * In the case of data dependencies, the AST subtrees create new values when
 * executed (ie. literals like number or strings, or expressions like unary or
 * binary operators), and these dependencies reflect uses of those values.
 * 
 * In the case of variable dependencies, the AST subtrees declare new variables,
 * and these dependencies reflect uses of those variables.
 */
public class Dependencies {

    private enum Type {
	VARIABLE, VALUE, VALUE_CHANGE, VARIABLE_CHANGE, CALL_CHANGE, CONDITION_CHANGE
    }

    private Set<Integer> deps;

    private Dependencies() {
	this.deps = new HashSet<Integer>();
    }

    private Dependencies(Integer dep) {
	this.deps = Collections.singleton(dep);
    }

    private Dependencies(Set<Integer> deps) {
	this.deps = new HashSet<Integer>(deps);
    }

    /**
     * Returns the dependencies.
     */
    public Collection<Integer> getDependencies() {
	return this.deps;
    }

    /**
     * Joins this dependency set with the given dependency set.
     */
    public Dependencies join(Dependencies depsToJoin) {
	Dependencies joined = new Dependencies();
	joined.deps.addAll(this.deps);
	joined.deps.addAll(depsToJoin.deps);
	return joined;
    }

    /**
     * Returns {@code true} if there are no dependencies.
     */
    public boolean isEmpty() {
	return deps.isEmpty();
    }

    /**
     * Returns an empty set of dependencies.
     */
    public static Dependencies bot() {
	return new Dependencies();
    }

    /**
     * Creates a new value change criterion and returns a dependency for the
     * criterion.
     */
    public static Dependencies injectValue(AstNode node) {
	return inject(node, Type.VALUE);
    }

    /**
     * Creates a new value criterion and returns a dependency for the criterion.
     */
    public static Dependencies injectValueChange(AstNode node) {
	return inject(node, Type.VALUE_CHANGE);
    }

    /**
     * Creates a new condition change criterion and returns a dependency for the
     * criterion.
     */
    public static Dependencies injectConditionChange(AstNode node) {
	return inject(node, Type.CONDITION_CHANGE);
    }

    /**
     * Creates a new call change criterion and returns a dependency for the
     * criterion.
     */
    public static Dependencies injectCallChange(AstNode node) {
	return inject(node, Type.CALL_CHANGE);
    }

    /**
     * Creates a new criterion and returns a dependency for the criterion.
     */
    private static Dependencies inject(AstNode node, Type type) {
	node.addCriterion(type.name(), node.getID());
	return new Dependencies(node.getID());
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof Dependencies))
	    return false;
	Dependencies that = (Dependencies) o;
	return this.deps.containsAll(that.deps) && that.deps.containsAll(this.deps);
    }

    @Override
    public String toString() {
	String s = "{";
	for (Integer dep : deps) {
	    s += dep.toString() + ",";
	}
	return s.substring(0, s.length() - 2) + "}";
    }

}
