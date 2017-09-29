package org.metaborg.meta.nabl2.regexp;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

public class RegExpMatcher<S> implements IRegExpMatcher<S>, Serializable {

    private static final long serialVersionUID = 42L;

    private final IRegExp<S> state;
    private final Object2ObjectMap<IRegExp<S>,Object2ObjectMap<S,IRegExp<S>>> stateTransitions;
    private final ObjectSet<IRegExp<S>> nonFinal;
    private final IAlphabet<S> alphabet;

    private RegExpMatcher(IRegExp<S> state,
            Object2ObjectMap<IRegExp<S>,Object2ObjectMap<S,IRegExp<S>>> stateTransitions,
            ObjectSet<IRegExp<S>> nonFinal) {
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

    @Override public boolean isEmpty() {
        return isFinal() && !isAccepting();
    }

    public static <S> IRegExpMatcher<S> create(final IRegExp<S> initial) {
        final List<Deriver<S>> derivers = Lists.newArrayList();
        for (S symbol : initial.getBuilder().getAlphabet()) {
            derivers.add(new Deriver<>(symbol, initial.getBuilder()));
        }

        final Object2ObjectMap<IRegExp<S>,Object2ObjectMap<S,IRegExp<S>>> stateTransitions = new Object2ObjectOpenHashMap<>();
        final Object2ObjectMap<IRegExp<S>,ObjectSet<IRegExp<S>>> reverseTransitions = new Object2ObjectOpenHashMap<>();
        final Stack<IRegExp<S>> worklist = new ObjectArrayList<>();
        worklist.push(initial);
        while (!worklist.isEmpty()) {
            final IRegExp<S> state = worklist.pop();
            final Object2ObjectMap<S,IRegExp<S>> transitions = new Object2ObjectOpenHashMap<>(derivers.size());
            if (!stateTransitions.containsKey(state)) {
                for (Deriver<S> deriver : derivers) {
                    final IRegExp<S> nextState = state.match(deriver);
                    ObjectSet<IRegExp<S>> reverseStates;
                    if ((reverseStates = reverseTransitions.get(nextState)) == null) {
                        reverseTransitions.put(nextState, (reverseStates = new ObjectOpenHashSet<>()));
                    }
                    reverseStates.add(state);
                    transitions.put(deriver.getSymbol(), nextState);
                    worklist.push(nextState);
                }
                stateTransitions.put(state, transitions);
            }
        }

        final ObjectSet<IRegExp<S>> nonFinal = new ObjectOpenHashSet<>();
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

}