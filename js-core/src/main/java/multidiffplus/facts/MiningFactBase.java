package multidiffplus.facts;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import multidiffplus.commit.SourceCodeFileChange;

/**
 * Registers and stores facts related to annotating the source code file.
 */
public class MiningFactBase extends FactBase {
	
	private static Map<SourceCodeFileChange, MiningFactBase> instances = new HashMap<SourceCodeFileChange, MiningFactBase>();
	
	private Integer insertedStatements;
	private Integer removedStatements;
	private Integer updatedStatements;
	
	private SortedSet<Annotation> annotations;
	
	/**
	 * @return The AnnotationFactBase for the given {@code SourceCodeFileChange}.
	 */
	public static MiningFactBase getInstance(SourceCodeFileChange sourceCodeFileChange) {
		MiningFactBase instance = instances.get(sourceCodeFileChange);
		if(instance == null) {
			instance = new MiningFactBase(sourceCodeFileChange);
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
	
	private MiningFactBase(SourceCodeFileChange sourceCodeFileChange) {

		super(sourceCodeFileChange);
		
		/* Initialize the number of modified statements to zero. */
		insertedStatements = 0;
		removedStatements = 0;
		updatedStatements = 0;

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
	 * Increments the number of inserted statements by one.
	 */
	public void incrementInsertedStatements() {
		insertedStatements++;
	}

	/**
	 * Increments the number of removed statements by one.
	 */
	public void incrementRemovedStatements() {
		removedStatements++;
	}

	/**
	 * Increments the number of updated statements by one.
	 */
	public void incrementUpdatedStatements() {
		updatedStatements++;
	}

	/**
	 * @return The number of inserted statements.
	 */
	public Integer getInsertedStatements() {
		return insertedStatements;
	}

	/**
	 * @return The number of updated statements.
	 */
	public Integer getUpdatedStatements() {
		return insertedStatements;
	}

	/**
	 * @return The number of removed statements.
	 */
	public Integer getRemovedStatements() {
		return removedStatements;
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

	public void printDataSet() {
		System.out.println("Modified Statements: " + insertedStatements);
		for(Annotation annotation : annotations) {
			System.out.println(annotation.toString());
		}
	}
	
}
