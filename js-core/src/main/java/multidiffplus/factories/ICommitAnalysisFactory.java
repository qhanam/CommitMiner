package multidiffplus.factories;

import multidiffplus.analysis.CommitAnalysis;

/**
 * Builds new instances of a commit analysis.
 */
public interface ICommitAnalysisFactory {
	CommitAnalysis newInstance();
}