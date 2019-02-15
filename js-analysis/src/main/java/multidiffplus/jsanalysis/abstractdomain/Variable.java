package multidiffplus.jsanalysis.abstractdomain;

import java.util.ArrayList;
import java.util.List;

import multidiffplus.commit.DependencyIdentifier;

/**
 * An object property identifier combined with a change lattice.
 */
public class Variable implements DependencyIdentifier {

    public Integer definerID;
    public String name;
    public Change change;
    public Addresses addresses;

    /**
     * Use for standard lookup operations when the change type does not matter.
     * 
     * @param name
     *            The name of the identifier to inject.
     */
    public Variable(Integer definerID, String name, Addresses addresses) {
	this.definerID = definerID;
	this.name = name;
	this.change = Change.bottom();
	this.addresses = addresses;
    }

    /**
     * Use for standard lookup operations when the change type does not matter.
     * 
     * @param name
     *            The name of the identifier to inject.
     * @param change
     *            How the identifier was changed.
     */
    public Variable(Integer definerID, String name, Change change, Addresses addresses) {
	this.definerID = definerID;
	this.name = name;
	this.change = change;
	this.addresses = addresses;
    }

    /**
     * Joins the given Identifier with this Identifier.
     */
    public Variable join(Variable id) {

	if (!definerID.equals(id.definerID) || !name.equals(id.name)) {
	    if (!name.equalsIgnoreCase("~retval~"))
		System.err.println("Variable::join -- WARNING -- joining different identifiers.");
	    // throw new Error("Identifier::join() -- Cannot join different Identifiers.");
	}

	change.join(id.change);
	addresses.join(id.addresses);

	return new Variable(definerID, name, change.join(id.change), addresses.join(id.addresses));
    }

    @Override
    public String toString() {
	return "<" + this.name + "," + this.change + "," + this.addresses + ">";
    }

    @Override
    public int hashCode() {
	return this.name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
	if (this == o)
	    return true;
	if (!(o instanceof Variable))
	    return false;
	Variable right = (Variable) o;
	return this.name.equals(right.name);
    }

    @Override
    public String getAddress() {
	if (definerID == -1)
	    return "";
	return definerID.toString();
    }

    @Override
    public List<Integer> getAddresses() {
	List<Integer> addresses = new ArrayList<Integer>();
	if (definerID != -1)
	    addresses.add(definerID);
	return addresses;
    }

}