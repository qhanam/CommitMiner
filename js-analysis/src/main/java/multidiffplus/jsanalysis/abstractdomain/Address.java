package multidiffplus.jsanalysis.abstractdomain;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * The abstract domain for an address.
 */
public class Address {

	/** A unique address for builtins. **/
	private static BigInteger builtinAddress = BigInteger.valueOf(-1);

	/** The hash-able address. **/
	public BigInteger addr;

	/** The variable or property being declared. */
	public String prop;

	/**
	 * @param bigInteger The abstract address.
	 */
	public Address(BigInteger bigInteger, String prop) {
		this.addr = bigInteger;
		this.prop = prop;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Address) {
			Address a = (Address)o;
			if(this.prop.equals(a.prop) && this.addr.equals(a.addr)) return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.addr.hashCode();
	}

	/**
	 * @param address The address lattice element to inject.
	 * @return The base value tuple with injected address.
	 */
	public static BValue inject(Address address, Change valChange, DefinerIDs definerIDs) {
		Set<Address> addresses = new HashSet<Address>();
		addresses.add(address);
		return new BValue(
				Str.bottom(),
				Num.bottom(),
				Bool.bottom(),
				Null.bottom(),
				Undefined.bottom(),
				new Addresses(addresses),
				valChange,
				definerIDs);
	}

	/**
	 * Builtin abstract addresses get negative values Same as in JSAI.
	 */
	public static synchronized Address createBuiltinAddr() {
		Address address = new Address(builtinAddress, "");
		builtinAddress = builtinAddress.subtract(BigInteger.valueOf(1));
		return address;
	}

	/**
	 * Builtin abstract addresses get negative values Same as in JSAI.
	 */
	public static synchronized Address createBuiltinAddr(String prop) {
		Address address = new Address(builtinAddress, prop);
		builtinAddress = builtinAddress.subtract(BigInteger.valueOf(1));
		return address;
	}

	@Override
	public String toString() {
		return this.addr.toString() + "." + prop;
	}

}