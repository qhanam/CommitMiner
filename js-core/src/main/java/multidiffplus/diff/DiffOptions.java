package multidiffplus.diff;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class DiffOptions {

	@Option(name="-o", aliases={"--output"}, usage="web for the web-based client and swing for the swing-based client.")
	private String output = "web";

	@Option(name="-pp", aliases={"--preprocess"}, usage="Pre-process the AST before running GumTree.")
	private boolean preProcess = false;

	@Argument(index=0, required=true)
	private String src;

	@Argument(index=1, required=true)
	private String dst;

	public String getOutput() {
		return output;
	}

	public String getSrc() {
		return src;
	}

	public String getDst() {
		return dst;
	}

	public boolean getPreProcess() {
		return preProcess;
	}

}