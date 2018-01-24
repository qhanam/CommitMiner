package multidiffplus.batch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;

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

import multidiffplus.commit.AnnotationFactBase;
import multidiffplus.commit.Commit;
import multidiffplus.commit.Commit.Type;
import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.factories.ICommitAnalysisFactory;
import multidiffplus.analysis.CommitAnalysis;

/**
 * Performs analysis on a Git project using an AnalysisRunner
 */
public class GitProjectAnalysis extends GitProject {

	protected static final Logger logger = LogManager.getLogger(GitProjectAnalysis.class);

	/** Runs an analysis on a source file. **/
	private ICommitAnalysisFactory commitAnalysisFactory;

	/**
	 * Constructor that is used by our static factory methods.
	 */
	protected GitProjectAnalysis(GitProject gitProject,
								 ICommitAnalysisFactory commitAnalysisFactory) {
		super(gitProject);
		this.commitAnalysisFactory = commitAnalysisFactory;
	}

	/**
	 * Analyze the repository (extract repairs).
	 *
	 * @throws GitAPIException
	 * @throws IOException
	 */
	public void analyze() throws GitAPIException, IOException, Exception {
		long startTime = System.currentTimeMillis();
		logger.info("[START ANALYSIS] {}", this.getURI());

		/* Get the list of bug fixing commits from version history. */
		List<Triple<String, String, Type>> commits = this.getCommitPairs();

		logger.info(" [ANALYZING] {} bug fixing commits", commits.size());

		/* Analyze the changes made in each bug fixing commit. */
		for(Triple<String, String, Type> commit : commits) {

			try {
				this.analyzeDiff(commit.getLeft(), commit.getMiddle(), commit.getRight());
			} catch (Exception e) {
				logger.error("[ERROR] {}, {}", commit.getMiddle(),  e.getMessage());
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
		buggyTreeIter.reset(reader, buggy);

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

		/* Iterate through the modified files and add them as
		 * {@code SourceCodeFileChange}s in the commit. */
		for(DiffEntry diff : diffs) {
			
			/* Skip files in dist folder. */
			if (diff.getOldPath().matches("^.*/dist/.*$") || diff.getNewPath().matches("^.*/dist/.*$")) {
				logger.info("[SKIP_FILE] dist file: " + diff.getOldPath());
				continue;
			}

			/* Skip files in lib folder. */
			if (diff.getOldPath().matches("^.*/lib/.*$") || diff.getNewPath().matches("^.*/lib/.*$")) {
				logger.info("[SKIP_FILE] lib file: " + diff.getOldPath());
				continue;
			}

			/* Skip jquery files. */
			if (diff.getOldPath().matches("^.*jquery.*$") || diff.getNewPath().matches("^.*jquery.*$")) {
				logger.info("[SKIP_FILE] jquery file: " + diff.getOldPath());
				continue;
			}

			/* Skip minified files. */
			if (diff.getOldPath().endsWith(".min.js") || diff.getNewPath().endsWith(".min.js")) {
				logger.info("[SKIP_FILE] Skipping minifed file: " + diff.getOldPath());
				return;
			}

			/* Skip anything that is not a js file. */
			if (!diff.getOldPath().endsWith(".js") || !diff.getNewPath().endsWith(".js")) {
				logger.info("[SKIP_FILE] Skipping non-js file: " + diff.getOldPath());
				return;
			}

			logger.debug("Exploring diff \n {} \n {} - {} \n {} - {}", getURI(), buggyRevision, diff.getOldPath(),
					bugFixingRevision, diff.getNewPath());

			/* Add this source code file change to the commit. */

			String oldFile = this.fetchBlob(buggyRevision, diff.getOldPath());
			String newFile = this.fetchBlob(bugFixingRevision, diff.getNewPath());

			commit.addSourceCodeFileChange(new SourceCodeFileChange(
					diff.getOldPath(), diff.getNewPath(),
					oldFile, newFile));

		}

		/* Run the {@code CommitAnalysis} through the AnalysisRunner. */

		try {

			Map<SourceCodeFileChange, AnnotationFactBase> annotations = new HashMap<SourceCodeFileChange, AnnotationFactBase>();
			CommitAnalysis commitAnalysis = commitAnalysisFactory.newInstance();

			/* Run the analysis with GumTree diff. */
			System.out.println(commit.url + "/commit/" + commit.repairedCommitID);
			commitAnalysis.analyze(commit);
			for(SourceCodeFileChange fileChange : commit.sourceCodeFileChanges)
				annotations.put(fileChange, AnnotationFactBase.getInstance(fileChange));

			/* TODO: Do something with the annotations. */
			for(SourceCodeFileChange fileChange : commit.sourceCodeFileChanges)
				AnnotationFactBase.getInstance(fileChange).printDataSet();
			
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
	 * @return An instance of GitProjectAnalysis.
	 * @throws GitProjectAnalysisException
	 */
	public static GitProjectAnalysis fromDirectory(String directory, ICommitAnalysisFactory commitAnalysisFactory)
			throws GitProjectAnalysisException {
		GitProject gitProject = GitProject.fromDirectory(directory);

		return new GitProjectAnalysis(gitProject, commitAnalysisFactory);
	}

	/**
	 * Creates a new GitProjectAnalysis instance from a URI.
	 *
	 * @param uri The remote .git address.
	 * @param directory The directory that stores the cloned repositories.
	 * @param commitMessageRegex The regular expression that a commit message
	 * 		  needs to match in order to be analyzed.
	 * @param commitAnalysis The analysis to run on each commit.
 	 * @return An instance of GitProjectAnalysis.
	 * @throws GitAPIException
	 * @throws TransportException
	 * @throws InvalidRemoteException
	 */
	public static GitProjectAnalysis fromURI(String uri, String directory, ICommitAnalysisFactory commitAnalysisFactory)
			throws GitProjectAnalysisException, InvalidRemoteException, TransportException, GitAPIException {
		GitProject gitProject = GitProject.fromURI(uri, directory);

		return new GitProjectAnalysis(gitProject, commitAnalysisFactory);
	}

}
