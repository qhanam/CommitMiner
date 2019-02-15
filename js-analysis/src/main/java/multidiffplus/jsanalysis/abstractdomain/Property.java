package multidiffplus.jsanalysis.abstractdomain;

import java.util.ArrayList;
import java.util.List;

import multidiffplus.commit.DependencyIdentifier;

/**
 * A variable identifier combined with a change lattice.
 */
public class Property implements DependencyIdentifier {

    public Integer definerID;
    public String name;
    public Address address;

    /**
     * Use for standard lookup operations when the change type does not matter.
     * 
     * @param name
     *            The name of the identifier to inject.
     */
    public Property(Integer definerID, String name, Address address) {
	this.definerID = definerID;
	this.name = name;
	this.address = address;
    }

    /**
     * Joins the given Identifier with this Identifier.
     */
    public Property join(Property id) {

	if (!definerID.equals(id.definerID) || !name.equals(id.name))
	    throw new Error("Identifier::join() -- Cannot join different Identifiers.");

	if (address.equals(id.address))
	    throw new Error("Property::join -- Cannot join two addresses that do not match.");

	return new Property(definerID, name, address);

    }

    @Override
    public int hashCode() {
	return this.name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
	if (this == o)
	    return true;
	if (!(o instanceof Property))
	    return false;
	Property right = (Property) o;
	return this.name.equals(right.name);
    }

    @Override
    public String getAddress() {
	return definerID.toString();
    }

    @Override
    public List<Integer> getAddresses() {
	List<Integer> addresses = new ArrayList<Integer>();
	addresses.add(definerID);
	return addresses;
    }

}