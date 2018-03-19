package multidiffplus.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.lang3.time.StopWatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.facts.Annotation;
import multidiffplus.facts.AnnotationFactBase;
import multidiffplus.facts.MiningFactBase;
import multidiffplus.analysis.CommitAnalysis;

/**
 * Performs analysis on a Git project using an AnalysisRunner
 */
public class GitProjectAnalysis extends GitProject {

	protected static final Logger logger = LogManager.getLogger(GitProjectAnalysis.class);

	/** Runs an analysis on a source file. **/
	private ICommitAnalysisFactory commitAnalysisFactory;
	
	/** Directory to write source code to. **/
	private File sourceDir;
	
	/** CSV file to write results to. **/
	private File csvFile;

	/**
	 * Constructor that is used by our static factory methods.
	 */
	protected GitProjectAnalysis(GitProject gitProject,
								 ICommitAnalysisFactory commitAnalysisFactory,
								 File sourceDir,
								 File csvFile) {
		super(gitProject);
		this.commitAnalysisFactory = commitAnalysisFactory;
		this.sourceDir = sourceDir;
		this.csvFile = csvFile;
	}

	/**
	 * Analyze the repository (extract repairs).
	 *
	 * @throws GitAPIException
	 * @throws IOException
	 */
	public void analyze() throws GitAPIException, IOException, Exception {
		
		/* Time the analysis so we can stop if it gets out of hand. */
		StopWatch projectTimer = new StopWatch();
		projectTimer.start();

		long startTime = System.currentTimeMillis();
		logger.info("[START ANALYSIS] {}", this.getURI());

		/* Get the list of bug fixing commits from version history. */
		List<Triple<String, String, Type>> commits = this.getCommitPairs();

		logger.info(" [ANALYZING] {} bug fixing commits", commits.size());

		/* Analyze the changes made in each bug fixing commit. */
		for(Triple<String, String, Type> commit : commits) {
			
			/* Abort if the analysis goes over 5 minutes. */
			if(projectTimer.getTime() > 300000) {
				logger.warn(" [WARNING] {} aborting due to project timeout", this.getURI());
				break;
			}

			try {
				this.analyzeDiff(commit.getLeft(), commit.getMiddle(), commit.getRight());
			} catch (Exception e) {
				logger.error("[ERROR] {}, {}", commit.getMiddle(),  e.getMessage());
				e.printStackTrace();
			}
			
		}

		long endTime = System.currentTimeMillis();
		logger.info("[END ANALYSIS] {}. Time (in seconds): {} ", this.getURI(), (endTime - startTime) / 1000.0);
	}

