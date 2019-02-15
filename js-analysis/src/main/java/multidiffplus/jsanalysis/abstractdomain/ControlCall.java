package multidiffplus.jsanalysis.abstractdomain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import multidiffplus.commit.DependencyIdentifier;

/**
 * Stores the state of control flow changes due to changes in function calls.
 */
public class ControlCall implements DependencyIdentifier {

    /**
     * AST node IDs of modified callsites of this method.
     */
    Set<Integer> callsites;

    public ControlCall() {
	this.callsites = new HashSet<Integer>();
    }

    public ControlCall(Set<Integer> callsites) {
	this.callsites = callsites;
    }

    /**
     * Update the control call domain for a new callsite. We only track callsites
     * one level deep.
     */
    public ControlCall update(Integer callsite) {
	Set<Integer> callsites = new HashSet<Integer>();
	callsites.add(callsite);
	return new ControlCall(callsites);
    }

    public ControlCall join(ControlCall cc) {
	Set<Integer> callsites = new HashSet<Integer>(this.callsites);
	callsites.addAll(cc.callsites);
	return new ControlCall(callsites);
    }

    public boolean isChanged() {
	return !callsites.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof ControlCall))
	    return false;
	ControlCall cc = (ControlCall) o;
	if (callsites.size() != cc.callsites.size())
	    return false;
	for (Integer callsite : callsites) {
	    if (!cc.callsites.contains(callsite))
		return false;
	}
	return true;
    }

    @Override
    public String getAddress() {
	String id = "";
	if (callsites.isEmpty())
	    return "";
	for (Integer callsite : callsites) {
	    id += callsite + ",";
	}
	return id.substring(0, id.length() - 1);
    }

    @Override
    public List<Integer> getAddresses() {
	List<Integer> addresses = new ArrayList<Integer>();
	addresses.addAll(callsites);
	return addresses;
    }
}
