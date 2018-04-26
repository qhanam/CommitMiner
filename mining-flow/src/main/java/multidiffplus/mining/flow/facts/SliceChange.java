package multidiffplus.mining.flow.facts;

import com.google.gson.JsonObject;

/**
 * Represents a change made to a program slice.
 */
public class SliceChange {
	
	/**
	 * Classification scheme for types of changes made to slices.
	 */
	public enum Type {
		REPAIR, // Implies before is buggy and after is fixed
		MUTANT_REPAIR, // Mutated such that before is buggy and after is fixed
		NOMINAL // Implies both versions are correct
	}
	
	private Slice before;
	private Slice after;
	private Type type; 
	
	public SliceChange(Slice before, Slice after, Type type) {
		this.before = before;
		this.after = after;
		this.type = type;
	}
	
	/** @return the slice before the commit. */
	public Slice getOriginalSlice() {
		return before;
	}
	
	/** @return the slice after the commit. */
	public Slice getNewSlice() {
		return after;
	}
	
	/** @return the type of behaviour changed by the commit. */
	public Type getChangeType() {
		return type;
	}
	
	/**
	 * @return the {@code SliceChange} as a {@code JsonObject}.
	 */
	public JsonObject getJsonObject() {
		
		JsonObject json = new JsonObject();
		json.addProperty("type", type.toString());
		json.addProperty("before", before.toString());
		json.add("before-ast", before.getStatementAST());
		json.addProperty("after", after.toString());
		json.add("after-ast", after.getStatementAST());
		return json;
		
	}
	
	@Override
	public String toString() {
		if(before != null && after != null)
			return type.name() + "\n" + before + "\n--------\n" + after;
		if(before != null)
			return type.name() + "\n" + before;
		if(after != null)
			return type.name() + "\n" + after;
		return "";
	}
	
}
