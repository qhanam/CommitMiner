package multidiff.analysis.flow;

/**
 * A function which is run asynchronously (ie. from the event loop).
 * 
 * {@code AsyncFunction}s are run after the analysis of the analysis beginning
 * at the main entry point finishes.
 */
public interface AsyncFunctionCall {

    /**
     * Schedules the function to be run by loading it onto the call stack as a new
     * frame.
     */
    public void run(CallStack callStack);

}
