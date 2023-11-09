package mb.nabl2.terms.matching;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.functions.Action2;
import org.metaborg.util.functions.Function0;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.tuple.Tuple2;

import org.metaborg.util.collection.Sets;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.u.IUnifier;

public abstract class Pattern implements Serializable {
    private static final long serialVersionUID = 1L;

    private final IAttachments attachments;

    protected Pattern(IAttachments attachments) {
        this.attachments = attachments;
    }

    public IAttachments getAttachments() {
        return attachments;
    }

    public abstract java.util.Set<ITermVar> getVars();

    public abstract boolean isConstructed();

    public Optional<ISubstitution.Immutable> match(ITerm term) {
        return match(term, Unifiers.Immutable.of()).match(t -> t, v -> Optional.empty());
    }

    public MaybeNotInstantiated<Optional<ISubstitution.Immutable>> match(ITerm term, IUnifier.Immutable unifier) {
        final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        final List<ITermVar> stuckVars = new ArrayList<>();
        final Eqs eqs = new Eqs() {

            @Override public void add(ITermVar var, ITerm pattern) {
                stuckVars.add(var);
            }

            @Override public void add(ITermVar var, Pattern pattern) {
                stuckVars.add(var);
            }

        };
        if(!matchTerm(term, subst, unifier, eqs)) {
            return MaybeNotInstantiated.ofResult(Optional.empty());
        } else if(!stuckVars.isEmpty()) {
            return MaybeNotInstantiated.ofNotInstantiated(stuckVars);
        } else {
            return MaybeNotInstantiated.ofResult(Optional.of(subst.freeze()));
        }
    }

    /**
     * Match terms against a pattern and generate additional equalities that result from the match.
     *
     * Fresh variables are generated for unmatched variables in the patterns. As a result, the resulting substitution
     * has entries for all the variables in the patterns, and no pattern variables escape in the equalities.
     */
    public Optional<MatchResult> matchWithEqs(ITerm term, IUnifier.Immutable unifier, VarProvider fresh) {
        // substitution from pattern variables to unifier variables
        final ISubstitution.Transient _subst = PersistentSubstitution.Transient.of();
        // equalities between unifier terms
        final List<Tuple2<ITermVar, ITerm>> termEqs = new ArrayList<>();
        // equalities between unifier variables and patterns
        final List<Tuple2<ITermVar, Pattern>> patternEqs = new ArrayList<>();

        // match
        final Eqs eqs = new Eqs() {

            @Override public void add(ITermVar var, ITerm term) {
                termEqs.add(Tuple2.of(var, term));
            }

            @Override public void add(ITermVar var, Pattern pattern) {
                patternEqs.add(Tuple2.of(var, pattern));
            }

        };
        if(!matchTerm(term, _subst, unifier, eqs)) {
            return Optional.empty();
        }

        // generate fresh unifier variables for unmatched pattern variables
        for(ITermVar freeVar : Sets.difference(getVars(), _subst.domainSet())) {
            _subst.put(freeVar, fresh.freshVar(freeVar));
        }
        final ISubstitution.Immutable subst = _subst.freeze();

        // create equalities between unifier terms from pattern equalities
        final Set.Transient<ITermVar> stuckVars = CapsuleUtil.transientSet();
        final ImList.Mutable<Tuple2<ITerm, ITerm>> allEqs = new ImList.Mutable<>(termEqs.size());
        for(Tuple2<ITermVar, ITerm> termEq : termEqs) {
            final ITermVar leftVar = termEq._1();
            final ITerm rightTerm = termEq._2();
            stuckVars.__insert(leftVar);
            allEqs.add(Tuple2.of(leftVar, rightTerm));
        }
        for(Tuple2<ITermVar, Pattern> patternEq : patternEqs) {
            final ITermVar leftVar = patternEq._1();
            final ITerm rightTerm = patternEq._2().asTerm((v, t) -> {
                allEqs.add(Tuple2.of(subst.apply(v), subst.apply(t)));
            }, (v) -> v.orElseGet(() -> fresh.freshWld()));
            stuckVars.__insert(leftVar);
            allEqs.add(Tuple2.of(leftVar, subst.apply(rightTerm)));
        }

        return Optional.of(new MatchResult(subst, stuckVars.freeze(), allEqs.freeze()));
    }

