package mb.statix.concurrent;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.IFuture;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.Transform.T;
import mb.p_raffrayi.impl.diff.IScopeGraphDifferOps;
import mb.statix.scopegraph.Scope;

public class StatixDifferOps implements IScopeGraphDifferOps<Scope, ITerm> {

    private static final IFuture<Boolean> FALSE = CompletableFuture.completedFuture(false);
    private static final IFuture<Boolean> TRUE = CompletableFuture.completedFuture(true);

    @Override public Immutable<Scope> getScopes(ITerm datum) {
        return CapsuleUtil.toSet(T.collecttd(Scope.matcher()::match).apply(datum));
    }

    @Override public IFuture<Boolean> matchDatums(ITerm currentDatum, ITerm previousDatum,
            Function2<Scope, Scope, IFuture<Boolean>> scopeMatch) {
        return termMatch(currentDatum, previousDatum, scopeMatch);
    }

    private IFuture<Boolean> termMatch(ITerm left, ITerm right, Function2<Scope, Scope, IFuture<Boolean>> matchScopes) {
        final Optional<Scope> leftScope = Scope.matcher().match(left);
        if(leftScope.isPresent()) {
            final Optional<Scope> rightScope = Scope.matcher().match(right);
            if(!rightScope.isPresent()) {
                return FALSE;
            }
            return matchScopes.apply(leftScope.get(), rightScope.get());
        }
        // @formatter:off
        return left.match(Terms.<IFuture<Boolean>>cases(
            applLeft -> right.match(Terms.<IFuture<Boolean>>cases()
                .appl(applRight -> {
                    if(applLeft.getArity() == applRight.getArity() &&
                        applLeft.getOp().equals(applRight.getOp())) {
                        return termsMatch(applLeft.getArgs(), applRight.getArgs(), matchScopes);
                    }
                    return FALSE;
                })
                .otherwise(t -> {
                    return FALSE;
                })
            ),
            listLeft -> right.match(Terms.<IFuture<Boolean>>cases()
                .list(listRight -> {
                    return listMatch(listLeft, listRight, matchScopes);
                })
                .otherwise(t -> {
                    return TRUE;
                })
            ),
            stringLeft -> right.match(Terms.<IFuture<Boolean>>cases()
                .string(stringRight -> {
                    return f(stringLeft.getValue().equals(stringRight.getValue()));
                })
                .otherwise(t -> {
                    return FALSE;
                })
            ),
            integerLeft -> right.match(Terms.<IFuture<Boolean>>cases()
                .integer(integerRight -> {
                    return f(integerLeft.getValue() == integerRight.getValue());
                })
                .otherwise(t -> {
                    return FALSE;
                })
            ),
            blobLeft -> right.match(Terms.<IFuture<Boolean>>cases()
                .blob(blobRight -> {
                    return f(blobLeft.getValue().equals(blobRight.getValue()));
                })
                .otherwise(t -> {
                    return FALSE;
                })
            ),
            varLeft -> right.match(Terms.<IFuture<Boolean>>cases()
                .var(varRight -> {
                    return f(varLeft.equals(varRight));
                })
                .otherwise(termRight -> {
                    return FALSE;
                })
            )
        ));
        // @formatter:on
    }

    private IFuture<Boolean> listMatch(final IListTerm left, final IListTerm right,
        Function2<Scope, Scope, IFuture<Boolean>> matchScopes) {
        // @formatter:off
        return left.match(ListTerms.<IFuture<Boolean>>cases(
            consLeft -> right.match(ListTerms.<IFuture<Boolean>>cases()
                .cons(consRight -> {
                    return termMatch(consLeft.getHead(), consRight.getHead(), matchScopes)
                        .thenCompose(match -> {
                            if(!match) {
                                return FALSE;
                            }
                            return listMatch(consLeft.getTail(), consRight.getTail(), matchScopes);
                        });
                })
                .otherwise(l -> {
                    return FALSE;
                })
            ),
            nilLeft -> right.match(ListTerms.<IFuture<Boolean>>cases()
                .nil(nilRight -> {
                    return FALSE;
                })
                .otherwise(l -> {
                    return FALSE;
                })
            ),
            varLeft -> right.match(ListTerms.<IFuture<Boolean>>cases()
                .var(varRight -> {
                    return f(varLeft.equals(varRight));
                })
                .otherwise(termRight -> {
                    return FALSE;
                })
            )
        ));
        // @formatter:on
    }

    private IFuture<Boolean> termsMatch(final List<ITerm> lefts, final List<ITerm> rights,
            Function2<Scope, Scope, IFuture<Boolean>> matchScopes) {
        if(lefts.size() != rights.size()) {
            return FALSE;
        }
        if(lefts.isEmpty()) {
            return TRUE;
        }

        return termMatch(lefts.get(0), rights.get(0), matchScopes).thenCompose(match -> {
            if(!match) {
                return FALSE;
            }
            return termsMatch(lefts.subList(1, lefts.size()), rights.subList(1, rights.size()), matchScopes);
        });
    }

    private static IFuture<Boolean> f(boolean bool) {
        return bool ? TRUE : FALSE;
    }

    /* public static ITerm toTerm(ScopeGraphDiff<Scope, ITerm, ITerm> diff, IUnifier.Immutable current,
            IUnifier.Immutable previous) {
        final List<ITerm> matchedScopes = diff.matchedScopes().entrySet().stream()
                .map(e -> B.newAppl(MATCH_OP, e.getKey(), e.getValue())).collect(ImmutableList.toImmutableList());
        final ITerm added = toTerm(diff.added(), current);
        final ITerm removed = toTerm(diff.removed(), previous);
        return B.newAppl(DIFF_OP, B.newList(matchedScopes), added, removed);
    }

    public static ITerm toTerm(ScopeGraphDiff.Changes<Scope, ITerm, ITerm> changes, IUnifier.Immutable unifier) {
        final List<ITerm> scopes = changes.scopes().entrySet().stream()
                .map(e -> B.newAppl(SCOPE_OP, e.getKey(), e.getValue().map(unifier::findRecursive).orElse(e.getKey())))
                .collect(ImmutableList.toImmutableList());
        final List<ITerm> edges = changes.edges().stream().map(e -> B.newAppl(EDGE_OP, e.source, e.label, e.target))
                .collect(ImmutableList.toImmutableList());
        return B.newAppl(CHANGES_OP, B.newList(scopes), B.newList(edges));
    } */

}
