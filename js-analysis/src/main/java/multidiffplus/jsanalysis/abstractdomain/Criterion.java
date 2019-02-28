package multidiffplus.jsanalysis.abstractdomain;

import org.mozilla.javascript.ast.AstNode;

/**
 * The criterion for a criterion/dependency relation.
 */
public class Criterion {

    public enum Type {
	VARIABLE, VALUE, VALUE_CHANGE, VARIABLE_CHANGE, CALL_CHANGE, CONDITION_CHANGE
    }

    // The type of relation.
    private Type type;

    // The identifier for the criterion.
    private Integer id;

    private Criterion(Type type, Integer id) {
	this.type = type;
	this.id = id;
    }

    public Type getType() {
	return type;
    }

    public Integer getId() {
	return id;
    }

    /**
     * Creates a new Criterion, uniquely identified by its type and id.
     */
    public static Criterion of(AstNode node, Type type) {
	node.addCriterion(type.name(), node.getID());
	return new Criterion(type, node.getID());
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
