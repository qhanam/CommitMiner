package multidiffplus.cfg;

/**
 * For creating unique IDs for CFGNodes and CFGEdges.
 */
public class IdGen {

    private int uniqueID;

    public IdGen() {
	this.uniqueID = 0;
    }

    public int getUniqueID() {
	int id = uniqueID;
	uniqueID++;
	return id;
    }

}
