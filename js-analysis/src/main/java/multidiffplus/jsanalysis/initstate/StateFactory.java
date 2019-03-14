package multidiffplus.jsanalysis.initstate;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ScriptNode;

import multidiffplus.cfg.CfgMap;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Control;
import multidiffplus.jsanalysis.abstractdomain.Dependencies;
import multidiffplus.jsanalysis.abstractdomain.Environment;
import multidiffplus.jsanalysis.abstractdomain.Scratchpad;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.abstractdomain.Variable;
import multidiffplus.jsanalysis.hoisting.GlobalVisitor;
import multidiffplus.jsanalysis.trace.FSCI;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * A factory which initializes the abstract state from a JavaScript file AST.
 */
public class StateFactory {

    /**
     * @param script
     *            The file under analysis.
     * @return The initial state ς ∈ State := ρ x σ
     */
    public static State createInitialState(ScriptNode script, CfgMap cfgMap) {
	Trace trace = new FSCI(script.getID());
	Store store = StoreFactory.createInitialStore();
	Pair<Environment, Store> lifted = EnvironmentFactory.createInitialEnvironment(script, store,
		cfgMap, trace);
	Scratchpad scratchpad = new Scratchpad();
	Control control = Control.bottom();
	Environment env = lifted.getLeft();
	store = lifted.getRight();
	store = liftGlobals(script, trace, env, store);
	return new State(store, env, scratchpad, trace, control, StoreFactory.global_binding_Addr);
    }

    private static Store liftGlobals(ScriptNode script, Trace trace, Environment env, Store store) {

	/* Lift global variables into the environment and initialize to undefined. */
	Set<String> globals = GlobalVisitor.getGlobals(script);
	int i = -1000;
	for (String global : globals) {
	    Address address = trace.makeAddr(i, "");
	    // Create a dummy variable declaration. This will not exist in the
	    // output, because the value and variable initialization exists
	    // outside the file.
	    Name dummyVariable = new Name();
	    env.strongUpdateNoCopy(global, Variable.inject(global, address, Change.bottom(),
		    Dependencies.injectVariable(dummyVariable)));
	    store = store.alloc(address,
		    BValue.top(Change.u(), Dependencies.injectValue(dummyVariable)), dummyVariable);
	    i--;
	}

	return store;

    }

}