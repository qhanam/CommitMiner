package multidiffplus.jsanalysis.abstractdomain;

/** JavaScript object classes **/
public enum JSClass {

	/* User instantiated object classes. */
	CObject(0),					// new Object | {}
	CFunction(1),				// function() {...}
	CArguments(3),

	/* Special classes that only exist in the init state. */
	CObject_Obj(2),							// Object
	CObject_prototype_Obj(3),		// Object.prototype
	CFunction_Obj(4),						// Function
	CFunction_prototype_Obj(5);	// Function.prototype

	private int value;

	private JSClass(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

}