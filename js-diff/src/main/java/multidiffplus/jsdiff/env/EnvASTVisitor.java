package multidiffplus.jsdiff.env;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;
import org.mozilla.javascript.ast.WhileLoop;
import org.mozilla.javascript.ast.WithStatement;

import multidiffplus.commit.Annotation;
import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Environment;
import multidiffplus.jsanalysis.abstractdomain.Variable;

public class EnvASTVisitor implements NodeVisitor {

	/**
	 * The set of changed variable annotations found in the statement.
	 **/
	public Set<Annotation> annotations;

	/** The abstract environment. **/
	private Environment env;

	/**
	 * Detects uses of the identifier.
	 * @return the set of nodes where the identifier is used.
	 */
	public static Set<Annotation> getAnnotations(Environment env, AstNode statement) {
		EnvASTVisitor visitor = new EnvASTVisitor(env);
		
		if(statement instanceof AstRoot) {
			/* This is the root. Nothing should be flagged. */
			return visitor.annotations;
		}
		else if(statement instanceof FunctionNode) {
			/* This is a function declaration, so only check the parameters
			 * and the function name. */

			FunctionNode function = (FunctionNode) statement;
			for(AstNode param : function.getParams()) {
				param.visit(visitor);
			}

			Name name = function.getFunctionName();
			if(name != null) name.visit(visitor);

		}
		else if(statement != null){
			statement.visit(visitor);
		}

		return visitor.annotations;
	}

	public EnvASTVisitor(Environment env) {
		this.annotations = new HashSet<Annotation>();
		this.env = env;
	}

	@Override
	public boolean visit(AstNode node) {

		if(node instanceof Name) {

			Variable var = env.environment.get(node.toSource());
			if(var != null) {
				if(var.change.le == Change.LatticeElement.CHANGED) {
					List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
					ids.add(var);
					
					/* Distinguish between definitions and uses of variables. */
					if(node.getParent() instanceof VariableDeclaration 
							|| node.getParent() instanceof VariableInitializer
							|| node.getParent() instanceof FunctionNode) {
						this.annotations.add(new Annotation("ENV-DEF", ids, node.getLineno(), node.getFixedPosition(), node.getLength()));
					}
					else {
						this.annotations.add(new Annotation("ENV-USE", ids, node.getLineno(), node.getFixedPosition(), node.getLength()));
					}
					
				}
			}

		}
		/* Inspect the expression part of an object property. */
		else if(node instanceof ObjectProperty) {
			ObjectProperty op = (ObjectProperty) node;
			op.getRight().visit(this);
			return false;
		}
		/* Inspect the variable part of a property access. */
		if(node instanceof PropertyGet) {
			PropertyGet pg = (PropertyGet) node;
			pg.getLeft().visit(this);
			return false;
		}
		/* Ignore the body of loops, ifs and functions. */
		else if(node instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement) node;
			ifStatement.getCondition().visit(this);
			return false;
		}
		else if(node instanceof WhileLoop) {
			WhileLoop whileLoop = (WhileLoop) node;
			whileLoop.getCondition().visit(this);
			return false;
		}
		else if(node instanceof ForLoop) {
			ForLoop loop = (ForLoop) node;
			loop.getCondition().visit(this);
			loop.getInitializer().visit(this);
			loop.getIncrement().visit(this);
			return false;
		}
		else if(node instanceof ForInLoop) {
			ForInLoop loop = (ForInLoop) node;
			loop.getIteratedObject().visit(this);
			loop.getIterator().visit(this);
			return false;
		}
		else if(node instanceof DoLoop) {
			DoLoop loop = (DoLoop) node;
			loop.getCondition().visit(this);
			return false;
		}
		else if(node instanceof WithStatement) {
			WithStatement with = (WithStatement) node;
			with.getExpression().visit(this);
			return false;
		}
		else if(node instanceof TryStatement || node instanceof FunctionNode) {
			return false;
		}

		return true;

	}

}