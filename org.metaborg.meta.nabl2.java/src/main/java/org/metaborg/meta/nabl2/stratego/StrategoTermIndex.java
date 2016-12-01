package org.metaborg.meta.nabl2.stratego;

import java.io.Serializable;

import javax.annotation.Nullable;

import org.metaborg.meta.nabl2.terms.annotations.ITermIndex;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.ISimpleTerm;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.attachments.AbstractTermAttachment;
import org.spoofax.terms.attachments.TermAttachmentType;

public class StrategoTermIndex extends AbstractTermAttachment implements ITermIndex, Serializable {

    private static final long serialVersionUID = 5958528158971840392L;

    public static final TermAttachmentType<StrategoTermIndex> TYPE = new TermAttachmentType<StrategoTermIndex>(
            StrategoTermIndex.class, "TermIndex", 2) {

        @Override protected IStrategoTerm[] toSubterms(ITermFactory factory, StrategoTermIndex attachment) {
            return new IStrategoTerm[] { factory.makeString(attachment.resource), factory.makeInt(attachment.id), };
        }

        @Override protected StrategoTermIndex fromSubterms(IStrategoTerm[] subterms) {
            return new StrategoTermIndex(Tools.asJavaString(subterms[0]), Tools.asJavaInt(subterms[1]));
        }
    };

    private final String resource;
    private final int id;

    private StrategoTermIndex(String resource, int id) {
        this.resource = resource;
        this.id = id;
    }


    @Override public TermAttachmentType<StrategoTermIndex> getAttachmentType() {
        return TYPE;
    }

    public IStrategoTerm toTerm(ITermFactory factory) {
        return TYPE.toTerm(factory, this);
    }


    @Override public String getResource() {
        return resource;
    }

    @Override public int getId() {
        return id;
    }


    public static void put(IStrategoTerm term, String resource, int nodeId) {
        term.putAttachment(new StrategoTermIndex(resource, nodeId));
    }

    public static boolean put(IStrategoTerm term, IStrategoAppl indexTerm) {
        StrategoTermIndex index = StrategoTermIndex.TYPE.fromTerm(indexTerm);
        if (index == null) {
            return false;
        }
        term.putAttachment(index);
        return true;
    }

    public static @Nullable StrategoTermIndex get(ISimpleTerm term) {
        return term.getAttachment(TYPE);
    }


    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resource == null) ? 0 : resource.hashCode());
        result = prime * result + id;
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StrategoTermIndex other = (StrategoTermIndex) obj;
        if (id != other.id)
            return false;
        if (resource == null) {
            if (other.resource != null)
                return false;
        } else if (!resource.equals(other.resource))
            return false;
        return true;
    }

    @Override public String toString() {
        return "@" + resource + ":" + id;
    }

}