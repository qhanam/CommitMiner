package multidiffplus.mining.flow;

import java.io.File;

import org.kohsuke.args4j.Option;

import multidiffplus.mining.flow.analysis.CommitAnalysisFactory.Sensitivity;

public class MiningOptions {

	@Option(name="--help", usage="Display the help file.")
	private boolean help = false;
	public boolean getHelp() { return help; }

	@Option(name="--source", usage="The directory containing the source files.")
	private String sourceDir = null;
	public File getSourceDir() { return sourceDir == null ? null : new File(sourceDir); }
	
	@Option(name="--candidates", usage="The path to the CSV file containing the transformation candidates.")
	private String candidates = null;
	public File getCandidatesFile() { return candidates == null ? null : new File(candidates); }

	@Option(name="--sensitivity", usage="The context sensitivity setting for the analysis.")
	private Sensitivity sensitivity = Sensitivity.AST;
	public Sensitivity getSensitivity() { return sensitivity; }

	@Option(name="--out", usage="The output file (json).")
	private String outFile = null;
	public File getOutFile() { return outFile == null ? null : new File(outFile); }

	@Option(name = "--threads", usage = "The number of threads to be used.")
	private Integer nThreads = 6;
	public Integer getNThreads() { return this.nThreads; }
	
}