package multidiffplus.mining.flow.facts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Stores the text, position and label of a statement in a slice.
 */
public class Statement {

	private String text;
	private JsonObject ast;
	private Integer line;
	private Integer absolutePosition;
	private Integer length;
	
	/**
	 * @param ast Esprima-style JSON AST
	 */
	public Statement(String text, JsonObject ast, Integer line, Integer absolutePosition, 
			Integer length) {
		this.text = text;
		this.ast = ast;
		this.line = line;
		this.absolutePosition = absolutePosition;
		this.length = length;
	}
	
	/** @return the statement text */
	public String getText() { return text; }

	/** @return the statement AST */
	public JsonObject getAST() { 
		return ast;
	}
	
	/** @return the serialized statement AST */
	public String getSerializedAST() { 
		Gson gson = new GsonBuilder().serializeNulls().create();
		String serial = gson.toJson(ast);
		return serial; 
	}
	
	/** @return the line number where the statement begins. */
	public Integer getLine() { return line; }
	
	/** @return the absolute position where the statement begins. */
	public Integer getAbsolutePosition() { return absolutePosition; }
	
	/** @return the length of the statement text. */
	public Integer getLength() { return length; }
	
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