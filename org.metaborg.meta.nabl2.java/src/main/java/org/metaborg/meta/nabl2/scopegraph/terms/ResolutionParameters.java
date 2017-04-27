package org.metaborg.meta.nabl2.scopegraph.terms;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.regexp.IAlphabet;
import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.regexp.IRegExpBuilder;
import org.metaborg.meta.nabl2.regexp.impl.FiniteAlphabet;
import org.metaborg.meta.nabl2.regexp.impl.RegExpBuilder;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.RelationException;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.base.Preconditions;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class ResolutionParameters implements IResolutionParameters<Label> {

    @Value.Parameter @Override public abstract IAlphabet<Label> getLabels();

    @Value.Parameter @Override public abstract Label getLabelD();

    @Value.Parameter @Override public abstract IRegExp<Label> getPathWf();

    @Value.Parameter @Override public abstract IRelation<Label> getSpecificityOrder();

    @Value.Check protected void check() {
        Preconditions.checkArgument(getLabels().contains(getLabelD()));
    }

    public static IMatcher<ResolutionParameters> matcher() {
        return term -> M.appl3("", matchLabels(), M.term(), matchOrder(), (t, labels, wfTerm, order) -> {
            RegExpBuilder<Label> builder = new RegExpBuilder<>(labels);
            return matchWf(builder).match(wfTerm)
                .<ResolutionParameters>map(wf -> ImmutableResolutionParameters.of(labels, Label.D, wf, order));
        }).match(term).flatMap(o -> o);
    }

    private static IMatcher<IAlphabet<Label>> matchLabels() {
        return M.listElems(Label.matcher(), (l, ls) -> {
            return new FiniteAlphabet<>(ls);
        });
    }

    private static IMatcher<IRelation<Label>> matchOrder() {
        IMatcher<Label> m_label = Label.matcher();
        return M.listElems(M.appl2("", m_label, m_label, (t, l1, l2) -> ImmutableTuple2.of(l1, l2)), (t, ps) -> {
            Relation<Label> order = new Relation<>(RelationDescription.STRICT_PARTIAL_ORDER);
            for(Tuple2<Label, Label> p : ps) {
                try {
                    order.add(p._1(), p._2());
                } catch(RelationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return order;
        });
    }

    private static IMatcher<IRegExp<Label>> matchWf(IRegExpBuilder<Label> builder) {
        return M.casesFix(m -> Iterables2.from(
            // @formatter:off
                M.appl0("Empty", (t) -> builder.emptySet()), M.appl0("Epsilon", (t) -> builder.emptyString()),
                M.appl1("Closure", m, (t, re) -> builder.closure(re)),
                M.appl2("Concat", m, m, (t, re1, re2) -> builder.concat(re1, re2)),
                M.appl2("And", m, m, (t, re1, re2) -> builder.and(re1, re2)),
                M.appl2("Or", m, m, (t, re1, re2) -> builder.or(re1, re2)), Label.matcher(l -> builder.symbol(l))
        // @formatter:on
        ));
    }

    public static ResolutionParameters getDefault() {
        IAlphabet<Label> labels = new FiniteAlphabet<>(Label.D, Label.P, Label.I);
        RegExpBuilder<Label> R = new RegExpBuilder<>(labels);
        IRegExp<Label> wf = R.concat(R.closure(R.symbol(Label.P)), R.closure(R.symbol(Label.I)));
        Relation<Label> order;
        try {
            order = new Relation<>(RelationDescription.STRICT_PARTIAL_ORDER);
            order.add(Label.D, Label.I);
            order.add(Label.I, Label.P);
        } catch(RelationException e) {
            throw new IllegalStateException(e);
        }
        return ImmutableResolutionParameters.of(labels, Label.D, wf, order);
    }

}