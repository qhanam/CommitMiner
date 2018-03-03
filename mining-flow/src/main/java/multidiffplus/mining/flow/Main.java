package multidiffplus.mining.flow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class Main {

	protected static final Logger logger = LogManager.getLogger(Main.class);

	/** The directory where repositories are checked out. **/
	public static final String CHECKOUT_DIR =  new String("repositories");

	/**
	 * Creates the learning data set for extracting repair patterns.
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

		/* Parse the file into a list of file pairs. */
		Set<Candidate> candidates = new HashSet<Candidate>();

		try(BufferedReader br = new BufferedReader(new FileReader(options.getCandidatesFile()))) {
			for(String line; (line = br.readLine()) != null; ) {
				
				/* Parse the line into a Candidate object. */
				String[] values = line.split(",");
				Candidate candidate = new Candidate(values[0], values[1], 
						new File(options.getSourceDir(), values[2]), 
						new File(options.getSourceDir(), values[3]));
				
				/* Add the candidate if it is is not already in the candidate
				* set (we will re-generate labels for each interesting change). */
				candidates.add(candidate);

			}
		}
		catch(Exception e) {
			System.err.println("Error while reading URI file: " + e.getMessage());
			return;
		}

		/* Create a pool of threads and use a CountDownLatch to check when
		 * all threads are done. */
		ExecutorService executor = Executors.newFixedThreadPool(options.getNThreads());
		CountDownLatch latch = new CountDownLatch(candidates.size());

		/* Analyze all projects. */
		for(Candidate candidate : candidates) {
			
			try {
				/* Perform the analysis (this may take some time) */
				CandidateAnalysis candidateAnalysis = new CandidateAnalysis(candidate, options.getOutFile(), options.getSensitivity());
				executor.submit(new CandidateAnalysisTask(candidateAnalysis, latch));
			} catch (Exception e) {
				e.printStackTrace(System.err);
				logger.error("[IMPORTANT] Project " + candidate.getURI() + "/" + candidate.getFile() + " threw an exception");
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