package multidiffplus.mining.flow.facts;

import java.util.EnumSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * Represents a change made to a program slice.
 */
public class SliceChange {
	
	/**
	 * Classification scheme for types of changes made to slices.
	 */
	public enum Type {
		REPAIR, // Shows a repair. The 'before' AST is buggy and the 'after' AST is correct.
		MUTATION_CANDIDATE // Function AST is correct, but can be mutated to be buggy
	}
	
	private Slice before;
	private Slice after;
	private Set<Type> labels; 
	
	public SliceChange(Slice before, Slice after) {
		this.before = before;
		this.after = after;
		this.labels = EnumSet.noneOf(Type.class);
	}
	
	/**
	 * Adds a label to this slice change.
	 * @param type The label.
	 */
	public void addLabel(Type type) {
		labels.add(type);
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
	public Set<Type> getLabels() {
		return labels;
	}
	
	/**
	 * @return the {@code SliceChange} as a {@code JsonObject}.
	 */
	public JsonObject getJsonObject() {
		
		JsonObject json = new JsonObject();
		
		JsonArray array = new JsonArray();
		for(Type type : labels) {
			array.add(type.toString());
		}
		
		json.add("labels", array);
		if(before != null) {
			json.addProperty("before", before.toString());
			json.add("before-ast", before.getStatementAST());
		}
		else {
			json.add("before", JsonNull.INSTANCE);
			json.add("before-ast", JsonNull.INSTANCE);
		}
		json.addProperty("after", after.toString());
		json.add("after-ast", after.getStatementAST());
		return json;
		
	}
	
	@Override
	public String toString() {
		return "labels=[" + StringUtils.join(labels.toArray(), ",") + "]";
	}
	
}
