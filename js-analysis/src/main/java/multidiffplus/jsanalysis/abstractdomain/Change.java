package multidiffplus.jsanalysis.abstractdomain;

import org.mozilla.javascript.ast.AstNode;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;

/**
 * Stores the state for the change type abstract domain. Lattice: TOP / \ C U \
 * / BOT Where BOT is 0, C means the lattice element was changed (inserted or
 * removed), U means the lattice element was unchanged, TOP means the lattice
 * element was both changed and unchanged on paths that include the current
 * state.
 */
public class Change {

    public LatticeElement le;

    /**
     * The set of criteria that have caused the lattice to include the 'C' element.
     */
    private Dependencies deps;

    private Change(LatticeElement le, Dependencies deps) {
	this.le = le;
	this.deps = deps;
    }

    /**
     * Returns the list of criteria on which this change depends.
     */
    public Dependencies getDependencies() {
	return this.deps;
    }

    /**
     * Joins this change with another change.
     * 
     * @param state
     *            The change to join with.
     * @return A new change that is the join of two changes.
     */
    public Change join(Change state) {

	LatticeElement l = this.le;
	LatticeElement r = state.le;

	Dependencies deps = this.deps.join(state.deps);

	if (l == r)
	    return new Change(l, deps);
	if (l == LatticeElement.BOTTOM)
	    return new Change(r, deps);
	if (r == LatticeElement.BOTTOM)
	    return new Change(l, deps);

	return new Change(LatticeElement.TOP, deps);

    }

    /**
     * @return {@code true} if TOP or CHANGED
     */
    public boolean isChanged() {
	if (le == Change.LatticeElement.TOP)
	    return true;
	if (le == Change.LatticeElement.CHANGED)
	    return true;
	return false;
    }

    /**
     * @return the top lattice element
     */
    public static Change top(Dependencies deps) {
	return new Change(LatticeElement.TOP, deps);
    }

    /**
     * @return the bottom lattice element
     */
    public static Change bottom() {
	return new Change(LatticeElement.BOTTOM, Dependencies.bot());
    }

    /**
     * @return The "unchanged" lattice element.
     */
    public static Change u() {
	return new Change(LatticeElement.UNCHANGED, Dependencies.bot());
    }

    /**
     * @param deps
     *            The criterion (the AST node) IDs that caused this change.
     * @return The "changed" lattice element.
     */
    public static Change c(Dependencies deps) {
	return new Change(LatticeElement.CHANGED, deps);
    }

    /**
     * Converts a change type to a change lattice element. Updated nodes are
     * considered unchanged.
     * 
     * @param node
     *            The change type will be extracted from node.
     * @return The change lattice element for the node.
     */
    public static Change conv(AstNode node, Dependencies deps) {
	ChangeType ct = node.getChangeType();
	switch (ct) {
	case INSERTED:
	case REMOVED:
	    return c(deps);
	default:
	    return u();
	}
    }

    /**
     * Converts a change type to a change lattice element. Updated nodes are
     * considered changed.
     * 
     * @param node
     *            The change type will be extracted from node.
     * @return The change lattice element for the node.
     */
    public static Change convU(AstNode node, Dependencies deps) {
	ChangeType ct = node.getChangeType();
	switch (ct) {
	case INSERTED:
	case REMOVED:
	case UPDATED:
	    return c(deps);
	default:
	    return u();
	}
    }

    /** The lattice elements for the abstract domain. **/
    public enum LatticeElement {
	TOP, CHANGED, UNCHANGED, BOTTOM
    }

    /**
     * Returns {@code true} when the node was inserted or removed.
     */
    public static boolean test(AstNode node) {
	ChangeType ct = node.getChangeType();
	switch (ct) {
	case INSERTED:
	case REMOVED:
	    return true;
	default:
	    return false;
	}
    }

    /**
     * Returns {@code true} when the node was inserted, removed, or updated.
     */
    public static boolean testU(AstNode node) {
	ChangeType ct = node.getChangeType();
	switch (ct) {
	case INSERTED:
	case REMOVED:
	case UPDATED:
	    return true;
	default:
	    return false;
	}
    }

    @Override
    public String toString() {
	return "Change:" + this.le.toString();
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof Change))
	    return false;
	Change change = (Change) o;
	if (this.le == change.le && this.deps.equals(change.deps)) {
	    return true;
	}
	return false;
    }

}