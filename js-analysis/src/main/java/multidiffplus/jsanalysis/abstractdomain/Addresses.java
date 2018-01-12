package multidiffplus.jsanalysis.abstractdomain;

import java.util.HashSet;
import java.util.Set;

/**
 * The abstract domain for the possible addresses pointed to by a BValue.
 */
public class Addresses {

	private static final int MAX_SIZE = 10;

	public LatticeElement le;

	/** Empty if LE = TOP. **/
	public Set<Address> addresses;

	/**
	 * Create the bottom lattice element.
	 */
	public Addresses(LatticeElement le) {
		this.addresses = new HashSet<Address>();
		this.le = le;
	}

	public Addresses(Set<Address> addresses) {
		this.addresses = addresses;
		this.le = LatticeElement.SET;
	}

	public Addresses(Address address) {
		this.addresses = new HashSet<Address>();
		this.addresses.add(address);
		this.le = LatticeElement.SET;
	}

	public Addresses(LatticeElement le, Set<Address> addresses) {
		this.addresses = addresses;
		this.le = le;
	}


	/**
	 * Performs a weak update on the set of addresses.
	 */
	public Addresses weakUpdate(Set<Address> addresses) {
		return this.join(new Addresses(addresses));
	}

	/**
	 * Performs a strong update on the set of addresses.
	 */
	public Addresses strongUpdate(Set<Address> addresses) {
		if(addresses.size() > MAX_SIZE)
			return new Addresses(LatticeElement.TOP);
		return new Addresses(addresses);
	}

	/**
	 * Joins this address with another address.
	 * @param a The address to join with.
	 * @return A new address that is the join of the two addresses.
	 */
	public Addresses join(Addresses a) {

		if(a.le == LatticeElement.BOTTOM)
			return new Addresses(this.le, new HashSet<Address>(this.addresses));
		if(this.le == LatticeElement.BOTTOM)
			return new Addresses(a.le, new HashSet<Address>(a.addresses));
		if(this.le == LatticeElement.TOP || a.le == LatticeElement.TOP)
			return new Addresses(LatticeElement.TOP);

		if(this.addresses.size() + a.addresses.size() > MAX_SIZE)
			return new Addresses(LatticeElement.TOP);

		/* Join the two address sets. */
		HashSet<Address> newAddressSet = new HashSet<Address>(this.addresses);
		newAddressSet.addAll(a.addresses);
		return new Addresses(LatticeElement.SET, newAddressSet);

	}

	/**
	 * @param addresses The address lattice element to inject.
	 * @return The base value tuple with injected addresses.
	 */
	public static BValue inject(Addresses addresses, Change valChange, DefinerIDs definerIDs) {
		return new BValue(
				Str.bottom(),
				Num.bottom(),
				Bool.bottom(),
				Null.bottom(),
				Undefined.bottom(),
				addresses,
				valChange,
				definerIDs);
	}

	public static BValue dummy(Change valChange, DefinerIDs definerIDs) {
		return new BValue(
				Str.top(),
				Num.top(),
				Bool.top(),
				Null.top(),
				Undefined.top(),
				Addresses.bottom(),
				valChange,
				definerIDs);
	}

	/**
	 * @return The top lattice element.
	 */
	public static Addresses top() {
		return new Addresses(LatticeElement.TOP);
	}

	/**
	 * @return The bottom lattice element.
	 */
	public static Addresses bottom() {
		return new Addresses(LatticeElement.BOTTOM);
	}

	/** The lattice elements for the abstract domain. **/
	public enum LatticeElement {
		TOP,	// May point to any address.
		SET,	// Some (limited?) combination of addresses.
		BOTTOM	// Does not point to an address (empty set)
	}

	@Override
	public String toString() {
		if(this.le != LatticeElement.SET) {
			return "Addr:" + this.le.toString();
		}
		else {
			String addrs = "Addrs: {";
			for(Address addr : this.addresses) {
				addrs += "Addr:" + addr + ",";
			}
			return addrs.substring(0, addrs.length() - 1) + "}";
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Addresses)) return false;
		Addresses addrs = (Addresses)o;
		if(this.addresses.size() != addrs.addresses.size()) return false;
		for(Address addr : this.addresses) {
			if(!addrs.addresses.contains(addr)) return false;
		}
		return true;
	}

}
