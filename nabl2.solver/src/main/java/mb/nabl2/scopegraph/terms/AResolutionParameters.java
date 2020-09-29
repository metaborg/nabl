package mb.nabl2.scopegraph.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.IOException;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.base.Preconditions;

import mb.nabl2.regexp.IAlphabet;
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpBuilder;
import mb.nabl2.regexp.impl.FiniteAlphabet;
import mb.nabl2.regexp.impl.RegExpBuilder;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.RelationException;
import mb.nabl2.relations.impl.Relation;
import mb.nabl2.scopegraph.IResolutionParameters;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.Tuple2;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class AResolutionParameters implements IResolutionParameters<Label> {

    @Value.Parameter @Override public abstract IAlphabet<Label> getLabels();

    @Value.Parameter @Override public abstract Label getLabelD();

    @Value.Parameter @Override public abstract Label getLabelR();

    @Value.Parameter @Override public abstract IRegExp<Label> getPathWf();

    @Value.Parameter @Override public abstract IRelation.Immutable<Label> getSpecificityOrder();

    @Value.Parameter @Override public abstract Strategy getStrategy();

    @Value.Parameter @Override public abstract boolean getPathRelevance();

    @Value.Check protected void check() {
        Preconditions.checkArgument(getLabels().contains(getLabelD()));
    }

    public static IMatcher<ResolutionParameters> matcher() {
        return (term, unifier) -> IMatcher.flatten(M.tuple5(matchLabels(), M.term(), matchOrder(), matchStrategy(),
                matchRelevance(), (t, labels, wfTerm, order, strategy, relevance) -> {
                    RegExpBuilder<Label> builder = new RegExpBuilder<>();
                    return matchWf(builder).match(wfTerm, unifier).<ResolutionParameters>map(
                            wf -> ResolutionParameters.of(labels, Label.D, Label.R, wf, order, strategy, relevance));
                })).match(term, unifier);
    }

    private static IMatcher<IAlphabet<Label>> matchLabels() {
        return M.listElems(Label.matcher(), (l, ls) -> {
            return new FiniteAlphabet<>(ls);
        });
    }

    private static IMatcher<IRelation.Immutable<Label>> matchOrder() {
        IMatcher<Label> m_label = Label.matcher();
        return M.listElems(M.appl2("", m_label, m_label, (t, l1, l2) -> Tuple2.of(l1, l2)), (t, ps) -> {
            final IRelation.Transient<Label> order = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
            for(Tuple2<Label, Label> p : ps) {
                try {
                    order.add(p._1(), p._2());
                } catch(RelationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return order.freeze();
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

    private static IMatcher<Strategy> matchStrategy() {
        // @formatter:off
        return M.cases(
            M.appl0("Search", (t) -> Strategy.SEARCH),
            M.appl0("Environments", (t) -> Strategy.ENVIRONMENTS)
        );
        // @formatter:on
    }

    private static IMatcher<Boolean> matchRelevance() {
        return M.casesFix(m -> Iterables2.from(
        // @formatter:off
                M.appl0("Relevant", (t) -> true),
                M.appl0("Irrelevant", (t) -> false)
        // @formatter:on
        ));
    }


    public static ResolutionParameters getDefault() {
        IAlphabet<Label> labels = new FiniteAlphabet<>(Label.D, Label.P, Label.I);
        RegExpBuilder<Label> R = new RegExpBuilder<>();
        IRegExp<Label> wf = R.concat(R.closure(R.symbol(Label.P)), R.closure(R.symbol(Label.I)));
        final IRelation.Transient<Label> order;
        try {
            order = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
            order.add(Label.D, Label.I);
            order.add(Label.I, Label.P);
        } catch(RelationException e) {
            throw new IllegalStateException(e);
        }
        return ResolutionParameters.of(labels, Label.D, Label.R, wf, order.freeze(), Strategy.SEARCH, true);
    }


    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

}