package multidiffplus.mining.flow;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import multidiffplus.batch.GitProjectAnalysis;
import multidiffplus.batch.GitProjectAnalysisTask;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.mining.flow.factories.MiningCommitAnalysisFactory;

public class Main {

	protected static final Logger logger = LogManager.getLogger(Main.class);

	/** The directory where repositories are checked out. **/
	public static final String CHECKOUT_DIR =  new String("repositories");

	/**
	 * Creates the learning data set for extracting repair patterns.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) {

		MiningOptions options = new MiningOptions();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			Main.printUsage(e.getMessage(), parser);
			return;
		}

		/* Print the help page. */
		if(options.getHelp()) {
			Main.printHelp(parser);
			return;
		}

		/* Create the commit analysis that will analyze commits. */
		ICommitAnalysisFactory factory = new MiningCommitAnalysisFactory();

		/* Analyzes git histories in batch. */
        GitProjectAnalysis gitProjectAnalysis;

		/* Parse the file into a list of file pairs. */
		List<String> versionPairs = new LinkedList<String>();

		try(BufferedReader br = new BufferedReader(new FileReader(options.getCandidatesFile()))) {
			for(String line; (line = br.readLine()) != null; ) {
				versionPairs.add(line);
			}
		}
		catch(Exception e) {
			System.err.println("Error while reading URI file: " + e.getMessage());
			return;
		}

		/*
		 * Create a pool of threads and use a CountDownLatch to check when
		 * all threads are done.
		 * http://stackoverflow.com/questions/1250643/how-to-wait-for-all-
		 * threads-to-finish-using-executorservice
		 *
		 * I was going to create a list of Callable objects and use
		 * executor.invokeAll, but this would remove the start of the
		 * execution of the tasks from the loop to outside the loop, which
		 * would mean all git project initializations would have to happen
		 * before starting the analysis.
		 */
		ExecutorService executor = Executors.newFixedThreadPool(options.getNThreads());
		CountDownLatch latch = new CountDownLatch(versionPairs.size());

		/* Analyze all projects. */
		for(String uri : versionPairs) {
			
			/* Ignore commented urls. */
			if(uri.startsWith("#")) {
				latch.countDown();
				continue;
			}

			try {
				/* Build git repository object */
				gitProjectAnalysis = GitProjectAnalysis.fromURI(uri,
						CHECKOUT_DIR, factory, 
						options.getSourceDir(), 
						options.getOutFile());

				/* Perform the analysis (this may take some time) */
				executor.submit(new GitProjectAnalysisTask(gitProjectAnalysis, latch));
			} catch (Exception e) {
				e.printStackTrace(System.err);
				logger.error("[IMPORTANT] Project " + uri + " threw an exception");
				logger.error(e);
				continue;
			}
		}

		/* Wait for all threads to finish their work */
		try {
			latch.await();
			executor.shutdown();
			System.out.println("All threads finished!");
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}

	}

	/**
	 * Prints the help file for main.
	 * @param parser The args4j parser.
	 */
	private static void printHelp(CmdLineParser parser) {
        System.out.print("Usage: Main ");
        parser.printSingleLineUsage(System.out);
        System.out.println("\n");
        parser.printUsage(System.out);
        System.out.println("");
        return;
	}

	/**
	 * Prints the usage of main.
	 * @param error The error message that triggered the usage message.
	 * @param parser The args4j parser.
	 */
	private static void printUsage(String error, CmdLineParser parser) {
        System.out.println(error);
        System.out.print("Usage: Main ");
        parser.printSingleLineUsage(System.out);
        System.out.println("");
        return;
	}

}