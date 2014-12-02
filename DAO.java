import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Data Access Object for every publication
 * @author Connie Shi
 *
 */
public class DAO {
	static final Logger logger = LogManager.getLogger(DAO.class);
	private Connection connWithArk;
	private Connection connWithWW;
	
	/**
	 * Create connections, query into Ark for all future issues
	 * Compare with existing issues in Woodwing, remove duplicates
	 * Adds issue data in smart_issues and channel data into smart_channeldata
	 * @param pub
	 */
	public DAO(WoodwingPublication pub) {
		try {
			connWithArk = getConnectionToDatabase(WWDataImporter.getArkUrl());
			connWithWW = getConnectionToDatabase(pub.getWwUrl());
			getIssuesFromArkForAllChannels(pub);
			compareIssuesForAllChannels(pub);
			addIssues(pub);
		}
		catch(Exception e) {
			logger.catching(e);
			System.exit(0);
		}
		finally {
			try {
				connWithArk.close();
				connWithWW.close();
			} catch (Exception e) {
				logger.catching(e);
			}
		}
	}
	
	/**
	 * Create a connection with intended database given the URL
	 * @param url
	 * @return
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static Connection getConnectionToDatabase(String url) throws SQLException, ClassNotFoundException {
		Connection con = null;

		Class.forName("com.mysql.jdbc.Driver");
		con = DriverManager.getConnection(
				"jdbc:mysql:"+ url,
				WWDataImporter.getUsername(), WWDataImporter.getPassword());

		return con;
	}

	/**
	 * For every future issue in Ark, it adds to a list of issues for all channels
	 * @param pub
	 * @param connWithArk
	 * @throws SQLException 
	 */
	private void getIssuesFromArkForAllChannels(WoodwingPublication pub) throws SQLException {

		int count = 0;
		Statement stmt = connWithArk.createStatement();

		ResultSet rs = stmt.executeQuery(
				" SELECT name, short_date "
			+ 	" FROM issues "
			+ 	" WHERE Publication_GroupId= "	+ pub.getArkId()
			+	" AND short_date > curdate();" );
		
		int woodwingCode = getWoodwingCode(pub);

		while (rs.next()) { 
			String name = rs.getString("name");
			String short_date = rs.getString("short_date");
			count++;

			for (WoodwingChannel chan: pub.getChannelsList()) {
				WoodwingIssue is = new WoodwingIssue(chan, name, short_date, ++woodwingCode); 
				chan.getArkIssuesList().add(is);
			}
		}
		logger.info(count + " future issue(s) in Ark");
	}

	/**
	 * Get a list of issues existing in the Woodwing Channel to compare
	 * @param pub
	 * @param chan
	 * @param connWithWW
	 * @return
	 * @throws SQLException 
	 */
	private ArrayList<WoodwingIssue> getIssuesFromWWChannel(WoodwingChannel chan) throws SQLException {
		ArrayList<WoodwingIssue> existingIssues = new ArrayList<WoodwingIssue>();

		Statement stmt = connWithWW.createStatement();

		ResultSet rs = stmt.executeQuery(
				" SELECT name "
			+ 	" FROM smart_issues "			
			+ 	" WHERE channelid= " + chan.getId());

		while (rs.next()) {
			String name = rs.getString("name");

			WoodwingIssue is = new WoodwingIssue(chan, name, null, 0);
			existingIssues.add(is);
		} 
		return existingIssues;
	}
	
	/**
	 * Code column in Woodwing's smart_issues dictates in what order digital magazines will appear
	 * Starts with 1000 or the last number entered greater than 1000
	 * @param connWithWW
	 * @param pub
	 * @return
	 * @throws SQLException 
	 */
	private int getWoodwingCode(WoodwingPublication pub) throws SQLException {
		int id = 1000;

		Statement stmt = connWithWW.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT MAX(code) FROM smart_issues;");
		rs.next();
		int temp = rs.getInt("MAX(code)");
		if (temp > 1000)
			id = temp;

		return id;
	}

	/**
	 * Using the entered list of future Ark issues, it compares with existing issues and removes duplicates
	 * @param pub
	 * @param connWithWW
	 * @throws SQLException  
	 */
	private void compareIssuesForAllChannels(WoodwingPublication pub) throws SQLException {
		for (WoodwingChannel chan: pub.getChannelsList()) {
			ArrayList<WoodwingIssue> existingIssues = 
					getIssuesFromWWChannel(chan);
			
			for (Iterator<WoodwingIssue> iterator = chan.getArkIssuesList().iterator(); iterator.hasNext(); ) {
				WoodwingIssue iss = iterator.next();
				if ( existingIssues.contains(iss) )
					iterator.remove();
			}
		}
	}
	
	/**
	 * Inserts records into the appropriate Woodwing's smart_issues
	 * @param pub
	 * @param connWithWW
	 * @throws SQLException 
	 */
	private void addIssues(WoodwingPublication pub) throws SQLException {
		for (WoodwingChannel chan: pub.getChannelsList()) {
			int count = 0;

			for (WoodwingIssue iss: chan.getArkIssuesList()) {
				count++;
				Statement stmt = connWithWW.createStatement();
				stmt.executeUpdate("INSERT INTO smart_issues (name, channelid, code, publdate, deadline, pages, subject, description, active)" +
						" VALUES (\"" + iss.getName() + "\", \"" +chan.getId()+ "\", \""+ iss.getWoodwingCode() +"\", \""+iss.getShortDate()+
						"\", \"" + iss.getShortDate() + "\", \"0\", \"" + iss.getShortDate() + "\", \"" + iss.getShortDate() + "\", \"on\");");
				ResultSet rs = stmt.executeQuery("SELECT last_insert_id();");
				rs.next();
				int	id = rs.getInt("last_insert_id()");
				addChannelData(stmt, chan, id);
				logger.info(iss.toString() + " has been inserted into Woodwing.");
			}
			logger.info(count+" record(s) total inserted into Woodwing Channel "+chan.getName());
		}
	}
	
	/**
	 * Inserts channel data into the appropriate Woodwing's smart_channeldata
	 * @param stmt
	 * @param chan
	 * @param id
	 * @throws SQLException
	 */
	private void addChannelData(Statement stmt, WoodwingChannel chan, int id) throws SQLException {
		Set<String> keys = chan.getHash().keySet();
		for (String key: keys) {
			String value = chan.getHash().get(key);
			stmt.executeUpdate("INSERT INTO smart_channeldata (issue, section, name, value, publication, pubchannel) "
					+ "VALUES ("+ id +", 0, \""+ key +"\", \""+ value +"\", 0, 0);");
		}
		logger.info(chan.getHash().toString());
	}
}
