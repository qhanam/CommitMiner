package multidiffplus.facts;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import multidiffplus.commit.SourceCodeFileChange;

/**
 * Registers and stores facts related to annotating the source code file.
 */
public class JsonFactBase extends FactBase {

    private static Map<SourceCodeFileChange, JsonFactBase> instances = new HashMap<SourceCodeFileChange, JsonFactBase>();

    private JsonObject json;

    /**
     * @return The AnnotationFactBase for the given {@code SourceCodeFileChange}.
     */
    public static JsonFactBase getInstance(SourceCodeFileChange sourceCodeFileChange) {
	JsonFactBase instance = instances.get(sourceCodeFileChange);
	if (instance == null) {
	    instance = new JsonFactBase(sourceCodeFileChange);
	    instances.put(sourceCodeFileChange, instance);
	}
	return instance;
    }

    /**
     * Removes the {@code AnnotationFactBase} for the given
     * {@code SourceCodeFileChange}.
     */
    public static void removeInstance(SourceCodeFileChange sourceCodeFileChange) {
	instances.remove(sourceCodeFileChange);
    }

    private JsonFactBase(SourceCodeFileChange sourceCodeFileChange) {

	super(sourceCodeFileChange);

	json = null;

    }

    /**
     * Register the JSON for the source code file.
     */
    public void registerJsonFact(JsonObject json) {
	this.json = json;
    }

    /**
     * @return The JSON for the source code file.
     */
    public JsonObject getJson() {
	return this.json;
    }

    /**
     * @return true if there are no annotations
     */
    public boolean isEmpty() {
	return this.json == null;
    }

}
