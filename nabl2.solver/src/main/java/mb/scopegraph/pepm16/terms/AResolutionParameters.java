package mb.scopegraph.pepm16.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.tuple.Tuple2;

import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.scopegraph.pepm16.IResolutionParameters;
import mb.scopegraph.regexp.IAlphabet;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpBuilder;
import mb.scopegraph.regexp.impl.FiniteAlphabet;
import mb.scopegraph.regexp.impl.RegExpBuilder;
import mb.scopegraph.relations.IRelation;
import mb.scopegraph.relations.RelationDescription;
import mb.scopegraph.relations.RelationException;
import mb.scopegraph.relations.impl.Relation;

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
        if (!getLabels().contains(getLabelD())) {
          throw new IllegalArgumentException("Labels do not contain LabelD");
        }
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

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + getLabels().hashCode();
        h += (h << 5) + getLabelD().hashCode();
        h += (h << 5) + getLabelR().hashCode();
        h += (h << 5) + getPathWf().hashCode();
        h += (h << 5) + getSpecificityOrder().hashCode();
        h += (h << 5) + getStrategy().hashCode();
        h += (h << 5) + Boolean.hashCode(getPathRelevance());
        return h;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder("ResolutionParameters{");
        b.append("labels=").append(getLabels());
        b.append(", ");
        b.append("labelD=").append(getLabelD());
        b.append(", ");
        b.append("labelR=").append(getLabelR());
        b.append(", ");
        b.append("pathWf=").append(getPathWf());
        b.append(", ");
        b.append("specificityOrder=").append(getSpecificityOrder());
        b.append(", ");
        b.append("strategy=").append(getStrategy());
        b.append(", ");
        b.append("pathRelevance=").append(getPathRelevance());
        return b.append('}').toString();
    }

}