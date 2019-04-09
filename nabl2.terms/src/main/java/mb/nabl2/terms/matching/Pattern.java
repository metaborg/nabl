package mb.nabl2.terms.matching;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;

public abstract class Pattern {

    public abstract Set<ITermVar> getVars();

    public Optional<ISubstitution.Immutable> match(ITerm term) {
        return match(term, PersistentUnifier.Immutable.of()).match(t -> t, v -> Optional.empty());
    }

    public MaybeNotInstantiated<Optional<Immutable>> match(ITerm term, IUnifier unifier) {
        final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        return matchTerm(term, subst, unifier).map(u -> u ? Optional.of(subst.freeze()) : Optional.empty());
    }

    protected abstract MaybeNotInstantiated<Boolean> matchTerm(ITerm term, ISubstitution.Transient subst,
            IUnifier unifier);

    protected static MaybeNotInstantiated<Boolean> matchTerms(final Iterable<Pattern> patterns,
            final Iterable<ITerm> terms, ISubstitution.Transient subst, IUnifier unifier) {
        final Iterator<Pattern> itPattern = patterns.iterator();
        final Iterator<ITerm> itTerm = terms.iterator();
        final List<ITermVar> stuckVars = Lists.newArrayList();
        while(itPattern.hasNext()) {
            if(!itTerm.hasNext()) {
                return MaybeNotInstantiated.ofResult(false);
            }
            final MaybeNotInstantiated<Boolean> result = itPattern.next().matchTerm(itTerm.next(), subst, unifier);
            final boolean canStillMatch = result.match(m -> m, vars -> {
                // continue the match, it might still fail, but collect stuck vars
                stuckVars.addAll(vars);
                return true;
            });
            if(!canStillMatch) {
                return MaybeNotInstantiated.ofResult(false);
            }
        }
        if(itTerm.hasNext()) {
            return MaybeNotInstantiated.ofResult(false);
        }
        if(stuckVars.isEmpty()) {
            return MaybeNotInstantiated.ofResult(true);
        } else {
            return MaybeNotInstantiated.ofNotInstantiated(stuckVars);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Pattern ordering                                                      //
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    public static java.util.Comparator<Pattern> leftRightOrdering = new LeftRightOrder();

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    private static class LeftRightOrder implements java.util.Comparator<Pattern> {

        @Override
        public int compare(Pattern p1, Pattern p2) {
            return compare(p1, p2, new AtomicInteger(), new HashMap<>(), new HashMap<>());
        }

        private int compare(Pattern p1, Pattern p2, AtomicInteger pos, Map<ITermVar, Integer> vars1,
                Map<ITermVar, Integer> vars2) {
            if(p1 instanceof ApplPattern) {
                final ApplPattern appl1 = (ApplPattern) p1;
                if(p2 instanceof ApplPattern) {
                    final ApplPattern appl2 = (ApplPattern) p2;
                    int c = 0;
                    if(c == 0) {
                        c = appl1.getOp().compareTo(appl2.getOp());
                    }
                    final Iterator<Pattern> it1 = appl1.getArgs().iterator();
                    final Iterator<Pattern> it2 = appl2.getArgs().iterator();
                    while(c == 0 && it1.hasNext()) {
                        if(!it2.hasNext()) {
                            return 1;
                        }
                        c = compare(it1.next(), it2.next(), pos, vars1, vars2);
                    }
                    if(c == 0 && it2.hasNext()) {
                        return -1;
                    }
                    return c;
                } else if(p2 instanceof ConsPattern || p2 instanceof NilPattern || p2 instanceof StringPattern
                        || p2 instanceof IntPattern) {
                    return -1;
                } else if(p2 instanceof PatternVar) {
                    final PatternVar var2 = (PatternVar) p2;
                    if(boundAt(var2, vars2) >= 0) {
                        return 0;
                    } else {
                        bind(var2.getVar(), vars2, pos.getAndIncrement());
                        return -1;
                    }
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    bind(as2.getVar(), vars2, pos.get());
                    return compare(p1, as2.getPattern(), pos, vars1, vars2);
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof ConsPattern) {
                final ConsPattern cons1 = (ConsPattern) p1;
                if(p2 instanceof ApplPattern) {
                    return 1;
                } else if(p2 instanceof ConsPattern) {
                    final ConsPattern cons2 = (ConsPattern) p2;
                    int c = 0;
                    if(c == 0) {
                        c = compare(cons1.getHead(), cons2.getHead(), pos, vars1, vars2);
                    }
                    if(c == 0) {
                        c = compare(cons1.getTail(), cons2.getTail(), pos, vars1, vars2);
                    }
                    return c;
                } else if(p2 instanceof NilPattern || p2 instanceof StringPattern || p2 instanceof IntPattern) {
                    return -1;
                } else if(p2 instanceof PatternVar) {
                    final PatternVar var2 = (PatternVar) p2;
                    if(boundAt(var2, vars2) >= 0) {
                        return 0;
                    } else {
                        bind(var2.getVar(), vars2, pos.getAndIncrement());
                        return -1;
                    }
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    bind(as2.getVar(), vars2, pos.get());
                    return compare(p1, as2.getPattern(), pos, vars1, vars2);
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof NilPattern) {
                if(p2 instanceof ApplPattern || p2 instanceof ConsPattern) {
                    return 1;
                } else if(p2 instanceof NilPattern) {
                    return 0;
                } else if(p2 instanceof StringPattern || p2 instanceof IntPattern) {
                    return -1;
                } else if(p2 instanceof PatternVar) {
                    final PatternVar var2 = (PatternVar) p2;
                    if(boundAt(var2, vars2) >= 0) {
                        return 0;
                    } else {
                        bind(var2.getVar(), vars2, pos.getAndIncrement());
                        return -1;
                    }
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    bind(as2.getVar(), vars2, pos.get());
                    return compare(p1, as2.getPattern(), pos, vars1, vars2);
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof StringPattern) {
                final StringPattern string1 = (StringPattern) p1;
                if(p2 instanceof ApplPattern || p2 instanceof ConsPattern || p2 instanceof NilPattern) {
                    return 1;
                } else if(p2 instanceof StringPattern) {
                    final StringPattern string2 = (StringPattern) p2;
                    return string1.getValue().compareTo(string2.getValue());
                } else if(p2 instanceof IntPattern) {
                    return -1;
                } else if(p2 instanceof PatternVar) {
                    final PatternVar var2 = (PatternVar) p2;
                    if(boundAt(var2, vars2) >= 0) {
                        return 0;
                    } else {
                        bind(var2.getVar(), vars2, pos.getAndIncrement());
                        return -1;
                    }
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    bind(as2.getVar(), vars2, pos.get());
                    return compare(p1, as2.getPattern(), pos, vars1, vars2);
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof IntPattern) {
                final IntPattern integer1 = (IntPattern) p1;
                if(p2 instanceof ApplPattern || p2 instanceof ConsPattern || p2 instanceof NilPattern
                        || p2 instanceof StringPattern) {
                    return 1;
                } else if(p2 instanceof IntPattern) {
                    final IntPattern integer2 = (IntPattern) p2;
                    return Integer.compare(integer1.getValue(), integer2.getValue());
                } else if(p2 instanceof PatternVar) {
                    final PatternVar var2 = (PatternVar) p2;
                    if(boundAt(var2, vars2) >= 0) {
                        return 0;
                    } else {
                        bind(var2.getVar(), vars2, pos.getAndIncrement());
                        return -1;
                    }
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    bind(as2.getVar(), vars2, pos.get());
                    return compare(p1, as2.getPattern(), pos, vars1, vars2);
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof PatternVar) {
                final PatternVar var1 = (PatternVar) p1;
                final int i1 = boundAt(var1, vars1);
                if(p2 instanceof ApplPattern || p2 instanceof ConsPattern || p2 instanceof NilPattern
                        || p2 instanceof StringPattern || p2 instanceof IntPattern) {
                    return (i1 >= 0) ? 0 : 1;
                } else if(p2 instanceof PatternVar) {
                    final PatternVar var2 = (PatternVar) p2;
                    final int i2 = boundAt(var2, vars2);
                    if(i1 < 0 && i2 < 0) {
                        bind(var1.getVar(), vars1, var2.getVar(), vars2, pos.getAndIncrement());
                        return 0;
                    } else if(i1 < 0 && i2 >= 0) {
                        bind(var2.getVar(), vars1, pos.getAndIncrement());
                        return 1;
                    } else if(i1 >= 0 && i2 < 0) {
                        bind(var2.getVar(), vars2, pos.getAndIncrement());
                        return -1;
                    } else {
                        return i1 - i2;
                    }
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    bind(as2.getVar(), vars2, pos.get());
                    return compare(p1, as2.getPattern(), pos, vars1, vars2);
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof PatternAs) {
                final PatternAs as1 = (PatternAs) p1;
                bind(as1.getVar(), vars1, pos.get());
                return compare(as1.getPattern(), p2, pos, vars1, vars2);
            } else {
                throw new IllegalStateException();
            }
        }

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

    }

}