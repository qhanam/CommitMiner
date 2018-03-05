package multidiffplus.mining.cfgvisitor.ajax;

import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.StringLiteral;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.mining.flow.facts.Slice;
import multidiffplus.mining.flow.facts.SliceChange;
import multidiffplus.mining.flow.facts.SliceFactBase;
import multidiffplus.mining.flow.facts.Statement;
import multidiffplus.mining.flow.mutations.MutateStringify;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.utilities.Utilities;

/**
 * Search for the repair pattern where a JSON object is incorrectly passed as an
 * object literal instead of a stringified JSON object.
 * 
 * From http://api.jquery.com/jquery.ajax/:
 * """
 * The data option can contain either a query string of the form
 * key1=value1&key2=value2, or an object of the form {key1: 'value1', key2:
 * 'value2'}. If the latter form is used, the data is converted into a query
 * string using jQuery.param() before it is sent. ... The processing might be
 * undesirable... in this case, change the contentType option from
 * application/x-www-form-urlencoded to a more appropriate MIME type.
 * """
 */
public class AjaxDataASTVisitor implements NodeVisitor {
	
	/** The program state at the current statement. */
	State state;
	
	/** The statement being visited. */
	AstNode statement;

	/** Register facts here. */
	SliceFactBase factBase;
	
	/** Look up definitions for the data field. **/
	Map<String, Definition> definitions;
	
	/**
	 * Add Ajax facts to the {@code SliceFactBase} for the {@code SourceCodeFileChange}.
	 */
	public static void generateFacts(Map<String, Definition> definitions, State state, SourceCodeFileChange sourceCodeFileChange, AstNode statement) {
		AjaxDataASTVisitor visitor = new AjaxDataASTVisitor(definitions, state, sourceCodeFileChange, statement);
		statement.visit(visitor);
	}
	
	/**
	 * @param sourceCodeFileChange used to look up the correct dataset for
	 * storing facts.
	 */
	public AjaxDataASTVisitor(Map<String, Definition> definitions, 
			State state,
			SourceCodeFileChange sourceCodeFileChange, 
			AstNode statement) {
		this.definitions = definitions;
		this.state = state;
		this.factBase = SliceFactBase.getInstance(sourceCodeFileChange);
		this.statement = statement;
	}

	@Override
	public boolean visit(AstNode node) {
		
		/* Stop on function declarations & investigate call sites. */
		switch(node.getType()) {
		case Token.SCRIPT:
		case Token.FUNCTION:
			return false;
		case Token.CALL:
			visitFunctionCall((FunctionCall)node);
			break;
		}
		
		/* Recursively search for AST analysis. */
		return true;

	}
	
	/**
	 * Register annotations for updated calls to $.ajax()
	 */
	public void visitFunctionCall(FunctionCall call) {
		
		/* Make a clone so we don't change anything. */
		ExpressionStatement dummy = new ExpressionStatement();
		call = (FunctionCall)call.clone(dummy);
		dummy.setExpression(call);
		
		/* Is this a call to $.ajax? */
		AstNode target = call.getTarget();
		if(!target.toSource().equals("$.ajax") 
				&& !target.toSource().equals("jQuery.ajax")) return;

		/* Is this a new or updated call? */
		if(call.getChangeType() != ChangeType.INSERTED
				&& call.getChangeType() != ChangeType.UPDATED) return;

		/* Find the settings argument (usually an object literal). */
		ObjectLiteral settings = null;
		for(AstNode arg : call.getArguments()) {
			if(arg.getType() == Token.OBJECTLIT) settings = (ObjectLiteral) arg;
		}
		if(settings == null) return;

		/* Find the data field. */
		ObjectProperty dataProperty = getDataField(settings);

		if(dataProperty == null) {

			/* There is no data being sent, so no repair should be applied. */
			this.registerSliceChange(call, call, SliceChange.Type.NOMINAL);
			return;

		}
		
		/* Find the call to JSON.stringify. */
		FunctionCall stringify = getStringify(dataProperty.getRight());

		if(stringify == null) {

			/* JSON.stringify is not used, so no repair should be applied. */
			registerSliceChange(call, call, SliceChange.Type.NOMINAL);
			return;

		}
		
		if(call.getChangeType() == ChangeType.UPDATED
				&& dataProperty.getChangeType() == ChangeType.UPDATED
				&& stringify.getChangeType() == ChangeType.INSERTED) {
			
			/* Attempt to resolve variables to literals. */
//			resolveVarInData((ObjectProperty)dataProperty.getMapping());
//			resolveVarInStringify(stringify);

			/* After the repair is applied, the code is nominal. */
			registerSliceChange(call, call, SliceChange.Type.NOMINAL);

			/* There is a concrete repair. */
			registerSliceChange((FunctionCall)call.getMapping(), call, SliceChange.Type.REPAIR);

			return;

		}
		else if(call.getChangeType() == ChangeType.UPDATED
				|| call.getChangeType() == ChangeType.INSERTED) {
			
			/* Attempt to resolve variables to literals. */
			resolveVarInStringify(stringify);

			/* JSON.stringify is already used, so no repair should be applied. */
			registerSliceChange(call, call, SliceChange.Type.NOMINAL);
			
			/* Mutate a repair (delete JSON.stringify). */
			MutateStringify mutation = new MutateStringify(call);
			FunctionCall mutant = mutation.mutate();
			if(mutant != null)
				registerSliceChange(mutant, call, SliceChange.Type.MUTANT_REPAIR);

			return;

		}

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
	
	/**
	 * Registers the change to the call's slice.
	 */
	private void registerSliceChange(FunctionCall callA, FunctionCall callB, SliceChange.Type type) {
		Slice before = callA == null ? null : buildSlice(callA);
		Slice after = callB == null ? null : buildSlice(callB);
		factBase.registerSliceFact(new SliceChange(before, after, type));
	}

	/**
	 * Builds a slice. Currently only supports one statement. 
	 * @param node The statement that constitutes the slice.
	 * @return A single-statement slice.
	 */
	private Slice buildSlice(AstNode node) {
		return new Slice(
			new Statement(
					node.toSource(),
					node.getLineno(),
					node.getAbsolutePosition(),
					node.getLength()));
	}
	
	private void resolveVarInStringify(FunctionCall stringify) {
		
		List<AstNode> args = stringify.getArguments();
	
		if(args.size() == 1) {
			
			AstNode arg = args.get(0);
			AstNode literal = resolveToAST(arg);
			
			if(literal == null) return;
			
			literal = literal.clone();
			literal.setParent(stringify);
			
			args.remove(0);
			args.add(literal);
			
		}
		
		return;
		
	}
	
	private void resolveVarInData(ObjectProperty dataProperty) {
		
		AstNode value = dataProperty.getRight();
		AstNode literal = resolveToAST(value);
		
		if(literal == null) return;
		
		dataProperty.setRight(literal);
		
	}

	/**
	 * @param node a variable or field
	 * @return the definition of the literal pointed to by the variable or
	 * 		   field, or {@code null} if it cannot be resolved to a literal.
	 */
	private AstNode resolveToAST(AstNode node) {
		
		/* Resolve the node to a set of def/use IDs. */
		List<DependencyIdentifier> ids = Utilities.resolveDefinerIDs(state, node);
		
		/* Look up the def/use IDs in the list of literal definitions. */
		for(DependencyIdentifier id : ids) {
			Definition definition = definitions.get(id.getAddress());
			if(definition != null) return definition.getAstNode();
		}
		
		return null;
		
	}

}
