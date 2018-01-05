package db.mysql;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

public class MySQLTableCreation {
	// Run this as Java application to reset db schema.
	public static void main(String[] args) {
		try {
			// Ensure the driver is imported.
			// this line can be deleted for the sake of new version of JDBC API
			// can do import automatically.
			// Class.forName("com.mysql.jdbc.Driver").newInstance();
			// This is java.sql.Connection. Not com.mysql.jdbc.Connection.
			Connection conn = null;

			// Step 1 Connect to MySQL.
			try {
				System.out.println("Connecting to \n" + MySQLDBUtil.URL);
				conn = DriverManager.getConnection(MySQLDBUtil.URL);
			} catch (SQLException e) {
				System.out.println("SQLException " + e.getMessage());
				System.out.println("SQLState " + e.getSQLState());
				System.out.println("VendorError " + e.getErrorCode());
			}
			if (conn == null) {
				return;
			}

			// Step 2 Drop tables in case they exist.
			Statement stmt = conn.createStatement();

			String sql = "DROP TABLE IF EXISTS history";
			stmt.executeUpdate(sql);

			sql = "DROP TABLE IF EXISTS categories";
			stmt.executeUpdate(sql);

			sql = "DROP TABLE IF EXISTS items";
			stmt.executeUpdate(sql);

			// need to drop the history table last, because we have foreign key constraints.
			// so does the creation of history table, need to be created after the constraints
			// have been satisfied.
			sql = "DROP TABLE IF EXISTS users";
			stmt.executeUpdate(sql);

			// Step 3. Create new tables.
			sql = "CREATE TABLE items " + "(item_id VARCHAR(255) NOT NULL, " + " name VARCHAR(255), "
					+ "city VARCHAR(255), " + "state VARCHAR(255), " + "country VARCHAR(255), "
					+ "zipcode VARCHAR(255), " + "rating FLOAT," + "address VARCHAR(255), " + "latitude FLOAT, "
					+ " longitude FLOAT, " + "description VARCHAR(255), " + "snippet VARCHAR(255), "
					+ "snippet_url VARCHAR(255), " + "image_url VARCHAR(255)," + "url VARCHAR(255),"
					+ " PRIMARY KEY ( item_id ))";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE categories " + "(item_id VARCHAR(255) NOT NULL, " + " category VARCHAR(255), "
					+ " PRIMARY KEY ( item_id, category), " + "FOREIGN KEY (item_id) REFERENCES items(item_id))";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE users " + "(user_id VARCHAR(255) NOT NULL, " + " password VARCHAR(255) NOT NULL, "
					+ " first_name VARCHAR(255), last_name VARCHAR(255), " + " PRIMARY KEY ( user_id ))";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE item_history " + "(history_id bigint(20) unsigned NOT NULL AUTO_INCREMENT, "
					+ " user_id VARCHAR(255) NOT NULL , " + " item_id VARCHAR(255) NOT NULL, "
					+ " last_favor_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, " + " PRIMARY KEY (history_id),"
					+ "FOREIGN KEY (item_id) REFERENCES items(item_id),"
					+ "FOREIGN KEY (user_id) REFERENCES users(user_id))";
			stmt.executeUpdate(sql);
			
			
			
			sql = "CREATE TABLE restaurants " + "(business_id VARCHAR(255) NOT NULL, " + " name VARCHAR(255), "
					+ "categories VARCHAR(255), " + "city VARCHAR(255), " + "state VARCHAR(255), " + "stars FLOAT,"
					+ "full_address VARCHAR(255), " + "latitude FLOAT, " + " longitude FLOAT, "
					+ "image_url VARCHAR(255)," + "url VARCHAR(255)," + " PRIMARY KEY ( business_id ))";
			stmt.executeUpdate(sql);

			sql = "CREATE TABLE restaurant_history " + "(visit_history_id bigint(20) unsigned NOT NULL AUTO_INCREMENT, "
					+ " user_id VARCHAR(255) NOT NULL , " + " business_id VARCHAR(255) NOT NULL, "
					+ " last_visited_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, "
					+ " PRIMARY KEY (visit_history_id),"
					+ "FOREIGN KEY (business_id) REFERENCES restaurants(business_id),"
					+ "FOREIGN KEY (user_id) REFERENCES users(user_id))";
			stmt.executeUpdate(sql);
			

			// Step 4: insert data
			// Create a fake user
			sql = "INSERT INTO users " + "VALUES (\"guest\", \"91e7a35e1ed68aa85be0256fd7a2c4a2\", \"Guest\", \"User\")";

			System.out.println("Executing query:\n" + sql);
			stmt.executeUpdate(sql);

			
			System.out.println("Import successfully done.");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}