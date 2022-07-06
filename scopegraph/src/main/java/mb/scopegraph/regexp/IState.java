package mb.scopegraph.regexp;

public interface IState<S> {

    boolean isFinal();

    boolean isAccepting();

    boolean isOblivion();

    IState<S> transition(S symbol);

}
