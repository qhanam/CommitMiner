package multidiffplus.mining.flow.facts;

import com.google.gson.JsonObject;

/**
 * Stores the label, references, and location of a source file annotation.
 */
public class Slice {

	public Statement statement;

	/**
	 * @param statement The statement in the slice (yes, we only support one statement right now)
	 */
	public Slice(Statement statement) {
		this.statement = statement;
	}
	
	public Statement getStatement() {
		return statement;
	}
	
	public JsonObject getStatementAST() {
		return statement.getAST();
	}
	
	@Override
	public boolean equals(Object o) {
		return false;
	}
	
	@Override
	public String toString() {
		return statement.toString(); 
	}

}