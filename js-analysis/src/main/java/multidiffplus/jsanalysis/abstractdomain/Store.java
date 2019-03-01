package multidiffplus.jsanalysis.abstractdomain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mozilla.javascript.ast.AstNode;

/**
 * The abstract domain for the program's store (memory). i.e. Store# := Address#
 * -> P(BValue# + Object#)
 */
public class Store {

    /** The store for {@code BValue}s. **/
    private Map<Address, BValue> bValueStore;

    /** The store for {@code Object}s. **/
    private Map<Address, Obj> objectStore;

    /**
     * Create the initial state for the store. The initial state is an empty store.
     * However, the environment may initialize the store with the following objects
     * before the analysis begins: (1) variables declared in the function and raised
     * - which will initially point to the primitive value 'undefined'. (2)
     * functions declared in the function and raised - which will point to their
     * function object.
     *
     * Changes to the store are driven by changes to the environment.
     */
    public Store() {
	/* Right now we only have an empty store. */
	this.bValueStore = new HashMap<Address, BValue>();
    }

    /**
     * Create a new store from an existing store.
     * 
     * @param bValueStore
     * @param objectStore
     */
    public Store(Map<Address, BValue> bValueStore, Map<Address, Obj> objectStore) {
	this.bValueStore = bValueStore;
	this.objectStore = objectStore;
    }

    @Override
    public Store clone() {
	return new Store(new HashMap<Address, BValue>(bValueStore),
		new HashMap<Address, Obj>(objectStore));
    }

    /**
     * Computes σ1 u σ2.
     * 
     * @return a new Store which is this Store joined with the store parameter.
     */
    public Store join(Store store) {

	/*
	 * Just copy the values into a new hash map. The values are essentially
	 * immutable since any transfer or join will produce a new value.
	 */

	Map<Address, BValue> bValueStore = new HashMap<Address, BValue>(this.bValueStore);
	Map<Address, Obj> objectStore = new HashMap<Address, Obj>(this.objectStore);

	/*
	 * Join the new abstract domain with the new map. New lattice elements are
	 * created for each join.
	 */

	for (Map.Entry<Address, BValue> entries : store.bValueStore.entrySet()) {
	    Address address = entries.getKey();
	    BValue right = entries.getValue();
	    BValue left = bValueStore.get(entries.getKey());

	    if (left == null)
		bValueStore.put(address, right);
	    else
		bValueStore.put(address, left.join(right));
	}

	for (Map.Entry<Address, Obj> entries : store.objectStore.entrySet()) {
	    Obj right = entries.getValue();
	    Obj left = objectStore.get(entries.getKey());

	    if (left == null)
		objectStore.put(entries.getKey(), right);
	    else
		objectStore.put(entries.getKey(), left.join(right, bValueStore));
	}

	return new Store(bValueStore, objectStore);

    }

    /**
     * Allocates a primitive value on the store. If the address already has a value,
     * performs a weak update.
     * 
     * @param address
     *            The address to allocate, generated in {@code Trace}.
     * @param value
     *            The value to place in the store.
     * @return The Store after allocation.
     */
    public Store alloc(Address address, BValue value, AstNode varOrFieldName) {
	Map<Address, BValue> bValueStore = new HashMap<Address, BValue>(this.bValueStore);
	BValue left = bValueStore.get(address);
	if (left == null) {
	    bValueStore.put(address, value);
	} else {
	    bValueStore.put(address, left.join(value));
	}

	// Update the value dependencies.
	for (Criterion crit : value.deps.getDependencies()) {
	    varOrFieldName.addDependency(crit.getType().toString(), crit.getId());
	}
	for (Criterion crit : value.change.getDependencies().getDependencies()) {
	    varOrFieldName.addDependency(crit.getType().toString(), crit.getId());
	}

	return new Store(bValueStore, this.objectStore);
    }

    /**
     * Allocates an object on the store. If the address already has a value,
     * performs a weak update.
     * 
     * @param address
     *            The address to allocate, generated in {@code Trace}.
     * @param object
     *            The object to place in the store.
     * @return The Store after allocation.
     */
    public Store alloc(Address address, Obj object) {
	Map<Address, BValue> bValueStore = new HashMap<Address, BValue>(this.bValueStore);
	Map<Address, Obj> objectStore = new HashMap<Address, Obj>(this.objectStore);
	Obj left = objectStore.get(address);
	if (left == null)
	    objectStore.put(address, object);
	else
	    objectStore.put(address, left.join(object, bValueStore));
	return new Store(bValueStore, objectStore);
    }

    /**
     * Replaces a value using a strong update.
     * 
     * @return The Store after the strong update.
     */
    public Store strongUpdate(Address addr, BValue val, AstNode varOrFieldName) {
	Map<Address, BValue> bValueStore = new HashMap<Address, BValue>(this.bValueStore);
	bValueStore.put(addr, val);

	// Update the value dependencies.
	for (Criterion crit : val.deps.getDependencies()) {
	    varOrFieldName.addDependency(crit.getType().toString(), crit.getId());
	}
	for (Criterion crit : val.change.getDependencies().getDependencies()) {
	    varOrFieldName.addDependency(crit.getType().toString(), crit.getId());
	}

	return new Store(bValueStore, this.objectStore);
    }

    /**
     * Replaces an object using a strong update.
     * 
     * @return The Store after the strong update.
     */
    public Store strongUpdate(Address addr, Obj obj) {
	Map<Address, Obj> objectStore = new HashMap<Address, Obj>(this.objectStore);
	objectStore.put(addr, obj);
	return new Store(this.bValueStore, objectStore);
    }

    /**
     * @param address
     *            The address of the base value to retrieve.
     * @param varOrFieldName
     *            the variable or field being accessed.
     * @return The BValue at the address or null if the BValue does not exist.
     */
    public BValue apply(Address address, AstNode varOrFieldName) {
	BValue val = this.bValueStore.get(address);

	// Update the value dependencies.
	for (Criterion crit : val.deps.getDependencies()) {
	    varOrFieldName.addDependency(crit.getType().toString(), crit.getId());
	}
	for (Criterion crit : val.change.getDependencies().getDependencies()) {
	    varOrFieldName.addDependency(crit.getType().toString(), crit.getId());
	}

	return val;
    }

    /**
     * @param addresses
     *            The possible addresses of the base value to retrieve.
     * @return The join of all BValues or null if no BValue exists.
     */
    public BValue apply(Addresses addresses) {
	BValue bvalue = null;
	for (Address address : addresses.addresses) {
	    if (bvalue == null)
		bvalue = this.bValueStore.get(address);
	    else if (this.bValueStore.containsKey(address))
		bvalue = bvalue.join(this.bValueStore.get(address));
	}
	return bvalue;
    }

    /**
     * @param address
     *            The address of the object to retrieve.
     * @return The object at the address or null if the object does not exist.
     */
    public Obj getObj(Address address) {
	return this.objectStore.get(address);
    }

    @Override
    public String toString() {
	String str = "-BValues-\n";
	for (Entry<Address, BValue> entry : this.bValueStore.entrySet()) {
	    str += entry.getKey().toString() + ": " + entry.getValue().toString() + "\n";
	}
	str += "-Objects-\n";
	for (Entry<Address, Obj> entry : this.objectStore.entrySet()) {
	    str += entry.getKey().toString() + ": " + entry.getValue().toString() + "\n";
	}
	return str;
    }

}