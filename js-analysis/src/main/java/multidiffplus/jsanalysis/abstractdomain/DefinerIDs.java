package multidiffplus.jsanalysis.abstractdomain;

import java.util.HashSet;
import java.util.Set;

public class DefinerIDs {

	/** The set of definer IDs this BValue may point to. **/
	private Set<Integer> definerIDs;

	private DefinerIDs() {
		this.definerIDs = new HashSet<Integer>();
	}

	private DefinerIDs(Set<Integer> definerIDs) {
		this.definerIDs = definerIDs;
	}
	
	public boolean isEmpty() {
		return definerIDs.isEmpty();
	}
	
	public DefinerIDs weakUpdate(Integer definerID) {
		Set<Integer> updated = new HashSet<Integer>(definerIDs);
		updated.add(definerID);
		return new DefinerIDs(updated);
	}
	
	public DefinerIDs strongUpdate(Integer definerID) {
		Set<Integer> updated = new HashSet<Integer>();
		updated.add(definerID);
		return new DefinerIDs(updated);
	}

	public DefinerIDs join(DefinerIDs dids) {
		Set<Integer> joined = new HashSet<Integer>(definerIDs);
		joined.addAll(dids.getDefinerIDs());
		return new DefinerIDs(joined);
	}
	
	public Set<Integer> getDefinerIDs() {
		return definerIDs;
	}
	
	public static DefinerIDs bottom() {
		return new DefinerIDs();
	}

	public static DefinerIDs inject(Integer definerID) {
		Set<Integer> definerIDs = new HashSet<Integer>();
		definerIDs.add(definerID);
		return new DefinerIDs(definerIDs);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof DefinerIDs)) return false;
		DefinerIDs d = (DefinerIDs)o;
		if(definerIDs.size() != d.definerIDs.size()) return false;
		for(Integer id : definerIDs) {
			if(!d.definerIDs.contains(id)) return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		String s = "";
		for(Integer definerID : this.definerIDs) {
			s += definerID + ",";
		}
		if(s.isEmpty()) return s;
		return s.substring(0, s.length() - 1);
	}

}