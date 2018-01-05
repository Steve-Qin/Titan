package rpc;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.ItemDBConnection;
import db.DBConnectionFactory;
import entity.Item;
import external.ExternalAPI;
import external.ExternalAPIFactory;
import external.TicketMasterAPI;

/**
 * Servlet implementation class SearchItem
 */
@WebServlet("/search")
public class SearchItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchItem() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    /**
     * 	test sample
     *	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	 *		// TODO Auto-generated method stub
     *		//Create a PrintWriter from response such that we can add data to response.
	 *		PrintWriter out = response.getWriter();
	 *		if (request.getParameter("username") != null) {
	 * 			//Get the username sent from the client
	 *			String username = request.getParameter("username");
	 *          //In the output stream, add something to response body. 
	 *			out.print("Hello " + username);
	 *		}
	 *		// Send response back to client
	 *		out.flush();
	 *		out.close();
	 *	}
	 *
	 *	test to return a html page
	 *	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	 * 		response.setContentType("text/html");
	 *		PrintWriter out = response.getWriter();
	 *		out.println("<html><body>");
	 *		out.println("<h1>This is a HTML page</h1>");
	 *		out.println("</body></html>");
	 *		out.flush();
	 *		out.close();
	 *	}
	 *	
	 *	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	 *		response.setContentType("application/json");
	 *		response.addHeader("Access-Control-Allow-Origin", "*");
	 *
	 *		JSONArray array = new JSONArray();
	 *		try {
	 *			array.put(new JSONObject().put("username", "Steve"));
	 *			array.put(new JSONObject().put("username", "Sylvia"));
	 *		} catch (JSONException e) {
	 *			e.printStackTrace();
	 *		}
	 *		RpcHelper.writeJsonArray(response, array);
	 *	}
	 *	
	 *	// logic without saveItem()
	 *	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	 *			throws ServletException, IOException {
	 *		// Get parameter from HTTP request
	 *		double lat = Double.parseDouble(request.getParameter("lat"));
	 *		double lon = Double.parseDouble(request.getParameter("lon"));
	 *		String term = request.getParameter("term"); // term can be null
	 *
	 *		// call TicketMasterAPI.search to get event data
	 *		ExternalAPI api = ExternalAPIFactory.getExternalAPI();
	 *		List<Item> items = api.search(lat, lon, term);
	 *
	 *		// There should be some saveItem logic here
	 *		
	 *		// Convert Item list back to JSONArray for client
	 *		List<JSONObject> list = new ArrayList<>();
	 *		try {
	 *			for (Item item : items) {
	 *				// Add a thin version of restaurant object
	 *				JSONObject obj = item.toJSONObject();
     *				list.add(obj);
	 *			}
	 *		} catch (Exception e) {
	 *			e.printStackTrace();
	 *		}
	 *		JSONArray array = new JSONArray(list);
	 *		RpcHelper.writeJsonArray(response, array);
	 *	}
	 *
     */
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Get parameter from HTTP request
		String userId = request.getParameter("user_id");
		double lat = Double.parseDouble(request.getParameter("lat"));
		double lon = Double.parseDouble(request.getParameter("lon"));
		String term = request.getParameter("term"); 
		// term can be null

		// saveItem logic here :
		// wrap the part of calling external api into the DBConnection
		// the specific implementation part of db, to not only fetch the data
		// but also save the items into the db as well.
		ItemDBConnection conn = DBConnectionFactory.getDBConnection();
		// in here we call the external api inside conn.searchItem()
		List<Item> items = conn.searchItems(userId, lat, lon, term);
		// Convert Item list back to JSONArray for client
		List<JSONObject> list = new ArrayList<>();

		// get the user's favorite list.
		Set<String> favorite = conn.getFavoriteItemIds(userId);
		try {
			for (Item item : items) {
				// add a thin version of item
				// containing only the information we want to the list
				JSONObject obj = item.toJSONObject();
				// make a new key-value pair representing the favorite information
				// to make it easier to handle in the front end.
				if (favorite != null) {
					obj.put("favorite", favorite.contains(item.getItemId()));
				}
				list.add(obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		JSONArray array = new JSONArray(list);
		RpcHelper.writeJsonArray(response, array);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
