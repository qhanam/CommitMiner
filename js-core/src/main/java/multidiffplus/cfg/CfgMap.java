package multidiffplus.cfg;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;

/**
 * A map of {@code ClassifiedAstNode}s to their CFGs.
 * 
 * This is used by the language's interpreter to add new frames to the call
 * stack when a call site is encountered.
 */
public class CfgMap {

    Map<ClassifiedASTNode, CFG> cfgMap;

    public CfgMap() {
	this.cfgMap = new HashMap<ClassifiedASTNode, CFG>();
    }

    /**
     * Returns the CFG created from the given function definition.
     * 
     * @return CFG or {@code null} if no CFG exists for the function definition.
     */
    public CFG getCfgFor(ClassifiedASTNode function) {
	return cfgMap.get(function);
    }

    /**
     * Adds a function -> CFG mapping.
     * 
     * @param function
     *            The function the CFG was built from.
     * @param cfg
     *            The CFG for the function.
     */
    public void addCfg(ClassifiedASTNode function, CFG cfg) {
	cfgMap.put(function, cfg);
    }

    /**
     * Returns the CFGs as a collection.
     */
    public Collection<CFG> getCfgs() {
	return cfgMap.values();
    }

}