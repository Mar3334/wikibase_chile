// Paquete que contiene la clase ExecutionLogger para registrar la ejecución de eventos.
package wikibase;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

// Clase que gestiona el registro de la ejecución en un archivo CSV.
public class ExecutionLogger {

    // Atributo estático para escribir en el archivo.
    private static PrintWriter writer;

    // Bloque estático que se ejecuta cuando la clase es cargada para inicializar el PrintWriter.
    static {
        try {
            // Inicializa el PrintWriter para escribir en el archivo "execution.csv" y permitir agregar al final del archivo existente.
            writer = new PrintWriter(new FileWriter("execution.csv", true));
            
            // Escribe el encabezado si el archivo está vacío
            writer.println("Timestamp,Duration (ms),Lines Read");
        } catch (IOException e) {
            // Imprime la traza del error si ocurre alguna excepción al intentar escribir el archivo.
            e.printStackTrace();
        }
    }

    // Método para registrar la duración de una ejecución y el número de líneas leídas.
    public static void log(long duration, int linesRead) {
        // Escribe una línea en el archivo con el formato: timestamp actual, duración en milisegundos y número de líneas leídas.
        writer.printf("%d,%d,%d%n", System.currentTimeMillis(), duration, linesRead);
        
        // Fuerza la escritura en el archivo.
        writer.flush();
    }
}
