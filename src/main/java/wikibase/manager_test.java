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
        
        
        System.out.println(manager.entityExistsByLabel("MAG", "item", "es"));
        
	}
}
