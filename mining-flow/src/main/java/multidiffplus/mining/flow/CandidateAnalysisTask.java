package multidiffplus.mining.flow;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * TaskRunner wrapper for CandidateAnalysis. Initially CandidateAnalysis would
 * implement Callable itself, but because of the use of a latch, a new class was
 * created.
 */
public class CandidateAnalysisTask implements Callable<Void> {
	
	protected final Logger logger = LogManager.getLogger(CandidateAnalysisTask.class);

	private CandidateAnalysis candidateAnalysis;
	private CountDownLatch latch;
	
	public CandidateAnalysisTask(CandidateAnalysis candidateAnalysis, CountDownLatch latch) {
		this.candidateAnalysis = candidateAnalysis;
		this.latch = latch;
	}
	
	@Override
	public Void call() throws Exception {

		try {
			candidateAnalysis.analyze();
		} catch (Exception e) {
			System.err.println("[ERR] Exception on GitProjectAnalysisTask");
			e.printStackTrace();
		} finally {
			logger.info(" [TASK FINALIZED] {} tasks left", latch.getCount());
			latch.countDown();
		}
		
		return null;

	}

}
