package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;

public class TermPattern {

    private final List<ITerm> patterns;

    public TermPattern(Iterable<ITerm> patterns) {
        this.patterns = ImmutableList.copyOf(patterns);
    }

    public TermPattern(ITerm... patterns) {
        this(Arrays.asList(patterns));
    }

    public ISubstitution.Immutable match(Iterable<ITerm> terms) throws MatchException {
        return match((t1, t2) -> t1.equals(t2), terms);
    }

    public ISubstitution.Immutable match(Predicate2<ITerm, ITerm> equals, ITerm... terms) throws MatchException {
        return match(equals, Arrays.asList(terms));
    }

    public ISubstitution.Immutable match(ITerm... terms) throws MatchException {
        return match(Arrays.asList(terms));
    }

    public ISubstitution.Immutable match(Predicate2<ITerm, ITerm> equals, Iterable<ITerm> terms) throws MatchException {
        final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        final ITerm pattern = B.newTuple(patterns);
        final ITerm term = B.newTuple(terms);
        matchTerms(pattern, term, subst, equals);
        return subst.freeze();
    }

    private Unit matchTerms(ITerm pattern, ITerm term, ISubstitution.Transient subst, Predicate2<ITerm, ITerm> equals)
            throws MatchException {
        // @formatter:off
        return pattern.matchOrThrow(Terms.<Unit, MatchException>checkedCases(
            applPattern -> term.matchOrThrow(Terms.<Unit, MatchException>checkedCases()
                .appl(applTerm -> matchIf(pattern, term, applPattern.getOp().equals(applTerm.getOp()) &&
                                                         applPattern.getArity() == applTerm.getArity() &&
                                                         matchs(applPattern.getArgs(), applTerm.getArgs(), subst, equals)))
                .otherwise(t -> matchIf(pattern, term, false))
            ),
            listPattern -> term.matchOrThrow(Terms.<Unit, MatchException>checkedCases()
                .list(listTerm -> matchLists(listPattern, listTerm, subst, equals))
                .otherwise(t -> matchIf(pattern, term, false))
            ),
            stringPattern -> term.matchOrThrow(Terms.<Unit, MatchException>checkedCases()
                .string(stringTerm -> matchIf(pattern, term, stringPattern.getValue().equals(stringTerm.getValue())))
                .otherwise(t -> matchIf(pattern, term, false))
            ),
            integerPattern -> term.matchOrThrow(Terms.<Unit, MatchException>checkedCases()
                .integer(integerTerm -> matchIf(pattern, term, integerPattern.getValue() == integerTerm.getValue()))
                .otherwise(t -> matchIf(pattern, term, false))
            ),
            blobPattern -> term.matchOrThrow(Terms.<Unit, MatchException>checkedCases()
                .blob(blobTerm -> matchIf(pattern, term, blobPattern.getValue().equals(blobTerm.getValue())))
                .otherwise(t -> matchIf(pattern, term, false))
            ),
            varPattern -> matchVar(varPattern, term, subst, equals)
        ));
        // @formatter:on
    }

    private Unit matchLists(IListTerm pattern, IListTerm term, ISubstitution.Transient subst,
            Predicate2<ITerm, ITerm> equals) throws MatchException {
        // @formatter:off
        return pattern.matchOrThrow(ListTerms.<Unit, MatchException>checkedCases(
            consPattern -> term.matchOrThrow(ListTerms.<Unit, MatchException>checkedCases()
                .cons(consTerm -> matchTerms(consPattern.getHead(), consTerm.getHead(), subst, equals).andThenOrThrow(() ->
                                      matchLists(consPattern.getTail(), consTerm.getTail(), subst, equals)))
                .otherwise(l -> matchIf(pattern, term, false))
            ),
            nilPattern -> term.matchOrThrow(ListTerms.<Unit, MatchException>checkedCases()
                .nil(nilTerm -> Unit.unit)
                .otherwise(l -> matchIf(pattern, term, false))
            ),
            varPattern -> matchVar(varPattern, term, subst, equals)
        ));
        // @formatter:on
    }

    private Unit matchVar(ITermVar var, ITerm term, ISubstitution.Transient subst, Predicate2<ITerm, ITerm> equals)
            throws MatchException {
        if(subst.contains(var)) {
            return matchIf(var, term, equals.test(subst.apply(var), term));
        } else {
            subst.put(var, term);
            return Unit.unit;
        }
    }

    private Unit matchIf(ITerm pattern, ITerm term, boolean condition) throws MatchException {
        if(condition) {
            return Unit.unit;
        } else {
            throw new MatchException(pattern, term);
        }
    }

    private boolean matchs(final Iterable<ITerm> patterns, final Iterable<ITerm> terms, ISubstitution.Transient subst,
            Predicate2<ITerm, ITerm> equals) throws MatchException {
        Iterator<ITerm> itPattern = patterns.iterator();
        Iterator<ITerm> itTerm = terms.iterator();
        while(itPattern.hasNext()) {
            if(!itTerm.hasNext()) {
                return false;
            }
            matchTerms(itPattern.next(), itTerm.next(), subst, equals);
        }
        if(itTerm.hasNext()) {
            return false;
        }
        return true;
    }

