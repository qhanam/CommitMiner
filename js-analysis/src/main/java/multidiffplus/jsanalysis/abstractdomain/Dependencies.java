package multidiffplus.jsanalysis.abstractdomain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.jsanalysis.abstractdomain.Criterion.Type;

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

    /** The set of criteria on which the parent object depends. */
    private Set<Criterion> deps;

    private Dependencies() {
	this.deps = new HashSet<Criterion>();
    }

    private Dependencies(Criterion crit) {
	this.deps = Collections.singleton(crit);
    }

    private Dependencies(Set<Criterion> deps) {
	this.deps = new HashSet<Criterion>(deps);
    }

    /**
     * Returns the dependencies.
     */
    public Collection<Criterion> getDependencies() {
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
     * Creates a new variable change criterion and returns a dependency for the
     * criterion.
     */
    public static Dependencies injectVariable(AstNode node) {
	return inject(Criterion.of(node, Criterion.Type.VARIABLE));
    }

    /**
     * Creates a new variable criterion and returns a dependency for the criterion.
     */
    public static Dependencies injectVariableChange(AstNode node) {
	return Change.testU(node) ? inject(Criterion.of(node, Type.VARIABLE_CHANGE))
		: Dependencies.bot();
    }

    /**
     * Creates a new value change criterion and returns a dependency for the
     * criterion.
     */
    public static Dependencies injectValue(AstNode node) {
	return inject(Criterion.of(node, Type.VALUE));
    }

    /**
     * Creates a new value criterion and returns a dependency for the criterion.
     */
    public static Dependencies injectValueChange(AstNode node) {
	return Change.testU(node) ? inject(Criterion.of(node, Type.VALUE_CHANGE))
		: Dependencies.bot();
    }

    /**
     * Creates a new condition change criterion and returns a dependency for the
     * criterion.
     */
    public static Dependencies injectConditionChange(AstNode node) {
	return inject(Criterion.of(node, Type.CONDITION_CHANGE));
    }

    /**
     * Creates a new call change criterion and returns a dependency for the
     * criterion.
     */
    public static Dependencies injectCallChange(AstNode node) {
	return inject(Criterion.of(node, Type.CALL_CHANGE));
    }

    /**
     * Creates a new criterion and returns a dependency for the criterion.
     */
    private static Dependencies inject(Criterion crit) {
	return new Dependencies(crit);
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
	for (Criterion crit : deps) {
	    s += crit.toString() + ",";
	}
	return s.substring(0, s.length() - 1) + "}";
    }

}
