package multidiffplus.jsdiff.view;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import multidiffplus.facts.Annotation;
import multidiffplus.facts.AnnotationFactBase;

/**
 * Prints source code as annotated HTML.
 */
public class HTMLMultiDiffViewer {

	/**
	 * Adds html alert annotations to the file.
	 * @param inputPath The original source code file (text)
	 * @param alerts The alerts used to annotate the file
	 * @return The annotated file
	 * @throws IOException when files cannot be read or written
	 */
	public static String annotate(String source,
						  AnnotationFactBase factBase) throws IOException {

		String out = "";

		Annotation current = null;
		while(!factBase.isEmpty()) {
			current = factBase.pop();
			if(current.absolutePosition > 0) break;
		}

		char[] chars = source.toCharArray();

		/* Track when to close tags key=position value=semaphore. */
		Map<Integer,Integer> closeAt = new HashMap<Integer,Integer>();

		/* Track which tags are currently open. */
		LinkedList<Annotation> openTags = new LinkedList<Annotation>();

		for(int i = 0; i < chars.length; i++) {

			/* Close tags where needed. */
			Integer sem = closeAt.get(i);
			for(int j = 0; sem != null && j < sem; j++) {
				out = out.concat("</span>");
				openTags.pop();
			}
			closeAt.remove(i);

			/* Re-open all closed tags after a line break. */
			if(i > 0 && chars[i-1] == '\n') {
				Iterator<Annotation> it = openTags.descendingIterator();
				while(it.hasNext()) {
					Annotation openTag = it.next();
					out = out.concat("<span class='context-menu " + openTag.label + "' data-address='" + openTag.getDependencyLabel() + "'>");
				}
			}

			/* Close all tags before a line break. */
			for(int k = 0; chars[i] == '\n' && k < openTags.size(); k++)
				out = out.concat("</span>");

			/* Open tags where needed. */
			while(current != null && current.absolutePosition.equals(i)) {

				/* Open the tag. */
				out = out.concat("<span class='context-menu " + current.label + "' data-address='" + current.getDependencyLabel() + "'>");
				openTags.push(current);

				/* Set the close tag position. */
				Integer closePosition = current.length + i;
				Integer count = closeAt.get(closePosition);
				if(count == null)
					closeAt.put(closePosition, 1);
				else
					closeAt.put(closePosition, count + 1);

				current = factBase.isEmpty() ? null : factBase.pop();
			}

			/* Write the next character in the file. */
			switch (chars[i]) {
			case '<':
				out = out.concat("&#60");
				break;
			case '>':
				out = out.concat("&#62");
				break;
			default:
				out = out.concat(String.valueOf(chars[i]));
			}

		}

		return out;

	}

}