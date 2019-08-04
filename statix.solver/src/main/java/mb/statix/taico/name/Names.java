package mb.statix.taico.name;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.ApplPattern;
import mb.nabl2.terms.matching.ConsPattern;
import mb.nabl2.terms.matching.IntPattern;
import mb.nabl2.terms.matching.NilPattern;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.matching.PatternVar;
import mb.nabl2.terms.matching.StringPattern;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CTrue;
import mb.statix.solver.IConstraint;
import mb.statix.spec.IRule;
import mb.statix.spoofax.StatixTerms;

public class Names {
    /**
     * Attempts to determine statically which name is matched by the given rule. If the given rule
     * matches more than one name, or if we cannot statically determine which names are matched,
     * this method returns an empty optional.
     * <p>
     * The returned name might still contain variables, which will need to be substituted.
     * 
     * @param rule
     *      the rule
     * @param relation
     *      the relation
     * 
     * @return
     *      the single name matched by the given rule, or an empty optional
     */
    public static Optional<NameAndRelation> getMatchedName(IRule rule, ITerm relation) {
        //A simple rule like the one we are looking for has no name and matches a single term
        if (!rule.name().isEmpty()) return Optional.empty();
        if (rule.params().size() != 1) return Optional.empty();
        
        Pattern pattern = rule.params().get(0);
        if (!(pattern instanceof ApplPattern)) return Optional.empty();
        
        //Tuple([Occurrence(...), Wld())
        ApplPattern tuple = (ApplPattern) pattern;
        if (!tuple.getOp().equals(Terms.TUPLE_OP)) return Optional.empty();
        if (tuple.getArgs().size() != 2) return Optional.empty();
        if (!isWildcard(tuple.getArgs().get(1))) return Optional.empty();
        
        Pattern tupleA = tuple.getArgs().get(0);
        if (!(tupleA instanceof ApplPattern)) return Optional.empty();
        
        //Occurrence({namespace}, [{arguments}], {position})
        ApplPattern pat = (ApplPattern) tupleA;
        if (!pat.getOp().equals(StatixTerms.OCCURRENCE_OP)) return Optional.empty();
        
        List<Pattern> args = pat.getArgs();
        if (args.size() != 3) return Optional.empty();
        
        String namespace = matchString(args.get(0));
        if (namespace == null) return Optional.empty();
        
        List<Pattern> arguments = matchList(args.get(1));
        if (arguments == null) return Optional.empty();
        
        //The position must be a wildcard
        if (!isWildcard(args.get(2))) return Optional.empty();
        
        //We need to verify that no wildcards are matched in the name
        //TODO Refinement: also support wildcard lookups
        if (!arguments.stream().noneMatch(Names::isWildcard)) return Optional.empty();
        
        //From the body we want to determine the substituation that we should apply
        ISubstitution.Transient subst = new PersistentSubstitution.Transient(Map.Transient.of());
        if (!checkBody(rule.body(), subst)) return Optional.empty();
        
        NameAndRelation name = new NameAndRelation(namespace, convertPatterns(arguments, subst.freeze()), relation);
        return Optional.of(name);
    }
    
    /**
     * Attempts to retrieve the name of the given term. If the given term is an occurrence, it is
     * converted to its corresponding name. If the given term is a tuple and the first argument is
     * an occurrence, it is converted to its corresponding name. Otherwise, an empty optional is
     * returned.
     * 
     * @param term
     *      the term
     * @param unifier
     *      the unifier
     * 
     * @return
     *      the name represented by the given term or contained in the given tuple
     */
    public static Optional<Name> getName(ITerm term, IUnifier unifier) {
        return M.cases(
                occurrence().map(Optional::of),
                M.tuple(t -> {
                    if (t.getArity() <= 0) return Optional.<Name>empty();
                    return occurrence().match(t.getArgs().get(1), unifier);
                })
        ).match(term, unifier).get();
    }
    
    /**
     * Attempts to retrieve the name of the given term. If the given term is an occurrence, it is
     * converted to its corresponding name. If the given term is a tuple and the first argument is
     * an occurrence, it is converted to its corresponding name. Otherwise, null is returned.
     * 
     * @param term
     *      the term
     * @param unifier
     *      the unifier
     * 
     * @return
     *      the name represented by the given term or contained in the given tuple, or null
     */
    public static @Nullable Name getNameOrNull(ITerm term, IUnifier unifier) {
        return getName(term, unifier).orElse(null);
    }
    
    private static IMatcher<Name> occurrence() {
        return M.appl3(StatixTerms.OCCURRENCE_OP, M.stringValue(), M.listElems(M.term()), M.term(),
                (t, n, l, p) -> new Name(n, l));
    }
    
