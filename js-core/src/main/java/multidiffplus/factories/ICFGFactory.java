package multidiffplus.factories;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.github.gumtreediff.gen.TreeGenerator;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGEdge;
import multidiffplus.cfg.CFGNode;

/**
 * Builds a CFG given some AST.
 *
 * Classes that implement this interface will use one specific parser and will
 * therefore have an implementation for a specific AST.
 */
public interface ICFGFactory {

    /**
     * Builds intra-procedural control flow graphs for the given artifact.
     * 
     * @param root
     *            The class or script to build CFGs for.
     * @param astClassifier
     *            Generates unique IDs for AST nodes.
     * @return One CFG for each function in the class or script.
     */
    List<CFG> createCFGs(ClassifiedASTNode root);

    /**
     * @param The
     *            file extension of the file that needs to be parsed.
     * @return The GumTree Tree generator (AST parser) for the file extension.
     */
    TreeGenerator getTreeGenerator(String extension);

    /**
     * @param extension
     *            The source code file extension.
     * @return true if the CFGFactory accepts the type of source code file specified
     *         by the extension.
     */
    boolean acceptsExtension(String extension);

    /**
     * Helper function for counting the number of incoming edges for each
     * {@code CFGNode} and labeling outgoing edges that loop back to the
     * {@code CFGNode} which they exit from.
     * 
     * @param cfg
     *            The CFG to count and update. It should not have had its incoming
     *            edges counted yet.
     */
    static void addIncomingAndDetectLoops(CFG cfg) {

	/* The edges which have already been visited. */
	Set<CFGEdge> visited = new HashSet<CFGEdge>();

	/* The edges to visit in a depth-first traversal. */
	Stack<CFGEdge> stack = new Stack<CFGEdge>();

	/* The edges in the current path. */
	Set<CFGEdge> path = new HashSet<CFGEdge>();

	/* The first edge in the CFG. */
	CFGEdge entryEdge = cfg.getEntryNode().getOutgoingEdges().get(0);
	stack.push(cfg.getEntryNode().getOutgoingEdges().get(0));

	path.add(entryEdge);
	addIncomingAndDetectLoops(visited, path, entryEdge);
	path.remove(entryEdge);
    }

    static void addIncomingAndDetectLoops(Set<CFGEdge> visited, Set<CFGEdge> path, CFGEdge edge) {
	CFGNode node = edge.getTo();

	// Add the incoming edge to the node it points to.
	node.addIncommingEdge(edge);

	// Do a depth first traversal.
	for (CFGEdge outEdge : node.getOutgoingEdges()) {
	    if (!visited.contains(edge)) {
		visited.add(edge);
		path.add(edge);
		addIncomingAndDetectLoops(visited, path, outEdge);
		path.remove(edge);
	    } else if (path.contains(edge)) {
		edge.isLoopEdge = true;
	    }
	}

    }

}