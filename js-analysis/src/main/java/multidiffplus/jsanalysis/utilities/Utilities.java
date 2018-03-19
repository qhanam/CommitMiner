package multidiffplus.jsanalysis.utilities;

import java.util.LinkedList;
import java.util.List;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.PropertyGet;

import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Variable;
import multidiffplus.jsanalysis.transfer.ExpEval;

public class Utilities {

	/**
	 * Resolves {@code DependencyIdentifier}s for the given variable or field.
	 * @param The state of the program where {@code node} is resolved.
	 * @param A variable or field.
	 * @return a list of definer IDs for the identifier.
	 */
	public static List<DependencyIdentifier> resolveDefinerIDs(
			State state, AstNode node) {
		
		List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();

		if(node instanceof Name) {

			/* Resolve the variable. */
			Variable var = state.env.environment.get(node.toSource());
			if(var != null) {
				BValue val = state.store.apply(var.addresses);
				ids.add(val);
			}

		}
		else if(node instanceof PropertyGet) {
			
			/* Resolve the full property get. */
			ExpEval expEval = new ExpEval(state);
			for(Address addr : expEval.resolveOrCreate(node)) {
				BValue val = state.store.apply(addr);
				ids.add(val);
			}

		}
		else if(node instanceof ElementGet) {
			
			/* Resolve the full property get. */
			ExpEval expEval = new ExpEval(state);
			for(Address addr : expEval.resolveOrCreate(node)) {
				BValue val = state.store.apply(addr);
				ids.add(val);
			}

		}
		
		return ids;
		
	}
	
}
