package wikibase;

public class VariablePosition {
    private final String variable;
    private final int position;

    public VariablePosition(String variable, int position) {
        this.variable = variable;
        this.position = position;
    }

    public String getVariable() {
        return variable;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "VariablePosition{" +
                "variable='" + variable + '\'' +
                ", position=" + position +
                '}';
    }
}