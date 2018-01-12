package symgen;

import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.UnaryExpression;

public class ExpressionBuilder {
	
	private Map<String, Integer> vars;
	public String postfix ;
	public String type ;
	
	/**
	 * Produce CVC4 code for the condition.
	 * @param type 
	 */
	public static String generateCVC4(AstNode expression, Map<String, Integer> vars, String old_new, String type) {
		ExpressionBuilder builder = new ExpressionBuilder(vars);
		builder.postfix = old_new;
		builder.type = type;
		String cvc4 = builder.build(expression);
		return cvc4;
	}
	
	private ExpressionBuilder(Map<String, Integer> vars) {
		this.vars = vars;
	}

	private String build(AstNode node) {
		
		switch(node.getType()) {
		case Token.NUMBER:
			return node.toSource();
		case Token.NAME:
			return visitName((Name) node);
		case Token.ADD:
			return visitOp((InfixExpression) node, "+");
		case Token.SUB:
			return visitOp((InfixExpression) node, "-");
		case Token.MUL:
			return visitOp((InfixExpression) node, "*");
		case Token.INC: 
			return visitUnary((UnaryExpression) node, "+");
		case Token.DEC: 
			return visitUnary((UnaryExpression)node, "-");
		case Token.GETELEM:
			return visitArrayAccess((ElementGet) node);
		case Token.CALL:
			return visitCall((FunctionCall)node);
		}
		
		return "";
		
	}
	
	// If the assignment is a function call
	private String visitCall(FunctionCall fCall) {
		AstNode targetNode = fCall.getTarget();

		if(targetNode.getType() == Token.NAME) {
			String argString = "";
			List<AstNode> arguments = fCall.getArguments();
			boolean first  = true;
			for(AstNode argument: arguments) {
				if(first)
					first = false;
				else
					argString += ",";
				argString += argument.toSource() + postfix + vars.get(argument.toSource());
			}
			String call = targetNode.toSource() + postfix + vars.get(targetNode.toSource()) + "(" + argString + ")";
			return call;
		}
		return null;
	}

	private String visitUnary(UnaryExpression node, String op) {
		return build(node.getOperand()) + op +" 1";
		
	}
	
	private String visitArrayAccess(ElementGet node) {
		return build(node.getTarget()) + "[" + build(node.getElement()) + "]";
	}
	
	private String visitName(Name node) {
		String nodeName = node.toSource() ;
		return nodeName + postfix + vars.get(nodeName);
	}

	private String visitOp(InfixExpression node, String op) {
		if(type.equalsIgnoreCase("STRING") && op.equalsIgnoreCase("+")) {
			return "CONCAT(" +  build((node.getLeft()))+ "," + build(node.getRight()) + ")";
		}
		return build(node.getLeft()) + " " + op + " " + build(node.getRight());
	}

}
