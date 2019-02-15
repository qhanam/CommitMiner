package multidiffplus.commit;

import java.util.Collections;
import java.util.List;

public class GenericDependencyIdentifier implements DependencyIdentifier {

    Integer dependencyID;

    public GenericDependencyIdentifier(Integer dependencyID) {
	this.dependencyID = dependencyID;
    }

    @Override
    public String getAddress() {
	return dependencyID.toString();
    }

    @Override
    public List<Integer> getAddresses() {
	return Collections.singletonList(dependencyID);
    }

}