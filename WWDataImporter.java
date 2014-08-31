import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Uses issue data from Ark to add records into Woodwing's tables: smart_issues and smart_channeldata
 * @author shic
 *
 */
public class WWDataImporter {
	static final Logger logger = LogManager.getLogger(WWDataImporter.class);
	private DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	private DocumentBuilder dBuilder;
	private static String username, password, arkUrl;
	
	/**
	 * WoodwingService reads in the configuration file path passed as argument
	 * It puts all publications in a list and for every publication, it creates a Data Access Object
	 * @param f
	 */
	public WWDataImporter(File f) {
		try {
			ArrayList<WoodwingPublication> pubsList = readConfigFile(f);
			logger.info(pubsList.size() + " Woodwing Publication(s) total in configuration file.");
		
			for(WoodwingPublication pub: pubsList) {
				new DAO(pub);
			}
		}
		catch(Exception e) {
			logger.catching(e);
			System.exit(0);
		}
	}
	
	/**
	 * Read configuration file in XML
	 * @param pathToConfig
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	private ArrayList<WoodwingPublication> readConfigFile(File pathToConfig) throws ParserConfigurationException, SAXException, IOException, SQLException, ClassNotFoundException {
		ArrayList<WoodwingPublication> pubsList = new ArrayList<WoodwingPublication>();

		dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(pathToConfig);
		doc.getDocumentElement().normalize();

		getAccessInfo(doc);
		readPublicationInfo(doc, pubsList);

		return pubsList;
	}
	
	/**
	 * Get user information to access database
	 * @param doc
	 */
	private void getAccessInfo(Document doc) {
		NodeList nList = doc.getElementsByTagName("user");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				username = eElement.getElementsByTagName("username").item(0).getTextContent();
				password = eElement.getElementsByTagName("password").item(0).getTextContent();
				arkUrl = eElement.getElementsByTagName("ark-db-url").item(0).getTextContent();
				
				logger.info("Accessing database with username: "+ username + " and Ark URL: " + arkUrl);
			}		
		}
	}
	
	/**
	 * Get information for every publication listed in configuration file
	 * @param doc
	 * @param pubsList
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	private void readPublicationInfo(Document doc, ArrayList<WoodwingPublication> pubsList) throws SQLException, ClassNotFoundException {
		NodeList nList = doc.getElementsByTagName("publication");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				String name = eElement.getAttributes().getNamedItem("name").getNodeValue();
				String arkId = eElement.getAttributes().getNamedItem("ark-id").getNodeValue();
				String wwUrl = eElement.getElementsByTagName("ww-db-url").item(0).getTextContent();
			
				WoodwingPublication pub = new WoodwingPublication (name, arkId, wwUrl);
				logger.info(pub.toString());
				HashMap<String, String> hashForPrint = getRequiredProperties(pub);
				HashMap<String, String> hashForFolio = getRequiredProperties(pub);
				readChannelInfo(doc, pub, hashForPrint, hashForFolio);
				pubsList.add(pub);
			}		
		}
	}
	
	
	/**
	 * Get information for all channels for all publications
	 * @param doc
	 * @param pub
	 */
	private void readChannelInfo(Document doc, WoodwingPublication pub, HashMap<String, String> hashForPrint, HashMap<String, String> hashForFolio) {
		NodeList nList = doc.getElementsByTagName("channel");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				String channelName = eElement.getAttributes().getNamedItem("name").getNodeValue();
				String id = eElement.getAttributes().getNamedItem("id").getNodeValue();
				String issueNamingConvention = eElement.getAttributes().getNamedItem("issue-naming-convention").getNodeValue();
				
				WoodwingChannel chan;

				if (channelName.toLowerCase().contains("folio")) {
					chan = new WoodwingChannel(channelName, id, issueNamingConvention, hashForFolio);
					getChannelData(doc, chan);
				}
				else {
					chan = new WoodwingChannel(channelName, id, issueNamingConvention, hashForPrint);
				}
				logger.info(chan.toString());
				pub.getChannelsList().add(chan);
			}		
		}
	}
	
	/**
	 * Get data for channel properties such as page navigation, etc.
	 * Matches property-name String in config file to String key in hash
	 * Values automatically set to null unless the Strings match
	 * @param doc
	 * @param chan
	 */
	private void getChannelData(Document doc, WoodwingChannel chan) {
		NodeList nList = doc.getElementsByTagName("data");
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				String propertyName = eElement.getElementsByTagName("property-name").item(0).getTextContent();
				String propertyValue = eElement.getElementsByTagName("property-value").item(0).getTextContent();

				chan.getHash().put(propertyName, propertyValue);
			}
		}
	}
	
	/**
	 * Queries into Woodwing for a list of properties required for every issue WHERE entity="Issue"
	 * Maps property names (keys) to null values
	 * @param pub
	 * @return
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	private HashMap<String, String> getRequiredProperties(WoodwingPublication pub) throws SQLException, ClassNotFoundException {
		HashMap<String, String> hash = new HashMap<String, String>();

		Connection connWithWW = DAO.getConnectionToDatabase(pub.getWwUrl());
		Statement stmt = connWithWW.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT name FROM smart_properties WHERE entity=\"Issue\";");

		while (rs.next()) { 
			String name = rs.getString("name");
			hash.put(name, null);
		}
		return hash;
	}

	/**
	 * @return the username
	 */
	public static String getUsername() {
		return username;
	}

	/**
	 * @return the password
	 */
	public static String getPassword() {
		return password;
	}

	/**
	 * @return the arkUrl
	 */
	public static String getArkUrl() {
		return arkUrl;
	}
	
	public static void main(String[]args) {
		if (args.length != 1) {
			logger.info("USAGE: java -jar WWDataImporter.jar [config-file-path]");
			System.exit(0);
		}
		File configuration = new File(args[0]);
		if (!configuration.isFile()) {
			logger.info("Invalid file path");
			System.exit(0);
		}
		if ( !configuration.canRead() ) {
			logger.info("File cannot be read");
			System.exit(0);
		}
		new WWDataImporter(configuration);
	}
}