	/**
	 * Extract the source files from Git and analyze them with the analysis
	 * runner.
	 *
	 * @param buggyRevision The hash that identifies the buggy revision.
	 * @param bugFixingRevision The hash that identifies the fixed revision.
	 * @param bugFixingCommit True if the commit is labeled as a bug fixing
	 * 		  commit (from NLP).
	 * @throws IOException
	 * @throws GitAPIException
	 */
	private void analyzeDiff(String buggyRevision, String bugFixingRevision, Type commitMessageType) throws IOException, GitAPIException, Exception {

		ObjectId buggy = this.repository.resolve(buggyRevision + "^{tree}");
		ObjectId repaired = this.repository.resolve(bugFixingRevision + "^{tree}");

		ObjectReader reader = this.repository.newObjectReader();

		CanonicalTreeParser buggyTreeIter = new CanonicalTreeParser();
		if(buggyRevision != null)
			buggyTreeIter.reset(reader, buggy);
		else
			buggyTreeIter.reset();

		CanonicalTreeParser repairedTreeIter = new CanonicalTreeParser();
		repairedTreeIter.reset(reader, repaired);

		DiffCommand diffCommand = this.git.diff().setShowNameAndStatusOnly(true).setOldTree(buggyTreeIter).setNewTree(repairedTreeIter);

		List<DiffEntry> diffs = diffCommand.call();

		/* The {@code Commit} is meta data and a set of source code changes. */
		Commit commit = new Commit(
				this.projectID,
				this.projectHomepage,
				buggyRevision, bugFixingRevision,
				commitMessageType);
		
		/* Skip merge commits. */
		if(commit.commitMessageType == Type.MERGE) {
			logger.info("[SKIP_COMMIT] merge commit: " + commit.repairedCommitID);
			return;
		}

		/* Iterate through the modified files and add them as
		 * {@code SourceCodeFileChange}s in the commit. */
		for(DiffEntry diff : diffs) {
			
			/* Skip files in dist folder. */
			if (diff.getNewPath().matches("^dist/.*$") || diff.getNewPath().matches("^.*/dist/.*$")) {
				logger.info("[SKIP_FILE] dist file: " + diff.getNewPath());
				continue;
			}

			/* Skip files in lib folder. */
			if (diff.getNewPath().matches("^lib/.*$") || diff.getNewPath().matches("^.*/lib/.*$")) {
				logger.info("[SKIP_FILE] lib file: " + diff.getNewPath());
				continue;
			}

			/* Skip files in bin folder. */
			if (diff.getNewPath().matches("^bin/.*$") || diff.getNewPath().matches("^.*/bin/.*$")) {
				logger.info("[SKIP_FILE] bin file: " + diff.getNewPath());
				continue;
			}

			/* Skip test files. */
			if (diff.getNewPath().matches("^.*test.*$") || diff.getNewPath().matches("^.*test.*$")) {
				logger.info("[SKIP_FILE] test file: " + diff.getNewPath());
				continue;
			}

			/* Skip jquery files. */
			if (diff.getNewPath().matches("^.*jquery.*$") || diff.getNewPath().matches("^.*jquery.*$")) {
				logger.info("[SKIP_FILE] jquery file: " + diff.getNewPath());
				continue;
			}

			/* Skip gulp files. */
			if (diff.getNewPath().matches("^.*gulpfile.js$")) {
				logger.info("[SKIP_FILE] gulp file: " + diff.getNewPath());
				continue;
			}

			/* Skip minified files. */
			if (diff.getNewPath().endsWith(".min.js") || diff.getNewPath().endsWith(".min.js")) {
				logger.info("[SKIP_FILE] Skipping minifed file: " + diff.getNewPath());
				continue;
			}

			/* Skip anything that is not a js file. */
			if (!diff.getNewPath().endsWith(".js") || !diff.getNewPath().endsWith(".js")) {
				logger.info("[SKIP_FILE] Skipping non-js file: " + diff.getNewPath());
				continue;
			}

			logger.debug("Exploring diff \n {} \n {} - {} \n {} - {}", getURI(), buggyRevision, diff.getOldPath(),
					bugFixingRevision, diff.getNewPath());

			/* Add this source code file change to the commit. */

			String oldFile = buggyRevision == null 
					? "" : this.fetchBlob(buggyRevision, diff.getOldPath());

			String newFile = this.fetchBlob(bugFixingRevision, diff.getNewPath());

			commit.addSourceCodeFileChange(new SourceCodeFileChange(
					diff.getOldPath(), diff.getNewPath(),
					oldFile, newFile));

		}

		/* Run the {@code CommitAnalysis} through the AnalysisRunner. */

		try {

			System.out.println(commit.url + "/commit/" + commit.repairedCommitID);

			/* Run the analysis with GumTree diff. */
			CommitAnalysis commitAnalysis = commitAnalysisFactory.newInstance();
			commitAnalysis.analyze(commit);
			
			/* Flush the results of the analysis to persistent storage. */
			if(sourceDir != null && csvFile != null) {

				for(SourceCodeFileChange fileChange : commit.sourceCodeFileChanges) {
					flushToFile(commit, fileChange, sourceDir, csvFile);
				}

			}

		}
		catch(Exception ignore) {
			System.err.println("Ignoring exception in ProjectAnalysis.runSDJSB.\nBuggy Revision: " + buggyRevision + "\nBug Fixing Revision: " + bugFixingRevision);
			ignore.printStackTrace();
		}
		catch(Error e) {
			System.err.println("Ignoring error in ProjectAnalysis.runSDJSB.\nBuggy Revision: " + buggyRevision + "\nBug Fixing Revision: " + bugFixingRevision);
			e.printStackTrace();
		}

	}

