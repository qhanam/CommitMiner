package multidiffplus.mining.flow.analysis;

import java.util.List;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ast.AstNode;

import changeimpact.InterleavedChangeImpactAnalysis;
import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.cfg.CFG;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.diff.Diff;
import multidiffplus.diff.DiffContext;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.factories.ICFGFactory;
import multidiffplus.factories.ICFGVisitorFactory;

/**
 * Gathers change impact facts about one source code file using an intra-procedural flow analysis.
 * 
 * This improves precision with limited effect on runtime.
 */
public class MiningIntraprocDomainAnalysis extends DomainAnalysis {
	
	public List<ICFGVisitorFactory> cfgVisitorFactories;

	public MiningIntraprocDomainAnalysis(List<ICFGVisitorFactory> cfgVisitorFactories,
			ICFGFactory cfgFactory) {
		super(null, null, cfgFactory, false, true);
		this.cfgVisitorFactories = cfgVisitorFactories;
	}

	/**
	 * Performs AST-differencing and launches the analysis of the pre-commit/post-commit
	 * source code file pair.
	 *
	 * @param sourceCodeFileChange The source code file change information.
	 * @param preProcess Set to true to enable AST pre-processing.
	 * @param srcAnalysisFactory The analysis to run on the buggy file.
	 * @param dstAnalysisClass The analysis to run on the repaired file.
	 */
	protected void analyzeFile(SourceCodeFileChange sourceCodeFileChange) throws Exception {
		
		System.out.println(sourceCodeFileChange.repairedFile);
		
		/* Get the file extension. */
		String fileExtension = getSourceCodeFileExtension(sourceCodeFileChange.repairedFile);

		/* Difference the files and analyze if they are an extension we handle. */
		if(fileExtension != null && cfgFactory.acceptsExtension(fileExtension)) {
			
			/* Abort on large files. */
			if(sourceCodeFileChange.repairedCode.length() > 150000) {
				System.err.println("File too large (> 150,000 characters)");
				return;
			}

			/* AST-diff the files. */
			Diff diff = null;
			try {
				String[] args = preProcess ? new String[] {sourceCodeFileChange.buggyFile, sourceCodeFileChange.repairedFile, "-pp"}
									: new String[] {sourceCodeFileChange.buggyFile, sourceCodeFileChange.repairedFile};
				diff = new Diff(cfgFactory, args, sourceCodeFileChange.buggyCode, sourceCodeFileChange.repairedCode);
			}
			catch(ArrayIndexOutOfBoundsException e) {
				System.err.println("ArrayIndexOutOfBoundsException: possibly caused by empty file.");
				e.printStackTrace();
				return;
			}
			catch(EvaluatorException e) {
				System.err.println("Evaluator exception: " + e.getMessage());
				return;
			}
			catch(Exception e) {
				throw e;
			}

			/* Get the results of the control flow differencing. The results
			 * include an analysis context: the source and destination ASTs
			 * and CFGs. */
			DiffContext diffContext = diff.getContext();
			
			/* To co-ordinate interleaving, we need to setup an analysis one level higher. */
			InterleavedChangeImpactAnalysis interleavedAnalysis = new InterleavedChangeImpactAnalysis(
					cfgVisitorFactories, sourceCodeFileChange, diffContext);

			/* Run the analysis. */
			interleavedAnalysis.analyze();
			
		}


	}

}