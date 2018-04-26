package multidiffplus.mining.flow.facts;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import multidiffplus.commit.SourceCodeFileChange;
import multidiffplus.facts.FactBase;

/**
 * Registers and stores facts related to annotating the source code file.
 */
public class SliceFactBase extends FactBase {

	private static Map<SourceCodeFileChange, SliceFactBase> instances = new HashMap<SourceCodeFileChange, SliceFactBase>();

	private Map<Integer, SliceChange> slices;

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
		slices = new HashMap<Integer, SliceChange>();
	}

	/**
	 * Register a slice with the fact database.
	 * 
	 * @param id The identifier of the slice (eliminates redundant slices).
	 */
	public void registerSliceFact(Integer id, SliceChange slice) {
		this.slices.put(id, slice);
	}
	
	/**
	 * Register an slice with the fact database.
	 * 
	 * The slice is assumed to be on the destination file.
	 */
	public void registerSliceFacts(Map<Integer, SliceChange> slices) {
		this.slices.putAll(slices);
	}
	
	/**
	 * @return The ordered set of slices in the fact base.
	 */
	public Collection<SliceChange> getSlices() {
		return this.slices.values();
	}
	
	/**
	 * @return The slice with identifier {@code id}.
	 */
	public SliceChange getSlice(Integer id) {
		return slices.get(id);
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
		json.addProperty("sourceFileChangeID", sourceCodeFileChange.getID());
		json.addProperty("fileName", sourceCodeFileChange.getFileName());
		
		JsonArray jsonArray = new JsonArray();
		json.add("sliceChangePair", jsonArray);

		for(SliceChange slice : slices.values()) {
			jsonArray.add(slice.getJsonObject());
		}
		
		return json;
		
	}
	
	public void printDataSet() {
		for(SliceChange slice : slices.values()) {
			System.out.println(slice.toString());
		}
	}
	
}
