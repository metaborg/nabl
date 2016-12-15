package org.metaborg.meta.nabl2.relations;

public interface IVariance {

    <T> T match(Cases<T> cases);
    
    interface Cases<T> {

        T caseInvariant();

        T caseCovariant(IRelationName name);

        T caseContravariant(IRelationName name);

    }

}