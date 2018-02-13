package multidiffplus.mining.flow.mutations;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.StringLiteral;

/**
 * Mutates a call to jQuery.ajax to remove JSON.stringify from the 'data' field.
 * 
 * Example:
 * $.ajax({
 * 		type: 'POST',
 * 		data: JSON.stringify({...}),
 * 		content-type: 'json'});
 * 
 * is muatated to
 * 
 * $.ajax({
 * 		type: 'POST',
 * 		data: {...},
 * 		content-type: 'json'});
 */
public class MutateStringify {
	
	private FunctionCall original;
	
	/**
	 * @param call The ajax call which wraps the data value in JSON.stringify.
	 */
	public MutateStringify(FunctionCall original) {
		this.original = original;
	}

	/**
	 * @param call The ajax call which wraps the data value in JSON.stringify.
	 * @return The ajax call without JSON.stringify.
	 */
	public FunctionCall mutate() {
	
		/* Clone the call so we don't modify the original. */
		FunctionCall call = (FunctionCall)original.clone(original.getParent());
		
		/* Find the settings argument (usually an object literal). */
		ObjectLiteral settings = getSettings(call);
		if(settings == null) return null;
		
		/* Find the data field. */
		ObjectProperty dataProperty = getDataField(settings);
		if(dataProperty == null) return null;
		
		/* Find the JSON.stringify call. */
		FunctionCall stringify = getStringify(dataProperty.getRight());
		if(stringify == null) return null;
		
		/* Pull up the contents of the JSON.stringify. */
		if(stringify.getArguments().size() != 1) return null;
		dataProperty.setRight(stringify.getArguments().get(0));
		
		return call;
		
	}

	/**
	 * @return the call to JSON.stringify(value), or {@code null} if not
	 * stringified
	 */
	private FunctionCall getStringify(AstNode value) {

		if(value.getType() != Token.CALL) return null;

		FunctionCall call = (FunctionCall) value;

		/* Call to JSON.stringify with one argument? */
		if(call.getTarget().toSource().equals("JSON.stringify")
				|| call.getTarget().toSource().equals("$.toJSON")
				|| call.getTarget().toSource().equals("jQuery.toJSON")) {
			return call;
		}
		
		return null;
		
	}

	/**
	 * @return The object literal which specifies the ajax arguments.
	 */
	private ObjectLiteral getSettings(FunctionCall call) {

		ObjectLiteral settings = null;

		for(AstNode arg : call.getArguments()) {
			if(arg.getType() == Token.OBJECTLIT) settings = (ObjectLiteral) arg;
		}

		if(settings == null) return null;
		
		return settings;
		
	}

	/**
	 * @return The object property of the 'data' field, or {@code null} if not
	 * found.
	 */
	private ObjectProperty getDataField(ObjectLiteral settings) {

		/* Find and check the data field. */
		for(ObjectProperty property : settings.getElements()) {

			AstNode field = property.getLeft();

			switch(field.getType()) {
			case Token.NAME:
				if(field.toSource().equals("data")) return property;
				break;
			case Token.STRING:
				if(((StringLiteral)field).getValue().equals("data")) return property;
				break;
			}

		}
		
		/* The 'data' field is not in the object literal. */
		return null;
		
	}
	
}
