package multidiffplus.jsanalysis.abstractdomain;

/**
 * An object property identifier combined with a change lattice.
 */
public class Variable {

    public Dependencies deps;
    public String name;
    public Change change;
    public Addresses addresses;

    private Variable(String name, Addresses addresses, Change change, Dependencies deps) {
	this.name = name;
	this.addresses = addresses;
	this.change = change;
	this.deps = deps;
    }

    /**
     * Joins the given Identifier with this Identifier.
     */
    public Variable join(Variable that) {
	return new Variable(this.name, this.addresses.join(that.addresses),
		this.change.join(that.change), this.deps.join(that.deps));
    }

    /**
     * @param name
     *            The name of the identifier to inject.
     * @param address
     *            The address of the value pointed to by the identifier in the
     *            abstract store.
     * @param change
     *            How the identifier was changed.
     * @param deps
     *            The location where the identifier was defined.
     */
    public static Variable inject(String name, Address address, Change change, Dependencies deps) {
	return new Variable(name, new Addresses(address), change, deps);
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

}