import java.util.ArrayList;

/**
 * Publication in Woodwing containing a list of channels related to each publication
 * @author shic
 *
 */
public class WoodwingPublication {
	private String name, arkId, wwUrl;
	private ArrayList<WoodwingChannel> channelsList = new ArrayList<WoodwingChannel>();

	/**
	 * @param name
	 * @param arkId
	 * @param minVersion
	 */
	public WoodwingPublication(String name, String arkId, String wwUrl) {
		this.name = name.trim();
		this.arkId = arkId.trim();
		this.wwUrl = wwUrl.trim();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return the arkId
	 */
	public String getArkId() {
		return arkId;
	}


	/**
	 * @return the channelsList
	 */
	public ArrayList<WoodwingChannel> getChannelsList() {
		return channelsList;
	}
	
	/**
	 * @return the wwUrl
	 */
	public String getWwUrl() {
		return wwUrl;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "WoodwingPublication [name=" + name + ", arkId=" + arkId
				+ ", wwUrl=" + wwUrl+"]";
	}
}
