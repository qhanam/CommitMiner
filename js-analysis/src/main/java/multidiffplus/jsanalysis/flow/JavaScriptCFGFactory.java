package multidiffplus.jsanalysis.flow;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.ScriptNode;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;
import org.mozilla.javascript.ast.ThrowStatement;
import org.mozilla.javascript.ast.TryStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.WhileLoop;
import org.mozilla.javascript.ast.WithStatement;

import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.js.RhinoTreeGenerator;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode.ChangeType;
import multidiff.analysis.flow.Analysis;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;
import multidiffplus.cfg.CfgMap;
import multidiffplus.cfg.IState;
import multidiffplus.cfg.IdGen;
import multidiffplus.factories.ICFGFactory;

/**
 * A CFG factory for JavaScript NOTE: This class only works with the Mozilla
 * Rhino AST.
 */
public class JavaScriptCFGFactory implements ICFGFactory {

    @Override
    public Analysis createAnalysis(ClassifiedASTNode root) {
	CfgMap cfgMap = createCFGs(root);
	IState initialState = JavaScriptAnalysisState.initializeScriptState(root, cfgMap);
	CFG entryPoint = cfgMap.getCfgFor(root);
	return new JavaScriptAnalysis(entryPoint, cfgMap, initialState);
    }

    @Override
    public CfgMap createCFGs(ClassifiedASTNode root) {
	if (!(root instanceof AstRoot))
	    throw new IllegalArgumentException("The AST must be parsed from Apache Rhino.");
	AstRoot script = (AstRoot) root;

	CFGBuilder builder = new CFGBuilder(script);
	return builder.getCfgMap();
    }

    @Override
    public TreeGenerator getTreeGenerator(String extension) {
	if (acceptsExtension(extension))
	    return new RhinoTreeGenerator();
	return null;
    }

    @Override
    public boolean acceptsExtension(String extension) {
	return extension.equals("js");
    }

    private class CFGBuilder {

	/* Generates IDs for CFG nodes and edges. */
	private IdGen idgen;

	/* Stores the CFGs from all the functions. */
	private CfgMap cfgMap;

	/* Stores the un-built `FunctionNode`s. */
	private Queue<FunctionNode> unbuilt;

	/**
	 * @param root
	 *            the script.
	 */
	public CFGBuilder(AstRoot root) {
	    cfgMap = new CfgMap();
	    idgen = new IdGen();
	    unbuilt = new LinkedList<FunctionNode>();
	    build(root);
	}

	/**
	 * @return The list of CFGs for the script.
	 */
	public CfgMap getCfgMap() {
	    return cfgMap;
	}

	private void build(AstRoot script) {

	    /* Start by getting the CFG for the script. */
	    cfgMap.addCfg(script, buildScriptCFG(script, idgen));

	    /* Get the list of functions in the script. */
	    for (FunctionNode function : FunctionVisitor.getFunctions(script)) {
		unbuilt.add(function);
	    }

	    /* For each function, generate its CFG. */
	    while (!unbuilt.isEmpty()) {
		FunctionNode nextFunction = unbuilt.remove();
		cfgMap.addCfg(nextFunction, buildScriptCFG(nextFunction, idgen));
	    }

	}

	/**
	 * Builds a CFG for a function or script.
	 * 
	 * @param scriptNode
	 *            An ASTRoot node or FunctionNode.
	 * @param astClassifier
	 *            Generates unique IDs for inserted statements.
	 * @return The complete CFG.
	 */
	private CFG buildScriptCFG(ScriptNode scriptNode, IdGen idgen) {

	    String name = "FUNCTION";
	    if (scriptNode instanceof AstRoot)
		name = "SCRIPT";

	    /*
	     * Start by getting the CFG for the script. There is one entry point and one
	     * exit point for a script and function.
	     */

	    CFGNode scriptEntry = new CFGNode(scriptNode, name + "_ENTRY", idgen.getUniqueID());
	    CFGNode scriptExit = new CFGNode(new EmptyStatement(), name + "_EXIT",
		    idgen.getUniqueID());

	    /* Build the CFG for the script. */
	    CFG cfg = new CFG(scriptEntry);
	    cfg.addExitNode(scriptExit);

	    /* Build the CFG subgraph for the script body. */
	    CFG subGraph = build(scriptNode, idgen);

	    if (subGraph == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		subGraph = new CFG(empty);
		subGraph.addExitNode(empty);
	    }

	    /* The next node in the graph is first node of the subgraph. */
	    scriptEntry.addOutgoingEdge(null, subGraph.getEntryNode(), idgen.getUniqueID());

	    /* Merge the subgraph's exit nodes into the script exit node. */
	    for (CFGNode exitNode : subGraph.getExitNodes()) {
		exitNode.addOutgoingEdge(null, scriptExit, idgen.getUniqueID());
	    }

	    /* The return nodes should point to the function exit. */
	    for (CFGNode returnNode : subGraph.getReturnNodes()) {
		returnNode.addOutgoingEdge(null, scriptExit, idgen.getUniqueID());
	    }

	    /* Count the number of incoming edges for each CFGNode. */
	    ICFGFactory.addIncomingAndDetectLoops(cfg);

	    return cfg;

	}

