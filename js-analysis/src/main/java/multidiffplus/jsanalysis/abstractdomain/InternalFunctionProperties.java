package multidiffplus.jsanalysis.abstractdomain;

import java.util.Stack;

import multidiffplus.jsanalysis.factories.StoreFactory;

/**
 * Internal properties for an abstract Function object.
 *
 * The meaning of the prototype is slightly different for functions: function
 * prototypes are used as prototypes for creating new objects when the function
 * is a constructor. However, function prototypes point to the same prototype
 * objects as object prototypes.
 */
public class InternalFunctionProperties extends InternalObjectProperties {

    /** The function's code and environment. **/
    public Closure closure;

    /**
     * @param prototype
     *            The address of the function prototype.
     * @param closure
     *            The control flow graph and environment stack.
     * @param jsclass
     *            The type of object being created.
     */
    public InternalFunctionProperties(BValue prototype, Closure closure, JSClass jsclass) {
	super(prototype, jsclass);
	this.closure = closure;
    }

    /**
     * Prototype defaults to Function_proto_Addr.
     * 
     * @param closure
     *            The control flow graph and environment stack.
     * @param jsclass
     *            The type of object being created.
     */
    public InternalFunctionProperties(Stack<Closure> closures, JSClass jsclass) {
	super(Address.inject(StoreFactory.Function_proto_Addr, Change.u(), Dependencies.bot()),
		jsclass);
    }

    @Override
    public Closure getCode() {
	return this.closure;
    }

}