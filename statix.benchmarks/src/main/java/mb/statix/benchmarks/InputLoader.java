package mb.statix.benchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.util.TermUtils;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTermIndices;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;

public class InputLoader {

    public static Spec loadSpecFromATerm(String resource) throws IOException {
        ITermFactory termFactory = new TermFactory();
        TAFTermReader reader = new TAFTermReader(termFactory);
        final InputStream is = InputLoader.class.getResourceAsStream(resource);
        IStrategoTerm sterm = reader.parseFromStream(is);
        StrategoTerms converter = new StrategoTerms(termFactory);
        ITerm term = converter.fromStratego(sterm);
        Spec spec = StatixTerms.spec().match(term).orElseThrow(() -> new IllegalArgumentException("Expected spec."));
        return spec;
    }

    public static java.util.Map<String, ITerm> loadFilesFromATerm(String resource) throws IOException {
        ITermFactory termFactory = new TermFactory();
        TAFTermReader reader = new TAFTermReader(termFactory);
        final InputStream is = InputLoader.class.getResourceAsStream(resource);
        IStrategoTerm filesSTerm = reader.parseFromStream(is);
        StrategoTerms converter = new StrategoTerms(termFactory);
        IStrategoList filesSTerms = TermUtils.toList(filesSTerm);
        java.util.Map<String, ITerm> files = new HashMap<>();
        for(IStrategoTerm fileSTerm : filesSTerms) {
            fileSTerm = TermUtils.toTuple(fileSTerm);
            String fileName = TermUtils.toJavaStringAt(fileSTerm, 0);
            IStrategoTerm astSTerm = fileSTerm.getSubterm(1);
            astSTerm = StrategoTermIndices.index(astSTerm, fileName, termFactory);
            ITerm ast = converter.fromStratego(astSTerm);
            files.put(fileName, ast);
        }
        return files;
    }

}