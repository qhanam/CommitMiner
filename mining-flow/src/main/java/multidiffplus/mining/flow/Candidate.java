package multidiffplus.mining.flow;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A commit containing a change to an interesting API call.
 */
public class Candidate {

    /** The project ID. */
    private String projectID;

    /** The commit ID. */
    private String commitID;

    /** The url of the commit on GitHub. */
    private String uri;

    /** The path to the file in the project. */
    private String file;

    /** The local address of the pre-commit file. */
    private File oldFile;

    /** The local address of the post-commit file. */
    private File newFile;

    public Candidate(String uri, String file, File oldFile, File newFile) {

	this.uri = uri;
	this.file = file;
	this.oldFile = oldFile;
	this.newFile = newFile;

	/* Extract the project ID and commit ID from the URL. */

	Pattern p = Pattern
		.compile("https?://github\\.com/(?<project>[^/]+/[^/]+)/commit/(?<commit>.+)");
	Matcher m = p.matcher(uri);

	if (m.find()) {
	    this.projectID = m.group("project");
	    this.commitID = m.group("commit");
	} else {
	    this.projectID = "UNK";
	    this.commitID = "UNK";
	}

    }

    /** @return The ID of the project which contains this candidate. */
    public String getProject() {
	return projectID;
    }

    /** @return The ID of the commit which contains this candidate. */
    public String getCommitID() {
	return commitID;
    }

    /** @return The URI of the commit which contains this candidate. */
    public String getURI() {
	return uri;
    }

    public String getFile() {
	return file;
    }

    public File getOldFile() {
	return oldFile;
    }

    public File getNewFile() {
	return newFile;
    }

    @Override
    public boolean equals(Object o) {
	if (!(o instanceof Candidate))
	    return false;
	Candidate c = (Candidate) o;
	return uri.equals(c.uri) && file.equals(c.file);
    }

    @Override
    public int hashCode() {
	return (uri + file).hashCode();
    }

}