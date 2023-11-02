package mb.statix.spoofax;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.interpreter.terms.TermType;
import org.spoofax.terms.util.TermUtils;

import mb.scopegraph.regexp.IAlphabet;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpBuilder;
import mb.scopegraph.regexp.IRegExpMatcher;
import mb.scopegraph.regexp.IState;
import mb.scopegraph.regexp.RegExpMatcher;
import mb.scopegraph.regexp.impl.FiniteAlphabet;
import mb.scopegraph.regexp.impl.RegExpNormalizingBuilder;

public class STX_labelre_to_states extends AbstractPrimitive {

    @javax.inject.Inject @jakarta.inject.Inject public STX_labelre_to_states(String name) {
        super(STX_labelre_to_states.class.getSimpleName(), 0, 2);
    }

    // State machine constructor names
    private static final String STATEMACHINE_OP = "StateMachine";
    private static final String STATE_OP = "State";

    private static final String INITIAL_STATE_NAME = "state_initial";

    // LabelRE constructor names
    private static final String EMPTY_OP = "Empty";
    private static final String EPSILON_OP = "Epsilon";
    private static final String CLOSURE_OP = "Closure";
    private static final String NEG_OP = "Neg";
    private static final String CONCAT_OP = "Concat";
    private static final String AND_OP = "And";
    private static final String OR_OP = "Or";

    private static final String EOP_LBL_OP = "EOP";

    @Override public boolean call(IContext context, Strategy[] svars, IStrategoTerm[] tvars) {
        final IStrategoTerm regexpTerm = context.current();
        final IStrategoTerm relationTerm = tvars[0];
        final IStrategoTerm alphabetTerm = tvars[1];

        // Prepare state machine
        final IAlphabet<IStrategoTerm> alphabet = parseAlphabet(alphabetTerm);
        final IRegExp<IStrategoTerm> regexp = parseRegexp(regexpTerm, alphabet, relationTerm);
        final IRegExpMatcher<IStrategoTerm> matcher = RegExpMatcher.create(regexp);
        final IState<IStrategoTerm> initial = matcher.state();

        // convert state machine to term representation
        final Map<IState<IStrategoTerm>, String> stateNames = new HashMap<>();
        final List<IStrategoTerm> stateList = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger();

        final Queue<IState<IStrategoTerm>> worklist = new ArrayDeque<>();
        worklist.add(matcher.state());
        stateNames.put(initial, INITIAL_STATE_NAME);

        final ITermFactory TF = context.getFactory();
        final IStrategoTerm ACCEPT = TF.makeAppl("Accept");
        final IStrategoTerm REJECT = TF.makeAppl("Reject");

        while(!worklist.isEmpty()) {
            final IState<IStrategoTerm> state = worklist.remove();
            final String name = stateNames.get(state);
            final List<IStrategoTerm> transitions = new ArrayList<>();
            for(IStrategoTerm lbl : alphabet) {
                final IState<IStrategoTerm> tgt = state.transition(lbl);
                if(!tgt.isOblivion()) { // filter oblivion state
                    final String tgtName = stateNames.computeIfAbsent(tgt, st -> {
                        final String n = "state_" + counter.incrementAndGet();
                        worklist.add(st);
                        return n;
                    });
                    transitions.add(TF.makeTuple(lbl, TF.makeString(tgtName)));
                }
            }
            final IStrategoTerm acceptance = state.isAccepting() ? ACCEPT : REJECT;
            stateList.add(TF.makeAppl(STATE_OP, TF.makeString(name), acceptance, TF.makeList(transitions)));
        }

        IStrategoTerm result = TF.makeAppl(STATEMACHINE_OP, TF.makeList(stateList), TF.makeString(INITIAL_STATE_NAME));
        context.setCurrent(result);
        return true;
    }

    private static IRegExp<IStrategoTerm> parseRegexp(IStrategoTerm regexpTerm, IAlphabet<IStrategoTerm> alphabet,
            IStrategoTerm relationTerm) {
        final IRegExpBuilder<IStrategoTerm> rb = new RegExpNormalizingBuilder<>(alphabet);
        final IRegExp<IStrategoTerm> re = parseRegexp(regexpTerm, alphabet, rb);
        if(TermUtils.isAppl(relationTerm, EOP_LBL_OP, 0)) {
            return re;
        } else {
            return rb.concat(re, rb.symbol(relationTerm));
        }
    }

    private static IRegExp<IStrategoTerm> parseRegexp(IStrategoTerm regexpTerm, IAlphabet<IStrategoTerm> alphabet,
            IRegExpBuilder<IStrategoTerm> rb) {
        if(TermUtils.isAppl(regexpTerm, EMPTY_OP, 0)) {
            return rb.emptySet();
        }
        if(TermUtils.isAppl(regexpTerm, EPSILON_OP, 0)) {
            return rb.emptyString();
        }
        if(TermUtils.isAppl(regexpTerm, CLOSURE_OP, 1)) {
            final IRegExp<IStrategoTerm> subExp = parseRegexp(regexpTerm.getSubterm(0), alphabet, rb);
            return rb.closure(subExp);
        }
        if(TermUtils.isAppl(regexpTerm, NEG_OP, 1)) {
            final IRegExp<IStrategoTerm> subExp = parseRegexp(regexpTerm.getSubterm(0), alphabet, rb);
            return rb.complement(subExp);
        }
        if(TermUtils.isAppl(regexpTerm, CONCAT_OP, 2)) {
            final IRegExp<IStrategoTerm> left = parseRegexp(regexpTerm.getSubterm(0), alphabet, rb);
            final IRegExp<IStrategoTerm> right = parseRegexp(regexpTerm.getSubterm(1), alphabet, rb);

            return rb.concat(left, right);
        }
        if(TermUtils.isAppl(regexpTerm, AND_OP, 2)) {
            final IRegExp<IStrategoTerm> left = parseRegexp(regexpTerm.getSubterm(0), alphabet, rb);
            final IRegExp<IStrategoTerm> right = parseRegexp(regexpTerm.getSubterm(1), alphabet, rb);

            return rb.and(left, right);
        }
        if(TermUtils.isAppl(regexpTerm, OR_OP, 2)) {
            final IRegExp<IStrategoTerm> left = parseRegexp(regexpTerm.getSubterm(0), alphabet, rb);
            final IRegExp<IStrategoTerm> right = parseRegexp(regexpTerm.getSubterm(1), alphabet, rb);

            return rb.or(left, right);
        }

        return rb.symbol(regexpTerm);
    }


    private IAlphabet<IStrategoTerm> parseAlphabet(IStrategoTerm alphabetTerm) {
        if(alphabetTerm.getType() != TermType.LIST) {
            throw new IllegalArgumentException("Expected list of labels as second term argument");
        }
        return new FiniteAlphabet<>(alphabetTerm.getAllSubterms());
    }

}
