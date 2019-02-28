package multidiffplus.jsanalysis.abstractdomain;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.ast.Name;

/**
 * The abstract domain for storing mappings from identifiers to addresses. i.e.
 * Environment# := String#->P(BValue# | Address#)
 *
 * Identifiers may be
 */
public class Environment {

    /** The possible memory address for each identifier. **/
    public Map<String, Variable> environment;

    /**
     * Creates an empty environment.
     */
    public Environment() {
	this.environment = new HashMap<String, Variable>();
    }

    /**
     * Creates an environment from an existing set of addresses.
     * 
     * @param env
     *            The environment to replicate.
     */
    private Environment(Map<String, Variable> env) {
	this.environment = env;
    }

    @Override
    public Environment clone() {
	Map<String, Variable> map = new HashMap<String, Variable>(this.environment);
	return new Environment(map);
    }

    /**
     * Retrieve a variable's address.
     * 
     * @param varName
     *            The variable.
     * @return The store addresses of the var.
     */
    public Addresses apply(Name varName) {
	Variable var = environment.get(varName.toSource());
	if (var == null)
	    return null;
	for (Criterion crit : var.deps.getDependencies()) {
	    varName.addDependency(crit.getType().name(), crit.getId());
	}
	for (Criterion crit : var.change.getDependencies().getDependencies()) {
	    varName.addDependency(crit.getType().name(), crit.getId());
	}
	return var.addresses;
    }

    /**
     * Performs a strong update on a variable in the environment.
     * 
     * @param variable
     *            The variable to update.
     * @param addresses
     *            The addresses for the variable.
     * @return The updated environment.
     */
    public Environment strongUpdate(String variable, Variable id) {
	Map<String, Variable> map = new HashMap<String, Variable>(this.environment);
	map.put(variable, id);
	return new Environment(map);
    }

    /**
     * Performs a strong update on a variable in the environment. Updates the object
     * directly without making a copy.
     * 
     * @param variable
     *            The variable to update.
     * @param addresses
     *            The addresses for the variable.
     * @return The updated environment.
     */
    public void strongUpdateNoCopy(String variable, Variable id) {
	this.environment.put(variable, id);
    }

    /**
     * Performs a weak update on a variable in the environment.
     * 
     * @param variable
     *            The variable to update.
     * @param addresses
     *            The addresses for the variable.
     * @return The updated environment.
     */
    public Environment weakUpdate(String variable, Variable id) {
	Map<String, Variable> map = new HashMap<String, Variable>(this.environment);
	map.put(variable, map.get(variable).join(id));
	return new Environment(map);
    }

    /**
     * Computes ρ ∪ ρ
     * 
     * @param environment
     *            The environment to join with this environment.
     * @return The joined environments as a new environment.
     */
    public Environment join(Environment environment) {
	Environment joined = new Environment(new HashMap<String, Variable>(this.environment));

	/*
	 * Because we dynamically allocate unexpected local variables to the
	 * environment, sometimes we will need to merge different environments.
	 *
	 * We do this by merging BValues and keeping only one address.
	 */

	for (Map.Entry<String, Variable> entry : environment.environment.entrySet()) {

	    /* The variable is missing from left. */
	    if (!joined.environment.containsKey(entry.getKey())) {
		joined.environment.put(entry.getKey(), entry.getValue());
	    }

	    /*
	     * The value of left != the value from right. Merge the address lists for both
	     * environments.
	     */
	    if (joined.environment.get(entry.getKey()) != entry.getValue()) {
		joined.environment.put(entry.getKey(),
			joined.environment.get(entry.getKey()).join(entry.getValue()));
	    }

	}
	return joined;
    }

    @Override
    public String toString() {
	String str = "-Variables-\n";
	for (Map.Entry<String, Variable> entry : this.environment.entrySet()) {
	    str += entry.getKey() + "_" + entry.getValue().change.toString() + ": "
		    + entry.getValue().toString() + "\n";
	}
	return str;
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof Environment))
	    return false;
	Environment env = (Environment) o;
	if (this.environment.keySet().size() != env.environment.keySet().size())
	    return false;
	for (Map.Entry<String, Variable> entry : env.environment.entrySet()) {
	    if (!this.environment.containsKey(entry.getKey()))
		return false;
	    if (!this.environment.get(entry.getKey()).equals(entry.getValue()))
		return false;
	}
	return true;
    }

}