    protected abstract boolean matchTerm(ITerm term, ISubstitution.Transient subst, IUnifier.Immutable unifier,
            Eqs eqs);

    protected static boolean matchTerms(final Iterable<Pattern> patterns, final Iterable<ITerm> terms,
            ISubstitution.Transient subst, IUnifier.Immutable unifier, Eqs eqs) {
        final Iterator<Pattern> itPattern = patterns.iterator();
        final Iterator<ITerm> itTerm = terms.iterator();
        while(itPattern.hasNext()) {
            if(!itTerm.hasNext()) {
                return false;
            }
            if(!itPattern.next().matchTerm(itTerm.next(), subst, unifier, eqs)) {
                return false;
            }
        }
        if(itTerm.hasNext()) {
            return false;
        }
        return true;
    }

    public abstract Pattern apply(IRenaming subst);

    public abstract Pattern eliminateWld(Function0<ITermVar> fresh);

    public Tuple2<ITerm, List<Tuple2<ITermVar, ITerm>>> asTerm(Function1<Optional<ITermVar>, ITermVar> fresh) {
        final ImList.Mutable<Tuple2<ITermVar, ITerm>> eqs = new ImList.Mutable<>(1);
        final ITerm term = asTerm((v, t) -> {
            eqs.add(Tuple2.of(v, t));
        }, fresh);
        return Tuple2.of(term, eqs.freeze());
    }

    protected abstract ITerm asTerm(Action2<ITermVar, ITerm> equalities, Function1<Optional<ITermVar>, ITermVar> fresh);

    protected interface Eqs {

        void add(ITermVar var, Pattern pattern);

        void add(ITermVar var, ITerm pattern);

    }

    ///////////////////////////////////////////////////////////////////////////
    // Pattern ordering                                                      //
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    public static final LeftRightOrder leftRightOrdering = new LeftRightOrder();

    public static class LeftRightOrder {

        /**
         * Compares two patterns for generality.
         *
         * If two patterns are comparable, it return an integer indicating which patterns is more general.
         * <ul>
         * <li>If the first pattern is more specific than the second, c < 0.
         * <li>If the first pattern is more general than the second, c > 0.
         * <li>If both are equally general, c = 0. When patterns are non-linear, patterns may be declared equal even if
         * their not.
         * </ul>
         * When used as an ordering (e.g., using asComparator) patterns are sorted such that more general patterns
         * appear after more specific.
         *
         */
        public Optional<Integer> compare(Pattern p1, Pattern p2) {
            return Optional.ofNullable(compare(p1, p2, new AtomicInteger(), new HashMap<>(), new HashMap<>()));
        }