	/**
	 * Builds a CFG for a block.
	 * 
	 * @param block
	 *            The block statement.
	 */
	private CFG build(Block block, IdGen idgen) {
	    return buildBlock(block, idgen);
	}

	/**
	 * Builds a CFG for a block.
	 * 
	 * @param block
	 *            The block statement.
	 */
	private CFG build(Scope scope, IdGen idgen) {
	    return buildBlock(scope, idgen);
	}

	/**
	 * Builds a CFG for a script or function.
	 * 
	 * @param script
	 *            The block statement ({@code AstRoot} or {@code FunctionNode}).
	 */
	private CFG build(ScriptNode script, IdGen idgen) {
	    if (script instanceof AstRoot) {
		return buildBlock(script, idgen);
	    }
	    return buildSwitch(((FunctionNode) script).getBody(), idgen);
	}

	/**
	 * Builds a CFG for a block, function or script.
	 * 
	 * @param block
	 * @return The CFG for the block.
	 */
	private CFG buildBlock(Iterable<Node> block, IdGen idgen) {
	    /*
	     * Special cases: - First statement in block (set entry point for the CFG and
	     * won't need to merge previous into it). - Last statement: The exit nodes for
	     * the block will be the same as the exit nodes for this statement.
	     */

	    CFG cfg = null;
	    CFG previous = null;

	    for (Node statement : block) {

		assert (statement instanceof AstNode);

		CFG subGraph = buildSwitch((AstNode) statement, idgen);

		if (subGraph != null) {

		    if (previous == null) {
			/* The first subgraph we find is the entry point to this graph. */
			cfg = new CFG(subGraph.getEntryNode());
		    } else {
			/* Merge the previous subgraph into the entry point of this subgraph. */
			for (CFGNode exitNode : previous.getExitNodes()) {
			    exitNode.addOutgoingEdge(null, subGraph.getEntryNode(),
				    idgen.getUniqueID());
			}
		    }

		    /* Propagate return, continue, break and throw nodes. */
		    cfg.addAllReturnNodes(subGraph.getReturnNodes());
		    cfg.addAllBreakNodes(subGraph.getBreakNodes());
		    cfg.addAllContinueNodes(subGraph.getContinueNodes());
		    cfg.addAllThrowNodes(subGraph.getThrowNodes());

		    previous = subGraph;
		}

	    }

	    if (previous != null) {

		/* Propagate exit nodes from the last node in the block. */
		cfg.addAllExitNodes(previous.getExitNodes());
	    } else {
		assert (cfg == null);
	    }

	    return cfg;
	}

