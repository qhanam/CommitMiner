package changeimpact;

import java.util.List;

import multidiffplus.analysis.Options;
import multidiffplus.cfg.CFG;
import multidiffplus.cfg.CFGNode;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.diff.DiffContext;
import multidiffplus.factories.ICFGVisitorFactory;
import multidiffplus.jsanalysis.flow.Analysis;
import multidiffplus.jsanalysis.flow.Instruction;

/**
 * Synchronizes the change impact analysis of the original and new files.
 */
public class InterleavedChangeImpactAnalysis {

	List<ICFGVisitorFactory> cfgVisitorFactories;
	SourceCodeFileChange sourceCodeFileChange;
	DiffContext diffContext;
	
	Analysis srcAnalysis;
	Analysis dstAnalysis;

	public InterleavedChangeImpactAnalysis(List<ICFGVisitorFactory> cfgVisitorFactories, 
			SourceCodeFileChange sourceCodeFileChange, DiffContext diffContext) {
		this.cfgVisitorFactories = cfgVisitorFactories;
		this.sourceCodeFileChange = sourceCodeFileChange;
		this.diffContext = diffContext;
		this.srcAnalysis = Analysis.build(diffContext.srcScript, diffContext.srcCFGs);
		this.dstAnalysis = Analysis.build(diffContext.dstScript, diffContext.dstCFGs);
	}

	/**
	 * Run the analysis.
	 */
	public void analyze() throws Exception {
		
		/* Run the src analysis to completion. The analysis data will be
		* available to the dst analysis. */
		while(!srcAnalysis.isFinished()) srcAnalysis.advance();
		
		/* Run the dst analysis and verify changes with symex. */
		while(!dstAnalysis.isFinished()) {

			/* Advance dst to the next common instruction. */
			Instruction dstInstruction = dstAnalysis.advance();

			/* If this is a common instruction, check for changes. */
			if(dstInstruction != null && dstInstruction.hasMappedInstruction()) {

				CFGNode srcNode = dstInstruction.getMappedInstruction();
				CFGNode dstNode = dstInstruction.getInstruction();
				
				/* Check for semantic equivalence of values with 'changed'
				* lattice elements. */
				if(Options.getInstance().useSymEx())
					checkSemanticEquivalence(srcNode, dstNode);
				
			}

		}
	
		/* Generate desired facts for post-analysis processing. */
		this.generateFacts(diffContext.dstCFGs);

	}
	
	/** 
	 * Attempt to prove equivalence. Progressively increase size of backwards slice. 
	 */
	private void checkSemanticEquivalence(CFGNode srcNode, CFGNode dstNode) {
		
		/* The maximum depth of statements in the backwards slice. */
		int MAX_DEPTH = 3;

		boolean equiv = false;
		
		int i = 0;
		while(!equiv && i < MAX_DEPTH) {
			VerificationTask task = VerificationTask.build(srcNode, dstNode, i + 1);
			equiv = task.verify();
			i++;
		}

	}
	
	/**
	 * Generate facts by accepting visitors to the CFGs.
	 */
	private void generateFacts(List<CFG> cfgs) {

		/* Generate facts from the results of the analysis. */
		for(CFG cfg : cfgs) {
			for(ICFGVisitorFactory cfgVF : cfgVisitorFactories) {
				cfg.accept(cfgVF.newInstance(sourceCodeFileChange));
			}
		}
		
	}
	
}
