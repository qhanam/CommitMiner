package multidiffplus.jsanalysis.abstractdomain;

import java.util.Map;

import multidiffplus.jsanalysis.factories.StoreFactory;

/** Internal properties for an abstract (non-function) object. **/
public class InternalObjectProperties {

    /**
     * The prototype of the object. This is either the prototype of the constructor
     * or the prototype of Object (if defined as an object literal).
     */
    public BValue prototype;

    /**
     * Includes things like the prototype object. The prototype property is either
     * the prototype of a constructor (if it is a function) or some other object (if
     * it's an object).
     */
    public Map<String, BValue> properties;

    /**
     * The type of object. This is the "based on the constructor function's object
     * address" [notJS Concrete Semantics].
     **/
    public JSClass klass;

    /**
     * @param prototype
     *            The address for this object's prototype.
     * @param klass
     *            The type of object being constructed.
     */
    public InternalObjectProperties(BValue prototype, JSClass klass) {
	this.prototype = prototype;
	this.klass = klass;
    }

    /**
     * Class defaults to CObject.
     * 
     * @param prototype
     *            The address for this object's prototype.
     */
    public InternalObjectProperties(BValue prototype) {
	this.prototype = prototype;
	this.klass = JSClass.CObject;
    }

    /**
     * Prototype defaults to Object_proto_Addr.
     * 
     * @param klass
     *            The type of object being constructed.
     */
    public InternalObjectProperties(JSClass klass) {
	this.prototype = Address.inject(StoreFactory.Object_proto_Addr, Change.u(),
		Dependencies.bot());
	this.klass = klass;
    }

    /**
     * Prototype defaults to Object_proto_Addr. Class defaults to CObject.
     */
    public InternalObjectProperties() {
	this.prototype = Address.inject(StoreFactory.Object_proto_Addr, Change.u(),
		Dependencies.bot());
	this.klass = JSClass.CObject;
    }

    /**
     * @return The set of closures. Empty if this object is not a function.
     */
    public Closure getCode() {
	return null;
    }

}