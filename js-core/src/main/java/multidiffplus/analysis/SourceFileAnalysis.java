package multidiffplus.analysis;

import java.util.List;

import ca.ubc.ece.salt.gumtree.ast.ClassifiedASTNode;
import multidiffplus.cfg.CFG;
import multidiffplus.commit.SourceCodeFileChange;

/**
 * Gathers Datalog facts about changes to a source code file. This class should
 * be extended to analyze a file in a particular language.
 */
public abstract class SourceFileAnalysis {

	/**
	 * Perform a single-file analysis.
	 * @param sourceCodeFileChange The source code file change information.
	 * @param root The script.
	 * @param cfgs The list of CFGs in the script (one for each function plus
	 * 			   one for the script).
	 */
	public abstract void analyze(SourceCodeFileChange sourceCodeFileChange,
								 ClassifiedASTNode root,
								 List<CFG> cfgs) throws Exception;

}