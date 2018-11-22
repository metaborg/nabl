package mb.nabl2.terms.matching;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.Ordering;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;

public abstract class Pattern {

    public abstract Set<ITermVar> getVars();

    public Optional<ISubstitution.Immutable> match(ITerm term) {
        try {
            return match(term, PersistentUnifier.Immutable.of());
        } catch(InsufficientInstantiationException e) {
            return Optional.empty();
        }
    }

    public Optional<ISubstitution.Immutable> match(ITerm term, IUnifier unifier)
            throws InsufficientInstantiationException {
        final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        return Optionals.when(matchTerm(term, subst, unifier)).map(u -> subst.freeze());
    }

    protected abstract boolean matchTerm(ITerm term, ISubstitution.Transient subst, IUnifier unifier)
            throws InsufficientInstantiationException;

    protected static boolean matchTerms(final Iterable<Pattern> patterns, final Iterable<ITerm> terms,
            ISubstitution.Transient subst, IUnifier unifier) throws InsufficientInstantiationException {
        Iterator<Pattern> itPattern = patterns.iterator();
        Iterator<ITerm> itTerm = terms.iterator();
        while(itPattern.hasNext()) {
            if(!itTerm.hasNext()) {
                return false;
            }
            if(!itPattern.next().matchTerm(itTerm.next(), subst, unifier)) {
                return false;
            }
        }
        if(itTerm.hasNext()) {
            return false;
        }
        return true;
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

        public int compare(Pattern p1, Pattern p2) {
            if(p1 instanceof ApplPattern) {
                final ApplPattern appl1 = (ApplPattern) p1;
                if(p2 instanceof ApplPattern) {
                    final ApplPattern appl2 = (ApplPattern) p2;
                    int c = 0;
                    if(c == 0) {
                        c = appl1.getOp().compareTo(appl2.getOp());
                    }
                    if(c == 0) {
                        c = Ordering.from(this).lexicographical().compare(appl1.getArgs(), appl2.getArgs());
                    }
                    return c;
                } else if(p2 instanceof ConsPattern) {
                    return -1;
                } else if(p2 instanceof NilPattern) {
                    return -1;
                } else if(p2 instanceof StringPattern) {
                    return -1;
                } else if(p2 instanceof IntPattern) {
                    return -1;
                } else if(p2 instanceof PatternVar) {
                    return -1;
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    return compare(p1, as2.getPattern());
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
                        c = compare(cons1.getHead(), cons2.getHead());
                    }
                    if(c == 0) {
                        c = compare(cons1.getTail(), cons2.getTail());
                    }
                    return c;
                } else if(p2 instanceof NilPattern) {
                    return -1;
                } else if(p2 instanceof StringPattern) {
                    return -1;
                } else if(p2 instanceof IntPattern) {
                    return -1;
                } else if(p2 instanceof PatternVar) {
                    return -1;
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    return compare(p1, as2.getPattern());
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof NilPattern) {
                if(p2 instanceof ApplPattern) {
                    return 1;
                } else if(p2 instanceof ConsPattern) {
                    return 1;
                } else if(p2 instanceof NilPattern) {
                    return 0;
                } else if(p2 instanceof StringPattern) {
                    return -1;
                } else if(p2 instanceof IntPattern) {
                    return -1;
                } else if(p2 instanceof PatternVar) {
                    return -1;
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    return compare(p1, as2.getPattern());
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof StringPattern) {
                final StringPattern string1 = (StringPattern) p1;
                if(p2 instanceof ApplPattern) {
                    return 1;
                } else if(p2 instanceof ConsPattern) {
                    return 1;
                } else if(p2 instanceof NilPattern) {
                    return 1;
                } else if(p2 instanceof StringPattern) {
                    final StringPattern string2 = (StringPattern) p2;
                    return string1.getValue().compareTo(string2.getValue());
                } else if(p2 instanceof IntPattern) {
                    return -1;
                } else if(p2 instanceof PatternVar) {
                    return -1;
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    return compare(p1, as2.getPattern());
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof IntPattern) {
                final IntPattern integer1 = (IntPattern) p1;
                if(p2 instanceof ApplPattern) {
                    return 1;
                } else if(p2 instanceof ConsPattern) {
                    return 1;
                } else if(p2 instanceof NilPattern) {
                    return 1;
                } else if(p2 instanceof StringPattern) {
                    return 1;
                } else if(p2 instanceof IntPattern) {
                    final IntPattern integer2 = (IntPattern) p2;
                    return Integer.compare(integer1.getValue(), integer2.getValue());
                } else if(p2 instanceof PatternVar) {
                    return -1;
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    return compare(p1, as2.getPattern());
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof PatternVar) {
                if(p2 instanceof ApplPattern) {
                    return 1;
                } else if(p2 instanceof ConsPattern) {
                    return 1;
                } else if(p2 instanceof NilPattern) {
                    return 1;
                } else if(p2 instanceof StringPattern) {
                    return 1;
                } else if(p2 instanceof IntPattern) {
                    return 1;
                } else if(p2 instanceof PatternVar) {
                    return 0; // all vars are equally general
                } else if(p2 instanceof PatternAs) {
                    final PatternAs as2 = (PatternAs) p2;
                    return compare(p1, as2.getPattern());
                } else {
                    throw new IllegalStateException();
                }
            } else if(p1 instanceof PatternAs) {
                final PatternAs as1 = (PatternAs) p1;
                return compare(as1.getPattern(), p2);
            } else {
                throw new IllegalStateException();
            }
        }

    }

}