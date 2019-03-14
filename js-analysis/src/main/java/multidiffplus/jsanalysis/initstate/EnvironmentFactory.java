package multidiffplus.jsanalysis.initstate;

import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ast.ScriptNode;

import multidiffplus.cfg.CfgMap;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Dependencies;
import multidiffplus.jsanalysis.abstractdomain.Environment;
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.abstractdomain.Variable;
import multidiffplus.jsanalysis.interpreter.Helpers;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * A factory which Initializes the abstract environment with JavaScript built-in
 * global variables and globally defined script variables.
 */
public class EnvironmentFactory {

    /**
     * Creates an initial environment for a function by lifting local variables and
     * functions into the environment. Local variables are initialized to undefined
     * in the store, while functions are initialized to objects.
     *
     * Variables are initialized in the store after the environment has been
     * computed. Variables that point to functions are initialized recursively so
     * that their closure can be properly computed.
     * 
     * @param script
     *            The root of the AST for the file under analysis.
     * @param store
     *            The initial store, variable values and functions will be
     *            initialized here.
     * @param trace
     *            The trace, which should be empty initially.
     * @return The initial ρ ∈ Environment
     */
    public static Pair<Environment, Store> createInitialEnvironment(ScriptNode script, Store store,
	    CfgMap cfgMap, Trace trace) {
	Environment env = createBaseEnvironment();
	store = Helpers.lift(env, store, script, cfgMap, trace);
	return Pair.of(env, store);
    }

    /**
     * @return The global environment of builtins, without user defined variables.
     */
    public static Environment createBaseEnvironment() {
	Environment env = new Environment();
	env = env.strongUpdate("this", Variable.inject("this", StoreFactory.global_binding_Addr,
		Change.u(), Dependencies.bot()));
	return env;
    }

}