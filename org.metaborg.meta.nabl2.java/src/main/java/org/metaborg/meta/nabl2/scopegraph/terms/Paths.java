package org.metaborg.meta.nabl2.scopegraph.terms;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IPath;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;

import com.google.common.collect.Lists;

public class Paths {

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IPath<S,L,O> decl(S scope, O decl) {
        return ImmutableDeclPath.of(scope, decl);
    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    abstract static class DeclPath<S extends IScope, L extends ILabel, O extends IOccurrence> implements IPath<S,L,O> {

        @Override @Value.Parameter public abstract S getScope();

        @Override @Value.Parameter public abstract O getDeclaration();

        @Override public <T> T match(org.metaborg.meta.nabl2.scopegraph.IPath.ICases<S,L,O,T> cases) {
            return cases.caseDecl(getScope(), getDeclaration());
        }

    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IPath<S,L,O> direct(S scope, L label,
            IPath<S,L,O> tail) {
        return ImmutableDirectPath.of(scope, label, tail);
    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    abstract static class DirectPath<S extends IScope, L extends ILabel, O extends IOccurrence> implements
            IPath<S,L,O> {

        @Override @Value.Parameter public abstract S getScope();

        @Value.Parameter public abstract L getLabel();

        @Value.Parameter public abstract IPath<S,L,O> getTail();

        @Override @Value.Lazy public O getDeclaration() {
            return getTail().getDeclaration();
        }

        @Override public <T> T match(org.metaborg.meta.nabl2.scopegraph.IPath.ICases<S,L,O,T> cases) {
            return cases.caseDirect(getScope(), getLabel(), getTail());
        }

    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IPath<S,L,O> named(S scope, L label,
            O ref, IPath<S,L,O> inner, IPath<S,L,O> tail) {
        return ImmutableNamedPath.of(scope, label, ref, inner, tail);
    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    abstract static class NamedPath<S extends IScope, L extends ILabel, O extends IOccurrence> implements IPath<S,L,O> {

        @Override @Value.Parameter public abstract S getScope();

        @Value.Parameter public abstract L getLabel();

        @Value.Parameter public abstract O getReference();

        @Value.Parameter public abstract IPath<S,L,O> getInner();

        @Value.Parameter public abstract IPath<S,L,O> getTail();

        @Override @Value.Lazy public O getDeclaration() {
            return getTail().getDeclaration();
        }

        @Override public <T> T match(org.metaborg.meta.nabl2.scopegraph.IPath.ICases<S,L,O,T> cases) {
            return cases.caseDirect(getScope(), getLabel(), getTail());
        }

    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> List<O> pathsToDecls(
            Iterable<IPath<S,L,O>> paths) {
        return Lists.newArrayList(paths).stream().map(IPath::getDeclaration).collect(Collectors.toList());
    }

    public static ITerm toTerm(IPath<Scope,Label,Occurrence> path) {
        return GenericTerms.newNil();
    }

}