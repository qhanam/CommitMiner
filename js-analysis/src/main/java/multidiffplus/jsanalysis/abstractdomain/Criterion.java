package multidiffplus.jsanalysis.abstractdomain;

import org.mozilla.javascript.ast.AstNode;

/**
 * The criterion for a criterion/dependency relation.
 */
public class Criterion {

    public enum Type {
	VARIABLE, /**/
	VALUE, /**/
	VALUE_CHANGE, /**/
	VARIABLE_CHANGE, /**/
	CALL_CHANGE, /**/
	CONDITION_CHANGE, /**/
	SYNC_ERROR, /**/
	ASYNC_ERROR_CALL_SITE, /**/
	ASYNC_ERROR_EPARAM, /**/
	ASYNC_ERROR_VPARAM, /**/
	MUTABLE_SYNC_ERROR_API, /**/
	MUTABLE_SYNC_ERROR_FUNCTION, /**/
    }

    // The type of relation.
    private Type type;

    // The identifier for the criterion.
    private Integer id;

    // The AstNode
    private AstNode node;

    private Criterion(Type type, AstNode node) {
	this.type = type;
	this.id = node.getID();
	this.node = node;
    }

    public Type getType() {
	return type;
    }

    public Integer getId() {
	return id;
    }

    public AstNode getNode() {
	return node;
    }

    /**
     * Creates a new Criterion, uniquely identified by its type and id.
     */
    public static Criterion of(AstNode node, Type type) {
	node.addCriterion(type.name(), node.getID());
	return new Criterion(type, node);
    }

    @Override
    public int hashCode() {
	return id;
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof Criterion))
	    return false;
	Criterion that = (Criterion) o;
	return this.type == that.type && this.id == that.id;
    }

    @Override
    public String toString() {
	return this.type.name() + "_" + id.toString();
    }

}
