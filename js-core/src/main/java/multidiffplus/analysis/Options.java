package multidiffplus.analysis;

/**
 * Contains analysis options.
 */
public class Options {

	private static Options instance;

	private Sensitivity sensitivity;
	
	private Labels labels;

	private Options() {
		this.sensitivity = Sensitivity.INTRAPROC;
		this.labels = Labels.NOMINAL;
	}

	private Options(Sensitivity sensitivity) {
		this.sensitivity = sensitivity;
		this.labels = Labels.NOMINAL;
	}
	
	private Options(Sensitivity sensitivity, Labels labels) {
		this.sensitivity = sensitivity;
		this.labels = labels;
	}
	
	/**
	 * @return {@code true} if the analysis is intra-procedural.
	 */
	public boolean intraProc() {
		return sensitivity.equals(Sensitivity.INTRAPROC);
	}
	
	/**
	 * @return The setting of the labels option.
	 */
	public Labels labels() {
		return labels;
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
	 * @return the singleton {@code Options}.
	 */
	public static Options createInstance(Sensitivity sensitivity, Labels labels) {
		if(instance == null) instance = new Options(sensitivity, labels);
		return instance;
	}
	
	/**
	 * Controls the precision of the analysis.
	 */
	public enum Sensitivity {
		INTRAPROC,
		INTERPROC
	}
	
	public enum Labels {
		NOMINAL,
		REPAIR,
		MUTABLE
	}
	
}