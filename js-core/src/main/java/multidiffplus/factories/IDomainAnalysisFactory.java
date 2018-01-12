package multidiffplus.factories;

import multidiffplus.analysis.DomainAnalysis;

/**
 * Builds new instances of a domain analysis.
 */
public interface IDomainAnalysisFactory {
	DomainAnalysis newInstance();
}