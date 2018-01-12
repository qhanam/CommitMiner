package multidiffplus.analysis;

/**
 * Contains analysis options.
 */
public class Options {

	private static Options instance;

	private SymEx useSymEx;

	private Options() {
		this.useSymEx = SymEx.OFF;
	}

	private Options(SymEx useSymEx) {
		this.useSymEx = useSymEx;
	}

	/**
	 * @return {@code true} if symbolic execution should be used to check for
	 * semantic equivalence during change impact analysis.
	 */
	public boolean useSymEx() {
		return useSymEx.equals(SymEx.ON);
	}

	/**
	 * @return the singleton {@code Options}.
	 */
	public static Options getInstance() {
		if(instance == null) instance = new Options();
		return instance;
	}

	/**
	 * @return the singleton {@code Options}.
	 */
	public static Options createInstance(SymEx symex) {
		if(instance == null) instance = new Options(symex);
		return instance;
	}
	
	/**
	 * Controls the use of symbolic execution to eliminate false positives.
	 */
	public enum SymEx {
		/** Use symbolic execution. **/
		ON,
		/** Do not use symbolic execution. **/
		OFF
	}
	
}