	/**
	 * Fetches the string contents of a file from a specific revision. from
	 * http://stackoverflow.com/questions/1685228/how-to-cat-a-file-in-jgit
	 *
	 * @param repo The repository to fetch the file from.
	 * @param revSpec The commit id.
	 * @param path The path to the file.
	 * @return The contents of the text file.
	 * @throws MissingObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	private String fetchBlob(String revSpec, String path) throws MissingObjectException, IncorrectObjectTypeException, IOException {

		// Resolve the revision specification
	    final ObjectId id = this.repository.resolve(revSpec);

        // Makes it simpler to release the allocated resources in one go
        try(ObjectReader reader = this.repository.newObjectReader()) {
			try(RevWalk walk = new RevWalk(reader)) {
				// Get the commit object for that revision
				RevCommit commit = walk.parseCommit(id);

				// Get the revision's file tree
				RevTree tree = commit.getTree();
				// .. and narrow it down to the single file's path
				TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);

				if (treewalk != null) {
					// use the blob id to read the file's data
					byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
					return new String(data, "utf-8");
				} else {
					return "";
				}
			}        
        }

    }

	/**
	 * Appends the annotations to a file for persistent storage.
	 * @throws IOException if a path does not exist.
	 */
	private static synchronized void flushToFile(Commit commit, 
			SourceCodeFileChange sourceCodeFileChange, 
			File sourceDir, File csvFile) throws IOException {

		MiningFactBase factBase = MiningFactBase.getInstance(sourceCodeFileChange);

		if(csvFile == null 
				|| commit.sourceCodeFileChanges.size() > 1 
				|| factBase.getUpdatedStatements() > 6
				|| factBase.getInsertedStatements() > 0 
				|| factBase.getRemovedStatements() > 0 
				|| factBase.getAnnotations().isEmpty()) return;

		/* Write the src/dst files to disk so that we can run a flow analysis on
		* them later. */
		File srcFile = new File(sourceDir, sourceCodeFileChange.getID() + "_old.js");
		File dstFile = new File(sourceDir, sourceCodeFileChange.getID() + "_new.js");

		sourceDir.mkdirs();
		srcFile.createNewFile();
		dstFile.createNewFile();

		try(PrintStream stream = new PrintStream(new FileOutputStream(srcFile, false))) {
			stream.print(sourceCodeFileChange.buggyCode);
		}
		
		try(PrintStream stream = new PrintStream(new FileOutputStream(dstFile, false))) {
			stream.print(sourceCodeFileChange.repairedCode);
		}

		/* Write the annotations to the CSV file. */
		csvFile.getParentFile().mkdirs();
		csvFile.createNewFile();

		try (PrintStream stream = new PrintStream(new FileOutputStream(csvFile, true))) {

			/* Write the data set. */
			for(Annotation annotation : factBase.getAnnotations()) {
				stream.println(commit.toString() 
						+ "," + sourceCodeFileChange.toString() 
						+ "," + srcFile.getName()
						+ "," + dstFile.getName()
						+ "," + annotation.getLabel()
						+ "," + annotation.getLine());
			}
		
		}
		
		/* We are done with the factbase and can recover the memory. */
		AnnotationFactBase.removeInstance(sourceCodeFileChange);

	}

	/*
	 * Static factory methods
	 */

	/**
	 * Creates a new GitProjectAnalysis instance from a git project directory.
	 *
	 * @param directory The base directory for the project.
	 * @param commitMessageRegex The regular expression that a commit message
	 * 		  needs to match in order to be analyzed.
	 * @param commitAnalysis The analysis to run on each commit.
	 * @param sourceDir The directory to write source files.
	 * @param outFile The file to output results (if null, no results will be stored)
	 * @return An instance of GitProjectAnalysis.
	 * @throws GitProjectAnalysisException
	 */
	public static GitProjectAnalysis fromDirectory(String directory, ICommitAnalysisFactory commitAnalysisFactory, File sourceDir, File outFile)
			throws GitProjectAnalysisException {
		GitProject gitProject = GitProject.fromDirectory(directory);

		return new GitProjectAnalysis(gitProject, commitAnalysisFactory, sourceDir, outFile);
	}

	/**
	 * Creates a new GitProjectAnalysis instance from a URI.
	 *
	 * @param uri The remote .git address.
	 * @param directory The directory that stores the cloned repositories.
	 * @param commitMessageRegex The regular expression that a commit message
	 * 		  needs to match in order to be analyzed.
	 * @param commitAnalysis The analysis to run on each commit.
	 * @param sourceDir The directory to write source files.
	 * @param outFile The file to output results (if null, no results will be stored)
 	 * @return An instance of GitProjectAnalysis.
	 * @throws GitAPIException
	 * @throws TransportException
	 * @throws InvalidRemoteException
	 */
	public static GitProjectAnalysis fromURI(String uri, String directory, ICommitAnalysisFactory commitAnalysisFactory, File sourceDir, File outFile)
			throws GitProjectAnalysisException, InvalidRemoteException, TransportException, GitAPIException {
		GitProject gitProject = GitProject.fromURI(uri, directory);

		return new GitProjectAnalysis(gitProject, commitAnalysisFactory, sourceDir, outFile);
	}

}
