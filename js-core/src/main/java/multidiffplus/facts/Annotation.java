package multidiffplus.facts;

import java.util.List;

import multidiffplus.commit.DependencyIdentifier;

/**
 * Stores the label, references, and location of a source file annotation.
 */
public class Annotation {

	public String label;
	public List<DependencyIdentifier> dependencyIDs;
	public Integer line;
	public Integer absolutePosition;
	public Integer length;

	/**
	 * @param label Describes the meaning of the annotation.
	 * @param dependencyIDs Unique IDs that links dependencies to criteria.
	 * @param line The line number where the annotation starts.
	 * @param absolutePosition The absolute position in the file where the annotation starts.
	 * @param length The length of the annotation (in characters).
	 */
	public Annotation(String label, 
					  List<DependencyIdentifier> dependencyIDs,
					  int line, int absolutePosition, int length) {
		this.label = label;
		this.dependencyIDs = dependencyIDs;
		this.line = line;
		this.absolutePosition = absolutePosition;
		this.length = length;
	}
	
	public String getLabel() {
		return label;
	}
	
	public List<DependencyIdentifier> getDependencyIdentifiers() {
		return dependencyIDs;
	}
	
	public String getDependencyLabel() {
		String s = "";
		if(dependencyIDs.isEmpty()) return "";
		for(DependencyIdentifier id : dependencyIDs) {
			String a = id.getAddress();
			if(!a.isEmpty()) s += id.getAddress() + ",";
		}
		if(s.isEmpty()) return s;
		return s.substring(0, s.length() - 1);
	}
	
	public Integer getLine() {
		return line;
	}
	
	public Integer getAbsolutePosition() {
		return absolutePosition;
	}
	
	public Integer getLength() {
		return length;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Annotation)) return false;
		Annotation a = (Annotation)o;
		if(label.equals(a.label)
				&& absolutePosition == a.absolutePosition 
				&& length == a.length)
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		String str = "Annotation: <" + label + "|" + line + "," + absolutePosition + "," + length + ",{";
		for(DependencyIdentifier id : dependencyIDs) {
			String addr = id.getAddress();
			if(!addr.isEmpty())
				str += id.getAddress() + ",";
		}
		if(str.charAt(str.length() - 1) == '{') return str + "}>";
		return str.substring(0, str.length() - 1) + "}>";
	}

}