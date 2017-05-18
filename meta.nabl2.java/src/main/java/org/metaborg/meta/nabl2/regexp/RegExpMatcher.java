package org.metaborg.meta.nabl2.regexp;

import java.io.Serializable;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

public class RegExpMatcher<S> implements IRegExpMatcher<S>, Serializable {

    private static final long serialVersionUID = 42L;

    private final IRegExp<S> state;
    private final Map<IRegExp<S>, Map<S, IRegExp<S>>> stateTransitions;
    private final Set<IRegExp<S>> nonFinal;
    private final IAlphabet<S> alphabet;

    private RegExpMatcher(IRegExp<S> state, Map<IRegExp<S>, Map<S, IRegExp<S>>> stateTransitions,
            Set<IRegExp<S>> nonFinal) {
        this.state = state;
        this.stateTransitions = stateTransitions;
        this.nonFinal = nonFinal;
        this.alphabet = state.getBuilder().getAlphabet();
    }

    @Override public RegExpMatcher<S> match(S symbol) {
        assert alphabet.contains(symbol);
        return new RegExpMatcher<>(stateTransitions.get(state).get(symbol), stateTransitions, nonFinal);
    }

    @Override public IRegExpMatcher<S> match(Iterable<S> symbols) {
        RegExpMatcher<S> matcher = this;
        for (S symbol : symbols) {
            matcher = matcher.match(symbol);
        }
        return matcher;
    }

    @Override public boolean isAccepting() {
        return state.isNullable();
    }

    @Override public boolean isFinal() {
        return !nonFinal.contains(state);
    }

    public static <S> IRegExpMatcher<S> create(final IRegExp<S> initial) {
        final List<Deriver<S>> derivers = Lists.newArrayList();
        for (S symbol : initial.getBuilder().getAlphabet()) {
            derivers.add(new Deriver<>(symbol, initial.getBuilder()));
        }

        final Map<IRegExp<S>, Map<S, IRegExp<S>>> stateTransitions = Maps.newHashMap();
        final Map<IRegExp<S>, Set<IRegExp<S>>> reverseTransitions = Maps.newHashMap();
        final Deque<IRegExp<S>> worklist = Queues.newArrayDeque();
        worklist.push(initial);
        while (!worklist.isEmpty()) {
            final IRegExp<S> state = worklist.pop();
            final Map<S, IRegExp<S>> transitions = Maps.newHashMapWithExpectedSize(derivers.size());
            if (!stateTransitions.containsKey(state)) {
                for (Deriver<S> deriver : derivers) {
                    final IRegExp<S> nextState = state.match(deriver);
                    Set<IRegExp<S>> reverseStates;
                    if ((reverseStates = reverseTransitions.get(nextState)) == null) {
                        reverseTransitions.put(nextState, (reverseStates = Sets.newHashSet()));
                    }
                    reverseStates.add(state);
                    transitions.put(deriver.getSymbol(), nextState);
                    worklist.push(nextState);
                }
                stateTransitions.put(state, transitions);
            }
        }

        final Set<IRegExp<S>> nonFinal = Sets.newHashSet();
        for (IRegExp<S> state : stateTransitions.keySet()) {
            if (state.isNullable()) {
                worklist.push(state);
            }
        }
        while (!worklist.isEmpty()) {
            final IRegExp<S> state = worklist.pop();
            if (!nonFinal.contains(state)) {
                if (reverseTransitions.containsKey(state)) {
                    for (IRegExp<S> nextState : reverseTransitions.get(state)) {
                        nonFinal.add(nextState);
                        worklist.push(nextState);
                    }
                }
            }
        }

        return new RegExpMatcher<>(initial, stateTransitions, nonFinal);
    }

    @Override
    public String toString() {
        return state.toString();
    }
    
}