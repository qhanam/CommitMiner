package multidiffplus.jsanalysis.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.PropertyGet;

class ParamUseVisitor implements NodeVisitor {

    Map<String, Name> params;
    Set<Name> usedParams;

    public static Set<Name> findUsedParams(AstNode statement, Set<Name> params) {
	ParamUseVisitor visitor = new ParamUseVisitor(params);
	statement.visit(visitor);
	return visitor.usedParams;
    }

    private ParamUseVisitor(Set<Name> params) {
	this.params = new HashMap<>();
	params.forEach(param -> this.params.put(param.toSource(), param));
	this.usedParams = new HashSet<>();
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
	} else if (node instanceof FunctionNode) {
	    return false;
	} else {
	    return true;
	}
    }

}