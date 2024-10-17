// Paquete que contiene la clase para inicializar datos en Wikibase.
package wikibase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Clase principal para inicializar y gestionar datos en Wikibase.
public class wikibase_inicializador {
	
    // Método principal que se ejecuta al iniciar el programa.
	public static void main(String[] args) throws IOException {
		
        // Declaración de la instancia del gestor de Wikibase.
		WikibaseManager manager;
    	
    	// Variables para el nombre de usuario, contraseña y ruta del archivo.
    	String username;
        String password;
        String filePath;
    	
        // Verifica si se pasaron menos de 3 argumentos al programa.
    	if (args.length < 3) {
            // Si no hay suficientes argumentos, se asignan valores vacíos a las variables.
    		username = "";
            password = "";
            filePath = "";
    		
        } else {   
            // Si se proporcionaron suficientes argumentos, se asignan a las variables correspondientes.
        	username = args[0];
            password = args[1];
            filePath = args[2];	     	
        }

        // Inicializar el gestor de Wikibase con el nombre de usuario y contraseña.
        manager = new WikibaseManager(username, password);
        
        // Iniciar sesión en Wikibase.
        manager.login();
        
        // Obtener el token CSRF necesario para hacer cambios autenticados.
        manager.fetchCsrfToken();
        
        // Crear varias propiedades en Wikibase para los establecimientos educativos.
        String regionPropertyId = manager.createProperty("región", "Nombre de la región donde se ubica la entidad", "wikibase-item");
        String comunaPropertyId = manager.createProperty("comuna", "Nombre de la comuna donde se ubica la entidad", "wikibase-item");
        String directorPropertyId = manager.createProperty("empleados", "Empleados de una organización", "quantity");
        String ubicacionPropertyId = manager.createProperty("ubicacion", "Coordenadas de un lugar o establecimiento", "globe-coordinate");
        String matriculaPropertyId = manager.createProperty("matrículados total", "Número de estudiantes matriculados en un establecimiento", "quantity");
        String tipoEstablecimientoId = manager.createProperty("tipo de establecimiento", "Tipos de Establecimientos Educacionales según el tipo de financiamiento", "wikibase-item");
        String ruralidadId = manager.createProperty("ruralidad", "Ruralidad de un establecimiento", "wikibase-item");
        String oriId = manager.createProperty("orientacion religiosa", "Orientación religiosa de la entidad", "wikibase-item");
        
        // Crear ítems en Wikibase relacionados con ruralidad y otros aspectos de la ubicación.
        String ruralID = manager.createItem("RURAL", "Establecimiento ubicado en una zona rural.");
        String urbanoID = manager.createItem("URBANO", "Establecimiento ubicado en una zona urbana.");
        
        // Crear ítems relacionados con la orientación religiosa de los establecimientos.
        manager.createItem("ORIENTACIÓN RELIGIOSA LAICA", "Establecimiento sin afiliación religiosa.");
        manager.createItem("ORIENTACIÓN RELIGIOSA CATÓLICA", "Establecimiento afiliado a la religión católica.");
        manager.createItem("ORIENTACIÓN RELIGIOSA EVANGÉLICA", "Establecimiento afiliado a la fe evangélica.");
        manager.createItem("ORIENTACIÓN MUSULMANA", "Establecimiento afiliado a la religión musulmana.");
        manager.createItem("ORIENTACIÓN JUDÍA", "Establecimiento afiliado a la religión judía.");
        manager.createItem("ORIENTACIÓN BUDISTA", "Establecimiento afiliado a la tradición budista.");
        manager.createItem("ORIENTACIÓN RELIGIOSA OTRO", "Establecimiento con orientación religiosa diferente o combinada.");

        // Crear ítems para definir estados operativos de los establecimientos.
        manager.createItem("FUNCIONANDO", "Establecimiento en funcionamiento activo.");
        manager.createItem("EN RECESO", "Establecimiento temporalmente inactivo.");
        manager.createItem("CERRADO", "Establecimiento cerrado permanentemente.");
        manager.createItem("AUTORIZADO SIN MATRICULA", "Establecimiento autorizado pero sin matrícula activa.");

        // Crear ítems para tipos de gestión de los establecimientos.
        manager.createItem("CORPORACIÓN MUNICIPAL", "Entidad educativa gestionada por una corporación municipal.");
        manager.createItem("MUNICIPAL DAEM", "Departamento de Administración de Educación Municipal (DAEM).");
        manager.createItem("PARTICULAR SUBVENCIONADO", "Establecimiento privado que recibe subvención estatal.");
        manager.createItem("PARTICULAR PAGADO", "Establecimiento privado financiado por pagos directos.");
        manager.createItem("CORP. DE ADMINISTRACIÓN DELEGADA (DL 3166)", "Establecimiento gestionado bajo el Decreto Ley 3166.");
        manager.createItem("SERVICIO LOCAL DE EDUCACIÓN", "Establecimiento gestionado por un Servicio Local de Educación Pública.");
        
        // Crear propiedades adicionales para regiones, personas y docentes.
        String establecimientosRegionPropertyId = manager.createProperty("establecimientos en la zona", "Establecimientos educativos en la región", "wikibase-item");
        String docentesRegionPropertyId = manager.createProperty("docentes en la Región", "Docentes que trabajan en la región", "wikibase-item");
        String trabajoEstablecimientoPropertyId = manager.createProperty("establecimiento de Trabajo", "Establecimiento donde trabaja una persona", "wikibase-item");
        String trabajoRegionPropertyId = manager.createProperty("región de trabajo", "Región donde trabaja la entidad", "wikibase-item");
        String trabajoComunaPropertyId = manager.createProperty("comuna de trabajo", "Comuna donde trabaja la entidad", "wikibase-item");
        String nacimientoPropertyId = manager.createProperty("fecha de nacimiento", "Fecha de nacimiento del docente", "time");
        String generoDocentePropertyId = manager.createProperty("género del Docente", "Género del docente", "string");
        String asignaturayId = manager.createProperty("asignatura", "Asignatura que imparte el docente", "string");
        
        // Crear propiedad para el año.
        String añoId = manager.createProperty("año", "Fecha asociada a un evento", "time");
        
        // Crear propiedad para definir la instancia de un objeto o entidad.
        String instanciaID = manager.createProperty("instancia de", "Instancia de un objeto o entidad", "wikibase-item");

        // Crear ítems para definir tipos de entidades como comuna, región, etc.
        String comunaID = manager.createItem("COMUNA", "Una subdivisión administrativa menor.");
        String regionID = manager.createItem("REGION", "Porción de territorio con características comunes.");
        String establecimientoID = manager.createItem("ESTABLECIMIENTO", "Unidad física diferenciada que ejerce actividades.");
        String personaID = manager.createItem("PERSONA", "Individuo de la especie humana.");

        // Relacionar los ítems creados con la instancia "clase".
        String claseId = manager.createItem("CLASE", "Instancia de una clase.");
        manager.addStatementToItem(comunaID, instanciaID, claseId, "wikibase-item");
        manager.addStatementToItem(regionID, instanciaID, claseId, "wikibase-item");
        manager.addStatementToItem(establecimientoID, instanciaID, claseId, "wikibase-item");
        manager.addStatementToItem(personaID, instanciaID, claseId, "wikibase-item");

        // Definir propiedades relacionadas con el género.
        String generoId = manager.createProperty("identificador género", "Género asignado a una estadística o valor", "wikibase-item");
        String hombreID = manager.createItem("HOMBRE", "Género masculino");
        String mujerID = manager.createItem("MUJER", "Género femenino");
        String nbID = manager.createItem("NO BINARIO", "Género no binario");
        String sinifID = manager.createItem("SIN INFORMACION", "Sin información de género");
        manager.addStatementToItem(hombreID, instanciaID, claseId, "wikibase-item");
        manager.addStatementToItem(mujerID, instanciaID, claseId, "wikibase-item");
        manager.addStatementToItem(nbID, instanciaID, claseId, "wikibase-item");
        manager.addStatementToItem(sinifID, instanciaID, claseId, "wikibase-item");

        // Definir propiedades relacionadas con el estado del establecimiento, matrícula, aprobaciones, etc.
        String estadoEstabId = manager.createProperty("estado del establecimiento", "Estado del establecimiento", "string");
        String matriculadosId = manager.createProperty("personas matrículadas", "Cantidad de personas matrículadas en un establecimiento", "quantity");
        String apId = manager.createProperty("personas aprobadas", "Cantidad de personas aprobados en un establecimiento", "quantity");
        String reId = manager.createProperty("personas reprobadas", "Cantidad de personas reprobadas en un establecimiento", "quantity");
        String retId = manager.createProperty("personas retiradas", "Cantidad de personas retiradas en un establecimiento", "quantity");
        String tranId = manager.createProperty("personas transferidas", "Cantidad de personas trasferidas en un establecimiento", "quantity");
        String sfdId = manager.createProperty("personas situacion final desconocida", "Cantidad de personas sin informacion de su situacion final en un establecimiento", "quantity");
        String codENSId = manager.createProperty("nivel de enseñanza", "Niveles de enseñanza agrupados", "string");
        String cursimId = manager.createProperty("total de cursos simples", "Total de cursos simples en el establecimiento", "quantity");
        String curcombId = manager.createProperty("total de cursos combinados", "Total de cursos combinados en el establecimiento", "quantity");
        String promAsisId = manager.createProperty("promedio de asistencia", "Porcentaje promedio de Asistencia de los alumnos de un mismo nivel de Enseñanza", "quantity");

        // Crear ítems adicionales relacionados con tipos de establecimiento.
        manager.createItem("COLEGIO", "Establecimiento público donde se da a los niños la instrucción primaria.");
        manager.createItem("ESCUELA", "Lugar donde se imparte educación o formación.");
        manager.createItem("LICEO", "Establecimiento de enseñanza secundaria.");
        manager.createItem("UNIVERSIDAD", "Institución de enseñanza superior e investigación.");
        manager.createItem("INSTITUTO", "Centro dedicado a la enseñanza o a la investigación.");
        manager.createItem("CENTRO", "Lugar donde se desarrollan actividades específicas.");
        manager.createItem("COMPLEJO", "Conjunto de instalaciones o edificios destinados a un fin común.");
        
        // Crear ítems relacionados con niveles de enseñanza.
        manager.createItem("ENSEÑANZA BÁSICA", "nivel de enseñanza básica para niños");
        manager.createItem("EDUCACIÓN BÁSICA COMÚN ADULTOS (DECRETO 77/1982)", "nivel de educación básica común para adultos según el Decreto 77/1982");
        manager.createItem("EDUCACIÓN BÁSICA ESPECIAL ADULTOS", "nivel de educación básica especial para adultos");
        manager.createItem("ESCUELAS CÁRCELES", "nivel de educación para adultos en cárceles");
        manager.createItem("EDUCACIÓN DE ADULTOS SIN OFICIOS (DECRETO 584/2007)", "nivel de educación para adultos sin oficios según el Decreto 584/2007");
        manager.createItem("EDUCACIÓN DE ADULTOS CON OFICIOS (DECRETO 584/2007 Y 999/2009)", "nivel de educación para adultos con oficios según los Decretos 584/2007 y 999/2009");
        manager.createItem("ENSEÑANZA MEDIA H-C NIÑOS Y JÓVENES", "nivel de enseñanza media H-C para niños y jóvenes");
        manager.createItem("EDUCACIÓN MEDIA H-C ADULTOS (DECRETO N°190/1975)", "nivel de educación media H-C para adultos según el Decreto N°190/1975");
        manager.createItem("EDUCACIÓN MEDIA H-C ADULTOS (DECRETO N°12/1987)", "nivel de educación media H-C para adultos según el Decreto N°12/1987");
        manager.createItem("EDUCACIÓN MEDIA H-C ADULTOS (DECRETO N°239/2004)", "nivel de educación media H-C para adultos según el Decreto N°239/2004");
        manager.createItem("ENSEÑANZA MEDIA T-P COMERCIAL NIÑOS", "nivel de enseñanza media T-P comercial para niños");
        manager.createItem("EDUCACIÓN MEDIA T-P COMERCIAL ADULTOS (DECRETO N° 152/1989)", "nivel de educación media T-P comercial para adultos según el Decreto N°152/1989");
        manager.createItem("EDUCACIÓN MEDIA T-P COMERCIAL ADULTOS (DECRETO N° 1000/2009)", "nivel de educación media T-P comercial para adultos según el Decreto N°1000/2009");
        manager.createItem("ENSEÑANZA MEDIA T-P INDUSTRIAL NIÑOS", "nivel de enseñanza media T-P industrial para niños");
        manager.createItem("EDUCACIÓN MEDIA T-P INDUSTRIAL ADULTOS (DECRETO N° 152/1989)", "nivel de educación media T-P industrial para adultos según el Decreto N°152/1989");
        manager.createItem("EDUCACIÓN MEDIA T-P INDUSTRIAL ADULTOS (DECRETO N° 1000/2009)", "nivel de educación media T-P industrial para adultos según el Decreto N°1000/2009");
        manager.createItem("ENSEÑANZA MEDIA T-P TÉCNICA NIÑOS", "nivel de enseñanza media T-P técnica para niños");
        manager.createItem("EDUCACIÓN MEDIA T-P TÉCNICA ADULTOS (DECRETO N° 152/1989)", "nivel de educación media T-P técnica para adultos según el Decreto N°152/1989");
        manager.createItem("EDUCACIÓN MEDIA T-P TÉCNICA ADULTOS (DECRETO N° 1000/2009)", "nivel de educación media T-P técnica para adultos según el Decreto N°1000/2009");
        manager.createItem("ENSEÑANZA MEDIA T-P AGRÍCOLA NIÑOS", "nivel de enseñanza media T-P agrícola para niños");
        manager.createItem("EDUCACIÓN MEDIA T-P AGRÍCOLA ADULTOS (DECRETO N° 152/1989)", "nivel de educación media T-P agrícola para adultos según el Decreto N°152/1989");
        manager.createItem("EDUCACIÓN MEDIA T-P AGRÍCOLA ADULTOS (DECRETO N° 1000/2009)", "nivel de educación media T-P agrícola para adultos según el Decreto N°1000/2009");
        manager.createItem("ENSEÑANZA MEDIA T-P MARÍTIMA NIÑOS", "nivel de enseñanza media T-P marítima para niños");
        manager.createItem("EDUCACIÓN MEDIA T-P MARÍTIMA ADULTOS (DECRETO N° 152/1989)", "nivel de educación media T-P marítima para adultos según el Decreto N°152/1989");
        manager.createItem("EDUCACIÓN MEDIA T-P MARÍTIMA ADULTOS (DECRETO N° 1000/2009)", "nivel de educación media T-P marítima para adultos según el Decreto N°1000/2009");
        manager.createItem("ENSEÑANZA MEDIA ARTÍSTICA NIÑOS Y JÓVENES", "nivel de enseñanza media artística para niños y jóvenes");
        manager.createItem("EDUCACIÓN MEDIA ARTÍSTICA ADULTOS", "nivel de educación media artística para adultos");
        manager.createProperty("identificador de nivel de educacion", "Identificador utilizado para separa datos por el nivel de educacion correspondiente", "wikibase-item");
        
        Map<String, String> regions = new HashMap<>();
        regions.put("REGIÓN DE TARAPACÁ", "TPCA");
        regions.put("REGIÓN DE ANTOFAGASTA", "ANTOF");
        regions.put("REGIÓN DE ATACAMA", "ATCMA");
        regions.put("REGIÓN DE COQUIMBO", "COQ");
        regions.put("REGIÓN DE VALPARAÍSO", "VALPO");
        regions.put("REGIÓN DEL LIBERTADOR GRAL. BERNARDO O’HIGGINS", "LGBO");
        regions.put("REGIÓN DEL MAULE", "MAULE");
        regions.put("REGIÓN DEL BIOBÍO", "BBIO");
        regions.put("REGIÓN DE LA ARAUCANÍA", "ARAUC");
        regions.put("REGIÓN DE LOS LAGOS", "LAGOS");
        regions.put("REGIÓN DE AYSÉN DEL GRAL. CARLOS IBÁÑEZ DEL CAMPO", "AYSEN");
        regions.put("REGIÓN DE MAGALLANES Y DE LA ANTÁRTICA CHILENA", "MAG");
        regions.put("REGIÓN METROPOLITANA DE SANTIAGO", "RM");
        regions.put("REGIÓN DE LOS RÍOS", "RIOS");
        regions.put("REGIÓN DE ARICA Y PARINACOTA", "AYP");
        regions.put("REGIÓN DE ÑUBLE", "NUBLE");

        // Create items for each region
        for (Map.Entry<String, String> entry : regions.entrySet()) {
            String regionName = entry.getKey();
            String abbreviatedName = entry.getValue();
            String regionItemId = manager.createItem(regionName, "Región de Chile");
            
            manager.addStatementToItem(regionItemId, instanciaID, regionID, "wikibase-item");

            if (regionItemId != null) {
                manager.addAlias(regionItemId, abbreviatedName, "es");
                System.out.println("Created item for " + regionName + " with alias " + abbreviatedName);
            }
        }
		
		System.out.println("Inicializacion finalizada");
	}

}

