package multidiffplus.mining.cfgvisitor.ajax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;

import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.ICFGVisitor;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.jsanalysis.abstractdomain.State;

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
public class AjaxCFGVisitor implements ICFGVisitor {
	
	/** Keep track of literal definitions. */
	Map<String, Definition> definitions;

	/** We need this for getting the SliceFactBase. */
	SourceCodeFileChange sourceCodeFileChange;
	
	/**
	 * @param sourceCodeFileChange used to look up the correct dataset for
	 * storing facts.
	 */
	public AjaxCFGVisitor(SourceCodeFileChange sourceCodeFileChange) {
		this.definitions = new HashMap<String, Definition>();
		this.sourceCodeFileChange = sourceCodeFileChange;
	}

	@Override
	public void visit(CFGNode node) {
		visit((AstNode) node.getStatement(), (State)node.getAfterState());
	}

	@Override
	public void visit(CFGEdge edge) {
		visit((AstNode) edge.getCondition(), (State)edge.getBeforeState());
	}

	/**
	 * Visit an AstNode (a statement or condition) and extract facts.
	 */
	private void visit(AstNode statement, State state) { 
		if(statement != null && state != null) {
			
			/* Look for modified Ajax calls. */
			AjaxDataASTVisitor.generateFacts(definitions, state, sourceCodeFileChange, statement);
			
			/* Look for definitons that we may use to inject AST nodes with. */
			definitions.putAll(DefValueASTVisitor.getDefinitions(state, statement));

		}
	}

}