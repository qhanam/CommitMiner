package multidiffplus.jsanalysis.initstate;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.mozilla.javascript.ast.Name;

import multidiff.analysis.flow.CallStack;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Bool;
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
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.abstractdomain.Str;
import multidiffplus.jsanalysis.flow.JavaScriptAnalysisState;
import multidiffplus.jsanalysis.trace.Trace;

/**
 * A factory which initializes the abstract store with JavaScript built-in
 * objects.
 */
public class ObjFactory {

    public Store store;
    FunctionFactory ff;

    public ObjFactory(Store store) {
	this.store = store;
	ff = new FunctionFactory(store);
    }

    public Obj Object_Obj() {
	Map<String, Property> ext = new HashMap<String, Property>();
	store = Utilities.addProp("prototype", -21,
		Address.inject(StoreFactory.Object_proto_Addr, Change.u(), Dependencies.bot()), ext,
		store, new Name());
	store = Utilities.addProp("number", -22,
		Num.inject(Num.top(), Change.u(), Dependencies.bot()), ext, store, new Name());
	store = Utilities.addProp("create", -23,
		Address.inject(StoreFactory.Object_create_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("defineProperties", -24, Address
		.inject(StoreFactory.Object_defineProperties_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("defineProperty", -25, Address
		.inject(StoreFactory.Object_defineProperty_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("freeze", -26,
		Address.inject(StoreFactory.Object_freeze_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("getOwnPropertyDescriptor", -27,
		Address.inject(StoreFactory.Object_getOwnPropertyDescriptor_Addr, Change.u(),
			Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("getOwnPropertyNames", -28,
		Address.inject(StoreFactory.Object_getOwnPropertyNames_Addr, Change.u(),
			Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("getPrototypeOf", -29, Address
		.inject(StoreFactory.Object_getPrototypeOf_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("isExtensible", -30, Address
		.inject(StoreFactory.Object_isExtensible_Addr, Change.u(), Dependencies.bot()), ext,
		store, new Name());
	store = Utilities.addProp("isFrozen", -31,
		Address.inject(StoreFactory.Object_isFrozen_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("isSealed", -32,
		Address.inject(StoreFactory.Object_isSealed_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("keys", -33,
		Address.inject(StoreFactory.Object_keys_Addr, Change.u(), Dependencies.bot()), ext,
		store, new Name());
	store = Utilities.addProp("preventExtensions", -34, Address
		.inject(StoreFactory.Object_preventExtensions_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("seal", -35,
		Address.inject(StoreFactory.Object_seal_Addr, Change.u(), Dependencies.bot()), ext,
		store, new Name());

	NativeClosure closure = new NativeClosure() {
	    @Override
	    public JavaScriptAnalysisState run(Address selfAddr, Store store, Scratchpad scratchpad,
		    Trace trace, Control control, CallStack callStack) {
		// TODO: Update the state
		return null;
	    }

	    @Override
	    public JavaScriptAnalysisState run(Address selfAddr, Store store, Scratchpad scratchpad,
		    Trace trace, Control control) {
		// TODO: Update the state
		return null;
	    }
	};

	Stack<Closure> closures = new Stack<Closure>();
	closures.push(closure);

	InternalObjectProperties internal = new InternalFunctionProperties(closures,
		JSClass.CObject_Obj);

	return new Obj(ext, internal);
    }

    // TODO: We can be more precise with these.
    public Obj Object_create_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_defineProperties_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_defineProperty_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_freeze_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_getOwnPropertyDescriptor_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_getOwnPropertyNames_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_getPrototypeOf_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_isExtensible_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_isFrozen_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_isSealed_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_keys_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_preventExtensions_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_seal_Obj() {
	return ff.constFunctionObj(BValue.top(Change.u(), Dependencies.bot()));
    }

    public Obj Object_proto_Obj() {
	Map<String, Property> ext = new HashMap<String, Property>();
	store = Utilities.addProp("toString", -11, Address
		.inject(StoreFactory.Object_proto_toString_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("toLocaleString", -12,
		Address.inject(StoreFactory.Object_proto_toLocaleString_Addr, Change.u(),
			Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("valueOf", -13, Address
		.inject(StoreFactory.Object_proto_valueOf_Addr, Change.u(), Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("hasOwnPrpoerty", -14,
		Address.inject(StoreFactory.Object_proto_hasOwnProperty_Addr, Change.u(),
			Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("isPrototypeOf", -15,
		Address.inject(StoreFactory.Object_proto_isPrototypeOf_Addr, Change.u(),
			Dependencies.bot()),
		ext, store, new Name());
	store = Utilities.addProp("propertyIsEnumerable", -16,
		Address.inject(StoreFactory.Object_proto_propertyIsEnumerable_Addr, Change.u(),
			Dependencies.bot()),
		ext, store, new Name());

	InternalObjectProperties internal = new InternalObjectProperties();

	return new Obj(ext, internal);
    }

    public Obj Object_proto_toString_Obj() {
	return ff.constFunctionObj(Str.inject(Str.top(), Change.u(), Dependencies.bot()));
    }

    public Obj Object_proto_toLocaleString_Obj() {
	return ff.constFunctionObj(Str.inject(Str.top(), Change.u(), Dependencies.bot()));
    }

    public Obj Object_proto_hasOwnProperty_Obj() {
	return ff.constFunctionObj(Bool.inject(Bool.top(), Change.u(), Dependencies.bot()));
    }

    public Obj Object_proto_isPrototypeOf_Obj() {
	return ff.constFunctionObj(Bool.inject(Bool.top(), Change.u(), Dependencies.bot()));
    }

    public Obj Object_proto_propertyIsEnumerable_Obj() {
	return ff.constFunctionObj(Bool.inject(Bool.top(), Change.u(), Dependencies.bot()));
    }

    public Obj Object_proto_valueOf_Obj() {
	return ff.constFunctionObj(BValue.primitive(Change.u(), Dependencies.bot()));
    }

}