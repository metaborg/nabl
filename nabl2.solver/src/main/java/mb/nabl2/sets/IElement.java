package mb.nabl2.sets;

public interface IElement<T> {

    T getValue();

    Object project(String name);

    T getPosition();

    T getName();

}