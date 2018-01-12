package symgen;

import java.util.Map;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.InfixExpression;

public class ConditionBuilder {
	
	private Map<String, Integer> vars;
	public String old_new;
	/**
	 * Produce CVC4 code for the condition.
	 */
	public static String generateCVC4(AstNode condition, Map<String, Integer> vars, String old_new) {
		ConditionBuilder builder = new ConditionBuilder(vars);
		builder.old_new = old_new;
		String cvc4 = builder.build(condition);
		return cvc4;
	}
	
	private ConditionBuilder(Map<String, Integer> vars) {
		this.vars = vars;
	}

	private String build(AstNode node) {
		
		String cvc4 = "";

		switch(node.getType()) {
		case Token.SHNE:
			cvc4 = visitInequality((InfixExpression) node);
			break;
		case Token.EQ:
		case Token.SHEQ:
		case Token.LT:
		case Token.LE:
		case Token.GT:
		case Token.GE:
			cvc4 = visitComparator((InfixExpression) node);
			break;
		case Token.AND:
			cvc4 = visitAnd((InfixExpression) node);
			break;
		case Token.OR:
			cvc4 = visitOr((InfixExpression) node);
			break;
		default:
			cvc4 = ExpressionBuilder.generateCVC4(node, vars, old_new, "INT");
		}
		
		return cvc4;
		
	}
	
	private String visitAnd(InfixExpression node) {
		String left = build(node.getLeft());
		String right = build(node.getRight());
		return left + " AND " + right;
	}

	private String visitOr(InfixExpression node) {
		String left = build(node.getLeft());
		String right = build(node.getRight());
		return left + " OR " + right;
	}

	private String visitComparator(InfixExpression node) {

		String operator;

		String left = build(node.getLeft());
		String right = build(node.getRight());
		
		switch(node.getType()) {
		case Token.EQ: operator = " = "; break;
		case Token.SHEQ: operator = " = "; break;
		case Token.LT: operator = " < "; break;
		case Token.LE: operator = " <= "; break;
		case Token.GT: operator = " > "; break;
		case Token.GE: operator = " >= "; break;
		default: return "";
		}
		
		return left + operator + right;

	}

	private String visitInequality(InfixExpression node) {
		String left = node.getLeft().toSource();
		left += vars.get(left);
		String right = node.getRight().toSource();
		right += vars.get(right);
		return "NOT " +  left + " = " + right;
	}

}
