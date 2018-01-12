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
 * Classes that implement this interface will use one specific parser and
 * will therefore have an implementation for a specific AST.
 */
public interface ICFGFactory {

	/**
	 * Builds intra-procedural control flow graphs for the given artifact.
	 * @param root The class or script to build CFGs for.
	 * @param astClassifier Generates unique IDs for AST nodes.
	 * @return One CFG for each function in the class or script.
	 */
	List<CFG> createCFGs(ClassifiedASTNode root);

	/**
	 * @param The file extension of the file that needs to be parsed.
	 * @return The GumTree Tree generator (AST parser) for the file extension.
	 */
	TreeGenerator getTreeGenerator(String extension);

	/**
	 * @param extension The source code file extension.
	 * @return true if the CFGFactory accepts the type of source code file
	 * specified by the extension.
	 */
	boolean acceptsExtension(String extension);

	/**
	 * Helper function for counting the number of incoming edges for each
	 * {@code CFGNode} and labeling outgoing edges that loop back to the
	 * {@code CFGNode} which they exit from.
	 * @param cfg The CFG to count and update. It should not have had its
	 * 			  incoming edges counted yet.
	 */
	static void labelIncommingEdgesAndLoopEdges(CFG cfg) {

		/* The nodes which have already visited in the graph traversal. */
		Set<CFGNode> visited = new HashSet<CFGNode>();

		/* The current path in the graph traversal. */
		Stack<CFGNode> path = new Stack<CFGNode>();

		/* The nodes that have yet to be visited. */
		Stack<CFGNode> stack = new Stack<CFGNode>();

		/* Traverse the CFG. */
		stack.push(cfg.getEntryNode());
		while(!stack.isEmpty()) {

			CFGNode current = stack.pop();
			path.push(current);

			/* Traverse each edge leaving the node. */
			for(CFGEdge edge : current.getOutgoingEdges()) {

				edge.getTo().addIncommingEdge(edge);

				if(!visited.contains(edge.getTo())) {
					stack.push(edge.getTo());
					visited.add(edge.getTo());
				}
				else {
					/* Pop nodes off the current path until we get to the
					 * parent of the next node. */
					while(!path.isEmpty() && !stack.isEmpty()
							&& !path.peek().getAdjacentNodes().contains(stack.peek())) {
						path.pop();
					}

					/* Find the loop edge and label. */
					CFGNode next = edge.getTo();
					for(CFGEdge loopEdge : next.getOutgoingEdges()) {
						if(path.contains(loopEdge.getTo())) loopEdge.isLoopEdge = true;
					}
				}

			}

		}

	}

}