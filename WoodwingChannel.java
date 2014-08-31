import java.util.ArrayList;
import java.util.HashMap;

/**
 * Channel containing a list of issues to add to Woodwing
 * Generally channel is FOLIO or Print
 * @author shic
 *
 */
public class WoodwingChannel {
	private String name, id, issueNamingConvention;
	private HashMap<String, String> hash;
	private ArrayList<WoodwingIssue> arkIssuesList = new ArrayList<WoodwingIssue>();
	
	/**
	 * @param name
	 * @param id
	 * @param issueNamingConvention
	 */
	public WoodwingChannel(String name, String id, String issueNamingConvention, HashMap<String, String> hash) {
		this.name = name.trim();
		this.id = id.trim();
		this.issueNamingConvention = issueNamingConvention.trim();
		this.hash = hash;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	/**
	 * @return the issueNamingConvention
	 */
	public String getIssueNamingConvention() {
		return issueNamingConvention;
	}

	/**
	 * @return
	 */
	public HashMap<String, String> getHash() {
		return hash;
	}

	/**
	 * @return the arkIssuesList
	 */
	public ArrayList<WoodwingIssue> getArkIssuesList() {
		return arkIssuesList;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "WoodwingChannel [name=" + name + ", id=" + id
				+ ", issueNamingConvention=" + issueNamingConvention+"]";
	}
}
