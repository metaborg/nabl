package org.metaborg.meta.nabl2.sets;

public interface IElement<T> {

    T getValue();

    Object project(String name);

    T getPosition();

}