	/**
	 * Builds a control flow subgraph for an if statement.
	 * 
	 * @param ifStatement
	 * @return
	 */
	private CFG build(IfStatement ifStatement, IdGen idgen) {

	    CFGNode ifNode = new CFGNode(new EmptyStatement(), "IF", idgen.getUniqueID());
	    CFG cfg = new CFG(ifNode);

	    /* Build the true branch. */

	    CFG trueBranch = buildSwitch(ifStatement.getThenPart(), idgen);

	    if (trueBranch == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		trueBranch = new CFG(empty);
		trueBranch.addExitNode(empty);
	    }

	    ifNode.addOutgoingEdge(new CFGEdge(ifStatement.getCondition(), ifNode,
		    trueBranch.getEntryNode(), idgen.getUniqueID()));

	    /* Propagate exit, return, continue, break and throw nodes. */
	    cfg.addAllExitNodes(trueBranch.getExitNodes());
	    cfg.addAllReturnNodes(trueBranch.getReturnNodes());
	    cfg.addAllBreakNodes(trueBranch.getBreakNodes());
	    cfg.addAllContinueNodes(trueBranch.getContinueNodes());
	    cfg.addAllThrowNodes(trueBranch.getThrowNodes());

	    /* Build the false branch. */

	    CFG falseBranch = buildSwitch(ifStatement.getElsePart(), idgen);

	    if (falseBranch == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		falseBranch = new CFG(empty);
		falseBranch.addExitNode(empty);
	    }

	    /*
	     * The false branch condition is the negation of the true branch condition. We
	     * give it the same change type label as the true branch condition.
	     */
	    ParenthesizedExpression pe = new ParenthesizedExpression();
	    pe.setExpression(ifStatement.getCondition().clone(pe));
	    for (FunctionNode function : FunctionVisitor.getFunctions(pe)) {
		unbuilt.add(function);
	    }
	    AstNode falseBranchCondition = new UnaryExpression(Token.NOT, 0, pe);
	    falseBranchCondition.setParent(ifStatement);
	    falseBranchCondition.setChangeType(ifStatement.getCondition().getChangeType());

	    ifNode.addOutgoingEdge(new CFGEdge(falseBranchCondition, ifNode,
		    falseBranch.getEntryNode(), idgen.getUniqueID()));

	    /* Propagate exit, return, continue and break nodes. */
	    cfg.addAllExitNodes(falseBranch.getExitNodes());
	    cfg.addAllReturnNodes(falseBranch.getReturnNodes());
	    cfg.addAllBreakNodes(falseBranch.getBreakNodes());
	    cfg.addAllContinueNodes(falseBranch.getContinueNodes());
	    cfg.addAllThrowNodes(falseBranch.getThrowNodes());

	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a while statement.
	 * 
	 * @param whileLoop
	 * @return The CFG for the while loop.
	 */
	private CFG build(WhileLoop whileLoop, IdGen idgen) {

	    CFGNode whileNode = new CFGNode(new EmptyStatement(), "WHILE", idgen.getUniqueID());
	    CFG cfg = new CFG(whileNode);

	    /* Build the true branch. */

	    CFG trueBranch = buildSwitch(whileLoop.getBody(), idgen);

	    if (trueBranch == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		trueBranch = new CFG(empty);
		trueBranch.addExitNode(empty);
	    }

	    whileNode.addOutgoingEdge(new CFGEdge(whileLoop.getCondition(), whileNode,
		    trueBranch.getEntryNode(), true, idgen.getUniqueID()));

	    /* Propagate return and throw nodes. */
	    cfg.addAllReturnNodes(trueBranch.getReturnNodes());
	    cfg.addAllThrowNodes(trueBranch.getThrowNodes());

	    /* The break nodes are exit nodes for this loop. */
	    cfg.addAllExitNodes(trueBranch.getBreakNodes());

	    /* Exit nodes point back to the start of the loop. */
	    for (CFGNode exitNode : trueBranch.getExitNodes()) {
		exitNode.addOutgoingEdge(null, whileNode, idgen.getUniqueID());
	    }

	    /* Continue nodes point back to the start of the loop. */
	    for (CFGNode continueNode : trueBranch.getContinueNodes()) {
		continueNode.addOutgoingEdge(null, whileNode, idgen.getUniqueID());
	    }

	    /* Build the false branch. */

	    /*
	     * The false branch condition is the negation of the true branch condition. We
	     * give it the same change type label as the true branch condition.
	     */
	    ParenthesizedExpression pe = new ParenthesizedExpression();
	    pe.setExpression(whileLoop.getCondition().clone(pe));
	    AstNode falseBranchCondition = new UnaryExpression(Token.NOT, 0, pe);
	    falseBranchCondition.setChangeType(whileLoop.getCondition().getChangeType());
	    falseBranchCondition.setParent(whileLoop);

	    CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
	    whileNode.addOutgoingEdge(
		    new CFGEdge(falseBranchCondition, whileNode, empty, idgen.getUniqueID()));
	    cfg.addExitNode(empty);

	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a do loop.
	 * 
	 * @param doLoop
	 * @return The CFG for the do loop.
	 */
	private CFG build(DoLoop doLoop, IdGen idgen) {

	    CFGNode doNode = new CFGNode(new EmptyStatement(), "DO", idgen.getUniqueID());
	    CFGNode whileNode = new CFGNode(new EmptyStatement(), "WHILE", idgen.getUniqueID());
	    CFG cfg = new CFG(doNode);

	    /* Build the loop branch. */

	    CFG loopBranch = buildSwitch(doLoop.getBody(), idgen);

	    if (loopBranch == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		loopBranch = new CFG(empty);
		loopBranch.addExitNode(empty);
	    }

	    /* We always execute the do block at least once. */
	    doNode.addOutgoingEdge(null, loopBranch.getEntryNode(), idgen.getUniqueID());

	    /* Add edges from exit nodes from the loop to the while node. */
	    for (CFGNode exitNode : loopBranch.getExitNodes()) {
		exitNode.addOutgoingEdge(null, whileNode, idgen.getUniqueID());
	    }

	    /* Propagate return and throw nodes. */
	    cfg.addAllReturnNodes(loopBranch.getReturnNodes());
	    cfg.addAllThrowNodes(loopBranch.getThrowNodes());

	    /* The break nodes are exit nodes for this loop. */
	    cfg.addAllExitNodes(loopBranch.getBreakNodes());

	    /* Continue nodes have edges to the while condition. */
	    for (CFGNode continueNode : loopBranch.getContinueNodes()) {
		continueNode.addOutgoingEdge(null, whileNode, idgen.getUniqueID());
	    }

	    /* Add edge for true condition back to the start of the loop. */
	    whileNode.addOutgoingEdge(doLoop.getCondition(), doNode, true, idgen.getUniqueID());

	    /* Add edge for false condition. */

	    /*
	     * The false branch condition is the negation of the true branch condition. We
	     * give it the same change type label as the true branch condition.
	     */
	    ParenthesizedExpression pe = new ParenthesizedExpression();
	    pe.setExpression(doLoop.getCondition().clone(pe));
	    AstNode falseBranchCondition = new UnaryExpression(Token.NOT, 0, pe);
	    falseBranchCondition.setChangeType(doLoop.getCondition().getChangeType());
	    falseBranchCondition.setParent(doLoop);

	    CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
	    whileNode.addOutgoingEdge(falseBranchCondition, empty, idgen.getUniqueID());
	    cfg.addExitNode(empty);

	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a for statement. A for statement is simply
	 * a while statement with an expression before and after the loop body.
	 * 
	 * @param forLoop
	 * @return The CFG for the for loop.
	 */
	private CFG build(ForLoop forLoop, IdGen idgen) {

	    CFGNode forNode = new CFGNode(forLoop.getInitializer(), idgen.getUniqueID());
	    CFG cfg = new CFG(forNode);

	    /* After variables are declared, add an empty node with two edges. */
	    CFGNode condition = new CFGNode(new EmptyStatement(), "FOR", idgen.getUniqueID());
	    forNode.addOutgoingEdge(null, condition, idgen.getUniqueID());

	    /*
	     * After the body of the loop executes, add the node to perform the increment.
	     */
	    CFGNode increment = new CFGNode(forLoop.getIncrement(), idgen.getUniqueID());
	    increment.addOutgoingEdge(null, condition, idgen.getUniqueID());

	    /* Build the true branch. */

	    CFG trueBranch = buildSwitch(forLoop.getBody(), idgen);

	    if (trueBranch == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		trueBranch = new CFG(empty);
		trueBranch.addExitNode(empty);
	    }

	    condition.addOutgoingEdge(forLoop.getCondition(), trueBranch.getEntryNode(), true,
		    idgen.getUniqueID());

	    /* Propagate return and throw nodes. */
	    cfg.addAllReturnNodes(trueBranch.getReturnNodes());
	    cfg.addAllThrowNodes(trueBranch.getThrowNodes());

	    /* The break nodes are exit nodes for this loop. */
	    cfg.addAllExitNodes(trueBranch.getBreakNodes());

	    /* Exit nodes point to the increment node. */
	    for (CFGNode exitNode : trueBranch.getExitNodes()) {
		exitNode.addOutgoingEdge(null, increment, idgen.getUniqueID());
	    }

	    /* Continue nodes point to the increment. */
	    for (CFGNode continueNode : trueBranch.getContinueNodes()) {
		continueNode.addOutgoingEdge(null, increment, idgen.getUniqueID());
	    }

	    /* Build the false branch. */

	    /*
	     * The false branch condition is the negation of the true branch condition. We
	     * give it the same change type label as the true branch condition.
	     */
	    ParenthesizedExpression pe = new ParenthesizedExpression();
	    pe.setExpression(forLoop.getCondition().clone(pe));
	    AstNode falseBranchCondition = new UnaryExpression(Token.NOT, 0, pe);
	    falseBranchCondition.setChangeType(forLoop.getCondition().getChangeType());
	    falseBranchCondition.setParent(forLoop);

	    CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
	    condition.addOutgoingEdge(falseBranchCondition, empty, idgen.getUniqueID());
	    cfg.addExitNode(empty);

	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a for in statement. A for in statement is
	 * a loop that iterates over the keys of an object. The Rhino IR represents this
	 * using the Node labeled "ENUM_INIT_KEYS". Here, we make a fake function that
	 * returns an object's keys.
	 * 
	 * @param forInLoop
	 * @return The CFG for the for-in loop.
	 */
	private CFG build(ForInLoop forInLoop, IdGen idgen) {

	    /*
	     * To represent key iteration, we make up two functions:
	     *
	     * ~getNextKey() - iterates through each key in an object. ~hasNextKey() - true
	     * if there is another key to iterate.
	     *
	     * These names are invalid in JavaScript to ensure that there isn't another
	     * function with the same name. Since we're not producing code, this is ok.
	     */

	    /* Start with the variable declaration. */
	    AstNode iterator = forInLoop.getIterator();
	    CFGNode forInNode = new CFGNode(iterator, idgen.getUniqueID());
	    CFG cfg = new CFG(forInNode);

	    /* Get the variable being assigned. */
	    AstNode target;
	    if (iterator instanceof VariableDeclaration) {
		target = ((VariableDeclaration) iterator).getVariables().get(0).getTarget();
	    } else if (iterator instanceof Name) {
		target = iterator;
	    } else {
		target = new Name(0, "~error~");
	    }

	    /*
	     * Create the node that gets the next key in an object and assigns the value to
	     * the iterator variable.
	     */

	    Name getNextKey = new Name(0, "~getNextKey");
	    getNextKey.setChangeType(iterator.getChangeType());
	    getNextKey.setVersion(forInLoop.getVersion());
	    PropertyGet keyIteratorMethod = new PropertyGet(forInLoop.getIteratedObject(),
		    getNextKey);
	    keyIteratorMethod.setChangeType(iterator.getChangeType());
	    keyIteratorMethod.setVersion(forInLoop.getVersion());
	    FunctionCall keyIteratorFunction = new FunctionCall();
	    keyIteratorFunction.setTarget(keyIteratorMethod);
	    keyIteratorFunction.setChangeType(iterator.getChangeType());
	    keyIteratorFunction.setVersion(forInLoop.getVersion());
	    Assignment targetAssignment = new Assignment(target, keyIteratorFunction);
	    targetAssignment.setType(Token.ASSIGN);
	    targetAssignment.setChangeType(target.getChangeType());
	    targetAssignment.setVersion(forInLoop.getVersion());

	    CFGNode assignment = new CFGNode(targetAssignment, idgen.getUniqueID());

	    /*
	     * Create the the condition that checks if an object still has keys. The
	     * condition is assigned to the true/false loop branches.
	     */

	    Name hasNextKey = new Name(0, "~hasNextKey");
	    PropertyGet keyConditionMethod = new PropertyGet(forInLoop.getIteratedObject(),
		    hasNextKey);
	    keyConditionMethod.setChangeType(iterator.getChangeType());
	    keyConditionMethod.setVersion(forInLoop.getVersion());
	    FunctionCall keyConditionFunction = new FunctionCall();
	    keyConditionFunction.setTarget(keyConditionMethod);
	    keyConditionFunction.setChangeType(iterator.getChangeType());
	    keyConditionFunction.setVersion(forInLoop.getVersion());
	    keyConditionFunction.setParent(forInLoop);

	    CFGNode condition = new CFGNode(new EmptyStatement(), "FORIN", idgen.getUniqueID());

	    /*
	     * Add the edges connecting the entry point to the assignment and assignment to
	     * condition.
	     */
	    forInNode.addOutgoingEdge(null, condition, idgen.getUniqueID());
	    condition.addOutgoingEdge(new CFGEdge(keyConditionFunction, condition, assignment, true,
		    idgen.getUniqueID()));

	    /* Create the CFG for the loop body. */

	    CFG trueBranch = buildSwitch(forInLoop.getBody(), idgen);

	    if (trueBranch == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		trueBranch = new CFG(empty);
		trueBranch.addExitNode(empty);
	    }

	    /* Propagate return and throw nodes. */
	    cfg.addAllReturnNodes(trueBranch.getReturnNodes());
	    cfg.addAllThrowNodes(trueBranch.getThrowNodes());

	    /* The break nodes are exit nodes for this loop. */
	    cfg.addAllExitNodes(trueBranch.getBreakNodes());

	    /* The exit nodes point back to the assignment node. */
	    for (CFGNode exitNode : trueBranch.getExitNodes()) {
		exitNode.addOutgoingEdge(null, condition, idgen.getUniqueID());
	    }

	    /* The continue nodes point back to the assignment node. */
	    for (CFGNode continueNode : trueBranch.getContinueNodes()) {
		continueNode.addOutgoingEdge(null, condition, idgen.getUniqueID());
	    }

	    /* Create a node for the false branch to exit the loop. */
	    CFGNode falseBranch = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
	    cfg.addExitNode(falseBranch);

	    /*
	     * The false branch condition is the negation of the true branch condition. We
	     * give it the same change type label as the true branch condition.
	     */
	    ParenthesizedExpression pe = new ParenthesizedExpression();
	    pe.setExpression(keyConditionFunction.clone(pe));
	    pe.setVersion(forInLoop.getVersion());
	    AstNode falseBranchCondition = new UnaryExpression(Token.NOT, 0, pe);
	    falseBranchCondition.setChangeType(keyConditionFunction.getChangeType());
	    falseBranchCondition.setParent(keyConditionFunction.getParent());
	    falseBranchCondition.setVersion(forInLoop.getVersion());
	    falseBranchCondition.setParent(forInLoop);

	    /* Add the edges from the assignment node to the start of the loop. */
	    assignment.addOutgoingEdge(null, trueBranch.getEntryNode(), idgen.getUniqueID());
	    condition.addOutgoingEdge(
		    new CFGEdge(falseBranchCondition, condition, falseBranch, idgen.getUniqueID()));

	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a switch statement. A for statement is
	 * simply a while statement with an expression before and after the loop body.
	 * 
	 * @param forLoop
	 * @return The CFG for the for loop.
	 */
	private CFG build(SwitchStatement switchStatement, IdGen idgen) {

	    CFGNode switchNode = new CFGNode(new EmptyStatement(), "SWITCH", idgen.getUniqueID());
	    CFG cfg = new CFG(switchNode);

	    /* Keep track of the default edge so we can update the condition later. */
	    CFGEdge defaultEdge = null;
	    AstNode defaultCondition = null;

	    /* Add edges for each case. */
	    List<SwitchCase> switchCases = switchStatement.getCases();
	    CFG previousSubGraph = null;
	    for (SwitchCase switchCase : switchCases) {

		/* Build the subgraph for the case. */
		CFG subGraph = null;
		if (switchCase.getStatements() != null) {
		    List<Node> statements = new LinkedList<Node>(switchCase.getStatements());
		    subGraph = buildBlock(statements, idgen);
		}

		/*
		 * If it is an empty case, make our lives easier by adding an empty statement as
		 * the entry and exit node.
		 */
		if (subGraph == null) {
		    CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		    subGraph = new CFG(empty);
		    subGraph.addExitNode(empty);
		}

		/*
		 * Add an edge from the switch to the entry node for the case. We build a
		 * comparison expression for the edge by comparing the switch expression to the
		 * case expression.
		 */
		if (switchCase.getExpression() != null) {
		    InfixExpression compare = new InfixExpression(switchStatement.getExpression(),
			    switchCase.getExpression());
		    compare.setType(Token.SHEQ);
		    switchNode.addOutgoingEdge(new CFGEdge(compare, switchNode,
			    subGraph.getEntryNode(), idgen.getUniqueID()));

		    if (defaultCondition == null) {
			defaultCondition = compare;
			defaultCondition.setChangeType(compare.getChangeType());
		    } else {
			AstNode infix = new InfixExpression(compare, defaultCondition);
			infix.setType(Token.OR);
			if (compare.getChangeType() == defaultCondition.getChangeType())
			    infix.setChangeType(compare.getChangeType());
			else
			    infix.setChangeType(ChangeType.UPDATED);
			defaultCondition = infix;
		    }

		} else {
		    defaultEdge = new CFGEdge(null, switchNode, subGraph.getEntryNode(),
			    idgen.getUniqueID());
		    switchNode.addOutgoingEdge(defaultEdge);
		}

		/* Propagate return and throw nodes. */
		cfg.addAllReturnNodes(subGraph.getReturnNodes());
		cfg.addAllThrowNodes(subGraph.getThrowNodes());

		/* Propagate continue nodes. */
		cfg.addAllContinueNodes(subGraph.getContinueNodes());

		/* The break nodes are exit nodes for the switch. */
		cfg.addAllExitNodes(subGraph.getBreakNodes());

		if (previousSubGraph != null) {

		    /*
		     * Add an edge from the exit nodes of the previous case to the entry node for
		     * this case.
		     */
		    for (CFGNode exitNode : previousSubGraph.getExitNodes()) {
			exitNode.addOutgoingEdge(null, subGraph.getEntryNode(),
				idgen.getUniqueID());
		    }

		}

		previousSubGraph = subGraph;

	    }

	    /* Setup the default path if wasn't explicitly given in the switch statement. */
	    if (defaultEdge == null) {
		CFGNode defaultPath = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		defaultEdge = new CFGEdge(null, switchNode,
			new CFGNode(new EmptyStatement(), idgen.getUniqueID()),
			idgen.getUniqueID());
		cfg.addExitNode(defaultPath);
	    }

	    /*
	     * The false branch condition is the negation of the true branch condition. We
	     * give it the same change type label as the true branch condition.
	     */
	    if (defaultCondition != null) {

		AstNode falseBranchCondition = new UnaryExpression(Token.NOT, 0,
			new ParenthesizedExpression(defaultCondition));
		falseBranchCondition.setChangeType(defaultCondition.getChangeType());
		defaultCondition = falseBranchCondition;

	    }

	    /* Add the final default condition. */
	    defaultEdge.setCondition(defaultCondition);

	    /* The rest of the exit nodes are exit nodes for the statement. */
	    cfg.addAllExitNodes(previousSubGraph.getExitNodes());

	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a with statement.
	 * 
	 * @param withStatement
	 * @return The CFG for the while loop.
	 */
	private CFG build(WithStatement withStatement, IdGen idgen) {

	    /*
	     * Create two functions to represent adding a scope: - One that loads the
	     * expression's fields and functions into the current scope. - One that unloads
	     * the expression's fields and functions from the current scope.
	     */

	    Name createScope = new Name(0, "~createScope");
	    FunctionCall createScopeFunction = new FunctionCall();
	    createScopeFunction.setTarget(createScope);
	    createScopeFunction.addArgument(withStatement.getExpression());

	    FunctionCall destroyScopeFunction = new FunctionCall();
	    destroyScopeFunction.setTarget(new Name(0, "~destroySceop"));
	    destroyScopeFunction.addArgument(withStatement.getExpression());

	    CFGNode withNode = new CFGNode(createScopeFunction, "BEGIN_SCOPE", idgen.getUniqueID());
	    CFGNode endWithNode = new CFGNode(destroyScopeFunction, "END_SCOPE",
		    idgen.getUniqueID());

	    CFG cfg = new CFG(withNode);
	    cfg.addExitNode(endWithNode);

	    CFG scopeBlock = buildSwitch(withStatement.getStatement(), idgen);

	    if (scopeBlock == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		scopeBlock = new CFG(empty);
		scopeBlock.addExitNode(empty);
	    }

	    withNode.addOutgoingEdge(null, scopeBlock.getEntryNode(), idgen.getUniqueID());

	    /* Exit nodes point to the scope destroy method. */
	    for (CFGNode exitNode : scopeBlock.getExitNodes()) {
		exitNode.addOutgoingEdge(null, endWithNode, idgen.getUniqueID());
	    }

	    /* Propagate return and throw nodes. */
	    cfg.addAllReturnNodes(scopeBlock.getReturnNodes());
	    cfg.addAllThrowNodes(scopeBlock.getThrowNodes());

	    /* Propagate break nodes. */
	    cfg.addAllBreakNodes(scopeBlock.getBreakNodes());

	    /* Propagate continue nodes. */
	    cfg.addAllContinueNodes(scopeBlock.getContinueNodes());

	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a try/catch statement.
	 *
	 * @param tryStatement
	 * @return The CFG for the while loop.
	 */
	private CFG build(TryStatement tryStatement, IdGen idgen) {

	    CFGNode tryNode = new CFGNode(new EmptyStatement(), "TRY", idgen.getUniqueID());
	    CFG cfg = new CFG(tryNode);

	    /* To make life easier, add a node that represents the exit of the try. */
	    CFGNode exit = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
	    cfg.addExitNode(exit);

	    /* Set up the finally block. */

	    CFG finallyBlock = buildSwitch(tryStatement.getFinallyBlock(), idgen);

	    if (finallyBlock == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		finallyBlock = new CFG(empty);
		finallyBlock.addExitNode(empty);
	    } else {
		/* Propagate return, break, continue and throw nodes. */
		cfg.addAllReturnNodes(finallyBlock.getReturnNodes());
		cfg.addAllBreakNodes(finallyBlock.getBreakNodes());
		cfg.addAllContinueNodes(finallyBlock.getContinueNodes());
		cfg.addAllThrowNodes(finallyBlock.getThrowNodes());

		for (CFGNode exitNode : finallyBlock.getExitNodes()) {
		    exitNode.addOutgoingEdge(null, exit, idgen.getUniqueID());
		}
	    }

	    /* Set up the try block. */

	    CFG tryBlock = buildSwitch(tryStatement.getTryBlock(), idgen);

	    if (tryBlock == null) {
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		tryBlock = new CFG(empty);
		tryBlock.addExitNode(finallyBlock.getEntryNode());
	    } else {
		/*
		 * Create empty exit nodes so there is an edge from each exit node in the
		 * finally block for the catch block.
		 */
		CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		cfg.addExitNode(empty);

		for (CFGNode exitNode : finallyBlock.getExitNodes()) {
		    exitNode.addOutgoingEdge(null, empty, idgen.getUniqueID());
		}

		/*
		 * Move the jump nodes after the finally block and propagate them through the
		 * CFG.
		 */
		cfg.addAllBreakNodes(
			moveJumpAfterFinally(finallyBlock, tryBlock.getBreakNodes(), null, idgen));
		cfg.addAllContinueNodes(moveJumpAfterFinally(finallyBlock,
			tryBlock.getContinueNodes(), null, idgen));

		List<CFGNode> returnNodes = moveJumpAfterFinally(finallyBlock,
			tryBlock.getReturnNodes(), null, idgen);
		cfg.addAllReturnNodes(returnNodes);

		/*
		 * Throw nodes point to a catch block. We assume the first because to get the
		 * correct one we need to do data flow analysis.
		 */
		for (CFGNode throwNode : tryBlock.getThrowNodes()) {
		    throwNode.addOutgoingEdge(null, finallyBlock.getEntryNode(),
			    idgen.getUniqueID());
		}

		/* Exit nodes exit to the finally block. */
		for (CFGNode exitNode : tryBlock.getExitNodes()) {
		    exitNode.addOutgoingEdge(null, finallyBlock.getEntryNode(),
			    idgen.getUniqueID());
		}
	    }

	    /* Set up the catch clauses. */

	    List<CatchClause> catchClauses = tryStatement.getCatchClauses();
	    for (CatchClause catchClause : catchClauses) {

		CFG catchBlock = buildSwitch(catchClause.getBody(), idgen);

		/* Create the clause for branching to the catch. */
		AstNode catchCondition = catchClause.getCatchCondition();
		if (catchCondition == null) {

		    /* Create a special method that contains the exception. */
		    Name n = new Name(0, "~exception");
		    FunctionCall exception = new FunctionCall();
		    List<AstNode> args = new LinkedList<AstNode>();
		    args.add(catchClause.getVarName());
		    exception.setArguments(args);
		    exception.setTarget(n);
		    exception.setChangeType(catchClause.getChangeType());
		    catchCondition = exception;

		}

		if (catchBlock == null) {
		    CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		    catchBlock = new CFG(empty);
		    catchBlock.addExitNode(empty);
		} else {

		    /*
		     * Create empty exit nodes so there is an edge from each exit node in the
		     * finally block for each clause.
		     */
		    CFGNode empty = new CFGNode(new EmptyStatement(), idgen.getUniqueID());
		    cfg.addExitNode(empty);

		    for (CFGNode exitNode : finallyBlock.getExitNodes()) {
			exitNode.addOutgoingEdge(catchCondition, empty, idgen.getUniqueID());
		    }

		    /*
		     * Move the jump nodes after the finally block and propagate them through the
		     * CFG.
		     */
		    cfg.addAllBreakNodes(moveJumpAfterFinally(finallyBlock,
			    catchBlock.getBreakNodes(), catchCondition, idgen));
		    cfg.addAllContinueNodes(moveJumpAfterFinally(finallyBlock,
			    catchBlock.getContinueNodes(), catchCondition, idgen));
		    cfg.addAllReturnNodes(moveJumpAfterFinally(finallyBlock,
			    catchBlock.getReturnNodes(), catchCondition, idgen));
		    cfg.addAllThrowNodes(moveJumpAfterFinally(finallyBlock,
			    catchBlock.getThrowNodes(), catchCondition, idgen));

		    /* Exit nodes exit to the finally block. */
		    for (CFGNode exitNode : catchBlock.getExitNodes()) {
			exitNode.addOutgoingEdge(null, finallyBlock.getEntryNode(),
				idgen.getUniqueID());
		    }

		}

		tryNode.addOutgoingEdge(catchCondition, catchBlock.getEntryNode(),
			idgen.getUniqueID());

	    }

	    tryNode.addOutgoingEdge(null, tryBlock.getEntryNode(), idgen.getUniqueID());

	    return cfg;

	}

	/**
	 * Move the jump nodes from a try or catch block to after the finally block.
	 * 
	 * @param finallyBlock
	 *            The finally block in the try statement.
	 * @param jumpNodes
	 *            The set of break, continue or return nodes.
	 * @return The set of newly created jump nodes (to be propagated to the CFG).
	 */
	private List<CFGNode> moveJumpAfterFinally(CFG finallyBlock, List<CFGNode> jumpNodes,
		AstNode condition, IdGen idgen) {

	    /* The list of newly created jump nodes to propagate to the cfg. */
	    List<CFGNode> newJumpNodes = new LinkedList<CFGNode>();

	    for (CFGNode jumpNode : jumpNodes) {

		/* Copy the finally block so that the nodes are unique. */
		CFG copyOfFinallyBlock = finallyBlock.copy(idgen);

		/* Make a shallow copy of the node. */
		CFGNode newJumpNode = CFGNode.copy(jumpNode, idgen.getUniqueID());
		newJumpNodes.add(newJumpNode);

		/* Add an edge from the finally block to the return node. */
		for (CFGNode exitNode : copyOfFinallyBlock.getExitNodes()) {
		    exitNode.getOutgoingEdges().clear();
		    exitNode.addOutgoingEdge(condition, newJumpNode, idgen.getUniqueID());
		    exitNode.getId();
		}

		/* Change the original jump node to do nothing. */
		jumpNode.setStatement(new EmptyStatement());

		/* Remove any previous edges from the jump node. */
		jumpNode.getOutgoingEdges().clear();

		/*
		 * Add an edge from the original jump node to the start of the finally block.
		 */
		jumpNode.addOutgoingEdge(null, copyOfFinallyBlock.getEntryNode(),
			idgen.getUniqueID());

	    }

	    return newJumpNodes;
	}

	/**
	 * Builds a control flow subgraph for a break statement.
	 * 
	 * @param entry
	 *            The entry point for the subgraph.
	 * @param exit
	 *            The exit point for the subgraph.
	 * @return A list of exit nodes for the subgraph.
	 */
	private CFG build(BreakStatement breakStatement, IdGen idgen) {

	    CFGNode breakNode = new CFGNode(breakStatement, idgen.getUniqueID());
	    CFG cfg = new CFG(breakNode);
	    cfg.addBreakNode(breakNode);
	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a continue statement.
	 * 
	 * @param entry
	 *            The entry point for the subgraph.
	 * @param exit
	 *            The exit point for the subgraph.
	 * @return A list of exit nodes for the subgraph.
	 */
	private CFG build(ContinueStatement continueStatement, IdGen idgen) {

	    CFGNode continueNode = new CFGNode(continueStatement, idgen.getUniqueID());
	    CFG cfg = new CFG(continueNode);
	    cfg.addContinueNode(continueNode);
	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a return statement.
	 * 
	 * @param entry
	 *            The entry point for the subgraph.
	 * @param exit
	 *            The exit point for the subgraph.
	 * @return A list of exit nodes for the subgraph.
	 */
	private CFG build(ReturnStatement returnStatement, IdGen idgen) {

	    CFGNode returnNode = new CFGNode(returnStatement, idgen.getUniqueID());
	    CFG cfg = new CFG(returnNode);
	    cfg.addReturnNode(returnNode);
	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a throw statement.
	 * 
	 * @param entry
	 *            The entry point for the subgraph.
	 * @param exit
	 *            The exit point for the subgraph.
	 * @return A list of exit nodes for the subgraph.
	 */
	private CFG build(ThrowStatement throwStatement, IdGen idgen) {

	    CFGNode throwNode = new CFGNode(throwStatement, idgen.getUniqueID());
	    CFG cfg = new CFG(throwNode);
	    cfg.addThrowNode(throwNode);
	    return cfg;

	}

	/**
	 * Builds a control flow subgraph for a statement. If other statement types are
	 * handled properly then this should only be an expression.
	 * 
	 * @param entry
	 *            The entry point for the subgraph.
	 * @param exit
	 *            The exit point for the subgraph.
	 * @return A list of exit nodes for the subgraph.
	 */
	private CFG build(AstNode statement, IdGen idgen) {

	    CFGNode expressionNode = new CFGNode(statement, idgen.getUniqueID());
	    CFG cfg = new CFG(expressionNode);
	    cfg.addExitNode(expressionNode);
	    return cfg;

	}

	/**
	 * Calls the appropriate build method for the node type.
	 */
	private CFG buildSwitch(AstNode node, IdGen idgen) {

	    if (node == null)
		return null;

	    if (node instanceof Block) {
		return build((Block) node, idgen);
	    } else if (node instanceof IfStatement) {
		return build((IfStatement) node, idgen);
	    } else if (node instanceof WhileLoop) {
		return build((WhileLoop) node, idgen);
	    } else if (node instanceof DoLoop) {
		return build((DoLoop) node, idgen);
	    } else if (node instanceof ForLoop) {
		return build((ForLoop) node, idgen);
	    } else if (node instanceof ForInLoop) {
		return build((ForInLoop) node, idgen);
	    } else if (node instanceof SwitchStatement) {
		return build((SwitchStatement) node, idgen);
	    } else if (node instanceof WithStatement) {
		return build((WithStatement) node, idgen);
	    } else if (node instanceof TryStatement) {
		return build((TryStatement) node, idgen);
	    } else if (node instanceof BreakStatement) {
		return build((BreakStatement) node, idgen);
	    } else if (node instanceof ContinueStatement) {
		return build((ContinueStatement) node, idgen);
	    } else if (node instanceof ReturnStatement) {
		return build((ReturnStatement) node, idgen);
	    } else if (node instanceof ThrowStatement) {
		return build((ThrowStatement) node, idgen);
	    } else if (node instanceof FunctionNode) {
		return null; // Function declarations shouldn't be part of the CFG.
	    } else if (node instanceof Scope) {
		return build((Scope) node, idgen);
	    } else {
		return build(node, idgen);
	    }

	}

    }

}