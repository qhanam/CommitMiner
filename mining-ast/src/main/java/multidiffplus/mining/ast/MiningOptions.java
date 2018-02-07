package multidiffplus.mining.ast;

import java.io.File;

import org.kohsuke.args4j.Option;

public class MiningOptions {

	@Option(name="-u", aliases={"--uri"}, usage="The uri of the public repository (e.g., https://github.com/qhanam/JSRepairClass.git).")
	private String host = null;
	public String getURI() { return this.host; }

	@Option(name="-d", aliases={"--directory"}, usage="The git directory (e.g., /path/to/project/.git/).")
	private String directory = null;
	public String getGitDirectory() { return this.directory; }
	
	@Option(name="--source", usage="The directory to store source files.")
	private String sourceDir = null;
	public File getSourceDir() { return sourceDir == null ? null : new File(sourceDir); }
	
	@Option(name="-o", aliases= {"--out"}, usage="The output file.")
	private String outFile = null;
	public File getOutFile() { return outFile == null ? null : new File(outFile); }

	@Option(name="-h", aliases={"--help"}, usage="Display the help file.")
	private boolean help = false;
	public boolean getHelp() { return help; }

	@Option(name="-r", aliases={"--repositories"}, usage="The path to the file specifying the repositories to analyze.")
	private String repoFile = null;
	public String getRepoFile() { return this.repoFile; }	

	@Option(name = "-tr", aliases = { "--threads" }, usage = "The number of threads to be used.")
	private Integer nThreads = 6;
	public Integer getNThreads() { return this.nThreads; }
	
}