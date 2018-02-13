package multidiffplus.mining.flow.facts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.facts.FactBase;

/**
 * Registers and stores facts related to annotating the source code file.
 */
public class SliceFactBase extends FactBase {
	
	private static Map<SourceCodeFileChange, SliceFactBase> instances = new HashMap<SourceCodeFileChange, SliceFactBase>();
	
	private Set<SliceChange> slices;
	
	/**
	 * @return The AnnotationFactBase for the given {@code SourceCodeFileChange}.
	 */
	public static SliceFactBase getInstance(SourceCodeFileChange sourceCodeFileChange) {
		SliceFactBase instance = instances.get(sourceCodeFileChange);
		if(instance == null) {
			instance = new SliceFactBase(sourceCodeFileChange);
			instances.put(sourceCodeFileChange, instance);
		}
		return instance;
	}
	
	/**
	 * Removes the {@code AnnotationFactBase} for the given {@code SourceCodeFileChange}.
	 */
	public static void removeInstance(SourceCodeFileChange sourceCodeFileChange) {
		instances.remove(sourceCodeFileChange);
	}
	
	private SliceFactBase(SourceCodeFileChange sourceCodeFileChange) {
		super(sourceCodeFileChange);
		slices = new HashSet<SliceChange>();
	}

	/**
	 * Register an slice with the fact database.
	 * 
	 * The slice is assumed to be on the destination file.
	 */
	public void registerSliceFact(SliceChange slice) {
		this.slices.add(slice);
	}
	
	/**
	 * Register an slice with the fact database.
	 * 
	 * The slice is assumed to be on the destination file.
	 */
	public void registerSliceFacts(Set<SliceChange> slices) {
		this.slices.addAll(slices);
	}
	
	/**
	 * @return The ordered set of slices in the fact base.
	 */
	public Set<SliceChange> getSlices() {
		return this.slices;
	}
	
	/**
	 * @return true if there are no slices
	 */
	public boolean isEmpty() {
		return this.slices.isEmpty();
	}
	
	/**
	 * @return the {@code SliceFactBase} as a {@code JsonObject}.
	 */
	public JsonObject getJsonObject() {
		
		JsonObject json = new JsonObject();
		json.addProperty("id", sourceCodeFileChange.getID());
		json.addProperty("file", sourceCodeFileChange.getFileName());
		
		JsonArray jsonArray = new JsonArray();
		json.add("changes", jsonArray);

		for(SliceChange slice : slices) {
			jsonArray.add(slice.getJsonObject());
		}
		
		return json;
		
	}
	
	public void printDataSet() {
		for(SliceChange slice : slices) {
			System.out.println(slice.toString());
		}
	}
	
}
