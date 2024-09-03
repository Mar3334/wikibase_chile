package wikibase;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ExecutionLogger {
    private static PrintWriter writer;

    static {
        try {
            writer = new PrintWriter(new FileWriter("execution.csv", true));
            // Escribe el encabezado si el archivo está vacío
            writer.println("Timestamp,Duration (ms),Lines Read");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(long duration, int linesRead) {
        writer.printf("%d,%d,%d%n", System.currentTimeMillis(), duration, linesRead);
        writer.flush();
    }
}