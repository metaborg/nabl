package mb.nabl2.terms.stratego;

import java.util.ArrayList;
import java.util.List;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.io.TermFactoryVisitor;
import org.spoofax.terms.io.TermVisitor;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.build.Attachments;
import mb.nabl2.terms.build.ITermBuild;

public abstract class TermBuildVisitor implements TermVisitor {

    private final ITermBuild build;
    private final ITermFactory factory;

    public TermBuildVisitor(ITermBuild build, ITermFactory factory) {
        this.build = build;
        this.factory = factory;
    }

    public abstract void setTerm(ITerm term);


    private Integer i;

    @Override public void visitInt(int value) {
        visit();
        this.i = value;
    }

    @Override public void endInt() {
        setTerm(withAnnos(build.newInt(i)));
    }


    @SuppressWarnings("unused") private Double d;

    @Override public void visitReal(double value) {
        visit();
        this.d = value;
    }

    @Override public void endReal() {
        throw new IllegalArgumentException("Reals are not supported.");
    }


    private String s;

    @Override public void visitString(String value) {
        visit();
        this.s = value;
    }

    @Override public void endString() {
        setTerm(withAnnos(build.newString(s)));
    }


    private String c;

    @Override public void visitAppl(String name) {
        visit();
        this.c = name;
    }

    @Override public void endAppl() {
        setTerm(withAnnos(build.newAppl(c, subTerms)));
    }


    @Override public void visitTuple() {
        visit();
    }

    @Override public void endTuple() {
        setTerm(withAnnos(build.newTuple(subTerms)));
    }


    @Override public void visitList() {
        visit();
    }

    @Override public void endList() {
        setTerm(withAnnos(build.newList(subTerms)));
    }


    @Override public TermVisitor visitPlaceholder() {
        throw new IllegalArgumentException("Placeholders are not supported.");
    }


    List<ITerm> subTerms = new ArrayList<>();

    @Override public TermVisitor visitSubTerm() {
        TermBuildVisitor outer = this;
        return new TermBuildVisitor(build, factory) {
            @Override public void setTerm(ITerm subTerm) {
                outer.subTerms.add(subTerm);
            }
        };
    }


    List<IStrategoTerm> annos = new ArrayList<>();

    @Override public TermVisitor visitAnnotation() {
        TermBuildVisitor outer = this;
        return new TermFactoryVisitor(factory) {
            @Override public void setTerm(IStrategoTerm anno) {
                outer.annos.add(anno);
            }
        };
    }

    private ITerm withAnnos(ITerm term) {
        if(annos.isEmpty()) {
            return term;
        } else {
            return term.withAttachments(Attachments.of(StrategoAnnotations.class, StrategoAnnotations.of(annos)));
        }
    }

    private void visit() {
        i = null;
        d = null;
        s = null;
        c = null;
        subTerms.clear();
        annos.clear();
    }

}
