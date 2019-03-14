package multidiffplus.jsanalysis.abstractdomain;

import multidiffplus.jsanalysis.trace.Trace;

/**
 * Stores the state of the function analysis at a point in the CFG.
 */
public class State {

    /*
     * The abstract domains that make up the program state. The abstract domains
     * have access to each other.
     */

    public Environment env;
    public Store store;
    public Scratchpad scratch;
    public Trace trace;
    public Control control;

    /** Points to the current value of 'this'. **/
    public Address selfAddr;

    /**
     * Create a new state after a transfer or join.
     * 
     * @param store
     *            The abstract store of the new state.
     * @param environment
     *            The abstract environment of the new state.
     */
    public State(Store store, Environment environment, Scratchpad scratchpad, Trace trace,
	    Control control, Address selfAddr) {
	this.store = store;
	this.env = environment;
	this.scratch = scratchpad;
	this.trace = trace;
	this.control = control;

	this.selfAddr = selfAddr;
    }

    @Override
    public State clone() {
	return new State(store.clone(), env.clone(), scratch.clone(), trace, control.clone(),
		selfAddr);
    }

    /**
     * We should only join states from the same trace.
     * 
     * @param state
     *            The state to join with.
     * @return A state representing the join of the two states.
     */
    public State join(State state) {

	if (state == null)
	    return this;

	State joined = new State(this.store.join(state.store), this.env.join(state.env),
		this.scratch.join(state.scratch), this.trace, this.control.join(state.control),
		this.selfAddr);

	return joined;

    }

}