package multidiffplus.jsdiff.view;

import java.io.IOException;
import java.util.LinkedList;

import multidiffplus.diff.DiffMatchPatch;

/**
 * Prints source code as annotated HTML.
 */
public class HTMLUnixDiffViewer {

	/**
	 * Creates an html file annotated with alerts.
	 * @param srcCode The original text of the source file
	 * @param dstCode The original text of the destination file
	 * @param dstAnnotated The annotated text of the destination file
	 * @throws IOException when files cannot be read or written
	 */
	public static String annotate(String srcCode,
						  		  String dstCode,
						  		  String dstAnnotated) throws IOException {
		
		String saniSrcCode = srcCode;
		saniSrcCode = saniSrcCode.replace("<", "&#60");
		saniSrcCode = saniSrcCode.replace(">", "&#62");

		String[] srcLines = saniSrcCode.split("\n");
		String[] dstLines = dstAnnotated.split("\n");

		String outSrc = "";
		String outDst = "";

		DiffMatchPatch dmp = new DiffMatchPatch();

		LinkedList<DiffMatchPatch.Diff> diffs;
		diffs = dmp.diff_main_line_mode(srcCode, dstCode);

		int i = 0; // Track the line number in the source file.
		int j = 0; // Track the line number in the destination file.

		int sem = 0; // Semaphore for aligning the deleted and inserted lines.

		for (DiffMatchPatch.Diff diff : diffs) {

			/* If this is a DELETE operation, we track the number of lines
			 * removed so we can align the deleted lines with the inserted
			 * lines. */
			if(diff.operation == DiffMatchPatch.Operation.DELETE) {
				if(sem > 0) throw new Error("Invalid state: DELETE operation with sem > 0");
				sem = diff.text.length();
			}

			/* If an EQUALS diff follows a DELETE diff, we need to add blank
			 * lines in the destination file to align the diff. */
			if(diff.operation == DiffMatchPatch.Operation.EQUAL && sem > 0)
				for(; sem > 0; sem--)
					outDst += "<td class='line alignment'></td><td class='alignment'></td>\n";

			/* Print the lines. */
			for (int y = 0; y < diff.text.length(); y++) {
			  switch(diff.operation) {
			  case EQUAL:
				  // Print lines from both files side by side
				  i++;
				  j++;
				  if(srcLines.length >= i)
					  outSrc += "<td class='line'>" + i + "</td><td>" + srcLines[i-1].replace("\t", "  ") + "</td>\n";
				  else
					  outSrc += "<td class='line'>" + i + "</td><td></td>\n";
				  if(dstLines.length >= j)
					  outDst += "<td class='line'>" + j + "</td><td>" + dstLines[j-1].replace("\t", "  ") + "</td>\n";
				  else
					  outDst += "<td class='line'>" + j + "</td><td></td>\n";
				  break;
			  case DELETE:
				  // Print source line and, if needed, blank lines in destination
				  i++;
				  if(srcLines.length >= i)
					  outSrc += "<td class='line deleteLine'>" + i + "</td><td class='delete'>" + srcLines[i-1].replace("\t", "  ") + "</td>\n";
				  else 
					  outSrc += "<td class='line deleteLine'>" + i + "</td><td class='delete'></td>\n";
				  break;
			  case INSERT:
				  // Print destination line and, if needed, blank lines in source
				  j++;
				  if(dstLines.length >= j)
					  outDst += "<td class='line insertLine'>" + j + "</td><td class='insert'>" + dstLines[j-1].replace("\t", "  ") + "</td>\n";
				  else
					  outDst += "<td class='line insertLine'>" + j + "</td><td class='insert'></td>\n";

				  /* Add lines to the source file as needed. */
				  if(sem > 0) sem--;
				  else outSrc += "<td class='line alignment'></td><td class='alignment'></td>\n";

				  break;
			  }
			}

			/* Add lines to the destination file as needed. */
			if(diff.operation == DiffMatchPatch.Operation.INSERT)
				for(; sem > 0; sem--)
					outDst += "<td class='line alignment'></td><td class='alignment'></td>\n";

		}

		String[] srcRows = outSrc.split("\n");
		String[] dstRows = outDst.split("\n");

		String out = "<html>\n";
		out += "<head>\n";
		out += "<link type='text/css' href='./stylesheets/multidiff.css' rel='stylesheet'>\n";
	    out += "<link href='https://swisnl.github.io/jQuery-contextMenu/dist/jquery.contextMenu.css' rel='stylesheet' type='text/css' />\n";
	    out += "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css'>";
	    out += "<script type='text/javascript' src='https://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js'></script>\n";
	    out += "<script src='https://swisnl.github.io/jQuery-contextMenu/dist/jquery.contextMenu.js' type='text/javascript'></script>\n";
	    out += "<script src='https://swisnl.github.io/jQuery-contextMenu/dist/jquery.ui.position.min.js' type='text/javascript'></script>\n";
		out += "</head>\n";
		out += "<body>\n";
		out += "<table border='0'>\n";
		out += "<colgroup>\n";
		out += "<col width='44'>\n";
		out += "<col>\n";
		out += "<col width='44'>\n";
		out += "<col>\n";
		out += "</colgroup>\n";
		out += "<tbody>\n";

		if(srcRows.length != dstRows.length) throw new Error("SRC rows and DST rows should be equal");

		for(int k = 0; k < srcRows.length; k++) {
			out += "<tr class='code context-menu'>\n";
			out += srcRows[k] + "\n";
			out += dstRows[k] + "\n";
			out += "</tr>";
		}

		out += "</tbody>\n";
		out += "</table>\n";
		out += "<script type='text/javascript' src='./scripts/multidiff.js'></script>\n";
		out += "</body>\n";
		out += "</html>";

		return out;

	}

}