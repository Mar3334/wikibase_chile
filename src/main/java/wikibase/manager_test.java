package wikibase;

import java.io.IOException;

public class manager_test {
public static void main(String[] args) throws IOException {
		
		WikibaseManager manager;
    	
    	String username;
        String password;
        String filePath;
    	
    	if (args.length < 3) {
            // System.err.println("Uso: WikibaseManager <usuario> <clave> <archivo>");
            // System.exit(1);

    		username = "";
            password = "";
            filePath = "";
    		
        } else {     	
        	username = args[0];
            password = args[1];
            filePath = args[2];	     	
        }

        manager = new WikibaseManager(username, password);
        manager.login();
        manager.fetchCsrfToken();
        
        
       
        // itemId propertyId value QualifierId qualifierValue
        String tipoEstablecimientoId = manager.createProperty("tipo de establecimiento", "Tipos de Establecimientos Educacionales existen según el tipo de financiamiento", "string");
        String ruralidadId = manager.createProperty("ruralidad", "Ruralidad de un establecimiento", "string");
	}
}
