package multidiffplus.jsanalysis.initstate;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.mozilla.javascript.ast.Name;

import multidiffplus.cfg.CfgMap;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Closure;
import multidiffplus.jsanalysis.abstractdomain.Control;
import multidiffplus.jsanalysis.abstractdomain.Dependencies;
import multidiffplus.jsanalysis.abstractdomain.InternalFunctionProperties;
import multidiffplus.jsanalysis.abstractdomain.InternalObjectProperties;
import multidiffplus.jsanalysis.abstractdomain.JSClass;
import multidiffplus.jsanalysis.abstractdomain.NativeClosure;
import multidiffplus.jsanalysis.abstractdomain.Num;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.Property;
import multidiffplus.jsanalysis.abstractdomain.Scratchpad;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.flow.JavaScriptAnalysisState;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * A factory which initializes the global Arguments object with JavaScript
 * built-in properties.
 */
public class ArgumentsFactory {

    private static final Integer ARG_DEFINER_ID = -2;

    Store store;

    public ArgumentsFactory(Store store) {
	this.store = store;
    }

    public Obj Arguments_Obj() {
	Map<String, Property> ext = new HashMap<String, Property>();
	store = Utilities.addProp("prototype", ARG_DEFINER_ID,
		Address.inject(StoreFactory.Object_proto_Addr, Change.u(), Dependencies.bot()), ext,
		store, new Name());
	store = Utilities.addProp("length", ARG_DEFINER_ID,
		Num.inject(Num.top(), Change.u(), Dependencies.bot()), ext, store, new Name());

	NativeClosure closure = new NativeClosure() {
	    @Override
	    public FunctionOrSummary initializeOrRun(State preTransferState, Address selfAddr,
		    Store store, Scratchpad scratchpad, Trace trace, Control control, CfgMap cfgs) {
		return new FunctionOrSummary(JavaScriptAnalysisState
			.initializeFunctionState(preTransferState.clone(), cfgs));
	    }
	};

	Stack<Closure> closures = new Stack<Closure>();
	closures.push(closure);

	InternalObjectProperties internal = new InternalFunctionProperties(closures,
		JSClass.CArguments);

	return new Obj(ext, internal);
    }

}