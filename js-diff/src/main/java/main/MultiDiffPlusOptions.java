package main;

import org.kohsuke.args4j.Option;

import multidiffplus.analysis.Options;

public class MultiDiffPlusOptions {
	
	@Option(name="-f", aliases={"--file"}, usage="The output file to write diff results to.")
	private String outFile = "out.html";

	@Option(name="-o", aliases={"--original"}, usage="The original file.")
	private String original = null;
	
	@Option(name="-m", aliases={"--modified"}, usage="The modified file.")
	private String modified = null;
	
//	@Option(name = "-s", aliases={"--symex"}, usage="Use symbolic exection (ON|OFF)")
//	private Options.SymEx symex = Options.SymEx.OFF;

	@Option(name="-h", aliases={"--help"}, usage="Display the help file.")
	private boolean help = false;
	
	public String getOutputFile() { return outFile; }
	public String getOriginal() { return original; }
	public String getModified() { return modified; }
//	public Options.SymEx useSymEx() { return symex; }

	public boolean getHelp() {
		return help;
	}

}