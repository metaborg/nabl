package org.metaborg.meta.nabl2.scopegraph;

public interface IPath<S extends IScope, L extends ILabel, O extends IOccurrence> {

    S getScope();

    O getDeclaration();

    <T> T match(ICases<S,L,O,T> cases);

    interface ICases<S extends IScope, L extends ILabel, O extends IOccurrence, T> {

        T caseDirect(S source, L label, IPath<S,L,O> tail);

        T caseNamed(S source, L label, O ref, IPath<S,L,O> inner, IPath<S,L,O> tail);

        T caseDecl(S scope, O decl);

    }

}