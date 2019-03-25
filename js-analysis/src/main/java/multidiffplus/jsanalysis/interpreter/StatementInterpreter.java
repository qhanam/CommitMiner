package multidiffplus.jsanalysis.interpreter;

import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.CfgMap;
import multidiffplus.jsanalysis.abstractdomain.Address;
import multidiffplus.jsanalysis.abstractdomain.BValue;
import multidiffplus.jsanalysis.abstractdomain.Change;
import multidiffplus.jsanalysis.abstractdomain.Dependencies;
import multidiffplus.jsanalysis.abstractdomain.State;
import multidiffplus.jsanalysis.abstractdomain.Undefined;
import multidiffplus.jsanalysis.abstractdomain.Variable;

/**
 * An interpreter for transferring the analysis state over a statement.
 */
public class StatementInterpreter {

    private State state;
    private ExpEval expEval;

    private StatementInterpreter(State state, CfgMap cfgs) {
	this.state = state;
	this.expEval = new ExpEval(state, cfgs);
    }

    /**
     * Performs an abstract interpretation on the node.
     */
    private void interpret(AstNode node) {

	// Add control dependencies.
	state.control.getCondition().conditions.forEach(condition -> {
	    condition.getChange().getDependencies().getDependencies().forEach(criterion -> {
		node.addDependency(criterion.getType().toString(), criterion.getId());
	    });
	});

	switch (node.getType()) {
	case Token.EMPTY:
	    break;
	case Token.EXPR_VOID:
	case Token.EXPR_RESULT:
	    expEval.eval(((ExpressionStatement) node).getExpression());
	    break;
	case Token.VAR:
	case Token.LET:
	case Token.CONST:
	    interpretVariableDeclaration((VariableDeclaration) node);
	    break;
	case Token.RETURN:
	    interpretReturn((ReturnStatement) node);
	    break;
	case Token.THROW:
	    interpretThrow((ThrowStatement) node);
	    break;
	}

    }

    /**
     * Evaluates the return expression and stores the value in the scratchpad for
     * use by the caller.
     */
    private void interpretReturn(ReturnStatement rs) {

	BValue retVal = null;

	/* Evaluate the return value from the return expression. */
	if (rs.getReturnValue() == null) {
	    retVal = Undefined.inject(Undefined.top(),
		    Change.convU(rs, Dependencies::injectValueChange),
		    Dependencies.injectValue(rs));
	} else {
	    retVal = expEval.eval(rs.getReturnValue());
	}

	/* Join the values if a return value already exists on the path. */
	BValue oldVal = state.scratch.applyReturn();
	if (oldVal != null)
	    retVal = retVal.join(oldVal);

	// Make a fake var in the environment and point it to the value so that
	// if it contains a function, it will be analyzed during the 'accessible
	// function' phase of the analysis.
	Address address = state.trace.makeAddr(rs.getID(), "");
	String name = "~retval~";
	state.env = state.env.strongUpdate(name,
		Variable.inject(name, address, Change.bottom(), Dependencies.injectVariable(rs)));
	state.store = state.store.alloc(address, retVal, new Name());

	/* Update the return value on the scratchpad. */
	state.scratch = state.scratch.strongUpdate(retVal);

    }

    /**
     * Evaluates the thrown expression. Error paths are not tracked, so we don't
     * need to do anything with scratch.
     */
    private void interpretThrow(ThrowStatement ts) {
	expEval.eval(ts.getExpression());
    }

    /**
     * Updates the store based on abstract interpretation of assignments.
     * 
     * @param vd
     *            The variable declaration. Variables have already been lifted into
     *            the environment.
     */
    private void interpretVariableDeclaration(VariableDeclaration vd) {
	for (VariableInitializer vi : vd.getVariables()) {
	    if (vi.getInitializer() != null) {
		expEval.evalAssignment(vi.getTarget(), vi.getInitializer());
	    }
	}
    }

    /**
     * Updates the {@code state} and {@code callStack} by interpreting
     * {@code statement}.
     */
    public static void interpret(CFGNode node, State state, CfgMap cfgs) {
	StatementInterpreter interpreter = new StatementInterpreter(state, cfgs);
	interpreter.interpret((AstNode) node.getStatement());
    }

}
