// Paquete que contiene la clase VariablePosition para gestionar posiciones de variables.
package wikibase;

// Clase que representa una posición de variable con un nombre y una posición en un índice.
public class VariablePosition {
    
    // Atributo que almacena el nombre de la variable.
    private final String variable;
    
    // Atributo que almacena la posición de la variable en un índice.
    private final int position;

    // Constructor de la clase que inicializa la variable y su posición.
    public VariablePosition(String variable, int position) {
        this.variable = variable;
        this.position = position;
    }

    // Método para obtener el nombre de la variable.
    public String getVariable() {
        return variable;
    }

    // Método para obtener la posición de la variable.
    public int getPosition() {
        return position;
    }

    // Método que convierte el objeto en una representación en forma de cadena.
    @Override
    public String toString() {
        return "VariablePosition{" +
                "variable='" + variable + '\'' +
                ", position=" + position +
                '}';
    }
}
