package mb.statix.solver;

public class Delay extends Exception {

    private static final long serialVersionUID = 1L;

    public Delay() {
    }

    public Delay(String message) {
        super(message);
    }

}