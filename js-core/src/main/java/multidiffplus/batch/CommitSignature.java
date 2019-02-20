package multidiffplus.batch;

import multidiffplus.commit.Commit.Type;

public class CommitSignature {

    /** The commit ID (hash) of the old revision. */
    private String oldRevision;

    /** The commit ID (hash) of the new revision. */
    private String newRevision;

    /** The type of commit (ie. OTHER, MERGE or BUG_FIX) */
    private Type type;

    /** The timestamp of the new revision. */
    private int timestamp;

    public CommitSignature(String oldRevision, String newRevision, Type type, int timestamp) {
	this.oldRevision = oldRevision;
	this.newRevision = newRevision;
	this.type = type;
	this.timestamp = timestamp;
    }

    public String getOldRevision() {
	return oldRevision;
    }

    public String getNewRevision() {
	return newRevision;
    }

    public Type getType() {
	return type;
    }

    public int getTimestamp() {
	return timestamp;
    }

}
