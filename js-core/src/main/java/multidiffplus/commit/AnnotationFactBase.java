package multidiffplus.commit;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Registers and stores facts related to annotating the source code file.
 * 
 * An analysis should extend this class to register concrete predicates that 
 * will be used to annotate the source code file.
 */
public class AnnotationFactBase extends FactBase {
	
	private static Map<SourceCodeFileChange, AnnotationFactBase> instances = new HashMap<SourceCodeFileChange, AnnotationFactBase>();

	private SortedSet<Annotation> annotations;
	
	/**
	 * @return The AnnotationFactBase for the given {@code SourceCodeFileChange}.
	 */
	public static AnnotationFactBase getInstance(SourceCodeFileChange sourceCodeFileChange) {
		AnnotationFactBase instance = instances.get(sourceCodeFileChange);
		if(instance == null) {
			instance = new AnnotationFactBase(sourceCodeFileChange);
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
	
	private AnnotationFactBase(SourceCodeFileChange sourceCodeFileChange) {

		super(sourceCodeFileChange);

		/* Order the annotations according to their absolute position in the
		 * file. */
		annotations = new TreeSet<Annotation>(new Comparator<Annotation>() {
			public int compare(Annotation a1, Annotation a2) {
				if(a1.absolutePosition < a2.absolutePosition) return -1;
				if(a1.absolutePosition > a2.absolutePosition) return 1;
				if(a1.length > a2.length) return -1;
				if(a1.length < a2.length) return 1;
				return(a1.label.compareTo(a2.label));
			}
		});

	}

	/**
	 * Register an annotation with the fact database.
	 * 
	 * The annotation is assumed to be on the destination file.
	 */
	public void registerAnnotationFact(Annotation annotation) {
		this.annotations.add(annotation);
	}
	
	/**
	 * Register an annotation with the fact database.
	 * 
	 * The annotation is assumed to be on the destination file.
	 */
	public void registerAnnotationFacts(Set<Annotation> annotations) {
		this.annotations.addAll(annotations);
	}
	
	/**
	 * @return The ordered set of annotations in the fact base.
	 */
	public SortedSet<Annotation> getAnnotations() {
		return this.annotations;
	}
	
	/**
	 * @return true if there are no annotations
	 */
	public boolean isEmpty() {
		return this.annotations.isEmpty();
	}
	
	/**
	 * Pops the first annotation (ie. the one that appears first in the source
	 * code file).
	 * @return the {@code Annotation} at the top of the list.
	 */
	public Annotation pop() {
		Annotation annotation =  annotations.first();
		annotations.remove(annotation);
		return annotation;
	}

	public void printDataSet() {
		for(Annotation annotation : annotations) {
			System.out.println(annotation.toString());
		}
	}
	
}
