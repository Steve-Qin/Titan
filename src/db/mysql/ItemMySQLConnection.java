package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.ItemDBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.ExternalAPI;
import external.ExternalAPIFactory;

// singleton pattern
public class ItemMySQLConnection implements ItemDBConnection {
	private static Connection conn;
	private static ItemMySQLConnection instance;
	private static final String CURRENT_PIPELINE = "ticketmaster";
	
	public static ItemMySQLConnection getInstance() {
		if (instance == null) {
			instance = new ItemMySQLConnection();
		}
		return instance;
	}
	
	private ItemMySQLConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").getDeclaredConstructor().newInstance();
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			return;
		}
		// no need to use IGNORE
		// if one user favorite one event multiple times,
		// the weight of this event for recommendation should be
		// greater than those liked once
		// here we use the auto incrementing index of db, so we do not need
		// to add IGNORE here.
		String query = "INSERT INTO item_history (user_id, item_id) VALUES (?, ?)";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String itemId : itemIds) {
				statement.setString(1, userId);
				statement.setString(2, itemId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			return;
		}
		String query = "DELETE FROM item_history WHERE user_id = ? and item_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String itemId : itemIds) {
				statement.setString(1, userId);
				statement.setString(2, itemId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		if (conn == null) {
			return null;
		}
		Set<String> favoriteItemIds = new HashSet<>();
		try {
			String sql = "SELECT item_id from item_history WHERE user_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			// get all favorite ids
			while (rs.next()) {
				String itemId = rs.getString("item_id");
				favoriteItemIds.add(itemId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return favoriteItemIds;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		if (conn == null) {
			return null;
		}
		// first we get ids from history table
		Set<String> itemIds = getFavoriteItemIds(userId);
		Set<Item> favoriteItems = new HashSet<>();
		try {
			for (String itemId : itemIds) {
				String sql = "SELECT * from items WHERE item_id = ? ";
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setString(1, itemId);
				ResultSet rs = statement.executeQuery();
				ItemBuilder builder = new ItemBuilder();
				
				// Because itemId is unique and given one item id there should
				// have only one result returned.
				if (rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setCity(rs.getString("city"));
					builder.setState(rs.getString("state"));
					builder.setCountry(rs.getString("country"));
					builder.setZipcode(rs.getString("zipcode"));
					builder.setRating(rs.getDouble("rating"));
					builder.setAddress(rs.getString("address"));
					builder.setLatitude(rs.getDouble("latitude"));
					builder.setLongitude(rs.getDouble("longitude"));
					builder.setDescription(rs.getString("description"));
					builder.setSnippet(rs.getString("snippet"));
					builder.setSnippetUrl(rs.getString("snippet_url"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setUrl(rs.getString("url"));
				}

				Set<String> categories = getCategories(itemId);
				builder.setCategories(categories);
				favoriteItems.add(builder.build());
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return favoriteItems;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		if (conn == null) {
			return null;
		}
		Set<String> categories = new HashSet<>();
		try {
			String sql = "SELECT category from categories WHERE item_id = ? ";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, itemId);
			ResultSet rs = statement.executeQuery();
			while (rs.next()) {
				categories.add(rs.getString("category"));
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return categories;
	}

	@Override
	public List<Item> searchItems(String userId, double lat, double lon, String term) {
		// connect to external API
		ExternalAPI api = ExternalAPIFactory.getExternalAPI(CURRENT_PIPELINE);
		// cleaned data only containing the info we want.
		List<Item> items = api.search(lat, lon, term);
		for (Item item : items) {
			// Save the item into our own db.
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		if (conn == null) {
			return;
		}
		try {
			// First, insert into items table
			// IGNORE is used to prevent duplicate item insertions, a.k.a same primary keys
			// to be added into the db.
			String sql = "INSERT IGNORE INTO items VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, item.getItemId());
			statement.setString(2, item.getName());
			statement.setString(3, item.getCity());
			statement.setString(4, item.getState());
			statement.setString(5, item.getCountry());
			statement.setString(6, item.getZipcode());
			statement.setDouble(7, item.getRating());
			statement.setString(8, item.getAddress());
			statement.setDouble(9, item.getLatitude());
			statement.setDouble(10, item.getLongitude());
			statement.setString(11, item.getDescription());
			statement.setString(12, item.getSnippet());
			statement.setString(13, item.getSnippetUrl());
			statement.setString(14, item.getImageUrl());
			statement.setString(15, item.getUrl());
			statement.execute();

			// Second, update categories table for each category.
			sql = "INSERT IGNORE INTO categories VALUES (?,?)";
			for (String category : item.getCategories()) {
				statement = conn.prepareStatement(sql);
				statement.setString(1, item.getItemId());
				statement.setString(2, category);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
	}

	@Override
	public String getFullname(String userId) {
		if (conn == null) {
			return null;
		}
		String name = "";
		try {
			String sql = "SELECT first_name, last_name from users WHERE user_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				name += String.join(" ", rs.getString("first_name"), rs.getString("last_name"));
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return name;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		if (conn == null) {
			return false;
		}
		try {
			String sql = "SELECT user_id from users WHERE user_id = ? and password = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			statement.setString(2, password);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				return true;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return false;
	}

	@Override
	public boolean exist(String userId) {
		if (conn == null) {
			return false;
		}
		try {
			String sql = "SELECT user_id from users WHERE user_id = ?";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			ResultSet rs = statement.executeQuery();
			if (rs.next()) {
				return true;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} 
		return false;

	}
	
	@Override
	public void insertUser(String userId, String pwd, String firstName, String lastName) {
		try {
			Connection curConn = null;
			try {
				System.out.println("Connecting to \n" + MySQLDBUtil.URL);
				curConn = DriverManager.getConnection(MySQLDBUtil.URL);
			} catch (SQLException e) {
				System.out.println("SQLException " + e.getMessage());
				System.out.println("SQLState " + e.getSQLState());
				System.out.println("VendorError " + e.getErrorCode());
			}
			if (conn == null) {
				return;
			}
			Statement stmt = curConn.createStatement();
			String sql = "INSERT INTO users VALUES (" + "\"" + userId + "\"" + ", \"" + pwd + "\"" + ", \"" + firstName + "\"" + ", \"" + lastName + "\")";
			System.out.println("Executing query:\n" + sql);
			stmt.executeUpdate(sql);
			System.out.println("Import successfully done.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
