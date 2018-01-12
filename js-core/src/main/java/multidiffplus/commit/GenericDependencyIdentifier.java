package multidiffplus.commit;

public class GenericDependencyIdentifier implements DependencyIdentifier {
	
	Integer dependencyID;

	public GenericDependencyIdentifier(Integer dependencyID) {
		this.dependencyID = dependencyID;
	}

	@Override
	public String getAddress() {
		return dependencyID.toString();
	}

}