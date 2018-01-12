package multidiffplus.jsanalysis.abstractdomain;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;

/**
 * Stores the state for the change type abstract domain.
 * Lattice:
 * 			TOP
 * 		   /   \
 * 		  C	   U
 * 		   \   /
 * 			BOT
 * Where BOT is 0, C means the lattice element was changed (inserted or
 * removed), U means the lattice element was unchanged, TOP means the
 * lattice element was both changed and unchanged on paths that include
 * the current state.
 */
public class Change {

	public LatticeElement le;

	public Change(LatticeElement le) {
		this.le = le;
	}

	/**
	 * Joins this change with another change.
	 * @param state The change to join with.
	 * @return A new change that is the join of two changes.
	 */
	public Change join(Change state) {

		LatticeElement l = this.le;
		LatticeElement r = state.le;

		if(l == r) return new Change(l);
		if(l == LatticeElement.BOTTOM) return new Change(r);
		if(r == LatticeElement.BOTTOM) return new Change(l);

		return new Change(LatticeElement.TOP);

	}
	
	/**
	 * @return {@code true} if TOP or CHANGED
	 */
	public boolean isChanged() {
		if(le == Change.LatticeElement.TOP) return true;
		if(le == Change.LatticeElement.CHANGED) return true;
		return false;
	}

	/**
	 * @return the top lattice element
	 */
	public static Change top() {
		return new Change(LatticeElement.TOP);
	}

	/**
	 * @return the bottom lattice element
	 */
	public static Change bottom() {
		return new Change(LatticeElement.BOTTOM);
	}

	/**
	 * @return The "unchanged" lattice element.
	 */
	public static Change u(){
		return new Change(LatticeElement.UNCHANGED);
	}

	/**
	 * @return The "changed" lattice element.
	 */
	public static Change c() {
		return new Change(LatticeElement.CHANGED);
	}

	/**
	 * Converts a change type to a change lattice element. Updated nodes are
	 * considered unchanged.
	 * @param node The change type will be extracted from node.
	 * @return The change lattice element for the node.
	 */
	public static Change conv(ClassifiedASTNode node) {
		ChangeType ct = node.getChangeType();
		switch(ct) {
		case INSERTED:
		case REMOVED: return c();
		default: return u();
		}
	}

	/**
	 * Converts a change type to a change lattice element. Updated nodes are
	 * considered changed.
	 * @param node The change type will be extracted from node.
	 * @return The change lattice element for the node.
	 */
	public static Change convU(ClassifiedASTNode node) {
		ChangeType ct = node.getChangeType();
		switch(ct) {
		case INSERTED:
		case REMOVED:
		case UPDATED: return c();
		default: return u();
		}
	}

	/** The lattice elements for the abstract domain. **/
	public enum LatticeElement {
		TOP,
		CHANGED,
		UNCHANGED,
		BOTTOM
	}

	@Override
	public String toString() {
		return "Change:" + this.le.toString();
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Change)) return false;
		Change change = (Change)o;
		if(this.le == change.le) return true;
		return false;
	}

}