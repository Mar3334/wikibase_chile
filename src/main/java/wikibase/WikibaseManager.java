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
        }  catch(Exception e) {
        	
        	throw e;
        	
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
    
    public void addQualifierToStatement(String statementId, String qualifierPropertyId, String qualifierValue, String qualifierType) throws IOException {
        // Construir el valor del calificador basado en el tipo de propiedad
        String qualifierValueFormatted;
        if ("time".equals(qualifierType)) {
            qualifierValueFormatted = String.format("{\"time\":\"+%s-01-01T00:00:00Z\",\"timezone\":0,\"before\":0,\"after\":0,\"precision\":9,\"calendarmodel\":\"http://www.wikidata.org/entity/Q1985727\"}", qualifierValue);
        } else if ("quantity".equals(qualifierType)) {
            qualifierValueFormatted = String.format("{\"amount\":\"+%s\",\"unit\":\"1\"}", qualifierValue);
        } else if ("wikibase-item".equals(qualifierType)) {
            qualifierValueFormatted = String.format("{\"entity-type\":\"item\",\"numeric-id\":%s}", qualifierValue).replace("Q", "");
        } else {
            throw new IllegalArgumentException("Unsupported qualifier type: " + qualifierType);
        }

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
        	
        	//System.out.println(qualifierPropertyId);
        	//System.out.println(qualifierValue);
        	//System.out.println(qualifierType);
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
    
    public void removeRegionClaims(String itemId, String claimId) throws IOException {
        // Crear los parámetros codificados como application/x-www-form-urlencoded
        String encodedToken = URLEncoder.encode(csrfToken, StandardCharsets.UTF_8.toString());
        String encodedItemId = URLEncoder.encode(itemId, StandardCharsets.UTF_8.toString());

        // Crear el objeto JSON para eliminar el claim
        String data = String.format("{\"claims\":[{\"id\":\"%s\",\"remove\":\"true\"}]}", claimId);

        String postData = String.format("id=%s&data=%s&token=%s", 
            encodedItemId, URLEncoder.encode(data, StandardCharsets.UTF_8.toString()), encodedToken);

        // Crear la entidad StringEntity con los datos codificados
        StringEntity entity = new StringEntity(postData, StandardCharsets.UTF_8);

        // Configurar y ejecutar la solicitud POST para eliminar el claim
        HttpPost post = new HttpPost(API_ENDPOINT + "?action=wbeditentity&format=json");
        post.setEntity(entity);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        // Ejecutar la solicitud y procesar la respuesta
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            String responseBody = EntityUtils.toString(response.getEntity());
            System.out.println("Response: " + responseBody);
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
                // Verificar el valor del mainsnak
                if (claim.get("mainsnak").getAsJsonObject().get("datavalue").getAsJsonObject().get("value").isJsonObject()) {
                    JsonObject datavalue = claim.get("mainsnak").getAsJsonObject().get("datavalue").getAsJsonObject();
                    String datatype = datavalue.get("type").getAsString();
                    if ("quantity".equals(datatype)) {
                        if (!datavalue.get("value").getAsJsonObject().get("amount").getAsString().equals("+" + value)) {
                            continue;
                        }
                    } else if ("wikibase-entityid".equals(datatype)) {
                        if (!datavalue.get("value").getAsJsonObject().get("id").getAsString().equals(value)) {
                            continue;
                        }
                    } else {
                        if (!datavalue.get("value").getAsString().equals(value)) {
                            continue;
                        }
                    }
                } else {
                    if (!claim.get("mainsnak").getAsJsonObject().get("datavalue").getAsJsonObject().get("value").getAsString().equals(value)) {
                        continue;
                    }
                } 
                // Verificar la existencia de calificadores
                if (!claim.has("qualifiers")) {
                    return false;
                }
                                
                JsonObject qualifiers = claim.getAsJsonObject("qualifiers");
                if (qualifiers.has(qualifierPropertyId)) {
                    JsonArray qualifierArray = qualifiers.getAsJsonArray(qualifierPropertyId);
                                        
                    for (JsonElement element : qualifierArray) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        String property = jsonObject.get("property").getAsString();
                        
                        if (qualifierPropertyId.equals(property)) {
                            JsonObject qualifierDatavalue = jsonObject.getAsJsonObject("datavalue").getAsJsonObject("value");
                            
                            // Verificación específica para P28 (wikibase-item)
                            if ("wikibase-entityid".equals(jsonObject.getAsJsonObject("datavalue").get("type").getAsString())) {
                                if (qualifierDatavalue.get("id").getAsString().equals(qualifierValue)) {
                                    return true;
                                }
                            } else if (qualifierDatavalue.has("time")) {
                                if (qualifierDatavalue.get("time").getAsString().contains("+" + qualifierValue)) {
                                    return true;
                                }
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
    
 // Método auxiliar para comprobar si el conjunto contiene el elemento, ignorando mayúsculas y minúsculas
    private static boolean containsIgnoreCase(Set<String> set, String value) {
        for (String item : set) {
            if (value.contains(item.toUpperCase())) {
                return true;
            }
        }
        return false;
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
        diccionarioPropiedades.put("DC_TOT", "empleados");
        diccionarioPropiedades.put("LATITUD", "ubicacion");
        diccionarioPropiedades.put("LONGITUD", "ubicacion");
        diccionarioPropiedades.put("MAT_TOTAL", "matrículados total");
        diccionarioPropiedades.put("DOC_FEC_NAC", "fecha de nacimiento");
        diccionarioPropiedades.put("DOC_GENERO", "género del Docente");
        diccionarioPropiedades.put("NOM_SUBSECTOR", "asignatura");
        diccionarioPropiedades.put("ESTADO_ESTAB", "P37");
        diccionarioPropiedades.put("COD_ENSE", "nivel de enseñanza");
        diccionarioPropiedades.put("PROM_ASIS", "promedio de asistencia");
        diccionarioPropiedades.put("CUR_SIM_TOT", "total de cursos simples");
        diccionarioPropiedades.put("CUR_COMB_TOT", "total de cursos combinados");
        diccionarioPropiedades.put("COD_DEPE", "P36");
        diccionarioPropiedades.put("RURAL_RBD", "P38");
        diccionarioPropiedades.put("ORI_RELIGIOSA", "P40");
        
        diccionarioPropiedades.put("MAT_HOM_TOT", "personas matrículadas");
        diccionarioPropiedades.put("MAT_MUJ_TOT", "personas matrículadas");
        diccionarioPropiedades.put("MAT_SI_TOT", "personas matrículadas");
        
        diccionarioPropiedades.put("APR_HOM_TO", "personas aprobadas");
        diccionarioPropiedades.put("APR_MUJ_TO", "personas aprobadas");
        diccionarioPropiedades.put("APR_SI_TO", "personas aprobadas");
        diccionarioPropiedades.put("APR_NB", "personas aprobadas");
        
        diccionarioPropiedades.put("REP_HOM_TO", "personas reprobadas");
        diccionarioPropiedades.put("REP_MUJ_TO", "personas reprobadas");
        diccionarioPropiedades.put("REP_SI_TO", "personas reprobadas");
        
        diccionarioPropiedades.put("RET_HOM_TO", "personas retiradas");
        diccionarioPropiedades.put("RET_MUJ_TO", "personas retiradas");
        diccionarioPropiedades.put("RET_SI_TO", "personas retiradas");
        
        diccionarioPropiedades.put("TRA_HOM_TO", "personas transferidas");
        diccionarioPropiedades.put("TRA_SI_TO", "personas transferidas");
        diccionarioPropiedades.put("TRA_MUJ_TO", "personas transferidas");
        
        diccionarioPropiedades.put("SI_HOM_TO", "personas situacion final desconocida");
        diccionarioPropiedades.put("SI_MUJ_TO", "personas situacion final desconocida");
        diccionarioPropiedades.put("SI_SI_TO", "personas situacion final desconocida");
        
        
        LinkedHashMap<String, String> diccionarioPropiedadesConCualificador = new LinkedHashMap<>();
        diccionarioPropiedadesConCualificador.put("empleados", "quantity");
        diccionarioPropiedadesConCualificador.put("matrículados total", "quantity");
        diccionarioPropiedadesConCualificador.put("asignatura", "string");
        diccionarioPropiedadesConCualificador.put("nivel de enseñanza", "wikibase-item");
        diccionarioPropiedadesConCualificador.put("promedio de asistencia", "quantity");
        diccionarioPropiedadesConCualificador.put("total de cursos simples", "quantity");
        diccionarioPropiedadesConCualificador.put("total de cursos combinados", "quantity");
        diccionarioPropiedadesConCualificador.put("personas matrículadas", "quantity");
        diccionarioPropiedadesConCualificador.put("personas aprobadas", "quantity");
        diccionarioPropiedadesConCualificador.put("personas reprobadas", "quantity");
        diccionarioPropiedadesConCualificador.put("personas retiradas", "quantity");
        diccionarioPropiedadesConCualificador.put("personas transferidas", "quantity");
        diccionarioPropiedadesConCualificador.put("personas situacion final desconocida", "quantity");
        
        
        LinkedHashMap<String, String> diccionarioPropiedadesSinCualificador = new LinkedHashMap<>();
        diccionarioPropiedadesSinCualificador.put("ubicacion", "globe-coordinate");
        diccionarioPropiedadesSinCualificador.put("fecha de nacimiento", "time");
        diccionarioPropiedadesSinCualificador.put("género del Docente", "string");
        diccionarioPropiedadesSinCualificador.put("P37", "wikibase-item");
        diccionarioPropiedadesSinCualificador.put("P38", "wikibase-item");
        diccionarioPropiedadesSinCualificador.put("P36", "wikibase-item");
        diccionarioPropiedadesSinCualificador.put("P40", "wikibase-item");
        
        
        Map<String, String> educationalLevels = new LinkedHashMap<>();

        educationalLevels.put("110", "Q17346"); // ENSEÑANZA BÁSICA
        educationalLevels.put("160", "Q17347"); // EDUCACIÓN BÁSICA COMÚN ADULTOS (DECRETO 77/1982)
        educationalLevels.put("161", "Q17348"); // EDUCACIÓN BÁSICA ESPECIAL ADULTOS
        educationalLevels.put("163", "Q17349"); // ESCUELAS CÁRCELES
        educationalLevels.put("165", "Q17350"); // EDUCACIÓN DE ADULTOS SIN OFICIOS (DECRETO 584/2007)
        educationalLevels.put("167", "Q17351"); // EDUCACIÓN DE ADULTOS CON OFICIOS (DECRETO 584/2007 Y 999/2009)
        educationalLevels.put("310", "Q17352"); // ENSEÑANZA MEDIA H-C NIÑOS Y JÓVENES
        educationalLevels.put("360", "Q17353"); // EDUCACIÓN MEDIA H-C ADULTOS (DECRETO N°190/1975)
        educationalLevels.put("361", "Q17354"); // EDUCACIÓN MEDIA H-C ADULTOS (DECRETO N°12/1987)
        educationalLevels.put("363", "Q17355"); // EDUCACIÓN MEDIA H-C ADULTOS (DECRETO N°239/2004)
        educationalLevels.put("410", "Q17356"); // ENSEÑANZA MEDIA T-P COMERCIAL NIÑOS
        educationalLevels.put("460", "Q17357"); // EDUCACIÓN MEDIA T-P COMERCIAL ADULTOS (DECRETO N° 152/1989)
        educationalLevels.put("461", "Q17357"); // EDUCACIÓN MEDIA T-P COMERCIAL ADULTOS (DECRETO N° 1000/2009)
        educationalLevels.put("463", "Q17358"); // EDUCACIÓN MEDIA T-P COMERCIAL ADULTOS (DECRETO N° 1000/2009)
        educationalLevels.put("510", "Q17359"); // ENSEÑANZA MEDIA T-P INDUSTRIAL NIÑOS
        educationalLevels.put("560", "Q17360"); // EDUCACIÓN MEDIA T-P INDUSTRIAL ADULTOS (DECRETO N° 152/1989)
        educationalLevels.put("561", "Q17360"); // EDUCACIÓN MEDIA T-P INDUSTRIAL ADULTOS (DECRETO N° 152/1989)
        educationalLevels.put("563", "Q17361"); // EDUCACIÓN MEDIA T-P INDUSTRIAL ADULTOS (DECRETO N° 1000/2009)
        educationalLevels.put("610", "Q17362"); // ENSEÑANZA MEDIA T-P TÉCNICA NIÑOS
        educationalLevels.put("660", "Q17363"); // EDUCACIÓN MEDIA T-P TÉCNICA ADULTOS (DECRETO N° 152/1989)
        educationalLevels.put("661", "Q17363"); // EDUCACIÓN MEDIA T-P TÉCNICA ADULTOS (DECRETO N° 1000/2009)
        educationalLevels.put("663", "Q17364"); // EDUCACIÓN MEDIA T-P TÉCNICA ADULTOS (DECRETO N° 1000/2009)
        educationalLevels.put("710", "Q17365"); // ENSEÑANZA MEDIA T-P AGRÍCOLA NIÑOS
        educationalLevels.put("760", "Q17366"); // EDUCACIÓN MEDIA T-P AGRÍCOLA ADULTOS (DECRETO N° 152/1989)
        educationalLevels.put("761", "Q17366"); // EDUCACIÓN MEDIA T-P AGRÍCOLA ADULTOS (DECRETO N° 1000/2009)
        educationalLevels.put("763", "Q17367"); // EDUCACIÓN MEDIA T-P AGRÍCOLA ADULTOS (DECRETO N° 1000/2009)
        educationalLevels.put("810", "Q17368"); // ENSEÑANZA MEDIA T-P MARÍTIMA NIÑOS
        educationalLevels.put("860", "Q17369"); // EDUCACIÓN MEDIA T-P MARÍTIMA ADULTOS (DECRETO N° 152/1989)
        educationalLevels.put("861", "Q17369"); // EDUCACIÓN MEDIA T-P MARÍTIMA ADULTOS (DECRETO N° 1000/2009)
        educationalLevels.put("863", "Q17370"); // EDUCACIÓN MEDIA T-P MARÍTIMA ADULTOS (DECRETO N° 1000/2009)
        educationalLevels.put("910", "Q17371"); // ENSEÑANZA MEDIA ARTÍSTICA NIÑOS Y JÓVENES
        educationalLevels.put("963", "Q17372"); // EDUCACIÓN MEDIA ARTÍSTICA ADULTOS
        
    	
    	String cualificador = "0001";
    	
    	String yearPropertyId = manager.getEntityByLabel("año", "property", "es");

    	Set<String> identificadorEstablecimiento = new HashSet<>(Arrays.asList(
    			"NOM_RBD",
    			"NOM_REG_RBD_A",
    			"NOM_COM_RBD"
        ));
    	
    	Set<String> propEstablecimiento = new HashSet<>(Arrays.asList(
                "DC_TOT",
                "LATITUD",
                "LONGITUD",
                "MAT_TOTAL",
                "ESTADO_ESTAB",
                "COD_ENSE",
                "PROM_ASIS", "CUR_SIM_TOT", "CUR_COMB_TOT", "MAT_HOM_TOT",
                "MAT_MUJ_TOT", "MAT_SI_TOT", "APR_HOM_TO", "APR_MUJ_TO",
                "APR_SI_TO", "APR_NB", "REP_HOM_TO", "REP_MUJ_TO",
                "REP_SI_TO", "RET_HOM_TO", "RET_MUJ_TO", "RET_SI_TO",
                "TRA_HOM_TO", "TRA_SI_TO", "TRA_MUJ_TO", "SI_HOM_TO",
                "SI_MUJ_TO", "SI_SI_TO", "COD_DEPE", "RURAL_RBD", "ORI_RELIGIOSA"
        ));
    	
    	
    	Set<String> identificadorRegion = new HashSet<>(Arrays.asList(
    			"NOM_REG_RBD_A"
        ));
    	
    	Set<String> propRegion = new HashSet<>(Arrays.asList(
    			
        ));
    	
    	Set<String> identificadorComuna = new HashSet<>(Arrays.asList(
    			"NOM_COM_RBD"
        ));
    	
    	Set<String> propComuna = new HashSet<>(Arrays.asList(
        ));
    	
    	
    	Set<String> identificadorDocente = new HashSet<>(Arrays.asList(
    			"MRUN"
        ));
    	
    	Set<String> propDocente = new HashSet<>(Arrays.asList(
                "DOC_FEC_NAC",
                "DOC_GENERO",
                "NOM_SUBSECTOR"
        ));
    	
    	LinkedHashMap<String, String> diccionarioPropiedadesId = new LinkedHashMap<>();
    	
    	
    	Set<String> hombresSet = new HashSet<>();
        Set<String> mujeresSet = new HashSet<>();
        Set<String> nbSet = new HashSet<>();
        Set<String> siSet = new HashSet<>();
        Set<String> todasVariablesSet = new HashSet<>();

        // Variables de hombres
        hombresSet.add("MAT_HOM_TOT");
        hombresSet.add("APR_HOM_TO");
        hombresSet.add("REP_HOM_TO");
        hombresSet.add("RET_HOM_TO");
        hombresSet.add("TRA_HOM_TO");
        hombresSet.add("SI_HOM_TO");

        // Variables de mujeres
        mujeresSet.add("MAT_MUJ_TOT");
        mujeresSet.add("APR_MUJ_TO");
        mujeresSet.add("REP_MUJ_TO");
        mujeresSet.add("RET_MUJ_TO");
        mujeresSet.add("TRA_MUJ_TO");
        mujeresSet.add("SI_MUJ_TO");

        // Variables de NB (no binario)
        nbSet.add("APR_NB");

        // Variables de SI (sin información específica)
        siSet.add("MAT_SI_TOT");
        siSet.add("APR_SI_TO");
        siSet.add("REP_SI_TO");
        siSet.add("RET_SI_TO");
        siSet.add("TRA_SI_TO");
        siSet.add("SI_SI_TO");
        
     // Combinar todos los conjuntos en un quinto conjunto
        todasVariablesSet.addAll(hombresSet);
        todasVariablesSet.addAll(mujeresSet);
        todasVariablesSet.addAll(nbSet);
        todasVariablesSet.addAll(siSet);
        
        
        // Instituciones
        Set<String> colegioSet = new HashSet<>();
        colegioSet.add("col.");
        colegioSet.add("colegio");

        Set<String> escuelaSet = new HashSet<>();
        escuelaSet.add("escuela");
        escuelaSet.add("school");
        escuelaSet.add("esc.");
        escuelaSet.add("skola");
        escuelaSet.add("es.");

        Set<String> liceoSet = new HashSet<>();
        liceoSet.add("liceo");
        liceoSet.add("l.");
        liceoSet.add("lic.");

        Set<String> universidadSet = new HashSet<>();
        universidadSet.add("universidad");
        universidadSet.add("college");

        Set<String> institutoSet = new HashSet<>();
        institutoSet.add("instituto");
        institutoSet.add("ins.");

        Set<String> centroSet = new HashSet<>();
        centroSet.add("centro");

        Set<String> complejoSet = new HashSet<>();
        complejoSet.add("complejo");

        try {

        	List<VariablePosition> matchEstablecimiento = new ArrayList<>();
        	List<VariablePosition> matchRegion = new ArrayList<>();
        	List<VariablePosition> matchComuna = new ArrayList<>();
        	List<VariablePosition> matchDocente = new ArrayList<>();
        	
        	List<VariablePosition> matchingPropEstablecimiento = new ArrayList<>();
        	List<VariablePosition> matchingPropRegion = new ArrayList<>();
        	List<VariablePosition> matchingComunaRegion = new ArrayList<>();
        	List<VariablePosition> matchingPropDocente = new ArrayList<>();
        	
        	
        	int posicionCodEnse = -1; 
        	
        
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
                        columnNames[i] = columnNames[i].trim().replaceAll("[^\\p{Print}]", "").replaceAll("\"", "");;
                    }
                    
                    // Check against predefined variables
                    for (int i = 0; i < columnNames.length; i++) {
                        String columnName = columnNames[i];
                        if (propEstablecimiento.contains(columnName)) {
                        	matchingPropEstablecimiento.add(new VariablePosition(columnName, i));
                        	
                        	if (columnName.equals("COD_ENSE")) {
                        		
                        		posicionCodEnse = i;
                        		
                        	}
                        	
                        }
                        if (propRegion.contains(columnName)) {
                        	matchingPropRegion.add(new VariablePosition(columnName, i));
                        }
                        if (propComuna.contains(columnName)) {
                        	matchingComunaRegion.add(new VariablePosition(columnName, i));
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
                        if (identificadorComuna.contains(columnName)) {
                        	matchComuna.add(new VariablePosition(columnName, i));
                        }
                        
                        if (identificadorDocente.contains(columnName)) {
                        	matchDocente.add(new VariablePosition(columnName, i));
                        }
                        
                    }
                }
                
                System.out.println("cualificador: " + cualificador);
                System.out.println("matchEstablecimiento: " + matchEstablecimiento);
                System.out.println("matchRegion: " + matchRegion);
                System.out.println("matchComuna: " + matchComuna);
                System.out.println("matchDocente: " + matchDocente);
                
                Boolean hayEstablecimiento = matchEstablecimiento.size() == 3;
                Boolean hayRegion = matchRegion.size() == 1;
                Boolean hayComuna = matchComuna.size() == 1;
                Boolean hayDocente = matchDocente.size() == 1;
                                
                List<Integer> posicionesEstablecimiento = null;
                List<Integer> posicionesRegion = null;
                List<Integer> posicionesComuna = null;
                List<Integer> posicionesDocente = null;
                
                if (hayEstablecimiento) {
                	
                	List<String> variablesOrdenadas = Arrays.asList("NOM_RBD", "NOM_REG_RBD_A", "NOM_COM_RBD");
                	posicionesEstablecimiento = getPositionsInOrder(matchEstablecimiento, variablesOrdenadas);
                }
                
                if (hayRegion) {
                	List<String> variablesOrdenadas = Arrays.asList("NOM_REG_RBD_A");
                	posicionesRegion = getPositionsInOrder(matchRegion, variablesOrdenadas);	
                }
                
                if (hayComuna) {
                	List<String> variablesOrdenadas = Arrays.asList("NOM_COM_RBD");
                	posicionesComuna = getPositionsInOrder(matchComuna, variablesOrdenadas);	
                }
				
				if (hayDocente) {
					
					List<String> variablesOrdenadas = Arrays.asList("MRUN");
					posicionesDocente = getPositionsInOrder(matchDocente, variablesOrdenadas);	
				}
				
				LinkedHashMap<String, String> establecimientos = new LinkedHashMap<>();
				LinkedHashMap<String, String> regiones = new LinkedHashMap<>();
				LinkedHashMap<String, String> comunas = new LinkedHashMap<>();
				LinkedHashMap<String, String> docentes = new LinkedHashMap<>();
                
				
                for (int i = 0; i < max_read; i++) {
                	
                	
                	
                	
                	String[] nextInLine = reader.readNext();
                	
                	if (nextInLine == null) {
                		
                		System.out.println("No hay mas lineas para leer");
                        System.exit(1);
                		
                	}
                    //Arrays.stream(nextInLine).forEach(element -> System.out.print(element + " "));

                	String nextInLineString = "";
                	
                	
                	for (int j = 0; j < nextInLine.length; j++) {
                		nextInLineString = nextInLineString + nextInLine[j];
                	    if (j < nextInLine.length - 1) {
                	    	nextInLineString = nextInLineString + ",";
                	    }
                	}
                	nextInLineString = nextInLineString.trim().replaceAll("[^\\p{Print}]", "").replaceAll("\"", "");
                    // Split the string into individual column names
                	
                    String[] nextInLineValues = nextInLineString.split(";");
                                      
                    
                    if (posicionCodEnse != -1) {
                    	
                    	if ( !educationalLevels.containsKey(nextInLineValues[posicionCodEnse]) ) {
                    		
                    		System.out.println("Linea saltada");
                    		
                    		continue;
                    		
                    	}
                    	
                    }
                    
                    String establecimientoId = "";
                    String regionId = "";
                    String comunaId = "";
                    String docenteId = "";
                    
					long startTime = System.currentTimeMillis();
                	
                	System.out.println("Lineas leidas: " + i);
                    
                    if (hayEstablecimiento){
                    	
                    	String establecimientoLabel = "";
                    	String establecimientoNombre = "";
                    	for (int j = 0; j < posicionesEstablecimiento.size(); j++) { 		
                    		establecimientoLabel = establecimientoLabel.replaceAll("[^\\p{Print}]", "").replaceAll("\"", "") + nextInLineValues[posicionesEstablecimiento.get(j)].trim().replaceAll("[^\\p{Print}]", "").replaceAll("\"", "");
                    		
                    		if (j == 0) {
                    			establecimientoNombre =  nextInLineValues[posicionesEstablecimiento.get(j)];
                    		}
                    		
                    		if (j != posicionesEstablecimiento.size() - 1) {
                    			establecimientoLabel = establecimientoLabel + " ";
                    		}

                    	}
                    	     
                    	boolean establecimientoTipo = false;
                    	
                    	if (establecimientos.containsKey(establecimientoLabel)) {
                    		
                    		establecimientoId = establecimientos.get(establecimientoLabel);
                    		
                    	} else {
                    		if (manager.entityExistsByLabel(establecimientoLabel, "item", "es")) {
                    			establecimientoId = manager.getEntityByLabel(establecimientoLabel, "item", "es");
                    			establecimientos.put(establecimientoLabel, establecimientoId);
                    			
                    			// System.out.println(establecimientoLabel);
                    			
                    			if (containsIgnoreCase(colegioSet, establecimientoLabel)) {
                    				
                    				if (!manager.statementExists(establecimientoId, "P15", "Q17305")) {
                    					
                    					manager.addStatementToItem(establecimientoId, "P15", "Q17305", "wikibase-item");
                    				
                    				}
                    				
                    	            establecimientoTipo = true;
                    	        }

                    	        if (containsIgnoreCase(escuelaSet, establecimientoLabel)) {
                    	        	
                    	        	if (!manager.statementExists(establecimientoId, "P15", "Q17306")) {
                    					
                    					manager.addStatementToItem(establecimientoId, "P15", "Q17306", "wikibase-item");
                    				
                    				}
                    	        	
                    	            establecimientoTipo = true;
                    	        }

                    	        if (containsIgnoreCase(liceoSet, establecimientoLabel)) {
                    	        	if (!manager.statementExists(establecimientoId, "P15", "Q17307")) {
                    					
                    					manager.addStatementToItem(establecimientoId, "P15", "Q17307", "wikibase-item");
                    				
                    				}                    	            
                    	        	establecimientoTipo = true;
                    	        }

                    	        if (containsIgnoreCase(universidadSet, establecimientoLabel)) {
                    	        	if (!manager.statementExists(establecimientoId, "P15", "Q17308")) {
                    					
                    					manager.addStatementToItem(establecimientoId, "P15", "Q17308", "wikibase-item");
                    				
                    				} 
                    	            establecimientoTipo = true;
                    	        }

                    	        if (containsIgnoreCase(institutoSet, establecimientoLabel)) {
                    	        	if (!manager.statementExists(establecimientoId, "P15", "Q17309")) {
                    					
                    					manager.addStatementToItem(establecimientoId, "P15", "Q17309", "wikibase-item");
                    				
                    				} 
                    	            establecimientoTipo = true;
                    	        }

                    	        if (containsIgnoreCase(centroSet, establecimientoLabel)) {
                    	        	if (!manager.statementExists(establecimientoId, "P15", "Q17310")) {
                    					
                    					manager.addStatementToItem(establecimientoId, "P15", "Q17310", "wikibase-item");
                    				
                    				}                    	            
                    	        	establecimientoTipo = true;
                    	        }

                    	        if (containsIgnoreCase(complejoSet, establecimientoLabel)) {
                    	        	if (!manager.statementExists(establecimientoId, "P15", "Q17311")) {
                    					
                    					manager.addStatementToItem(establecimientoId, "P15", "Q17311", "wikibase-item");
                    				
                    				}   
                    	            establecimientoTipo = true;
                    	        }
                    			
                    			if (!establecimientoTipo) {
                    				
                    				if (!manager.statementExists(establecimientoId, "P15", "Q3")) {
                    					
                    					manager.addStatementToItem(establecimientoId, "P15", "Q3", "wikibase-item");
                    			
                    				}  
                    			} 
                    			
                    		} else {
                    			if (!establecimientoLabel.equals("")) {
                        			establecimientoId = manager.createItem(establecimientoNombre, "");
                        			
                        			manager.addAlias(establecimientoId, establecimientoLabel, "es");
                        			establecimientos.put(establecimientoLabel, establecimientoId);
                        			
                        			// System.out.println(establecimientoLabel);
                        			
                        			if (containsIgnoreCase(colegioSet, establecimientoLabel)) {
                        				manager.addStatementToItem(establecimientoId, "P15", "Q17305", "wikibase-item");
                        	            establecimientoTipo = true;
                        	        }

                        	        if (containsIgnoreCase(escuelaSet, establecimientoLabel)) {
                        	        	manager.addStatementToItem(establecimientoId, "P15", "Q17306", "wikibase-item");
                        	            establecimientoTipo = true;
                        	        }

                        	        if (containsIgnoreCase(liceoSet, establecimientoLabel)) {
                        	        	manager.addStatementToItem(establecimientoId, "P15", "Q17307", "wikibase-item");
                        	            establecimientoTipo = true;
                        	        }

                        	        if (containsIgnoreCase(universidadSet, establecimientoLabel)) {
                        	        	manager.addStatementToItem(establecimientoId, "P15", "Q17308", "wikibase-item");
                        	            establecimientoTipo = true;
                        	        }

                        	        if (containsIgnoreCase(institutoSet, establecimientoLabel)) {
                        	        	manager.addStatementToItem(establecimientoId, "P15", "Q17309", "wikibase-item");
                        	            establecimientoTipo = true;
                        	        }

                        	        if (containsIgnoreCase(centroSet, establecimientoLabel)) {
                        	        	manager.addStatementToItem(establecimientoId, "P15", "Q17310", "wikibase-item");
                        	            establecimientoTipo = true;
                        	        }

                        	        if (containsIgnoreCase(complejoSet, establecimientoLabel)) {
                        	        	manager.addStatementToItem(establecimientoId, "P15", "Q17311", "wikibase-item");
                        	            establecimientoTipo = true;
                        	        }
                        			
                        			if (!establecimientoTipo) {
                        				
                        				manager.addStatementToItem(establecimientoId, "P15", "Q3", "wikibase-item");
                        				
                        			} 
     
                    			}
                    		}

                    		
                    	}
                    	
                    	String Latitud = "";
                    	String Longitud = "";
                    	                    	                    	                    	
                    	for (VariablePosition vp : matchingPropEstablecimiento) {
                    		
                    		String propiedadCodigo = vp.getVariable();
                    		int propiedadPosicion = vp.getPosition();
                    		// System.out.println(propiedadCodigo);
                    		String propiedad = diccionarioPropiedades.get(propiedadCodigo);
                    		
							String propiedadId = "";
							
                    		if (diccionarioPropiedadesId.containsKey(propiedad)) {
                    			
                    			propiedadId = diccionarioPropiedadesId.get(propiedad);

                    			
                    		} else {

                    			// System.out.println(propiedad);
                    			
                    			propiedadId = manager.getEntityByLabel(propiedad, "property", "es");
                    			
                    			diccionarioPropiedadesId.put(docenteId, propiedadId);
                    			
                    		};
                    		
                    		if (diccionarioPropiedadesConCualificador.containsKey(propiedad)) {
                				
                				String propiedadType = diccionarioPropiedadesConCualificador.get(propiedad);
                					
                				String statementId = "";
                				
                				
                				if (propiedadCodigo.equals("COD_ENSE")) {
                					
                					if (educationalLevels.containsKey(educationalLevels.get(nextInLineValues[propiedadPosicion]))) {
                						
                						if (manager.statementExists(establecimientoId, propiedadId, educationalLevels.get(nextInLineValues[propiedadPosicion]))) {
                        					statementId = manager.getStatementId(establecimientoId, propiedadId, educationalLevels.get(nextInLineValues[propiedadPosicion]), propiedadType);
                        				} else {
                        					statementId = manager.addStatementToItem(establecimientoId, propiedadId, educationalLevels.get(nextInLineValues[propiedadPosicion]), propiedadType);
                        				}
                						
                					} else {
                						
                						continue;
                						
                					}
                					
                					
                						
                				} else if (propiedadType.equals("quantity")) {
                					
                					//System.out.println(establecimientoId);
                					//System.out.println(propiedadPosicion);
                					
                					if (manager.statementExists(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion].replace(",", "."))) {
                						
                						statementId = manager.getStatementId(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion].replace(",", "."), propiedadType);
                						
                					} else {
                						
                						statementId = manager.addStatementToItem(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion].replace(",", "."), propiedadType);
                						
                					}
                					
                						
                				} else if (manager.statementExists(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion])) {
                					
                					statementId = manager.getStatementId(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion].replace(",", "."), propiedadType);
                					
                				} else {
                					//System.out.println(establecimientoId);
                					//System.out.println(propiedadId);
                					//System.out.println(nextInLineValues[propiedadPosicion]);
                					//System.out.println(propiedadType);
                					
                					statementId = manager.addStatementToItem(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
                					
                					
                				}
                				
                				//System.out.println("ID: " + statementId);
                				//System.out.println("Propiedad tipo: " + propiedadType);
                				//System.out.println("Propiedad: " + propiedadId);
                				//System.out.println("valor: " + nextInLineValues[propiedadPosicion]);
                				
                				
                				
                				if (todasVariablesSet.contains(propiedadCodigo)) {
                					                					                					
                					if (hombresSet.contains(propiedadCodigo)) {
	
                						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], yearPropertyId,  nextInLineValues[Integer.parseInt(cualificador)])) {
                							                							
                        					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
                        					
                        				}
                						
                						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], "P28", "Q2403")) {
                							
                        					manager.addQualifierToStatement(statementId, "P28", "Q2403", "wikibase-item");
                        					
                        				}
                						
                						if (posicionCodEnse != -1) {

                    						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], "P29", educationalLevels.get(nextInLineValues[posicionCodEnse]))) {
                    							
                            					manager.addQualifierToStatement(statementId, "P29", educationalLevels.get(nextInLineValues[posicionCodEnse]), "wikibase-item");
                            					
                            				}
                							
                						}
                						
                						
                					} else if (mujeresSet.contains(propiedadCodigo)) {
                						
                						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], yearPropertyId,  nextInLineValues[Integer.parseInt(cualificador)])) {
	                    					                							
                							manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
                        					
                        				}
                						
                						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], "P28", "Q2404")) {
                							
                        					manager.addQualifierToStatement(statementId, "P28", "Q2404", "wikibase-item");
                        					
                        				}
                						
                						if (posicionCodEnse != -1) {
                							
            
                    						
                    						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], "P29", educationalLevels.get(nextInLineValues[posicionCodEnse]))) {
                    							
                            					manager.addQualifierToStatement(statementId, "P29", educationalLevels.get(nextInLineValues[posicionCodEnse]), "wikibase-item");
                            					
                            				}
                							
                						}
                						
                					} else if (siSet.contains(propiedadCodigo)) {
                						
                						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], yearPropertyId,  nextInLineValues[Integer.parseInt(cualificador)])) {
	                    					
                        					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
                        					
                        				}
                						
                						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], "P28", "Q2406")) {
                							
                        					manager.addQualifierToStatement(statementId, "P28", "Q2406", "wikibase-item");
                        					
                        				}
                						
                						if (posicionCodEnse != -1) {
                							
                							
                    						
                    						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], "P29", educationalLevels.get(nextInLineValues[posicionCodEnse]))) {
                    							
                            					manager.addQualifierToStatement(statementId, "P29", educationalLevels.get(nextInLineValues[posicionCodEnse]), "wikibase-item");
                            					
                            				}
                							
                						}
                						
                					} else {
                						
                						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], yearPropertyId,  nextInLineValues[Integer.parseInt(cualificador)])) {
	                    					
                        					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
                        					
                        				}
                						
                						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], "P28", "Q2405")) {
                							
                        					manager.addQualifierToStatement(statementId, "P28", "Q2405", "wikibase-item");
                        					
                        				}
                						
                						if (posicionCodEnse != -1) {
                							
                    						
                    						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], "P29", educationalLevels.get(nextInLineValues[posicionCodEnse]))) {
                    							
                            					manager.addQualifierToStatement(statementId, "P29", educationalLevels.get(nextInLineValues[posicionCodEnse]), "wikibase-item");
                            					
                            				}
                							
                						}
                						
                					}   				
                					
                				} else if (propiedadCodigo.equals("COD_ENSE")) {
                					
                					if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, educationalLevels.get(nextInLineValues[propiedadPosicion]), yearPropertyId,  nextInLineValues[Integer.parseInt(cualificador)])) {
                						
                						manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
                						
                					}
                					
                					
                					
                				} else if (propiedadType.equals("quantity")) {
                					
                					if (propiedadCodigo.equals("PROM_ASIS")) {
                						
                						if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion].replace(",", "."), "P29", educationalLevels.get(nextInLineValues[posicionCodEnse]))) {
                							
                        					manager.addQualifierToStatement(statementId, "P29", educationalLevels.get(nextInLineValues[posicionCodEnse].replace(",", ".")), "wikibase-item");
                        					
                        				}
                						
                						
                					} 

                				
                					if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion].replace(",","."), yearPropertyId,  nextInLineValues[Integer.parseInt(cualificador)])) {

                						
                    					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
                    					
                    				}
                					
                				} else if (!manager.doesStatementWithQualifierExist(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], yearPropertyId,  nextInLineValues[Integer.parseInt(cualificador)])) {

                				
                					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
                					
                				}
                				
                				

                				
                			} else if (diccionarioPropiedadesSinCualificador.containsKey(propiedad)) {
                				
                				
                				String propiedadType = diccionarioPropiedadesSinCualificador.get(propiedad);
                				
                				String statementId = "";
                				
                				if (propiedadCodigo.equals("COD_DEPE")) {                					
                					switch (nextInLineValues[propiedadPosicion]) {
                					
	                					case "1":
	                						if (!manager.statementExists(establecimientoId, propiedadId, "Q18723")) {
	                							statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18723", propiedadType);
	                						}
	                						break;
	                					case "2":
											if (!manager.statementExists(establecimientoId, propiedadId, "Q18724")) {
	                							statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18724", propiedadType);
	                						}
											break;
	                					case "3":
	                						if (!manager.statementExists(establecimientoId, propiedadId, "Q18725")) {
	                							statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18725", propiedadType);
	                						}
	                						break;
	                					case "4":
											if (!manager.statementExists(establecimientoId, propiedadId, "Q18726")) {
	                							statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18726", propiedadType);
	                						}
											break;
	                					case "5":
	                						if (!manager.statementExists(establecimientoId, propiedadId, "Q18727")) {
	                							statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18727", propiedadType);
	                						}
	                						break;
	                					case "6":
											if (!manager.statementExists(establecimientoId, propiedadId, "Q18728")) {
	                							statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18728", propiedadType);
	                						}
											break;
                					
                					}
                					
                				} else if (propiedadCodigo.equals("RURAL_RBD")) {
                					switch (nextInLineValues[propiedadPosicion]) {
                					
                					case "0":
                						if (!manager.statementExists(establecimientoId, propiedadId, "Q18714")) {
                							statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18714", propiedadType);
                						}
                						break;
                					case "1":
										if (!manager.statementExists(establecimientoId, propiedadId, "Q18715")) {
                							statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18715", propiedadType);
                						}
										break;
									
                					}
            					
            					} else if (propiedadCodigo.equals("ORI_RELIGIOSA")) {
                					switch (nextInLineValues[propiedadPosicion]) {
                					
	                					case "1":
	                				        if (!manager.statementExists(establecimientoId, propiedadId, "Q18716")) {
	                				            statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18716", propiedadType);
	                				        }
	                				        break;
	
	                				    case "2":
	                				        if (!manager.statementExists(establecimientoId, propiedadId, "Q18717")) {
	                				        	
	                				            statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18717", propiedadType);
	                				        }
	                				        break;
	
	                				    case "3":
	                				        if (!manager.statementExists(establecimientoId, propiedadId, "Q18718")) {
	                				            statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18718", propiedadType);
	                				        }
	                				        break;
	
	                				    case "4":
	                				        if (!manager.statementExists(establecimientoId, propiedadId, "Q18719")) {
	                				            statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18719", propiedadType);
	                				        }
	                				        break;
	
	                				    case "5":
	                				        if (!manager.statementExists(establecimientoId, propiedadId, "Q18720")) {
	                				            statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18720", propiedadType);
	                				        }
	                				        break;
	
	                				    case "6":
	                				        if (!manager.statementExists(establecimientoId, propiedadId, "Q18721")) {
	                				            statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18721", propiedadType);
	                				        }
	                				        break;
	
	                				    case "7":
	                				        if (!manager.statementExists(establecimientoId, propiedadId, "Q18722")) {
	                				            statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q18722", propiedadType);
	                				        }
	                				        break;
	
	                				    case "9":
	                				        if (!manager.statementExists(establecimientoId, propiedadId, "Q2406")) {
	                				            statementId = manager.addStatementToItem(establecimientoId, propiedadId, "Q2406", propiedadType);
	                				        }
	                				        break;
	            					
	                				} 
                					
                				} else if (!propiedadCodigo.equals("LATITUD") && !propiedadCodigo.equals("LONGITUD") && !propiedadCodigo.equals("ESTADO_ESTAB")) {
                					
                					if (!manager.statementExists(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion])) {
                						
                						//System.out.println(docenteId);
                    					//System.out.println(propiedadId);
                    					//System.out.println(nextInLineValues[propiedadPosicion]);
                    					//System.out.println(propiedadType);
			         					
                    					statementId = manager.addStatementToItem(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
                    					
                    				}
                					
                				}
                				
                				String value = nextInLineValues[propiedadPosicion];
                				
                				switch (propiedadCodigo.toUpperCase()) {
                				
                					case "LATITUD":

                						value = nextInLineValues[propiedadPosicion].trim();
                						Latitud = value;
                						break;
                					
                					case "LONGITUD":
                						value = nextInLineValues[propiedadPosicion].trim();
                						Longitud= value;
                						break;
                						
                					case "ESTADO_ESTAB":
                						                						
                						switch (nextInLineValues[propiedadPosicion]) {
                    					
		                					case "1":
		                						value = "Q18729";
		                						break;
		                					case "2":
		                						value = "Q18730";
												break;
		                					case "3":
		                						value = "Q18731";
												break;
		                					case "4":
		                						value = "Q18732";
												break;
	                					
	                					}
                						if (!manager.statementExists(establecimientoId, propiedadId, value)) {
                    						statementId = manager.addStatementToItem(establecimientoId, propiedadId, value, propiedadType);
		
                        				}
                						break;
                				
                				}
                				                    				
                				if ((propiedadCodigo.toUpperCase().equals("LATITUD") || propiedadCodigo.toUpperCase().equals("LONGITUD")) && (Latitud != null && !Latitud.isEmpty() && Longitud != null && !Longitud.isEmpty())) {
                					
                					if (!manager.statementExists(establecimientoId, propiedadId, nextInLineValues[propiedadPosicion])) {
                						manager.addStatementToItem(establecimientoId, propiedadId, Latitud + ";" + Longitud, propiedadType);
	
                    				}
                					                    					
                				} 
                				
                			}
                    		
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
                        			manager.addStatementToItem(regionId, "P15", "Q2", "wikibase-item");
                    				
                    			} 
                    		}
                    		
                    		
                    		regiones.put(regionLabel, regionId);
                    		
                    	}
                    }
					
					if (hayComuna){
                    	
                    	String comunaLabel = "";
                    	for (int j = 0; j < posicionesComuna.size(); j++) { 		
                    		comunaLabel = comunaLabel + nextInLineValues[posicionesComuna.get(j)];
                    		
                    		if (j != posicionesComuna.size() - 1) {
                    			comunaLabel = comunaLabel + " ";
                    		}

                    	}
                 
                    	if (comunas.containsKey(comunaLabel)) {
                    		
                    		comunaId = comunas.get(comunaLabel);
                    		
                    	} else {
                    		
                    		
                    		if (manager.entityExistsByLabel(comunaLabel, "item", "es")) {
                    			comunaId = manager.getEntityByLabel(comunaLabel, "item", "es");
                    			comunas.put(comunaLabel, comunaId);
                    			
                    			
                    		} else {
                    			if (!comunaLabel.replace(" ", "").equals("")) {
                    			
                        			comunaId = manager.createItem(comunaLabel, "");
                        			comunas.put(comunaLabel, comunaId);
                        			manager.addStatementToItem(comunaId, "P15", "Q1", "wikibase-item");
                    			} 
                    		}
                    		
                    		
                    		comunas.put(comunaLabel, comunaId);
                    		
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
						
						if (!docenteLabel.trim().equals("")) {
							
							docenteLabel = "MRUN: " + docenteLabel;
							
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
										manager.addStatementToItem(docenteId, "P15", "Q4", "wikibase-item");
										
									} 
								}
								
								
								
							}
							
							for (VariablePosition vp : matchingPropDocente) {
	                    		
	                    		String propiedadCodigo = vp.getVariable();
	                    		int propiedadPosicion = vp.getPosition();
	                    		String propiedad = diccionarioPropiedades.get(propiedadCodigo);
	                    		
	                    		String propiedadId = "";
	                    		
	                    		if (diccionarioPropiedadesId.containsKey(propiedad)) {
	                    			
	                    			propiedadId = diccionarioPropiedadesId.get(propiedad);
	
	                    			
	                    		} else {
	                    			
	                    			propiedadId = manager.getEntityByLabel(propiedad, "property", "es");
	                    			
	                    			diccionarioPropiedadesId.put(docenteId, propiedadId);
	                    			
	                    		};
	                    		
	                    		if (diccionarioPropiedadesConCualificador.containsKey(propiedad)) {
	                				
	                				String propiedadType = diccionarioPropiedadesConCualificador.get(propiedad);
	                					
	                				String statementId = "";
	                				
	                				
	                				if (manager.statementExists(docenteId, propiedadId, nextInLineValues[propiedadPosicion])) {
	                					statementId = manager.getStatementId(docenteId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
	                				} else {
	                					statementId = manager.addStatementToItem(docenteId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
	                				}
	             
	                				if (!manager.doesStatementWithQualifierExist(docenteId, propiedadId, nextInLineValues[propiedadPosicion], yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)])) {
	                					                    					
	                					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
	                					
	                				}
	
	                				
	                			} else if (diccionarioPropiedadesSinCualificador.containsKey(propiedad)) {
	                				
	                				
	                				String propiedadType = diccionarioPropiedadesSinCualificador.get(propiedad);
	                				
	                				String statementId = "";
	                				
	                				if (propiedadCodigo.equals("DOC_GENERO")) {                					
	                					switch (nextInLineValues[propiedadPosicion]) {
	                					
		                					case "1":
		                						if (!manager.statementExists(docenteId, propiedadId, "HOMBRE")) {
		                							statementId = manager.addStatementToItem(docenteId, propiedadId, "HOMBRE", propiedadType);
		                						}
		                						break;
		                					case "2":
												if (!manager.statementExists(docenteId, propiedadId, "MUJER")) {
		                							statementId = manager.addStatementToItem(docenteId, propiedadId, "MUJER", propiedadType);
		                						}
												break;
	                					
	                					}
	                					
	                				} else {
	                					
	                					if (!manager.statementExists(docenteId, propiedadId, nextInLineValues[propiedadPosicion])) {
				         					
	                    					statementId = manager.addStatementToItem(docenteId, propiedadId, nextInLineValues[propiedadPosicion], propiedadType);
	                    					
	                    				}
	                					
	                				}
	
	
	                			}
	                    		
	                    	}
							
						}
						
						
						
					}
					
					
					if (!establecimientoId.equals("")) {
						
						
						if (!regionId.equals("")){
							
							
							String regionPropiedadId = "P1";
							
							if (!manager.statementExists(establecimientoId, regionPropiedadId, regionId)) {
								manager.addStatementToItem(establecimientoId, regionPropiedadId, regionId, "wikibase-item");
            				} 
							
							
						}
						
						if (!comunaId.equals("")){
							
							
							String comunaPropiedadId = "P2";
							
							if (!manager.statementExists(establecimientoId, comunaPropiedadId, comunaId)) {
								manager.addStatementToItem(establecimientoId, comunaPropiedadId, comunaId, "wikibase-item");
            				} 
							
							
						}

					
					}
					
					if (!regionId.equals("")) {
						
						
						if (!establecimientoId.equals("")){
							
							
							String establecimientoPropiedadId = "P6";
							
							if (!manager.statementExists(regionId, establecimientoPropiedadId, establecimientoId)) {
								manager.addStatementToItem(regionId, establecimientoPropiedadId, establecimientoId, "wikibase-item");
            				} 
							
							
						}
						
						if (!comunaId.equals("")){
							
							
							String comunaPropiedadId = "P2";
							
							if (!manager.statementExists(regionId, comunaPropiedadId, comunaId)) {
								manager.addStatementToItem(regionId, comunaPropiedadId, comunaId, "wikibase-item");
            				} 
							
							
						}

					
					}
					
					
					if (!comunaId.equals("")) {
						
						
						if (!establecimientoId.equals("")){
							
							
							String establecimientoPropiedadId = "P6";
							
							if (!manager.statementExists(comunaId, establecimientoPropiedadId, establecimientoId)) {
								manager.addStatementToItem(comunaId, establecimientoPropiedadId, establecimientoId, "wikibase-item");
            				} 
							
							
						}
						
						if (!regionId.equals("")){
							
							
							String regionPropiedadId = "P1";
							
							if (!manager.statementExists(comunaId, regionPropiedadId, regionId)) {
								manager.addStatementToItem(comunaId, regionPropiedadId, regionId, "wikibase-item");
            				} 
							
							
						}

					
					}
					
					
					if (!docenteId.equals("")) {
						
						if (!regionId.equals("")){
							
							String statementId = "";
							
							String trabajoRegionPropertyId = "P9";
							
							if (manager.statementExists(docenteId, trabajoRegionPropertyId, regionId)) {
								statementId = manager.getStatementId(docenteId, trabajoRegionPropertyId, regionId, "wikibase-item");
            				} else {
            					statementId = manager.addStatementToItem(docenteId, trabajoRegionPropertyId, regionId, "wikibase-item");	
            				}
							            				

            				if (!manager.doesStatementWithQualifierExist(docenteId, trabajoRegionPropertyId, regionId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)])) {
            					
            					
            					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
            					
            				}
							
							
						}
						
						if (!comunaId.equals("")){
							
							String statementId = "";
							
							String trabajoComunaPropertyId = "P10";
							
							if (manager.statementExists(docenteId, trabajoComunaPropertyId, comunaId)) {
								statementId = manager.getStatementId(docenteId, trabajoComunaPropertyId, comunaId, "wikibase-item");
            				} else {
            					statementId = manager.addStatementToItem(docenteId, trabajoComunaPropertyId, comunaId, "wikibase-item");	
            				}
							            				

            				if (!manager.doesStatementWithQualifierExist(docenteId, trabajoComunaPropertyId, comunaId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)])) {
            					
            					
            					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
            					
            				}
							
							
						}
						
						if (!establecimientoId.equals("")){
							
							String statementId = "";
							
							String trabajoEstablecimientoPropertyId = "P8";
							
							if (manager.statementExists(docenteId, trabajoEstablecimientoPropertyId, establecimientoId)) {
								statementId = manager.getStatementId(docenteId, trabajoEstablecimientoPropertyId, establecimientoId, "wikibase-item");
            				} else {
            					statementId = manager.addStatementToItem(docenteId, trabajoEstablecimientoPropertyId, establecimientoId, "wikibase-item");	
            				}
							            				

            				if (!manager.doesStatementWithQualifierExist(docenteId, trabajoEstablecimientoPropertyId, establecimientoId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)])) {
            					
            					
            					manager.addQualifierToStatement(statementId, yearPropertyId, nextInLineValues[Integer.parseInt(cualificador)], "time");
            					
            				}
							
							
						}
						
						
						
					}
					
					long endTime = System.currentTimeMillis();
		            long duration = endTime - startTime;

		            // Registra el tiempo de ejecución y el número de líneas leídas
		            ExecutionLogger.log(duration, i);

                }
            }
            System.out.println("Fin");
            
            
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        
        //System.out.println(manager.getEntityByLabel("TPCA", "item", "es"));
        
    }

}
