package mb.scopegraph.pepm16.terms.path;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IDeclPath;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.path.IScopePath;
import mb.scopegraph.pepm16.path.IStep;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.Scope;

public final class Paths {

    public static final String PATH_SEPERATOR = " ";

    // --------------------------------

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IStep<S, L, O> direct(S source, L label,
            S target) {
        return EStep.of(source, label, target);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IStep<S, L, O> named(S source, L label,
            IResolutionPath<S, L, O> importPath, S target) {
        return NStep.of(source, label, importPath, target);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IScopePath<S, L, O> empty(S scope) {
        return EmptyScopePath.of(scope);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IDeclPath<S, L, O>
            decl(IScopePath<S, L, O> path, O decl) {
        return DeclPath.of(path, decl);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IScopePath<S, L, O>>
            append(IScopePath<S, L, O> left, IScopePath<S, L, O> right) {
        if(left instanceof EmptyScopePath) {
            final EmptyScopePath<S, L, O> empty = (EmptyScopePath<S, L, O>) left;
            return empty.getScope().equals(right.getSource()) ? Optional.of(right) : Optional.empty();
        } else if(right instanceof EmptyScopePath) {
            final EmptyScopePath<S, L, O> empty = (EmptyScopePath<S, L, O>) right;
            return left.getTarget().equals(empty.getScope()) ? Optional.of(left) : Optional.empty();
        } else if(left instanceof ComposedScopePath) {
            final ComposedScopePath<S, L, O> inner = (ComposedScopePath<S, L, O>) left;
            return append(inner.getRight(), right).flatMap(r -> append(inner.getLeft(), r));
        } else {
            return Optional.ofNullable(ComposedScopePath.of(left, right));
        }
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IDeclPath<S, L, O>>
            append(IScopePath<S, L, O> left, IDeclPath<S, L, O> right) {
        return append(left, right.getPath()).map(p -> DeclPath.of(p, right.getDeclaration()));
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IResolutionPath<S, L, O>>
            resolve(O reference, IScopePath<S, L, O> path, O declaration) {
        return Optional.ofNullable(ResolutionPath.of(reference, path, declaration));
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IResolutionPath<S, L, O>>
            resolve(O reference, IDeclPath<S, L, O> path) {
        return Optional.ofNullable(ResolutionPath.of(reference, path.getPath(), path.getDeclaration()));
    }

    // -------------------------------------------

    public static IListTerm toTerm(IResolutionPath<Scope, Label, Occurrence> path) {
        ITerm dstep = B.newAppl("D", path.getPath().getTarget(), path.getDeclaration());
        return B.newListTail(toTerms(path.getPath()), B.newList(dstep));
    }

    public static IListTerm toTerm(IScopePath<Scope, Label, Occurrence> path) {
        return B.newList(toTerms(path));
    }

    private static List<ITerm> toTerms(IScopePath<Scope, Label, Occurrence> path) {
        List<ITerm> steps = Lists.newArrayList();
        for(IStep<Scope, Label, Occurrence> step : path) {
            steps.add(step.match(IStep.ICases.of(
            // @formatter:off
                (source, label, target) -> B.newAppl("E", source, label),
                (source, label, importPath, target) -> B.newAppl("N", source, label, importPath.getReference(), toTerm(importPath))
                // @formatter:on
            )));
        }
        return steps;
    }

    // -------------------------------------------


    public static <S extends IScope, L extends ILabel, O extends IOccurrence> List<O>
            declPathsToDecls(Iterable<IDeclPath<S, L, O>> paths) {
        final List<O> decls = new ArrayList<>();
        for(IDeclPath<S, L, O> path : paths) {
            decls.add(path.getDeclaration());
        }
        return decls;
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> List<O>
            resolutionPathsToDecls(Iterable<IResolutionPath<S, L, O>> paths) {
        final List<O> decls = new ArrayList<>();
        for(IDeclPath<S, L, O> path : paths) {
            decls.add(path.getDeclaration());
        }
        return decls;
    }

}