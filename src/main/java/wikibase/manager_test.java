package wikibase;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;


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
        
        
        System.out.println(manager.getEntityByLabel("orientacion religiosa", "property", "es"));
       
        //String statId = manager.getStatementId("Q5323", "P1", "Q5168", "wikibase-item");
        
        // itemId propertyId value QualifierId qualifierValue
        //manager.removeRegionClaims("Q5323", statId);
	}
}
