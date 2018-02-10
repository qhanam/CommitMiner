package multidiffplus.mining.flow.facts;

/**
 * Stores the text, position and label of a statement in a slice.
 */
public class Statement {

	/**
	 * Specifies the statement's role in the slice.
	 */
	public enum Annotation {
		DEFECTIVE,
		NOMINAL
	}

	private String text;
	private Integer line;
	private Integer absolutePosition;
	private Integer length;
	private Statement.Annotation annotation;
	
	public Statement(String text, Integer line, Integer absolutePosition, 
			Integer length, Statement.Annotation annotation) {
		this.text = text;
		this.line = line;
		this.absolutePosition = absolutePosition;
		this.length = length;
		this.annotation = annotation;
	}
	
	/** @return the statement text */
	public String getText() { return text; }
	
	/** @return the line number where the statement begins. */
	public Integer getLine() { return line; }
	
	/** @return the absolute position where the statement begins. */
	public Integer getAbsolutePosition() { return absolutePosition; }
	
	/** @return the length of the statement text. */
	public Integer getLength() { return length; }
	
	/** @return the annotation for the statement. */
	public Statement.Annotation getAnnotation() { return annotation; }
	
	@Override
	public String toString() {
		return text;
	}
	
	@Override
	public int hashCode() {
		int hash = 17;
		hash = hash * 31 + absolutePosition.hashCode();
		hash = hash * 31 + length.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this) return true;
		if(!(o instanceof Statement)) return false;
		Statement s = (Statement)o;
		if(absolutePosition == s.absolutePosition 
				&& length == s.length) return true;
		return false;
	}
	
}