package org.metaborg.meta.nabl2.controlflow.terms;

import java.util.List;

import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

public interface ICFGNode extends ITerm {

    String getResource();

    String getName();

    Kind getKind();

    enum Kind implements IApplTerm {
        Normal,
        Start,
        End,
        Artificial;

        private static final ImmutableMultiset<ITermVar> NO_VARS = ImmutableMultiset.<ITermVar>builder().build();

        @Override
        public boolean isGround() {
            return true;
        }

        @Override
        public boolean isLocked() {
            return false;
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

        @Override
        public IApplTerm withLocked(boolean locked) {
            return this;
        }
    }
}