package org.metaborg.meta.nabl2.terms;

public interface ITerm {

    boolean isGround();

    <T> T apply(ITermFunction<T> visitor);

}