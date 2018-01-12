package multidiffplus.jsanalysis.abstractdomain;

import multidiffplus.jsanalysis.flow.Analysis;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * A native (builtin) function. The abstract interpretation of the function is
 * specified in Java, usually as a lambda expression.
 */
public abstract class Closure {
	
	/**
	 * @param selfAddr The value of the 'this' variable (a set of objects).
	 * @param arbObj The argument array object.
	 * @param store Main memory.
	 * @param scratchpad Scratchpad memory.
	 * @param trace The execution trace.
	 * @param control Tracks control flow changes.
	 * @param analysis The top level analysis for pushing new calls.
	 * @return The new state after executing the function.
	 */
	public abstract State run(Address selfAddr, 
							  Store store, Scratchpad scratchpad,
							  Trace trace, Control control,
							  Analysis analysis);
	
	/**
	 * Compare states to determine if the closure needs to be re-analyzed.
	 * @param envClo The environment for the closure, including parameters.
	 * @param storeClo The store for the closure, including parameter values.
	 * @param controlClo The control change environment for the closure.
	 * @return {@code true} if the states are not equivalent and the function needs
	 * 		   to be re-analyzed. {@code false} otherwise.
	 */
	protected boolean reAnalyze(Environment envClo, Store storeClo, Control controlClo,
								Environment envCall, Store storeCall, Control controlCall) {
		return false;
	}

}