package wikibase;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import com.google.gson.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class WikibaseManager {
	
	private static final String API_ENDPOINT = "https://chileopendata.imfd.cl/w/api.php";
    private final String username;
    private final String password;
    private final CloseableHttpClient httpClient;
    private String loginToken;
    private String csrfToken;
    private String sessionCookie;

    public WikibaseManager(String username, String password) {
        this.username = username;
        this.password = password;
        this.httpClient = HttpClients.createDefault();
    }

    public void fetchLoginToken() throws IOException {
        HttpGet get = new HttpGet(API_ENDPOINT + "?action=query&meta=tokens&type=login&format=json");
        get.setHeader("Content-Type", "application/x-www-form-urlencoded");

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            this.loginToken = json.get("query").getAsJsonObject().get("tokens").getAsJsonObject().get("logintoken").getAsString();
            System.out.println("Login Token: " + this.loginToken);
        }
    }

    public void login() throws IOException {
        fetchLoginToken();
        
        HttpPost post = new HttpPost(API_ENDPOINT + "?action=login&format=json");
        StringEntity entity = new StringEntity("username=" + username + "&password=" + password + "&logintoken=" + loginToken);
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Login response: " + responseBody);

            // Extract session cookie
            sessionCookie = response.getFirstHeader("Set-Cookie").getValue();
            System.out.println("Session Cookie: " + sessionCookie);
        }
    }

    public void fetchCsrfToken() throws IOException {
        HttpGet get = new HttpGet(API_ENDPOINT + "?action=query&meta=tokens&type=csrf&format=json");
        get.setHeader("Content-Type", "application/x-www-form-urlencoded");
        get.setHeader("Cookie", sessionCookie);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            this.csrfToken = json.get("query").getAsJsonObject().get("tokens").getAsJsonObject().get("csrftoken").getAsString();
            System.out.println("CSRF Token: " + this.csrfToken);
        }
    }

    public String createProperty(String label, String description, String datatype) throws IOException {
        String data = String.format("{\"labels\":{\"es\":{\"language\":\"es\",\"value\":\"%s\"}},\"descriptions\":{\"es\":{\"language\":\"es\",\"value\":\"%s\"}},\"datatype\":\"%s\"}",
                label, description, datatype);
        StringEntity entity = new StringEntity(String.format("new=property&token=%s&data=%s", URLEncoder.encode(csrfToken, StandardCharsets.UTF_8.toString()), URLEncoder.encode(data, StandardCharsets.UTF_8.toString())));

        HttpPost post = new HttpPost(API_ENDPOINT + "?action=wbeditentity&format=json");
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Create Property response: " + responseBody);
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.get("entity").getAsJsonObject().get("id").getAsString();
        }
    }

    public String createItem(String label, String description) throws IOException {
        String data = String.format("{\"labels\":{\"es\":{\"language\":\"es\",\"value\":\"%s\"}},\"descriptions\":{\"es\":{\"language\":\"es\",\"value\":\"%s\"}}}",
                label, description);
        StringEntity entity = new StringEntity(String.format("new=item&token=%s&data=%s", URLEncoder.encode(csrfToken, StandardCharsets.UTF_8.toString()), URLEncoder.encode(data, StandardCharsets.UTF_8.toString())));

        HttpPost post = new HttpPost(API_ENDPOINT + "?action=wbeditentity&format=json");
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Create Item response: " + responseBody);
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.get("entity").getAsJsonObject().get("id").getAsString();
        }
    }
    
    public String addStatementToItem(String itemId, String propertyId, String value, String valueType) throws IOException {
        // Crear el valor principal de la declaración
    	String mainValue = "";
        
        switch (valueType) {
            case "string":
            	mainValue = String.format("\"%s\"", value);
                break;
            case "quantity":
                mainValue = String.format("{\"amount\":\"+%s\",\"unit\":\"1\"}", value);
                break;
            case "time":
            	String year = value.substring(0, 4);
                String month = value.substring(4, 6);
            	mainValue = String.format("{\"time\":\"+%s-%s-01T00:00:00Z\",\"timezone\":0,\"before\":0,\"after\":0,\"precision\":10,\"calendarmodel\":\"http://www.wikidata.org/entity/Q1985727\"}", year, month);                break;
            case "globe-coordinate":
                String[] coordinates = value.replace(',', '.').split(";");
                mainValue = String.format("{\"latitude\":%s,\"longitude\":%s,\"precision\":0.0001,\"globe\":\"http://www.wikidata.org/entity/Q2\"}", coordinates[0], coordinates[1]);
                break;
            case "wikibase-item":
                mainValue = String.format("{\"entity-type\":\"item\",\"numeric-id\":%s}", value.replace("Q", ""));
                break;
        }

        // Crear los parámetros codificados como application/x-www-form-urlencoded
        String encodedToken = URLEncoder.encode(csrfToken, StandardCharsets.UTF_8.toString());
        String encodedValue = URLEncoder.encode(mainValue, StandardCharsets.UTF_8.toString());
        String postData = String.format("entity=%s&property=%s&snaktype=value&value=%s&token=%s",
                URLEncoder.encode(itemId, StandardCharsets.UTF_8.toString()), 
                URLEncoder.encode(propertyId, StandardCharsets.UTF_8.toString()), 
                encodedValue, encodedToken);

        // Crear la entidad StringEntity con los datos codificados
        StringEntity entity = new StringEntity(postData, StandardCharsets.UTF_8);

        // Configurar y ejecutar la solicitud POST
        HttpPost post = new HttpPost(API_ENDPOINT + "?action=wbcreateclaim&format=json");
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        // Ejecutar la solicitud y procesar la respuesta
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Add Statement response: " + responseBody);
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject claim = json.getAsJsonObject("claim");
            return claim.get("id").getAsString();
        }
    }
    
    public boolean entityExists(String entityId) throws IOException {
        String url = API_ENDPOINT + "?action=wbgetentities&format=json&ids=" + entityId;
        HttpGet get = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("entities") && json.getAsJsonObject("entities").has(entityId)) {
                // La entidad (ítem o propiedad) existe
                return true;
            } else {
                // La entidad no existe
                return false;
            }
        }
    }
    
    public boolean entityExistsByLabel(String label, String type, String language) throws IOException {
        String url = API_ENDPOINT + "?action=wbsearchentities&format=json&search=" + URLEncoder.encode(label, "UTF-8") + "&type=" + type + "&language=" + language;
        HttpGet get = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("search") && json.getAsJsonArray("search").size() > 0) {
                // La entidad (ítem o propiedad) existe
                return true;
            } else {
                // La entidad no existe
                return false;
            }
        }
    }
    
    public boolean entityExistsByAlias(String label, String alias, String type, String language) throws IOException {
        String url = API_ENDPOINT + "?action=wbsearchentities&format=json&search=" + URLEncoder.encode(label, "UTF-8") + "&type=" + type + "&language=" + language;
        HttpGet get = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("search") && json.getAsJsonArray("search").size() > 0) {
                for (JsonElement element : json.getAsJsonArray("search")) {
                    JsonObject entity = element.getAsJsonObject();
                    String entityLabel = entity.getAsJsonObject("label").get("value").getAsString();
                 

                    // Check if the label matches
                    if (entityLabel.equalsIgnoreCase(label)) {
                        
                        // Check if the alias matches
                        if (entity.has("aliases")) {
                            JsonObject aliasesObject = entity.getAsJsonObject("aliases");
                            if (aliasesObject.has(language)) {
                                JsonArray aliases = aliasesObject.getAsJsonArray(language);
                                for (JsonElement aliasElement : aliases) {
                                    JsonObject aliasObject = aliasElement.getAsJsonObject();
                                    String aliasValue = aliasObject.get("value").getAsString();
                                    if (aliasValue.equalsIgnoreCase(alias)) {
                                        return true;
                                    }
                                }
                            }
                        }

                    }
                }
            }
            // La entidad no existe
            return false;
        }
    }
    
    public String getEntityByLabel(String label, String type, String language) throws IOException {
        String url = API_ENDPOINT + "?action=wbsearchentities&format=json&search=" + URLEncoder.encode(label, "UTF-8") + "&type=" + type + "&language=" + language;
        HttpGet get = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("search") && json.getAsJsonArray("search").size() > 0) {
                JsonObject firstResult = json.getAsJsonArray("search").get(0).getAsJsonObject();
                // Return the ID of the entity if it exists
                return firstResult.get("id").getAsString();
            } else {
                // The entity does not exist
                return null;
            }
        }
    }
    
    public boolean statementExists(String itemId, String propertyId, String value) throws IOException {
        String url = API_ENDPOINT + "?action=wbgetclaims&format=json&entity=" + itemId + "&property=" + propertyId;
        HttpGet get = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("claims") && json.getAsJsonObject("claims").has(propertyId)) {
                JsonArray claims = json.getAsJsonObject("claims").getAsJsonArray(propertyId);
                for (JsonElement claimElement : claims) {
                    JsonObject claim = claimElement.getAsJsonObject();
                    JsonObject mainsnak = claim.getAsJsonObject("mainsnak");
                    if (mainsnak.has("datavalue")) {
                        JsonObject dataValue = mainsnak.getAsJsonObject("datavalue");
                        String dataType = mainsnak.get("datatype").getAsString();

                        if (dataType.equals("wikibase-item") && dataValue.getAsJsonObject("value").has("numeric-id")) {
                            // Comparar con el ID del ítem (Q)
                            String claimValue = "Q" + dataValue.getAsJsonObject("value").get("numeric-id").getAsString();
                            if (claimValue.equals(value)) {
                                return true; // La declaración existe
                            }
                        } else if (dataType.equals("string") && dataValue.has("value")) {
                            // Comparar con un string
                            String claimValue = dataValue.get("value").getAsString();
                            if (claimValue.equals(value)) {
                                return true; // La declaración existe
                            }
                        } else if (dataType.equals("quantity") && dataValue.has("value")) {
                            // Comparar con un string
                            String claimValue = dataValue.get("value").getAsJsonObject().get("amount").getAsString();
                            if (claimValue.equals("+"+value)) {
                                return true; // La declaración existe
                            }
                        } else if (dataType.equals("time") && dataValue.has("value")) {
                            // Comparar con un string
                        	// Extraer el año y el mes usando substring
                            String year = value.substring(0, 4); 
                            String month = value.substring(4, 6);
                            String claimValue = dataValue.get("value").getAsJsonObject().get("time").getAsString();
                            if (claimValue.contains("+" + year + "-" + month )) {
                                return true; // La declaración existe
                            }
                        }
                        else if (dataType.equals("globe-coordinate") && dataValue.has("value")) {
                            // Comparar con un string
                            return true;
                        }
                    }
                }
            }
            return false; // La declaración no existe
        }
    }
    
    public void addQualifierToStatement(String statementId, String qualifierPropertyId, String qualifierValue) throws IOException {
        // Crear el valor del calificador (qualifier) para el año (tipo "time")
        String qualifierValueFormatted = String.format("{\"time\":\"+%s-01-01T00:00:00Z\",\"timezone\":0,\"before\":0,\"after\":0,\"precision\":9,\"calendarmodel\":\"http://www.wikidata.org/entity/Q1985727\"}", qualifierValue);

        // Crear los parámetros codificados como application/x-www-form-urlencoded
        String encodedToken = URLEncoder.encode(csrfToken, StandardCharsets.UTF_8.toString());
        String encodedQualifierValue = URLEncoder.encode(qualifierValueFormatted, StandardCharsets.UTF_8.toString());
        
        String postData = String.format("claim=%s&property=%s&snaktype=value&value=%s&token=%s",
                URLEncoder.encode(statementId, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(qualifierPropertyId, StandardCharsets.UTF_8.toString()),
                encodedQualifierValue, encodedToken);

        // Crear la entidad StringEntity con los datos codificados
        StringEntity entity = new StringEntity(postData, StandardCharsets.UTF_8);

        // Configurar y ejecutar la solicitud POST
        HttpPost post = new HttpPost(API_ENDPOINT + "?action=wbsetqualifier&format=json");
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        // Ejecutar la solicitud y procesar la respuesta
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Add Qualifier response: " + responseBody);
        }
    }
    
    public JsonObject getClaims(String itemId) throws IOException {
        String url = API_ENDPOINT + "?action=wbgetclaims&entity=" + URLEncoder.encode(itemId, StandardCharsets.UTF_8.toString()) + "&format=json";
        HttpGet get = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.getAsJsonObject("claims");
        }
    }
    
    public String getStatementId(String itemId, String propertyId, String value, String valueType) throws IOException {
        String url = API_ENDPOINT + "?action=wbgetclaims&entity=" + URLEncoder.encode(itemId, StandardCharsets.UTF_8.toString()) + "&format=json";
        HttpGet get = new HttpGet(url);

        try (CloseableHttpResponse response = httpClient.execute(get)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject claims = json.getAsJsonObject("claims");

            if (claims.has(propertyId)) {
                JsonArray propertyClaims = claims.getAsJsonArray(propertyId);
                for (JsonElement claimElement : propertyClaims) {
                    JsonObject claim = claimElement.getAsJsonObject();
                    JsonObject mainsnak = claim.getAsJsonObject("mainsnak");

                    if (mainsnak.has("datavalue")) {
                        JsonObject dataValue = mainsnak.getAsJsonObject("datavalue");
                        String dataType = mainsnak.get("datatype").getAsString();
                        
                        if (valueType.equals("wikibase-item") && dataType.equals("wikibase-item") && dataValue.getAsJsonObject("value").has("numeric-id")) {
                            String claimValue = "Q" + dataValue.getAsJsonObject("value").get("numeric-id").getAsString();
                            if (claimValue.equals(value)) {
                                return claim.get("id").getAsString(); // Return the statement ID
                            }
                        } else if (valueType.equals("string") && dataType.equals("string") && dataValue.get("value").getAsString().equals(value)) {
                            return claim.get("id").getAsString(); // Return the statement ID
                        } else if (valueType.equals("quantity") && dataType.equals("quantity") && dataValue.get("value").getAsJsonObject().get("amount").getAsString().equals("+" + value)) {
                            return claim.get("id").getAsString(); // Return the statement ID
                        } else if (valueType.equals("time") && dataType.equals("time") && dataValue.get("time").getAsString().equals("+" + value + "-01-01T00:00:00Z")) {
                            return claim.get("id").getAsString(); // Return the statement ID
                        }
                    }
                }
            }
        }
        return null; // No matching statement found
    }
    
    public boolean doesStatementWithQualifierExist(String itemId, String propertyId, String value, String qualifierPropertyId, String qualifierValue) throws IOException {
        JsonObject claims = getClaims(itemId);
        if (claims.has(propertyId)) {
            JsonArray propertyClaims = claims.getAsJsonArray(propertyId);
            for (JsonElement claimElement : propertyClaims) {
                JsonObject claim = claimElement.getAsJsonObject();
                if (claim.get("mainsnak").getAsJsonObject().get("datavalue").getAsJsonObject().get("value").isJsonObject()) {
                	if (claim.get("mainsnak").getAsJsonObject().get("datavalue").getAsJsonObject().get("type").toString().replace("\"","").equals("quantity")) {
                		if (!claim.get("mainsnak").getAsJsonObject().get("datavalue").getAsJsonObject().get("value").getAsJsonObject().get("amount").toString().replace("\"","").equals("+" + value)) {
                			continue;
                		}
                	} else if (!claim.get("mainsnak").getAsJsonObject().get("datavalue").getAsJsonObject().get("value").getAsJsonObject().get("id").toString().replace("\"","").equals(value)) {
                		continue;
                	}
                	
                } else {
                	
                	if (!claim.get("mainsnak").getAsJsonObject().get("datavalue").getAsJsonObject().get("value").toString().replace("\"","").equals(value)) {
                    	continue;	
                    }
                	
                }
                
                                
                if (!claim.has("qualifiers")) {
                	return false;
                }
                JsonObject mainsnak = claim.getAsJsonObject("qualifiers");
                if (mainsnak.has(qualifierPropertyId)) {
                	JsonArray qualifierProperty = mainsnak.get(qualifierPropertyId).getAsJsonArray();
                	
                	for (JsonElement element : qualifierProperty) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        String property = jsonObject.get("property").getAsString();
                        if (qualifierPropertyId.equals(property)) {
                        	JsonObject datavalue = jsonObject.getAsJsonObject("datavalue").getAsJsonObject("value");
                            if (datavalue.has("time") && datavalue.get("time").getAsString().contains("+" + qualifierValue)) {
                                return true;
                            }
                        }
                    }
                	
                    
                }
            }
        }
        return false; // La declaración y el calificador no existen
    }
    
    public void addAlias(String itemId, String alias, String language) throws IOException {

        // Crear los parámetros codificados como application/x-www-form-urlencoded
        String encodedToken = URLEncoder.encode(csrfToken, StandardCharsets.UTF_8.toString());
        String encodedAliasValue = URLEncoder.encode(alias, StandardCharsets.UTF_8.toString());

        String postData = String.format("id=%s&set=%s&token=%s",
                URLEncoder.encode(itemId, StandardCharsets.UTF_8.toString()),
                encodedAliasValue, encodedToken);

        // Crear la entidad StringEntity con los datos codificados
        StringEntity entity = new StringEntity(postData, StandardCharsets.UTF_8);

        // Configurar y ejecutar la solicitud POST
        HttpPost post = new HttpPost(API_ENDPOINT + "?action=wbsetaliases&format=json&language=" + language);
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        // Ejecutar la solicitud y procesar la respuesta
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String responseBody = EntityUtils.toString(response.getEntity());
        }
    }
    
    public static List<Integer> getPositionsInOrder(List<VariablePosition> variablePositions, List<String> orderedVariables) {
        List<Integer> positions = new ArrayList<>();

        for (String variable : orderedVariables) {
            for (VariablePosition vp : variablePositions) {
                if (vp.getVariable().equals(variable)) {
                    positions.add(vp.getPosition());
                    break;
                }
            }
        }

        return positions;
    }

    public static void main(String[] args) throws IOException {
    	
    
    	    	
    	WikibaseManager manager;
    	
    	String username = "";
        String password = "";
        String csvFile = "";
        int max_read = 0;
    	
    	if (args.length < 3) {
            System.err.println("Uso: WikibaseManager <usuario> <clave> <archivo> (opcional: <numero de lineas a leer>)");
            System.exit(1);
            
        } else {
        	System.out.println("Corriendo con argumentos");
        	username = args[0];
            password = args[1];
            csvFile = args[2];
            if (args.length >= 4) {
            	max_read = Math.max(Integer.parseInt(args[3]), 1);
            
            }
            else {
            	max_read = 20;
            }
        }

        manager = new WikibaseManager(username, password);
        manager.login();
        manager.fetchCsrfToken();
       

        
        LinkedHashMap<String, String> diccionarioPropiedades = new LinkedHashMap<>();
        diccionarioPropiedades.put("NOM_COM_RBD", "Comuna");
        diccionarioPropiedades.put("DC_TOT", "Empleados");
        diccionarioPropiedades.put("LATITUD", "Ubicacion");
        diccionarioPropiedades.put("LONGITUD", "Ubicacion");
        diccionarioPropiedades.put("MAT_TOTAL", "Matrículados");
        diccionarioPropiedades.put("DOC_FEC_NAC", "Fecha de nacimiento");
        diccionarioPropiedades.put("DOC_GENERO", "Género del Docente");
        diccionarioPropiedades.put("NOM_SUBSECTOR", "Asignatura");
       
        
        LinkedHashMap<String, String> diccionarioPropiedadesConCualificador = new LinkedHashMap<>();
        diccionarioPropiedadesConCualificador.put("Empleados", "quantity");
        diccionarioPropiedadesConCualificador.put("Matrículados", "quantity");
        diccionarioPropiedadesConCualificador.put("Asignatura", "string");
        
        LinkedHashMap<String, String> diccionarioPropiedadesSinCualificador = new LinkedHashMap<>();
        diccionarioPropiedadesSinCualificador.put("Comuna", "string");
        diccionarioPropiedadesSinCualificador.put("Ubicacion", "globe-coordinate");
        diccionarioPropiedadesSinCualificador.put("Fecha de nacimiento", "time");
        diccionarioPropiedadesSinCualificador.put("Género del Docente", "string");
        
    	
    	String cualificador = "0001";
    	
    	String yearPropertyId = manager.getEntityByLabel("Año", "property", "es");
    	    	
    	
    	Set<String> identificadorEstablecimiento = new HashSet<>(Arrays.asList(
    			"NOM_RBD",
    			"NOM_REG_RBD_A",
    			"NOM_COM_RBD"
        ));
    	
    	Set<String> propEstablecimiento = new HashSet<>(Arrays.asList(
                "NOM_COM_RBD",
                "DC_TOT",
                "LATITUD",
                "LONGITUD",
                "MAT_TOTAL"
        ));
    	
    	
    	Set<String> identificadorRegion = new HashSet<>(Arrays.asList(
    			"NOM_REG_RBD_A"
        ));
    	
    	Set<String> propRegion = new HashSet<>(Arrays.asList(
        ));
    	
    	
    	Set<String> identificadorDocente = new HashSet<>(Arrays.asList(
    			"MRUN"
        ));
    	
    	Set<String> propDocente = new HashSet<>(Arrays.asList(
                "DOC_FEC_NAC",
                "DOC_GENERO",
                "NOM_SUBSECTOR"
        ));

        try {

        	List<VariablePosition> matchEstablecimiento = new ArrayList<>();
        	List<VariablePosition> matchRegion = new ArrayList<>();
        	List<VariablePosition> matchDocente = new ArrayList<>();
        	
        	List<VariablePosition> matchingPropEstablecimiento = new ArrayList<>();
        	List<VariablePosition> matchingPropRegion = new ArrayList<>();
        	List<VariablePosition> matchingPropDocente = new ArrayList<>();
        
            try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
                // Read the first line which contains the column names
                String[] columnNamesLine = reader.readNext();
                
                if (columnNamesLine != null && columnNamesLine.length > 0) {
                   
                    String columnNamesString = "";
                    
					for (String line : columnNamesLine) {
                		
						columnNamesString = columnNamesString + line;
                		
                	}
                                        
                    // Split the string into individual column names
                    String[] columnNames = columnNamesString.split(";");
                    
                    // Clean column names by trimming any whitespace
                    for (int i = 0; i < columnNames.length; i++) {
                        columnNames[i] = columnNames[i].trim().replaceAll("[^\\p{Print}]", "");;
                    }
                    
                    // Check against predefined variables
                    for (int i = 0; i < columnNames.length; i++) {
                        String columnName = columnNames[i];
                        if (propEstablecimiento.contains(columnName)) {
                        	matchingPropEstablecimiento.add(new VariablePosition(columnName, i));
                        }
                        if (propRegion.contains(columnName)) {
                        	matchingPropRegion.add(new VariablePosition(columnName, i));
                        }
                        if (propDocente.contains(columnName)) {
                        	matchingPropDocente.add(new VariablePosition(columnName, i));
                        }
                        if ((columnName.compareTo("AGNO")) == 0){
                        	cualificador = String.valueOf(i);
                        }
                        
                        if (identificadorEstablecimiento.contains(columnName)) {
                        	matchEstablecimiento.add(new VariablePosition(columnName, i));
                        }
                        
                        if (identificadorRegion.contains(columnName)) {
                        	matchRegion.add(new VariablePosition(columnName, i));
                        }
                        
                        if (identificadorDocente.contains(columnName)) {
                        	matchDocente.add(new VariablePosition(columnName, i));
                        }
                        
                    }
                }
                
                System.out.println("cualificador: " + cualificador);
                System.out.println("matchEstablecimiento: " + matchEstablecimiento);
                System.out.println("matchRegion: " + matchRegion);
                System.out.println("matchDocente: " + matchDocente);
                
                Boolean hayEstablecimiento = matchEstablecimiento.size() == 3;
                Boolean hayRegion = matchRegion.size() == 1;
                Boolean hayDocente = matchDocente.size() == 1;
                
                List<Integer> posicionesEstablecimiento = null;
                List<Integer> posicionesRegion = null;
                List<Integer> posicionesDocente = null;
                
                if (hayEstablecimiento) {
                	
                	List<String> variablesOrdenadas = Arrays.asList("NOM_RBD", "NOM_REG_RBD_A", "NOM_COM_RBD");
                	posicionesEstablecimiento = getPositionsInOrder(matchEstablecimiento, variablesOrdenadas);
                }
                
                if (hayRegion) {
                	List<String> variablesOrdenadas = Arrays.asList("NOM_REG_RBD_A");
                	posicionesRegion = getPositionsInOrder(matchRegion, variablesOrdenadas);	
                }
				
				if (hayDocente) {
					
					List<String> variablesOrdenadas = Arrays.asList("MRUN");
					posicionesDocente = getPositionsInOrder(matchDocente, variablesOrdenadas);	
				}
				
				LinkedHashMap<String, String> establecimientos = new LinkedHashMap<>();
				LinkedHashMap<String, String> regiones = new LinkedHashMap<>();
				LinkedHashMap<String, String> docentes = new LinkedHashMap<>();
                
				
                for (int i = 0; i < max_read; i++) {
                	String[] nextInLine = reader.readNext();
                    //Arrays.stream(nextInLine).forEach(element -> System.out.print(element + " "));

                	String nextInLineString = "";
                	
                	
                	for (int j = 0; j < nextInLine.length; j++) {
                		nextInLineString = nextInLineString + nextInLine[j];
                	    if (j < nextInLine.length - 1) {
                	    	nextInLineString = nextInLineString + ",";
                	    }
                	}
                    // Split the string into individual column names
                	
                    String[] nextInLineValues = nextInLineString.split(";");
                                      
                    String establecimientoId = "";
                    String regionId = "";
                    String docenteId = "";
                    
                    if (hayEstablecimiento){
                    	
                    	String establecimientoLabel = "";
                    	String establecimientoNombre = "";
                    	for (int j = 0; j < posicionesEstablecimiento.size(); j++) { 		
                    		establecimientoLabel = establecimientoLabel + nextInLineValues[posicionesEstablecimiento.get(j)];
                    		
                    		if (j == 0) {
                    			establecimientoNombre =  nextInLineValues[posicionesEstablecimiento.get(j)];
                    		}
                    		
                    		if (j != posicionesEstablecimiento.size() - 1) {
                    			establecimientoLabel = establecimientoLabel + " ";
                    		}

                    	}
                    	                    	
                    	if (establecimientos.containsKey(establecimientoLabel)) {
                    		
                    		establecimientoId = establecimientos.get(establecimientoLabel);
                    		
                    	} else {
                    		
                    		if (manager.entityExistsByLabel(establecimientoLabel, "item", "es")) {
                    			establecimientoId = manager.getEntityByLabel(establecimientoLabel, "item", "es");
                    			establecimientos.put(establecimientoLabel, establecimientoId);
                    			
                    		} else {
                    			if (!establecimientoLabel.equals("")) {
                    				
                        			establecimientoId = manager.createItem(establecimientoNombre, "");
                        			manager.addAlias(establecimientoId, establecimientoLabel, "es");
                        			establecimientos.put(establecimientoLabel, establecimientoId);
                    				
                    			}
                    		}

                    		
                    	}
                    	
                    	String Latitud = "";
                    	String Longitud = "";
                    	                    	                    	                    	
                    	for (VariablePosition vp : matchingPropEstablecimiento) {
                    		
                    		String propiedadCodigo = vp.getVariable();
                    		int propiedadPosicion = vp.getPosition();
                    		String propiedad = diccionarioPropiedades.get(propiedadCodigo);
                    		
                    		if (manager.entityExistsByLabel(propiedad, "property", "es")) {
                    			
                    			String propiedadId = manager.getEntityByLabel(propiedad, "property", "es");
                    			
                    			if (diccionarioPropiedadesConCualificador.containsKey(propiedad)) {
                    				
                    				String propiedadType = diccionarioPropiedadesConCualificador.get(propiedad);
                    					
                    				String statementId = "";
                    				
                    				
                    				if (manager.statementExists(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion])) {
                    					statementId = manager.getStatementId(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
                    				} else {
                    					statementId = manager.addStatementToItem(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
                    				}
                    				
                    				
                    				//if (manager.addQualifierToStatement(statementId, Latitud, Longitud))
                    				
                    			
                    				
                    				if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], yearPropertyId,  nextInLineValues[Integer.parseInt(cualificador)])) {
                    					                    					
                    					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)]);
                    					
                    				}

                    				
                    			} else if (diccionarioPropiedadesSinCualificador.containsKey(propiedad)) {
                    				
                    				
                    				String propiedadType = diccionarioPropiedadesSinCualificador.get(propiedad);
                    				
                    				String statementId = "";
                    			
                    				switch (propiedadCodigo.toUpperCase()) {
                    				
                    					case "LATITUD":
                    						
                    						Latitud = nextInLineValues[propiedadPosicion];
                    						break;
                    					
                    					case "LONGITUD":
                    						Longitud= nextInLineValues[propiedadPosicion];
                    						break;
                    						
                    					default:
                    						
                    						if (!manager.statementExists(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion])) {
                        						statementId = manager.addStatementToItem(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
    		
                            				}
                    				
                    				}
                    				                    				
                    				if ((propiedadCodigo.toUpperCase().equals("LATITUD") || propiedadCodigo.toUpperCase().equals("LONGITUD")) && (Latitud != null && !Latitud.isEmpty() && Longitud != null && !Longitud.isEmpty())) {
                    					
                    					if (!manager.statementExists(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion])) {
                    						manager.addStatementToItem(establecimientoId, propiedadId, Latitud + ";" + Longitud, propiedadType);
		
                        				}
                    					                    					
                    				} 
                    				
                    			}
                    			
                    			
                    		};
                    		
                    	}
                    	
                    }
                    
					if (hayRegion){
                    	
                    	String regionLabel = "";
                    	for (int j = 0; j < posicionesRegion.size(); j++) { 		
                    		regionLabel = regionLabel + nextInLineValues[posicionesRegion.get(j)];
                    		
                    		if (j != posicionesRegion.size() - 1) {
                    			regionLabel = regionLabel + " ";
                    		}

                    	}
                    	
                    	                    	
                    	if (regiones.containsKey(regionLabel)) {
                    		
                    		regionId = regiones.get(regionLabel);
                    		
                    	} else {
                    		
                    		if (manager.entityExistsByLabel(regionLabel, "item", "es")) {
                    			regionId = manager.getEntityByLabel(regionLabel, "item", "es");
                    			regiones.put(regionLabel, regionId);
                    			
                    		} else {
                    			if (!regionLabel.equals("")) {
                    				
                        			regionId = manager.createItem(regionLabel, "");
                        			regiones.put(regionLabel, regionId);
                    				
                    			} 
                    		}
                    		
                    		
                    		regiones.put(regionLabel, regionId);
                    		
                    	}
                    }

					if (hayDocente){
						
						String docenteLabel = "";
						for (int j = 0; j < posicionesDocente.size(); j++) { 		
							docenteLabel = docenteLabel + nextInLineValues[posicionesDocente.get(j)];
							if (j != posicionesDocente.size() - 1) {
								docenteLabel = docenteLabel + " ";
							}
					
						}
						
						
						
						if (docentes.containsKey(docenteLabel)) {
							
							docenteId = docentes.get(docenteLabel);
							
						} else {
							
							if (manager.entityExistsByLabel(docenteLabel, "item", "es")) {
								docenteId = manager.getEntityByLabel(docenteLabel, "item", "es");
								docentes.put(docenteLabel, docenteId);
								
							} else {
								if (!docenteLabel.replace(" ", "").equals("")) {
									
									docenteId = manager.createItem(docenteLabel, "");
									docentes.put(docenteLabel, docenteId);
									
								} 
							}
							
							
							
						}
						
						for (VariablePosition vp : matchingPropDocente) {
                    		
                    		String propiedadCodigo = vp.getVariable();
                    		int propiedadPosicion = vp.getPosition();
                    		String propiedad = diccionarioPropiedades.get(propiedadCodigo);
                    		
                    		if (manager.entityExistsByLabel(propiedad, "property", "es")) {
                    			
                    			String propiedadId = manager.getEntityByLabel(propiedad, "property", "es");
                    			
                    			if (diccionarioPropiedadesConCualificador.containsKey(propiedad)) {
                    				
                    				String propiedadType = diccionarioPropiedadesConCualificador.get(propiedad);
                    					
                    				String statementId = "";
                    				
                    				
                    				if (manager.statementExists(docenteId, propiedadId, nextInLineValues[propiedadPosicion])) {
                    					statementId = manager.getStatementId(docenteId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
                    				} else {
                    					statementId = manager.addStatementToItem(docenteId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
                    				}
                 
                    				if (!manager.doesStatementWithQualifierExist(docenteId, propiedadId, nextInLineValues[propiedadPosicion], yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)])) {
                    					                    					
                    					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)]);
                    					
                    				}

                    				
                    			} else if (diccionarioPropiedadesSinCualificador.containsKey(propiedad)) {
                    				
                    				
                    				String propiedadType = diccionarioPropiedadesSinCualificador.get(propiedad);
                    				
                    				String statementId = "";
                    				
                    	
                    			
                    				if (!manager.statementExists(docenteId, propiedadId, nextInLineValues[propiedadPosicion])) {
                    					
                						statementId = manager.addStatementToItem(docenteId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
	
                    				}

                    			}
                    			
                    			
                    		};
                    		
                    	}
						
					}
					
					
					if (!establecimientoId.equals("")) {
						
						
						if (!docenteId.equals("")) {
							
							String statementId = "";
							
							String lugarPropiedadId = manager.getEntityByLabel("Establecimiento de Trabajo", "property", "es");
							
							if (manager.statementExists(docenteId, lugarPropiedadId, establecimientoId)) {
            					statementId = manager.getStatementId(docenteId, lugarPropiedadId, establecimientoId, "wikibase-item");
            				} else {
            					statementId = manager.addStatementToItem(docenteId, lugarPropiedadId, establecimientoId, "wikibase-item");
            				}
            				
            				
            				//if (manager.addQualifierToStatement(statementId, Latitud, Longitud))
            				

            				if (!manager.doesStatementWithQualifierExist(docenteId, lugarPropiedadId, establecimientoId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)])) {
            					            					
            					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)]);
            					
            				}
							
						}
						
						if (!regionId.equals("")){
							
							
							String regionPropiedadId = manager.getEntityByLabel("Región", "property", "es");
							
							if (!manager.statementExists(establecimientoId, regionPropiedadId, regionId)) {
								manager.addStatementToItem(establecimientoId, regionPropiedadId, regionId, "wikibase-item");
            				} 
							
							
						}

					
					}
					
					if (!docenteId.equals("")) {
						
						if (!regionId.equals("")){
							
							String statementId = "";
							
							String trabajoRegionPropertyId = manager.getEntityByLabel("Región de Trabajo", "property", "es");
							
							if (manager.statementExists(docenteId, trabajoRegionPropertyId, regionId)) {
								statementId = manager.getStatementId(docenteId, trabajoRegionPropertyId, regionId, "wikibase-item");
            				} else {
            					statementId = manager.addStatementToItem(docenteId, trabajoRegionPropertyId, regionId, "wikibase-item");	
            				}
							
            				//if (manager.addQualifierToStatement(statementId, Latitud, Longitud))
            				

            				if (!manager.doesStatementWithQualifierExist(docenteId, trabajoRegionPropertyId, regionId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)])) {
            					
            					
            					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)]);
            					
            				}
							
							
						}
						
					}
					
					

                }
            }
            
            
            
            
            
            
            System.out.println("Fin");
            
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        
        //System.out.println(manager.getEntityByLabel("TPCA", "item", "es"));
        
    }

}
