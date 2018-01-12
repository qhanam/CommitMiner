package symgen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;

import problem.VerificationProblem;

public class CVC4Runner {
	String cvcExecLocation;
	
	/*
	 * Constructor
	 * Location of cvc4 executable
	 */
	public CVC4Runner(String cvcExecLocation) {
		this.cvcExecLocation = cvcExecLocation;
	}
	
	/**
	 * Takes in a cvc4 file and returns a boolean to say whether the QUERY is valid or not
	 * @param cvc4File
	 * @return 
	 */
	private boolean runCVC4(String cvc4File) {
		
		File outputFile = new File("out/result.txt");
		
		// Delete the file generated for previous run.
		outputFile.delete();
		
		// start a process to execute cvc4 using the executable
		ProcessBuilder pb = new ProcessBuilder(this.cvcExecLocation, cvc4File, "--incremental");
		pb.redirectOutput(Redirect.appendTo(outputFile));
		pb.redirectError(Redirect.INHERIT);
		try {
			Process p = pb.start();
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// The output of cvc4 execution is saved to a file. The file is read to find the result
		try (BufferedReader reader = new BufferedReader(new FileReader(outputFile))){
			String valid = reader.readLine();
			if(valid.equalsIgnoreCase("valid"))
				return true;
			else 
				return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
		
	}
	
	/**
	 * @param verificationProblem The verification problem (code, constraints and assertions).
	 * @param cvcFileLocation The path to the CVC4 binary
	 * @return {@code true} if query variables are equivalent in both versions
	 */
	public boolean verify(VerificationProblem verificationProblem) {
		
		String translated = CVC4Translator.translateProblemToCVC4(verificationProblem);
		
		try {
			Files.createDirectories(Paths.get("out"));
			Files.write(Paths.get("out/" + "compare_js.cvc4"), translated.getBytes());
			boolean cvc4Result  = runCVC4("out/"+"compare_js.cvc4");
			return cvc4Result;
		} catch (IOException e) {
			System.err.println("Could not write to file: " + e.getMessage());
		}
		
		return false;
		
	}
    
}
