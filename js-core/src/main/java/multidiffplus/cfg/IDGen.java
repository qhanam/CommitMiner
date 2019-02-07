package multidiffplus.cfg;

/**
 * For creating unique IDs for CFGNodes and CFGEdges.
 */
public class IDGen {

    private int uniqueID;

    public IDGen() {
	this.uniqueID = 0;
    }

    public int getUniqueID() {
	int id = uniqueID;
	uniqueID++;
	return id;
    }

}
