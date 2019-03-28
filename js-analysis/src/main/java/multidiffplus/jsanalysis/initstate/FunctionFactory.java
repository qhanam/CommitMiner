package multidiffplus.jsanalysis.initstate;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.mozilla.javascript.ast.Name;

import multidiffplus.cfg.CfgMap;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
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
import multidiffplus.jsanalysis.abstractdomain.Str;
import multidiffplus.jsanalysis.flow.JavaScriptAnalysisState;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * A factory which initializes the function object prototype in the abstract
 * store.
 */
public class FunctionFactory {

    public Store store;

    public FunctionFactory(Store store) {
	this.store = store;
    }

    public Obj Function_proto_Obj() {
	Map<String, Property> ext = new HashMap<String, Property>();
	store = Utilities.addProp("external", -41,
		Num.inject(Num.top(), Change.u(), Dependencies.bot()), ext, store, new Name());
	store = Utilities.addProp("apply", -42, Address
		.inject(StoreFactory.Function_proto_apply_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("call", -43, Address.inject(StoreFactory.Function_proto_call_Addr,
		Change.u(), Dependencies.bot()), ext, store, new Name());
	store = Utilities.addProp("toString", -44, Address
		.inject(StoreFactory.Function_proto_toString_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());

	InternalObjectProperties internal = new InternalObjectProperties(
		Address.inject(StoreFactory.Function_proto_Addr, Change.u(), Dependencies.bot()),
		JSClass.CFunction_prototype_Obj);

	return new Obj(ext, internal);
    }

    // TODO: apply and call need native closures because their behaviour
    // affects the analysis.
    public Obj Function_proto_toString_Obj() {
	return constFunctionObj(Str.inject(Str.top(), Change.u(), Dependencies.bot()));
    }

    public Obj Function_proto_apply_Obj() {
	return constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Function_proto_call_Obj() {
	return constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    /**
     * Approximate a function which is not modeled.
     * 
     * @return A function which has no side effects that that returns the BValue
     *         lattice element top.
     */
    public Obj constFunctionObj(BValue retVal) {

	Map<String, Property> external = new HashMap<String, Property>();

	Closure closure = new NativeClosure() {
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
		JSClass.CFunction);

	return new Obj(external, internal);

    }

}