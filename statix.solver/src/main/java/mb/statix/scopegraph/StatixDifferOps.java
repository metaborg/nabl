package mb.statix.scopegraph;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import static mb.nabl2.terms.matching.Transform.T;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.scopegraph.oopsla20.diff.ScopeGraphDiff;
import mb.scopegraph.oopsla20.diff.ScopeGraphDifferOps;

public class StatixDifferOps implements ScopeGraphDifferOps<Scope, ITerm> {

    private static final String DIFF_OP = "Diff";
    private static final String CHANGES_OP = "Changes";
    private static final String MATCH_OP = "MatchEntry";
    private static final String SCOPE_OP = "ScopeEntry";
    private static final String EDGE_OP = "EdgeEntry";

    private final IUnifier.Immutable currentUnifier;
    private final IUnifier.Immutable previousUnifier;

    public StatixDifferOps(IUnifier.Immutable currentUnifier, IUnifier.Immutable previousUnifier) {
        this.currentUnifier = currentUnifier;
        this.previousUnifier = previousUnifier;
    }

    @Override public boolean isMatchAllowed(Scope current, Scope previous) {
        return current.getResource().equals(previous.getResource());
    }

    @Override public Set<Scope> getCurrentScopes(ITerm current) {
        return CapsuleUtil.toSet(T.collecttd(Scope.matcher()::match).apply(currentUnifier.findRecursive(current)));
    }

    @Override public Set<Scope> getPreviousScopes(ITerm previous) {
        return CapsuleUtil.toSet(T.collecttd(Scope.matcher()::match).apply(previousUnifier.findRecursive(previous)));
    }

    @Override public boolean matchDatums(ITerm current, ITerm previous, Predicate2<Scope, Scope> matchScopes) {
        return termMatch(currentUnifier.findRecursive(current), previousUnifier.findRecursive(previous), matchScopes);
    }

    private boolean termMatch(ITerm left, ITerm right, Predicate2<Scope, Scope> matchScopes) {
        final Optional<Scope> leftScope = Scope.matcher().match(left);
        if(leftScope.isPresent()) {
            final Optional<Scope> rightScope = Scope.matcher().match(right);
            if(!rightScope.isPresent()) {
                return false;
            }
            return matchScopes.test(leftScope.get(), rightScope.get());
        }
        // @formatter:off
        return left.match(Terms.<Boolean>cases(
            applLeft -> right.match(Terms.<Boolean>cases()
                .appl(applRight -> {
                    return applLeft.getArity() == applRight.getArity() &&
                            applLeft.getOp().equals(applRight.getOp()) &&
                            termsMatch(applLeft.getArgs(), applRight.getArgs(), matchScopes);
                })
                .otherwise(t -> {
                    return false;
                })
            ),
            listLeft -> right.match(Terms.<Boolean>cases()
                .list(listRight -> {
                    return listMatch(listLeft, listRight, matchScopes);
                })
                .otherwise(t -> {
                    return false;
                })
            ),
            stringLeft -> right.match(Terms.<Boolean>cases()
                .string(stringRight -> {
                    return stringLeft.getValue().equals(stringRight.getValue());
                })
                .otherwise(t -> {
                    return false;
                })
            ),
            integerLeft -> right.match(Terms.<Boolean>cases()
                .integer(integerRight -> {
                    return integerLeft.getValue() == integerRight.getValue();
                })
                .otherwise(t -> {
                    return false;
                })
            ),
            blobLeft -> right.match(Terms.<Boolean>cases()
                .blob(blobRight -> {
                    return blobLeft.getValue().equals(blobRight.getValue());
                })
                .otherwise(t -> {
                    return false;
                })
            ),
            varLeft -> right.match(Terms.<Boolean>cases()
                .var(varRight -> {
                    return varLeft.equals(varRight);
                })
                .otherwise(termRight -> {
                    return false;
                })
            )
        ));
        // @formatter:on
    }

    private boolean listMatch(final IListTerm _left, final IListTerm _right, Predicate2<Scope, Scope> matchScopes) {
        final IListTerm left = (IListTerm) currentUnifier.findTerm(_left);
        final IListTerm right = (IListTerm) currentUnifier.findTerm(_right);
        // @formatter:off
        return left.match(ListTerms.<Boolean>cases(
            consLeft -> right.match(ListTerms.<Boolean>cases()
                .cons(consRight -> {
                    return termMatch(consLeft.getHead(), consRight.getHead(), matchScopes)
                            && listMatch(consLeft.getTail(), consRight.getTail(), matchScopes);
                })
                .otherwise(l -> {
                    return false;
                })
            ),
            nilLeft -> right.match(ListTerms.<Boolean>cases()
                .nil(nilRight -> {
                    return true;
                })
                .otherwise(l -> {
                    return false;
                })
            ),
            varLeft -> right.match(ListTerms.<Boolean>cases()
                .var(varRight -> {
                    return varLeft.equals(varRight);
                })
                .otherwise(termRight -> {
                    return false;
                })
            )
        ));
        // @formatter:on
    }

    private boolean termsMatch(final Iterable<ITerm> lefts, final Iterable<ITerm> rights,
            Predicate2<Scope, Scope> matchScopes) {
        final Iterator<ITerm> itLeft = lefts.iterator();
        final Iterator<ITerm> itRight = rights.iterator();
        while(itLeft.hasNext()) {
            if(!itRight.hasNext()) {
                return false;
            }
            if(!termMatch(itLeft.next(), itRight.next(), matchScopes)) {
                return false;
            }
        }
        if(itRight.hasNext()) {
            return false;
        }
        return true;
    }

    public static ITerm toTerm(ScopeGraphDiff<Scope, ITerm, ITerm> diff, IUnifier.Immutable current,
            IUnifier.Immutable previous) {
        final List<ITerm> matchedScopes = diff.matchedScopes().entrySet().stream()
                .map(e -> B.newAppl(MATCH_OP, e.getKey(), e.getValue())).collect(ImmutableList.toImmutableList());
        final ITerm added = toTerm(diff.added(), current);
        final ITerm removed = toTerm(diff.removed(), previous);
        return B.newAppl(DIFF_OP, B.newList(matchedScopes), added, removed);
    }

    public static ITerm toTerm(ScopeGraphDiff.Changes<Scope, ITerm, ITerm> changes, IUnifier.Immutable unifier) {
        final List<ITerm> scopes = changes.scopes().entrySet().stream()
                .map(e -> B.newAppl(SCOPE_OP, e.getKey(), unifier.findRecursive(e.getValue())))
                .collect(ImmutableList.toImmutableList());
        final List<ITerm> edges = changes.edges().stream().map(e -> B.newAppl(EDGE_OP, e.source, e.label, e.target))
                .collect(ImmutableList.toImmutableList());
        return B.newAppl(CHANGES_OP, B.newList(scopes), B.newList(edges));
    }

}
