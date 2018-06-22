package mb.nabl2.terms.matching;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.nabl2.terms.unification.UnificationException;

public class TermPattern implements IPattern {

    private final List<ITerm> patterns;

    public TermPattern(List<ITerm> patterns) {
        this.patterns = ImmutableList.copyOf(patterns);
    }

    public TermPattern(ITerm... patterns) {
        this(Arrays.asList(patterns));
    }

    @Override public MatchResult match(Iterable<ITerm> terms) throws MatchException {
        final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        final IUnifier.Transient unifier = PersistentUnifier.Transient.of();
        final ITerm pattern = B.newTuple(patterns);
        final ITerm term = B.newTuple(terms);
        if(!matchTerms(pattern, term, subst, unifier)) {
            throw new MatchException(pattern, term);
        }
        return ImmutableMatchResult.of(subst.freeze(), unifier.freeze());
    }

    private boolean matchTerms(ITerm pattern, ITerm term, ISubstitution.Transient subst, IUnifier.Transient unifier) {
        // @formatter:off
        return pattern.<Boolean>match(Terms.cases(
            applPattern -> term.match(Terms.<Boolean>cases()
                .appl(applTerm -> applPattern.getOp().equals(applTerm.getOp()) &&
                                  applPattern.getArity() == applTerm.getArity() &&
                                  matchs(applPattern.getArgs(), applTerm.getArgs(), subst, unifier))
                .otherwise(t -> false)
            ),
            listPattern -> term.match(Terms.<Boolean>cases()
                .list(listTerm -> matchLists(listPattern, listTerm, subst, unifier))
                .otherwise(t -> false)
            ),
            stringPattern -> term.match(Terms.<Boolean>cases()
                .string(stringTerm -> stringPattern.getValue().equals(stringTerm.getValue()))
                .otherwise(t -> false)
            ),
            integerPattern -> term.match(Terms.<Boolean>cases()
                .integer(integerTerm -> integerPattern.getValue() == integerTerm.getValue())
                .otherwise(t -> false)
            ),
            blobPattern -> term.match(Terms.<Boolean>cases()
                .blob(blobTerm -> blobPattern.getValue().equals(blobTerm.getValue()))
                .otherwise(t -> false)
            ),
            varPattern -> matchVar(varPattern, term, subst, unifier)
        ));
        // @formatter:on
    }

    private boolean matchLists(IListTerm pattern, IListTerm term, ISubstitution.Transient subst,
            IUnifier.Transient unifier) {
        // @formatter:off
        return pattern.<Boolean>match(ListTerms.cases(
            consPattern -> term.match(ListTerms.<Boolean>cases()
                .cons(consTerm -> matchTerms(consPattern.getHead(), consTerm.getHead(), subst, unifier) &&
                                  matchLists(consPattern.getTail(), consTerm.getTail(), subst, unifier))
                .otherwise(l -> false)
            ),
            nilPattern -> term.match(ListTerms.<Boolean>cases()
                .nil(nilTerm -> true)
                .otherwise(l -> false)
            ),
            varPattern -> matchVar(varPattern, term, subst, unifier)
        ));
        // @formatter:on
    }

    private boolean matchVar(ITermVar var, ITerm term, ISubstitution.Transient subst, IUnifier.Transient unifier) {
        if(subst.contains(var)) {
            try {
                unifier.unify(term, subst.apply(var));
            } catch(UnificationException e) {
                return false;
            }
        } else {
            subst.put(var, term);
        }
        return true;
    }

    private boolean matchs(final Iterable<ITerm> patterns, final Iterable<ITerm> terms, ISubstitution.Transient subst,
            IUnifier.Transient unifier) {
        Iterator<ITerm> itPattern = patterns.iterator();
        Iterator<ITerm> itTerm = terms.iterator();
        while(itPattern.hasNext()) {
            if(!itTerm.hasNext()) {
                return false;
            }
            if(!matchTerms(itPattern.next(), itTerm.next(), subst, unifier)) {
                return false;
            }
        }
        if(itTerm.hasNext()) {
            return false;
        }
        return true;
    }

}