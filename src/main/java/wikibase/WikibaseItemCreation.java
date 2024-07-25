package wikibase;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

public class WikibaseItemCreation {
	
public static void main(String[] args) throws Exception{
		
		Scanner sc = new Scanner(new File("/Users/ruben/Desktop/titulo/Dotación_docente_2023.csv"));
		
		sc.useDelimiter(";");
		
		for(int i = 0; i < 10; i++) {
			
			System.out.println(sc.next());
			
		}
		
		//String wikibaseApiUrl = "http://localhost:80/w/api.php";
		
		//String user = "ChileWikibase";
		//String password = "kravZnr19RReudY";
		
		
		//String loginToken;
		
		//try {
		//	loginToken = getLoginToken(wikibaseApiUrl, user, password);
		//} catch (JSONException e) {
			
		//	loginToken = null;
		//}
		
		
		//if (loginToken != null) {
        //    System.out.println("Login successful!");

            // Create a new item
            //String newItemId1 = createNewItem(wikibaseApiUrl, loginToken, "IT7");
            
            //String newItemId2 = createNewItem(wikibaseApiUrl, loginToken, "IT8");
            
            //String newPropertyId = createNewProperty(wikibaseApiUrl, loginToken, "PT8");
            
          //  String newLink = linkItems(wikibaseApiUrl, loginToken, "P20", "Q29", "28");
            

            // Check if item creation was successful
            //if (newLink != null) {
                //System.out.println("Item created with ID: " + newItemId1);
                //System.out.println("Item created with ID: " + newItemId2);
                //System.out.println("Property created with ID: " + newPropertyId);
            //    System.out.println("Link created with ID: " + newLink);
            //} else {
            //    System.out.println("Failed to create item.");
            //}
        //} else {
        //    System.out.println("Login failed.");
        //}
    }

    private static String getLoginToken(String apiUrl, String user, String password) throws JSONException {
        Map<Object, Object> loginTokenData = new HashMap<>();
        loginTokenData.put("action", "query");
        loginTokenData.put("meta", "tokens");
        loginTokenData.put("type", "csrf");
        loginTokenData.put("format", "json");

        HttpResponse<String> loginTokenResponse = performPostRequest(apiUrl, loginTokenData);
        
		JSONObject json = new JSONObject(loginTokenResponse.body());
		
		
        JSONObject query = json.getJSONObject("query");
        JSONObject tokens = query.getJSONObject("tokens");
        String loginToken = tokens.getString("csrftoken");  
        
        System.out.println(json);
        
        if (loginToken != null && loginTokenResponse.statusCode() == 200) {
            
            return loginToken;
        } else {
            return null; // Request error
        }
    }
    private static String createNewItem(String apiUrl, String loginToken, String itemName) {
        Map<Object, Object> data = new HashMap<>();
        data.put("action", "wbeditentity");
        data.put("new", "item");
        data.put("token", loginToken);
        data.put("format", "json");
        
        System.out.println(loginToken);
        
        data.put("data", "{\"labels\":{\"de\":{\"language\":\"de\",\"value\":\"de-value\"},\"en\":{\"language\":\"en\",\"value\":\"" + itemName + "\"}}}");

        HttpResponse<String> response = performPostRequest(apiUrl, data);
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            return null;
        }
    }
    
    private static String createNewProperty(String apiUrl, String loginToken, String propertyName) {
        Map<Object, Object> data = new HashMap<>();
        data.put("action", "wbeditentity");
        data.put("new", "property");
        data.put("token", loginToken);
        data.put("format", "json");
       
        
        data.put("data", "{\"labels\":{\"en\":{\"language\":\"en\",\"value\":\"" + propertyName + "\"}}, \"datatype\": \"wikibase-item\"}");
        
        HttpResponse<String> response = performPostRequest(apiUrl, data);
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            return null;
        }
    }
    
    private static String linkItems(String apiUrl, String loginToken, String propertyId, String itemId1, String itemId2) {
        // Construct the claim data
    	Map<Object, Object> data = new HashMap<>();
        data.put("action", "wbcreateclaim");
        data.put("format", "json");
        data.put("token", loginToken);
        data.put("entity", itemId1);  // The first object/entity ID
        data.put("snaktype", "value");
        data.put("property", propertyId);  // The property ID
        // The second object/entity ID as the value of the property
        data.put("value", "{\"entity-type\":\"item\",\"numeric-id\":" + itemId2 + "}");
        // Send the API request and handle the response
        HttpResponse<String> response = performPostRequest(apiUrl, data);
        return response.body();
    }

    private static HttpResponse<String> performPostRequest(String apiUrl, Map<Object, Object> data) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(buildFormDataFromMap(data))
                    .build();

            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
        var builder = new StringBuilder();
        
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
    
    

}