        private @Nullable Integer compare(Pattern p1, Pattern p2, AtomicInteger pos, Map<ITermVar, Integer> vars1,
                Map<ITermVar, Integer> vars2) {
            if(p1 instanceof ApplPattern) {
                final ApplPattern appl1 = (ApplPattern) p1;
                if(p2 instanceof ApplPattern) {
                    final ApplPattern appl2 = (ApplPattern) p2;
                    if(!appl1.getOp().equals(appl2.getOp())) {
                        return null;
                    }
                    if(appl1.getArgs().size() != appl2.getArgs().size()) {
                        return null;
                    }
                    final Iterator<Pattern> it1 = appl1.getArgs().iterator();
                    final Iterator<Pattern> it2 = appl2.getArgs().iterator();
                    Integer c = 0;
                    while(c != null && c == 0 && it1.hasNext()) {
                        c = compare(it1.next(), it2.next(), pos, vars1, vars2);
                    }
                    return c;
                } else {
                    return compareVar(p1, p2, pos, vars1, vars2);
                }
            } else if(p1 instanceof ConsPattern) {
                final ConsPattern cons1 = (ConsPattern) p1;
                if(p2 instanceof ConsPattern) {
                    final ConsPattern cons2 = (ConsPattern) p2;
                    Integer c = 0;
                    c = compare(cons1.getHead(), cons2.getHead(), pos, vars1, vars2);
                    if(c != null && c == 0) {
                        c = compare(cons1.getTail(), cons2.getTail(), pos, vars1, vars2);
                    }
                    return c;
                } else {
                    return compareVar(p1, p2, pos, vars1, vars2);
                }
            } else if(p1 instanceof NilPattern) {
                if(p2 instanceof NilPattern) {
                    return 0;
                } else {
                    return compareVar(p1, p2, pos, vars1, vars2);
                }
            } else if(p1 instanceof StringPattern) {
                final StringPattern string1 = (StringPattern) p1;
                if(p2 instanceof StringPattern) {
                    final StringPattern string2 = (StringPattern) p2;
                    return string1.getValue().equals(string2.getValue()) ? 0 : null;
                } else {
                    return compareVar(p1, p2, pos, vars1, vars2);
                }
            } else if(p1 instanceof IntPattern) {
                final IntPattern integer1 = (IntPattern) p1;
                if(p2 instanceof IntPattern) {
                    final IntPattern integer2 = (IntPattern) p2;
                    return integer1.getValue() == integer2.getValue() ? 0 : null;
                } else {
                    return compareVar(p1, p2, pos, vars1, vars2);
                }
            } else if(p1 instanceof PatternVar) {
                final PatternVar var1 = (PatternVar) p1;
                final int i1 = boundAt(var1, vars1);
                if(p2 instanceof PatternVar) {
                    final PatternVar var2 = (PatternVar) p2;
                    final int i2 = boundAt(var2, vars2);
                    if(i1 < 0 && i2 < 0) { // neither are bound
                        bind(var1.getVar(), vars1, var2.getVar(), vars2, pos.getAndIncrement());
                        return 0;
                    } else if(i1 < 0 && i2 >= 0) { // p2 is bound
                        bind(var2.getVar(), vars1, pos.getAndIncrement());
                        return 1;
                    } else if(i1 >= 0 && i2 < 0) { // p1 is bound
                        bind(var2.getVar(), vars2, pos.getAndIncrement());
                        return -1;
                    } else { // both are bound, the left-most takes precedence
                        return i1 - i2;
                    }
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    bind(as2.getVar(), vars2, pos.get());
                    return compare(p1, as2.getPattern(), pos, vars1, vars2);
                } else {
                    return 1;
                }
            } else if(p1 instanceof PatternAs) {
                final PatternAs as1 = (PatternAs) p1;
                bind(as1.getVar(), vars1, pos.get()); // FIXME what if this is already bound?
                return compare(as1.getPattern(), p2, pos, vars1, vars2);
            } else {
                return null;
            }
        }

        private Integer compareVar(Pattern p1, Pattern p2, AtomicInteger pos, Map<ITermVar, Integer> vars1,
                Map<ITermVar, Integer> vars2) {
            if(p2 instanceof PatternVar) {
                final PatternVar var2 = (PatternVar) p2;
                if(boundAt(var2, vars2) == -1) {
                    bind(var2.getVar(), vars2, pos.getAndIncrement());
                }
                return -1;
            } else if(p2 instanceof PatternAs) {
                final PatternAs as2 = (PatternAs) p2;
                bind(as2.getVar(), vars2, pos.get());
                return compare(p1, as2.getPattern(), pos, vars1, vars2);
            } else {
                return null;
            }
        }

        // Binding positions of variables.

        private int boundAt(PatternVar vp, Map<ITermVar, Integer> vars) {
            final @Nullable ITermVar v = vp.getVar();
            if(v == null) {
                return -1;
            } else {
                return vars.getOrDefault(v, -1);
            }
        }

        private void bind(@Nullable ITermVar v1, Map<ITermVar, Integer> vars1, @Nullable ITermVar v2,
                Map<ITermVar, Integer> vars2, int pos) {
            bind(v1, vars1, pos);
            bind(v2, vars2, pos);
        }

        private void bind(@Nullable ITermVar v, Map<ITermVar, Integer> vars, int pos) {
            if(v != null && !vars.containsKey(v)) {
                vars.put(v, pos);
            }
        }

        /**
         * Return a comparator for patterns.
         *
         * Can be used to order patterns. It cannot not differentiate between incomparable patterns, and equivalent
         * patterns: both return 0.
         *
         * Note: this comparator imposes orderings that are inconsistent with equals.
         */
        public java.util.Comparator<Pattern> asComparator() {
            return new java.util.Comparator<Pattern>() {
                @Override public int compare(Pattern p1, Pattern p2) {
                    return LeftRightOrder.this.compare(p1, p2).orElse(0);
                }
            };
        }

    }

}