    @Override public String toString() {
        return B.newTuple(patterns).toString();
    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    public static java.util.Comparator<TermPattern> leftRightOrdering = new LeftRightOrder();

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    private static class LeftRightOrder implements java.util.Comparator<TermPattern> {

        public int compare(TermPattern p1, TermPattern p2) {
            return new LeftRightPatternOrder().compare(B.newTuple(p1.patterns), B.newTuple(p2.patterns));
        }

    }

    /**
     * Note: this comparator imposes orderings that are inconsistent with equals.
     */
    private static class LeftRightPatternOrder implements java.util.Comparator<ITerm> {

        public int compare(ITerm p1, ITerm p2) {
            // @formatter:off
            return p1.match(Terms.<Integer>cases(
                appl1 -> p2.match(Terms.<Integer>cases(
                    appl2 -> {
                        int c = 0;
                        if(c == 0) {
                            c = appl1.getOp().compareTo(appl2.getOp());
                        }
                        if(c == 0) {
                            c = Ordering.from(this).lexicographical().compare(appl1.getArgs(), appl2.getArgs());
                        }
                        return c;
                    },
                    list2 -> {
                        return -1;
                    },
                    string2 -> {
                        return -1;
                    },
                    integer2 -> {
                        return -1;
                    },
                    blob2 -> {
                        return -1;
                    },
                    var2 -> {
                        return -1;
                    }
                )),
                list1 -> p2.match(Terms.<Integer>cases(
                    appl2 -> {
                        return 1;
                    },
                    list2 -> {
                        return compare(list1, list2);
                    },
                    string2 -> {
                        return -1;
                    },
                    integer2 -> {
                        return -1;
                    },
                    blob2 -> {
                        return -1;
                    },
                    var2 -> {
                        return -1;
                    }
                )),
                string1 -> p2.match(Terms.<Integer>cases(
                    appl2 -> {
                        return 1;
                    },
                    list2 -> {
                        return 1;
                    },
                    string2 -> {
                        return string1.getValue().compareTo(string2.getValue());
                    },
                    integer2 -> {
                        return -1;
                    },
                    blob2 -> {
                        return -1;
                    },
                    var2 -> {
                        return -1;
                    }
                )),
                integer1 -> p2.match(Terms.<Integer>cases(
                    appl2 -> {
                        return 1;
                    },
                    list2 -> {
                        return 1;
                    },
                    string2 -> {
                        return 1;
                    },
                    integer2 -> {
                        return Integer.compare(integer1.getValue(), integer2.getValue());
                    },
                    blob2 -> {
                        return -1;
                    },
                    var2 -> {
                        return -1;
                    }
                )),
                blob1 -> p2.match(Terms.<Integer>cases(
                    appl2 -> {
                        return 1;
                    },
                    list2 -> {
                        return 1;
                    },
                    string2 -> {
                        return 1;
                    },
                    integer2 -> {
                        return 1;
                    },
                    blob2 -> {
                        return 0; // all blobs considered equal, since we cannot compare them in a meaningful way
                    },
                    var2 -> {
                        return -1;
                    }
                )),
                var1 -> p2.match(Terms.<Integer>cases(
                    appl2 -> {
                        return 1;
                    },
                    list2 -> {
                        return 1;
                    },
                    string2 -> {
                        return 1;
                    },
                    integer2 -> {
                        return 1;
                    },
                    blob2 -> {
                        return 1;
                    },
                    var2 -> {
                        return 0; // all vars are equally general
                    }
                ))
            ));
            // @formatter:on
        }

        private int compare(IListTerm p1, IListTerm p2) {
            // @formatter:off
            return p1.match(ListTerms.<Integer>cases(
                cons1 -> p2.match(ListTerms.<Integer>cases(
                    cons2 -> {
                        int c = 0;
                        if(c == 0) {
                            c = compare(cons1.getHead(), cons2.getHead());
                        }
                        if(c == 0) {
                            c = compare(cons1.getTail(), cons2.getTail());
                        }
                        return c;
                    },
                    nil2 -> {
                        return -1;
                    },
                    var2 -> {
                        return -1;
                    }
                )),
                nil1 -> p2.match(ListTerms.<Integer>cases(
                    cons2 -> {
                        return 1;
                    },
                    nil2 -> {
                        return 0;
                    },
                    var2 -> {
                        return -1;
                    }
                )),
                var1 -> p2.match(ListTerms.<Integer>cases(
                    cons2 -> {
                        return 1;
                    },
                    nil2 -> {
                        return 1;
                    },
                    var2 -> {
                        return 0;
                    }
                ))
            ));
            // @formatter:on
        }

    }

}