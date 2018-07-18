package mb.nabl2.controlflow.terms;

import java.util.List;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.Terms;

public interface ICFGNode extends ITerm {

    TermIndex getIndex();

    String getName();

    Kind getKind();

    enum Kind implements IApplTerm {
        Normal,
        Start,
        End,
        Entry,
        Exit;

        private static final ImmutableMultiset<ITermVar> NO_VARS = ImmutableMultiset.<ITermVar>builder().build();

        @Override
        public boolean isGround() {
            return true;
        }

        @Override
        public Multiset<ITermVar> getVars() {
            return NO_VARS;
        }

        @Override
        public ImmutableClassToInstanceMap<Object> getAttachments() {
            return Terms.NO_ATTACHMENTS;
        }

        @Override
        public <T> T match(Cases<T> cases) {
            return cases.caseAppl(this);
        }

        @Override
        public <T, E extends Throwable> T matchOrThrow(CheckedCases<T, E> cases) throws E {
            return cases.caseAppl(this);
        }

        @Override
        public String getOp() {
            return this.name();
        }

        @Override
        public int getArity() {
            return 0;
        }

        @Override
        public List<ITerm> getArgs() {
            return ImmutableList.of();
        }

        @Override
        public IApplTerm withAttachments(ImmutableClassToInstanceMap<Object> value) {
            return this;
        }

    }
}