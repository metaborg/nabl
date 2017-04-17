package org.metaborg.meta.nabl2.scopegraph.terms.path;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.path.IStep;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Lists;

public final class Paths {

    public static final String PATH_SEPERATOR = " ";

    // --------------------------------

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IScopePath<S, L, O> direct(S source,
            L label, S target) {
        return ImmutableEStep.of(source, label, target);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IScopePath<S, L, O> named(S source,
            L label, IResolutionPath<S, L, O> importPath, S target) {
        return ImmutableNStep.of(source, label, importPath, target);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IScopePath<S, L, O> empty(S scope) {
        return ImmutableEmptyScopePath.of(scope);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IDeclPath<S, L, O>
            decl(IScopePath<S, L, O> path, O decl) {
        return ImmutableDeclPath.of(path, decl);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IScopePath<S, L, O>>
            append(IScopePath<S, L, O> left, IScopePath<S, L, O> right) {
        return Optional.ofNullable(ImmutableComposedScopePath.of(left, right));
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IDeclPath<S, L, O>>
            append(IScopePath<S, L, O> left, IDeclPath<S, L, O> right) {
        return Optional.ofNullable(ImmutableComposedScopePath.of(left, right.getPath()))
                .map(p -> ImmutableDeclPath.of(p, right.getDeclaration()));
    }


    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IResolutionPath<S, L, O>>
            resolve(O reference, IScopePath<S, L, O> path, O declaration) {
        return Optional.ofNullable(ImmutableResolutionPath.of(reference, path, declaration));
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IResolutionPath<S, L, O>>
            resolve(O reference, IDeclPath<S, L, O> path) {
        return Optional.ofNullable(ImmutableResolutionPath.of(reference, path.getPath(), path.getDeclaration()));
    }

    // -------------------------------------------

    public static IListTerm toTerm(IResolutionPath<Scope, Label, Occurrence> path) {
        ITerm dstep = TB.newAppl("D", path.getPath().getTarget(), path.getDeclaration());
        return TB.newListTail(toTerm(path.getPath()), TB.newList(dstep));
    }

    public static IListTerm toTerm(IScopePath<Scope, Label, Occurrence> path) {
        List<ITerm> steps = Lists.newArrayList();
        for(IStep<Scope, Label, Occurrence> step : path) {
            steps.add(step.match(IStep.ICases.of(
                // @formatter:off
                (source, label, target) -> TB.newAppl("E", source, label),
                (source, label, importPath, target) -> TB.newAppl("N", source, label, importPath.getReference(), toTerm(importPath))
                // @formatter:on
            )));
        }
        return TB.newList(steps);
    }

    // -------------------------------------------


    public static List<Occurrence> declPathsToDecls(Iterable<IDeclPath<Scope, Label, Occurrence>> paths) {
        return Iterables2.stream(paths).map(IDeclPath::getDeclaration).collect(Collectors.toList());
    }

    public static List<Occurrence> resolutionPathsToDecls(Iterable<IResolutionPath<Scope, Label, Occurrence>> paths) {
        return Iterables2.stream(paths).map(IResolutionPath::getDeclaration).collect(Collectors.toList());
    }

}