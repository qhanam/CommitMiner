package multidiffplus.jsanalysis.initstate;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ObjectLiteral;

import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Dependencies;
import multidiffplus.jsanalysis.abstractdomain.InternalObjectProperties;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.Property;
import multidiffplus.jsanalysis.abstractdomain.Store;
import multidiffplus.jsanalysis.abstractdomain.Undefined;

/**
 * A factory which initializes the global Object with JavaScript built-in
 * properties.
 */
public class GlobalFactory {

    private static final Integer OBJECT_DEFINER_ID = -10;
    private static final Integer UNDEFINED_DEFINER_ID = -11;
    private static final Integer MODULE_DEFINER_ID = -12;

    public static final Address UNDEFINED_ADDR = Address.createBuiltinAddr("undefined");

    Store store;

    public GlobalFactory(Store store) {
	this.store = store;
    }

    public Obj Global_Obj() {
	Map<String, Property> ext = new HashMap<String, Property>();
	store = Utilities
		.addProp("Object", OBJECT_DEFINER_ID,
			Address.inject(StoreFactory.Object_Addr, Change.u(),
				Dependencies.injectValue(new ObjectLiteral())),
			ext, store, new Name());
	Name undefined = new Name();
	store = Utilities.addProp("undefined", UNDEFINED_DEFINER_ID,
		Undefined.inject(Undefined.top(), Change.u(), Dependencies.injectValue(undefined)),
		ext, store, undefined);
	Name module = new Name();
	store = Utilities.addProp("module", MODULE_DEFINER_ID,
		Undefined.inject(Undefined.top(), Change.u(), Dependencies.injectValue(module)),
		ext, store, module);

	InternalObjectProperties internal = new InternalObjectProperties();
	return new Obj(ext, internal);
    }

}