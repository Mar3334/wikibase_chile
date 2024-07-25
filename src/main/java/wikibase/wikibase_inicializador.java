package wikibase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class wikibase_inicializador {
	
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
       
		
		
		// Crear propiedades para establecimientos
        String regionPropertyId = manager.createProperty("Región", "Nombre de la región donde se ubica el establecimiento", "wikibase-item");
        String comunaPropertyId = manager.createProperty("Comuna", "Nombre de la comuna donde se ubica el establecimiento", "string");
        String directorPropertyId = manager.createProperty("Empleados", "Empleados de una organizacion", "quantity");
        String ubicacionPropertyId = manager.createProperty("Ubicacion", "Coordenadas del establecimiento", "globe-coordinate");
        String matriculaPropertyId = manager.createProperty("Matrículados", "Número de estudiantes matriculados en el establecimiento", "quantity");

        // Crear propiedades para Regiones
        String establecimientosRegionPropertyId = manager.createProperty("Establecimientos en la Región", "Establecimientos educativos ubicados en la región", "wikibase-item");
        String docentesRegionPropertyId = manager.createProperty("Docentes en la Región", "Nombre de los docentes que trabajan en la región", "wikibase-item");

        // Crear propiedades para Personas
        String trabajoEstablecimientoPropertyId = manager.createProperty("Establecimiento de Trabajo", "Nombre del establecimiento donde trabaja o trabajo una la persona", "wikibase-item");
        String trabajoRegionPropertyId = manager.createProperty("Región de Trabajo", "Nombre de la región donde trabaja la persona", "wikibase-item");
        String nacimientoPropertyId = manager.createProperty("Fecha de nacimiento", "Fecha de nacimiento del docente", "time");
        String generoDocentePropertyId = manager.createProperty("Género del Docente", "Género del docente", "string");
        String asignaturayId = manager.createProperty("Asignatura", "Asignatura realizada por el docente", "string");
        
        // Propiedad del año
        String añoId = manager.createProperty("Año", "Fecha asociada a un evento", "time");
        
        
           
        Map<String, String> regions = new HashMap<>();
        regions.put("Región de Tarapacá", "TPCA");
        regions.put("Región de Antofagasta", "ANTOF");
        regions.put("Región de Atacama", "ATCMA");
        regions.put("Región de Coquimbo", "COQ");
        regions.put("Región de Valparaíso", "VALPO");
        regions.put("Región del Libertador Gral. Bernardo O’Higgins", "LGBO");
        regions.put("Región del Maule", "MAULE");
        regions.put("Región del Biobío", "BBIO");
        regions.put("Región de la Araucanía", "ARACU");
        regions.put("Región de Los Lagos", "LAGOS");
        regions.put("Región de Aysén del Gral. Carlos Ibáñez del Campo", "AYSEN");
        regions.put("Región de Magallanes y de la Antártica Chilena", "MAG");
        regions.put("Región Metropolitana de Santiago", "RM");
        regions.put("Región de Los Ríos", "RIOS");
        regions.put("Región de Arica y Parinacota", "AYP");
        regions.put("Región de Ñuble", "NUBLE");

        // Create items for each region
        for (Map.Entry<String, String> entry : regions.entrySet()) {
            String regionName = entry.getKey();
            String abbreviatedName = entry.getValue();
            String regionItemId = manager.createItem(regionName, "Región de Chile");

            if (regionItemId != null) {
                manager.addAlias(regionItemId, abbreviatedName, "es");
                System.out.println("Created item for " + regionName + " with alias " + abbreviatedName);
            }
        }
		
		
	}

}
