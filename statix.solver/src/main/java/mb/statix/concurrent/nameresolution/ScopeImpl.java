package mb.statix.concurrent.nameresolution;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import static mb.nabl2.terms.matching.Transform.T;
import mb.p_raffrayi.IScopeImpl;
import mb.scopegraph.oopsla20.diff.BiMap;
import mb.scopegraph.oopsla20.diff.BiMaps;
import mb.statix.scopegraph.Scope;

public class ScopeImpl implements IScopeImpl<Scope, ITerm> {

    @Override public Scope make(String id, String name) {
        return Scope.of(id, name);
    }

    @Override public String id(Scope scope) {
        return scope.getResource();
    }

    @Override public ITerm substituteScopes(ITerm datum, Map<Scope, Scope> substitution) {
        return T.sometd(Scope.matcher().map(s -> (ITerm) substitution.getOrDefault(s, s))::match).apply(datum);
    }
    @Override public Immutable<Scope> getScopes(ITerm datum) {
        return CapsuleUtil.toSet(T.collecttd(Scope.matcher()::match).apply(datum));
    }

    @Override public ITerm embed(Scope scope) {
        return scope;
    }

    @Override public Optional<BiMap.Immutable<Scope>> matchDatums(ITerm currentDatum, ITerm previousDatum) {
        return termMatch(currentDatum, previousDatum);
    }

    private Optional<BiMap.Immutable<Scope>> termMatch(ITerm left, ITerm right) {
        final Optional<Scope> leftScope = Scope.matcher().match(left);
        if(leftScope.isPresent()) {
            final Optional<Scope> rightScope = Scope.matcher().match(right);
            if(!rightScope.isPresent()) {
                return Optional.empty();
            }
            return Optional.of(BiMap.Immutable.of(leftScope.get(), rightScope.get()));
        }
        // @formatter:off
        return left.match(Terms.<Optional<BiMap.Immutable<Scope>>>cases(
            applLeft -> right.match(Terms.<Optional<BiMap.Immutable<Scope>>>cases()
                .appl(applRight -> {
                    if(applLeft.getArity() == applRight.getArity() &&
                        applLeft.getOp().equals(applRight.getOp())) {
                        return termsMatch(applLeft.getArgs(), applRight.getArgs());
                    }
                    return Optional.empty();
                })
                .otherwise(t -> {
                    return Optional.empty();
                })
            ),
            listLeft -> right.match(Terms.<Optional<BiMap.Immutable<Scope>>>cases()
                .list(listRight -> {
                    return listMatch(listLeft, listRight);
                })
                .otherwise(t -> {
                    return Optional.of(BiMap.Immutable.of());
                })
            ),
            stringLeft -> right.match(Terms.<Optional<BiMap.Immutable<Scope>>>cases()
                .string(stringRight -> {
                    return fromEquality(stringLeft.getValue().equals(stringRight.getValue()));
                })
                .otherwise(t -> {
                    return Optional.empty();
                })
            ),
            integerLeft -> right.match(Terms.<Optional<BiMap.Immutable<Scope>>>cases()
                .integer(integerRight -> {
                    return fromEquality(integerLeft.getValue() == integerRight.getValue());
                })
                .otherwise(t -> {
                    return Optional.empty();
                })
            ),
            blobLeft -> right.match(Terms.<Optional<BiMap.Immutable<Scope>>>cases()
                .blob(blobRight -> {
                    return fromEquality(blobLeft.getValue().equals(blobRight.getValue()));
                })
                .otherwise(t -> {
                    return Optional.empty();
                })
            ),
            varLeft -> right.match(Terms.<Optional<BiMap.Immutable<Scope>>>cases()
                .var(varRight -> {
                    return fromEquality(varLeft.equals(varRight));
                })
                .otherwise(termRight -> {
                    return Optional.empty();
                })
            )
        ));
        // @formatter:on
    }

    private Optional<BiMap.Immutable<Scope>> listMatch(final IListTerm left, final IListTerm right) {
        // @formatter:off
        return left.match(ListTerms.<Optional<BiMap.Immutable<Scope>>>cases(
            consLeft -> right.match(ListTerms.<Optional<BiMap.Immutable<Scope>>>cases()
                .cons(consRight -> {
                    return termMatch(consLeft.getHead(), consRight.getHead()).flatMap(headMatches -> {
                        return listMatch(consLeft.getTail(), consRight.getTail()).flatMap(tailMatches -> {
                            return BiMaps.safeMerge(headMatches, tailMatches);
                        });
                    });
                })
                .otherwise(l -> {
                    return Optional.empty();
                })
            ),
            nilLeft -> right.match(ListTerms.<Optional<BiMap.Immutable<Scope>>>cases()
                .nil(nilRight -> {
                    return Optional.of(BiMap.Immutable.of());
                })
                .otherwise(l -> {
                    return Optional.empty();
                })
            ),
            varLeft -> right.match(ListTerms.<Optional<BiMap.Immutable<Scope>>>cases()
                .var(varRight -> {
                    return fromEquality(varLeft.equals(varRight));
                })
                .otherwise(termRight -> {
                    return Optional.empty();
                })
            )
        ));
        // @formatter:on
    }

    private Optional<BiMap.Immutable<Scope>> termsMatch(final List<ITerm> lefts, final List<ITerm> rights) {
        if(lefts.size() != rights.size()) {
            return Optional.empty();
        }
        if(lefts.isEmpty()) {
            return Optional.of(BiMap.Immutable.of());
        }

        return termMatch(lefts.get(0), rights.get(0)).flatMap(headMatch -> {
            final List<ITerm> leftsSub = lefts.subList(1, lefts.size());
            final List<ITerm> rightsSub = rights.subList(1, rights.size());
            return termsMatch(leftsSub, rightsSub).flatMap(tailMatch -> {
                return BiMaps.safeMerge(headMatch, tailMatch);
            });
        });
    }

    private static Optional<BiMap.Immutable<Scope>> fromEquality(boolean equal) {
        return equal ? Optional.of(BiMap.Immutable.of()) : Optional.empty();
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
