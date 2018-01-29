package multidiffplus.mining.analysis;

import java.util.List;
import java.util.regex.Matcher;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ast.AstNode;

import multidiffplus.analysis.DomainAnalysis;
import multidiffplus.cfg.CFG;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.diff.Diff;
import multidiffplus.diff.DiffContext;
import multidiffplus.factories.IASTVisitorFactory;
import multidiffplus.factories.ICFGFactory;

/**
 * Gathers change impact facts about one source code file.
 */
public class MiningDomainAnalysis extends DomainAnalysis {

	public List<IASTVisitorFactory> astVisitorFactories;

	public MiningDomainAnalysis(List<IASTVisitorFactory> astVisitorFactories,
			ICFGFactory cfgFactory) {
		super(null, null, cfgFactory, false, true);
		this.astVisitorFactories = astVisitorFactories;
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
		String fileExtension = getSourceCodeFileExtension(sourceCodeFileChange.buggyFile, sourceCodeFileChange.repairedFile);

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
			
			/* TODO: Hook up the flow analysis here if needed (use InterleavedChangeImpactAnalysis). */
			
			/* Run the AST visitors. */
			analyzeAST(sourceCodeFileChange, diffContext);

		}


	}

	/**
	 * Generate facts by accepting visitors to the ASTs.
	 */
	private void analyzeAST(SourceCodeFileChange sourceCodeFileChange, DiffContext diffContext) {
		
		/* TODO: Generate facts by analyzing the source functions? */

		/* Generate facts by analyzing the destination functions. */
		for(CFG cfg : diffContext.dstCFGs) {
			for(IASTVisitorFactory astVF : astVisitorFactories) {
				AstNode root = (AstNode)cfg.getEntryNode().getStatement();
				root.visit(astVF.newInstance(sourceCodeFileChange, root));
			}
		}
		
	}

	/**
	 * @param preCommitPath The path of the file before the commit.
	 * @param postCommitPath The path of the file after the commit.
	 * @return The extension of the source code file or null if none is found
	 * 	or the extensions of the pre and post paths do not match.
	 */
	protected static String getSourceCodeFileExtension(String preCommitPath, String postCommitPath) {

		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\.([a-z]+)$");
		Matcher preMatcher = pattern.matcher(preCommitPath);
		Matcher postMatcher = pattern.matcher(postCommitPath);

		String preExtension = null;
		String postExtension = null;

		if(preMatcher.find() && postMatcher.find()) {
			preExtension = preMatcher.group(1);
			postExtension = postMatcher.group(1);
			if(preExtension.equals(postExtension)) return preExtension;
		}

		return null;

	}

}