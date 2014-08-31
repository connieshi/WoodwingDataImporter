/**
 * Issue to add to Woodwing
 * @author shic
 *
 */
public class WoodwingIssue {
	private String name, shortDate;
	private int woodwingCode;
	
	/**
	 * @param name
	 * @param shortDate
	 */
	public WoodwingIssue(WoodwingChannel chan, String name, String shortDate, int woodwingCode) {
		this.name = getIssueName(chan, name);
		this.shortDate = shortDate;
		this.woodwingCode = woodwingCode;
	}
	
	/**
	 * Change name of issue according to issue naming convention and trim starting or trailing spaces
	 * @param chan
	 * @param issue
	 * @return
	 */
	private String getIssueName(WoodwingChannel chan, String issueName) {
		if (!issueName.contains("FOLIO") && !issueName.equalsIgnoreCase("FOLIO")) {
			String name = chan.getIssueNamingConvention();
			issueName = name.replace("${name}", issueName);
		}
		return issueName.trim();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the shortDate
	 */
	public String getShortDate() {
		return shortDate;
	}

	/**
	 * @return the woodwingCode
	 */
	public int getWoodwingCode() {
		return woodwingCode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "WoodwingIssue [name=" + name + ", shortDate=" + shortDate
				+ ", woodwingCode=" + woodwingCode + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof WoodwingIssue))
			return false;
		WoodwingIssue other = (WoodwingIssue) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