    /**
     * Checks the constraints in the body of the given constraint to see if they are valid for a
     * single item query. If any constraint in the body is not a {@link CExists}, {@link CTrue},
     * {@link CEqual} or {@link CConj}, this method returns false.
     * <p>
     * The given substitution is built from any {@link CEqual} constraints in the body.
     * 
     * @param constraint
     *      the constraint to check
     * @param subst
     *      the substutiton to append to
     * 
     * @return
     *      true if the given body is valid, false otherwise
     */
    public static boolean checkBody(IConstraint constraint, ISubstitution.Transient subst) {
        //For exists constraints we check that they introduce no variables
        if (constraint instanceof CExists) {
            CExists exists = (CExists) constraint;
            if (!exists.vars().isEmpty()) return false;
            
            return checkBody(exists.constraint(), subst);
        }
        
        //True constraints are trivially true
        if (constraint instanceof CTrue) return true;
        
        //For equality constraints, we add them to the substitution. 
        if (constraint instanceof CEqual) {
            CEqual equal = (CEqual) constraint;
            ITerm a = equal.term1();
            ITerm b = equal.term2();
            
            if (!(a instanceof ITermVar)) return false;
            
            subst.put((ITermVar) a, b);
            return true;
        }
        
        //For conjunctions we simply check both sides
        if (constraint instanceof CConj) {
            CConj conj = (CConj) constraint;
            return checkBody(conj.left(), subst) && checkBody(conj.right(), subst);
        }
        
        return false;
    }
    
    /**
     * Converts the given list of patterns to a list of terms with {@link #convertPattern}. 
     * 
     * @param patterns
     *      the patterns to convert
     * @param subst
     *      the substitution to apply
     * 
     * @return
     *      the list of terms
     */
    public static List<ITerm> convertPatterns(List<Pattern> patterns, ISubstitution.Immutable subst) {
        List<ITerm> tbr = new ArrayList<>(patterns.size());
        for (Pattern pattern : patterns) {
            tbr.add(convertPattern(pattern, subst));
        }
        return tbr;
    }
    
    /**
     * Converts a pattern to a corresponding ITerm, applying the given substitution to the term.
     * 
     * @param pattern
     *      the pattern
     * @param subst
     *      the substitution
     * 
     * @return
     *      the ITerm
     */
    public static ITerm convertPattern(Pattern pattern, ISubstitution.Immutable subst) {
        if (pattern instanceof StringPattern) {
            return B.newString(((StringPattern) pattern).getValue());
        }
        if (pattern instanceof IntPattern) {
            return B.newInt(((IntPattern) pattern).getValue());
        }
        if (pattern instanceof PatternVar) {
            return subst.apply(((PatternVar) pattern).getVar());
        }
        if (pattern instanceof ApplPattern) {
            ApplPattern p = (ApplPattern) pattern;
            String op = p.getOp();
            List<ITerm> args = convertPatterns(p.getArgs(), subst);
            return B.newAppl(op, args);
        }
        if (pattern instanceof ConsPattern) {
            return B.newList(convertPatterns(matchList(pattern), subst));
        }
        if (pattern instanceof NilPattern) {
            return B.EMPTY_LIST;
        }
        throw new IllegalArgumentException("Unexpected / unsupported pattern type: " + pattern.getClass().getName());
    }
    
    //---------------------------------------------------------------------------------------------
    //Matching patterns
    //---------------------------------------------------------------------------------------------
    
    /**
     * If the given pattern is a list (nil or cons), then this method returns the of patterns in
     * that list. Otherwise, this method returns null.
     * 
     * @param listPattern
     *      the list
     * 
     * @return
     *      the patterns in the list, or null if the given pattern is not a list
     */
    public static List<Pattern> matchList(Pattern listPattern) {
        if (listPattern instanceof NilPattern) return Collections.emptyList();
        if (!(listPattern instanceof ConsPattern)) return null;
        
        ConsPattern cons = (ConsPattern) listPattern;
        List<Pattern> list = new ArrayList<>();
        while (true) {
            list.add(cons.getHead());
            Pattern tail = cons.getTail();
            if (tail instanceof NilPattern) break;
            if (!(tail instanceof ConsPattern)) throw new IllegalArgumentException("Malformed list");
            cons = (ConsPattern) tail;
        }
        
        return list;
    }
    
    /**
     * If the given pattern is a StringPattern, then this method returns the string represented by
     * the pattern. Otherwise, this method returns null.
     * 
     * @param pat
     *      the pattern
     * 
     * @return
     *      the string, or null if the given pattern does not represent a string
     */
    public static String matchString(Pattern pat) {
        if (!(pat instanceof StringPattern)) return null;
        
        StringPattern p = (StringPattern) pat;
        return p.getValue();
    }
    
    /**
     * If the given pattern is a PatternVar, then this method returns the variable represented by
     * the pattern. Otherwise, this method returns null.
     * 
     * @param pat
     *      the pattern
     * 
     * @return
     *      the variable, or null if the given pattern does not represent a variable
     */
    public static ITermVar matchVar(Pattern pat) {
        if (!(pat instanceof PatternVar)) return null;
        
        PatternVar p = (PatternVar) pat;
        return p.getVar();
    }
    
    /**
     * If the given pattern is a PatternVar representing a wildcard, this method returns true.
     * Otherwise, this method returns false.
     * 
     * @param pat
     *      the pattern
     * 
     * @return
     *      true if the given pattern represents a variable and is a wildcard, false otherwise
     */
    public static boolean isWildcard(Pattern pat) {
        if (!(pat instanceof PatternVar)) return false;
        
        return ((PatternVar) pat).isWildcard();
    }
}
