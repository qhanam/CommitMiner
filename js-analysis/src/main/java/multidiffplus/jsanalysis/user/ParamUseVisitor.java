package multidiffplus.jsanalysis.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.PropertyGet;

import multidiffplus.jsanalysis.abstractdomain.Criterion;
import multidiffplus.jsanalysis.abstractdomain.Variable;

class ParamUseVisitor implements NodeVisitor {

    Map<String, Criterion> params;
    Set<Criterion> usedParams;

    public static Set<Criterion> findUsedParams(AstNode statement,
	    Map<Variable, Criterion> params) {
	ParamUseVisitor visitor = new ParamUseVisitor(params);
	statement.visit(visitor);
	return visitor.usedParams;
    }

    private ParamUseVisitor(Map<Variable, Criterion> params) {
	this.params = new HashMap<String, Criterion>();
	params.entrySet().forEach(entry -> this.params.put(entry.getKey().name, entry.getValue()));
	usedParams = new HashSet<Criterion>();
    }

    @Override
    public boolean visit(AstNode node) {
	if (node instanceof Name) {
	    if (params.containsKey(node.toSource())) {
		usedParams.add(params.get(node.toSource()));
	    }
	    return false;
	} else if (node instanceof PropertyGet) {
	    visit(((PropertyGet) node).getLeft());
	    return false;
	} else {
	    return true;
	}
    }

}