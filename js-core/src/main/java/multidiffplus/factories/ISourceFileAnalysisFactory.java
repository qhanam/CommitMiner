package multidiffplus.factories;

import multidiffplus.analysis.SourceFileAnalysis;

/**
 * Builds new instances of a source code file analysis.
 */
public interface ISourceFileAnalysisFactory {
	SourceFileAnalysis newInstance();
}