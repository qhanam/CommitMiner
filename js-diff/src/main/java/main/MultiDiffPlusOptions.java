package main;

import org.kohsuke.args4j.Option;

public class MultiDiffPlusOptions {

    @Option(name = "-j", aliases = { "--json" },
	    usage = "The output file to write JSON diff results to.")
    private String jsonOutFile = "out.json";

    @Option(name = "-f", aliases = { "--file" },
	    usage = "The output file to write HTML diff results to.")
    private String htmlOutFile = "out.html";

    @Option(name = "-o", aliases = { "--original" }, usage = "The original file.")
    private String original = null;

    @Option(name = "-m", aliases = { "--modified" }, usage = "The modified file.")
    private String modified = null;

    @Option(name = "-h", aliases = { "--help" }, usage = "Display the help file.")
    private boolean help = false;

    public String getJsonFile() {
	return jsonOutFile;
    }

    public String getOutputFile() {
	return htmlOutFile;
    }

    public String getOriginal() {
	return original;
    }

    public String getModified() {
	return modified;
    }

    public boolean getHelp() {
	return help;
    }

}