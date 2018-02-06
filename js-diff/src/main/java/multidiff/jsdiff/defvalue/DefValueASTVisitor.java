package multidiff.jsdiff.defvalue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.WhileLoop;
import org.mozilla.javascript.ast.WithStatement;

import multidiffplus.commit.DependencyIdentifier;
import multidiffplus.commit.GenericDependencyIdentifier;
import multidiffplus.facts.Annotation;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Obj;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Variable;
import multidiffplus.jsanalysis.transfer.ExpEval;

public class DefValueASTVisitor implements NodeVisitor {

	/**
	 * The set of changed variable annotations found in the statement.
	 **/
	public Set<Annotation> annotations;
	
	/** The abstract state. **/
	private State state;

	/**
	 * Detects uses of the identifier.
	 * @return the set of nodes where the identifier is used.
	 */
	public static Set<Annotation> getAnnotations(State state, AstNode statement) {
		DefValueASTVisitor visitor = new DefValueASTVisitor(state);
		
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

			/* Register a value-def for all functions. */
			List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
			ids.add(new GenericDependencyIdentifier(function.getID()));
			visitor.annotations.add(new Annotation("DVAL-DEF", ids, function.getLineno(), function.getFixedPosition(), 8));

		}
		else if(statement != null){
			statement.visit(visitor);
		}

		return visitor.annotations;
	}

	public DefValueASTVisitor(State state) {
		this.annotations = new HashSet<Annotation>();
		this.state = state;
	}

	@Override
	public boolean visit(AstNode node) {
		
		/* If this node is a dummy definer, register an annotation. */
		if(node.isDummy()) {

			List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
			ids.add(new GenericDependencyIdentifier(node.getID()));

			this.annotations.add(new Annotation("DVAL-DEF", ids, node.getLineno(), node.getFixedPosition(), node.getLength()));

		}
		
		/* Inspect variables and properties that use changed values. */
		if(node instanceof Name) {

			Variable var = state.env.environment.get(node.toSource());
			if(var != null) {
				
				BValue val = state.store.apply(var.addresses);
				List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
				ids.add(val);

				this.annotations.add(new Annotation("DVAL-USE", ids, node.getLineno(), node.getFixedPosition(), node.getLength()));
				
			}

		}
		else if(node instanceof PropertyGet) {
			
			PropertyGet pg = (PropertyGet) node;
			
			/* Try to resolve the full property get. */
			ExpEval expEval = new ExpEval(state);
			List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
			for(Address addr : expEval.resolveOrCreate(node)) {
				BValue val = state.store.apply(addr);
				ids.add(val);
			}

			if(ids.size() > 0) {
				this.annotations.add(new Annotation("DVAL-USE", ids, 
						pg.getRight().getLineno(), 
						pg.getRight().getFixedPosition(), 
						pg.getRight().getLength()));
			}
			
			/* Visit the left hand side in case any objects have changed. */
			pg.getLeft().visit(this);

			return false;

		}
		else if(node instanceof ElementGet) {
			
			ElementGet pg = (ElementGet) node;
			
			/* Try to resolve the full property get. */
			ExpEval expEval = new ExpEval(state);
			List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
			for(Address addr : expEval.resolveOrCreate(node)) {
				BValue val = state.store.apply(addr);
				ids.add(val);
			}

			if(ids.size() > 0) {
				this.annotations.add(new Annotation("DVAL-USE", ids, 
						pg.getLineno(), 
						pg.getFixedPosition(), 
						pg.getLength()));
			}
			
			/* Visit the left hand side in case any objects have changed. */
			pg.getTarget().visit(this);
			pg.getElement().visit(this);

			return false;

		}
		else if(node instanceof ObjectProperty) {
			
			ObjectProperty op = (ObjectProperty)node;
			ObjectLiteral ol = (ObjectLiteral)op.getParent();
			
			String propName = null;
			AstNode prop = op.getLeft();
			if(!(prop instanceof Name)) return true;
			propName = prop.toSource();
			
			/* Get the object from the store, using the  address re-constructed from
			* Trace. */
			Address objAddr = state.trace.makeAddr(ol.getID(), "");
			Obj obj = state.store.getObj(objAddr);
			
			Address propAddr = obj.apply(propName);
			BValue val = state.store.apply(propAddr);

			List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
			ids.add(val);
			this.annotations.add(new Annotation("DVAL-USE", ids, 
					prop.getLineno(), 
					prop.getFixedPosition(), 
					prop.getLength()));
			
			op.getRight().visit(this);
			
			return false;
			
		}
		/* Register DVAL-DEF annotations for  literals. */
		else if(node instanceof KeywordLiteral) {
			KeywordLiteral kwl = (KeywordLiteral)node;

			switch(kwl.getType()) {
			case Token.NULL:
			case Token.TRUE:
			case Token.FALSE:
				List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
				ids.add(new GenericDependencyIdentifier(kwl.getID()));
				this.annotations.add(new Annotation("DVAL-DEF", ids, kwl.getLineno(), kwl.getFixedPosition(), kwl.getLength()));
			}
				
		}
		else if(node instanceof NumberLiteral 
				|| node instanceof StringLiteral) {
			List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
			ids.add(new GenericDependencyIdentifier(node.getID()));
			this.annotations.add(new Annotation("DVAL-DEF", ids, node.getLineno(), node.getFixedPosition(), node.getLength()));
		}
		else if(node instanceof ObjectLiteral
				|| node instanceof ArrayLiteral) {
			List<DependencyIdentifier> ids = new LinkedList<DependencyIdentifier>();
			ids.add(new GenericDependencyIdentifier(node.getID()));
			this.annotations.add(new Annotation("DVAL-DEF", ids, node.getLineno(), node.getFixedPosition(), 1));
			this.annotations.add(new Annotation("DVAL-DEF", ids, node.getLineno(), node.getFixedPosition() + node.getLength() - 1, 1));
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
		else if(node instanceof FunctionNode) {
			return false;
		}
		else if(node instanceof TryStatement) {
			return false;
		}

		return true;

	}

}