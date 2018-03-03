package multidiffplus.analysis;

/**
 * Contains analysis options.
 */
public class Options {

	private static Options instance;

	private Sensitivity sensitivity;

	private Options() {
		this.sensitivity = Sensitivity.INTRAPROC;
	}

	private Options(Sensitivity sensitivity) {
		this.sensitivity = sensitivity;
	}
	
	/**
	 * @return {@code true} if the analysis is intra-procedural.
	 */
	public boolean intraProc() {
		return sensitivity.equals(Sensitivity.INTRAPROC);
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
	public static Options createInstance(Sensitivity sensitivity) {
		if(instance == null) instance = new Options(sensitivity);
		return instance;
	}
	
	/**
	 * Controls the precision of the analysis.
	 */
	public enum Sensitivity {
		INTRAPROC,
		INTERPROC
	}
	
}