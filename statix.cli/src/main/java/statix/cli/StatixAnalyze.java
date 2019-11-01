package statix.cli;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.FacetContribution;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.IMessagePrinter;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.analysis.AnalysisFacet;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzeResults;
import org.metaborg.spoofax.core.context.constraint.IConstraintContext;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ParseContrib;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.statix.solver.ISolverResult;

public class StatixAnalyze {
    private static final ILogger logger = LoggerUtils.logger(StatixAnalyze.class);
    
    private Spoofax S;
    private IConstraintContext context;
    private IMessagePrinter printer;
    
    public StatixAnalyze(Spoofax S, IConstraintContext context, IMessagePrinter printer) throws MetaborgException {
        this.S = S;
        this.context = context;
        this.printer = printer;
        
        //Verify that the analysisService is available
        if (!S.analysisService.available(context.language())) {
            throw new MetaborgException("No analysis service available for language " + context.language() + ".");
        }
    }
    
    public IConstraintContext getContext() {
        return context;
    }
    
    /**
     * Loads the context state from the given file.
     * 
     * @param file
     *      the file to load from
     * 
     * @throws MetaborgException 
     *      If loading the context fails.
     */
    public void loadContextFrom(File file) throws MetaborgException {
        StatixUtil.loadFrom(S, context, file);
    }
    
    /**
     * Saves the context to the given file.
     * 
     * @param file
     *      the file to save to
     * 
     * @throws MetaborgException 
     *      If writing the context fails.
     */
    public void saveContextTo(File file) throws MetaborgException {
        StatixUtil.saveTo(S, context, file);
    }
    
    /**
     * Unloads the context.
     */
    public void unloadContext() {
        try {
            context.clear();
        } catch (NullPointerException ex) {}
        context.unload();
    }
    
    /**
     * Clears all analysis results so far, i.e. performs a clean run.
     */
    public void clearAnalysisResults() throws MetaborgException {
        StatixUtil.initContext(context);
    }
    
    /**
     * Performs a clean analysis run.
     * 
     * @param files
     *      the files that were changed
     * @param print
     *      if the output should be printed
     * 
     * @return
     *      the results of the analysis
     * 
     * @throws MetaborgException
     *      If any of the analysis results are not valid. 
     */
    public ISpoofaxAnalyzeResults cleanAnalysis(Iterable<ISpoofaxParseUnit> files, boolean print) throws MetaborgException {
        clearAnalysisResults();
        return analyzeAll(files, print);
    }
    
    public ISolverResult getAnalysisResult(String resource) {
        IStrategoTerm term = context.get(resource);
        System.out.println("Term: " + term);
        
        //TODO Look into prettyprint service
        
        StrategoTerms terms = new StrategoTerms(S.termFactoryService.getGeneric());
        ITerm sterm = terms.fromStratego(term);
        
        System.out.println("STerm: " + sterm);
        return M.blobValue(ISolverResult.class).match(sterm).orElse(null);
        //TODO
    }
    
    /**
     * Desugars the given parse unit into a new parse unit.
     * If it failed or is empty, this method returns the input unchanged.
     */
    public ISpoofaxParseUnit desugarAst(ISpoofaxParseUnit unit) throws MetaborgException {
        //Return unit unchanged in case of failure or empty
        if (!unit.valid() || !unit.success() || isEmptyAST(unit.ast())) return unit;
        
        IConstraintContext context = getContext();

        final ILanguageImpl langImpl = context.language();

        final FacetContribution<AnalysisFacet> facetContribution = langImpl.facetContribution(AnalysisFacet.class);
        if(facetContribution == null) {
            System.err.println("No analysis required for " + langImpl);
            return unit;
        }

        final HybridInterpreter runtime;
        try {
            runtime = S.strategoRuntimeService.runtime(facetContribution.contributor, context, false);
        } catch(MetaborgException e) {
            throw new MetaborgException("Failed to get Stratego runtime", e);
        }
        
        final IStrategoTerm injected;
        try {
            injected = S.strategoCommon.invoke(runtime, unit.ast(), "java-explicate-injections");
        } catch (MetaborgException ex) {
            throw new MetaborgException("Unable to explicate injections for " + unit.source() + "!", ex);
        }
        
        if (injected == null) System.out.println("INJECTED IS NULL!");
        
        final IStrategoTerm desugared;
        try {
            desugared = S.strategoCommon.invoke(runtime, injected, "desugar-all");
        } catch (MetaborgException ex) {
            throw new MetaborgException("Unable to desugar " + unit.source() + "!", ex);
        }
        if (desugared == null) System.out.println("DESUGARED IS NULL!");
        
        return S.unitService.parseUnit(unit.input(), new ParseContrib(true, true, desugared, unit.messages(), unit.duration()));
    }
    
    /**
     * Applies the given stratego strategy to the parse unit, returns the transformed parse unit.
     * Does nothing if the input is invalid, unsucessful or empty.
     */
    public ISpoofaxParseUnit applyStrategoTransformation(ISpoofaxParseUnit unit, String strategy) throws MetaborgException {
        //Return unit unchanged in case of failure or empty
        if (!unit.valid() || !unit.success() || isEmptyAST(unit.ast())) return unit;
        
        IConstraintContext context = getContext();

        final ILanguageImpl langImpl = context.language();

        final FacetContribution<AnalysisFacet> facetContribution = langImpl.facetContribution(AnalysisFacet.class);
        if(facetContribution == null) {
            System.err.println("No analysis required for " + langImpl);
            return unit;
        }

        final HybridInterpreter runtime;
        try {
            runtime = S.strategoRuntimeService.runtime(facetContribution.contributor, context, false);
        } catch(MetaborgException e) {
            throw new MetaborgException("Failed to get Stratego runtime", e);
        }
        
        final IStrategoTerm result;
        try {
            result = S.strategoCommon.invoke(runtime, unit.ast(), strategy);
        } catch (MetaborgException ex) {
            throw new MetaborgException("Unable to execute " + strategy + " on " + unit.source() + "!", ex);
        }
        
        return S.unitService.parseUnit(unit.input(), new ParseContrib(true, true, result, unit.messages(), unit.duration()));
    }
    
    /**
     * Applies the given stratego strategy to the parse unit, returns the transformed parse unit.
     * Does nothing if the input is invalid, unsucessful or empty.
     */
    public ISpoofaxParseUnit applyStrategoTransformation(ISpoofaxParseUnit unit, String strategy, int nr1, int nr2) throws MetaborgException {
        //Return unit unchanged in case of failure or empty
        if (!unit.valid() || !unit.success() || isEmptyAST(unit.ast())) return unit;
        
        IConstraintContext context = getContext();

        final ILanguageImpl langImpl = context.language();

        final FacetContribution<AnalysisFacet> facetContribution = langImpl.facetContribution(AnalysisFacet.class);
        if(facetContribution == null) {
            System.err.println("No analysis required for " + langImpl);
            return unit;
        }

        final HybridInterpreter runtime;
        try {
            runtime = S.strategoRuntimeService.runtime(facetContribution.contributor, context, false);
        } catch(MetaborgException e) {
            throw new MetaborgException("Failed to get Stratego runtime", e);
        }
        
        ITermFactory factory = S.termFactoryService.getGeneric();
        
        final IStrategoTerm result;
        try {
            result = S.strategoCommon.invoke(runtime, factory.makeTuple(factory.makeInt(nr1), factory.makeInt(nr2), unit.ast()), strategy);
        } catch (MetaborgException ex) {
            throw new MetaborgException("Unable to execute " + strategy + " on " + unit.source() + "!", ex);
        }
        
        return S.unitService.parseUnit(unit.input(), new ParseContrib(true, true, result, unit.messages(), unit.duration()));
    }
    
    private boolean isEmptyAST(IStrategoTerm ast) {
        return Tools.isTermTuple(ast) && ast.getSubtermCount() == 0;
    }
    
    /**
     * @return
     *      a list of all the files in the context
     */
    public List<File> getFilesInContext() {
        return context.entrySet().stream()
                .map(Entry::getKey)
                .map(context::keyResource)
                .map(this::getFile)
                .collect(Collectors.toList());
    }
    
    /**
     * @return
     *      a list of all the files in the context
     */
    public List<String> getFileKeysInContext() {
        return context.entrySet().stream()
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }
    
    private File getFile(FileObject fo) {
        try {
            return new File(fo.getURL().toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Analyzes a single (changed) file.
     * 
     * @param parseUnit
     *      the parse unit of the file to analyze
     * 
     * @return
     *      the analysis result
     * 
     * @throws MetaborgException
     *      
     */
    public ISpoofaxAnalyzeUnit analyzeSingle(ISpoofaxParseUnit parseUnit) throws MetaborgException {
        final ISpoofaxAnalyzeUnit analysisUnit;
        try (IClosableLock lock = context.write()) {
            analysisUnit = S.analysisService.analyze(parseUnit, context).result();
        }
        
        if (printer != null) {
            for (IMessage message : analysisUnit.messages()) {
                printer.print(message, false);
            }
        }
        
        if (!analysisUnit.valid()) {
            throw new MetaborgException("Analysis of " + parseUnit.source() + " failed.");
        }
        
        if (!analysisUnit.success()) {
            logger.info("{} has type errors.", parseUnit.source());
        }
        
        return analysisUnit;
    }
    
    /**
     * Analyzes all the given files in a multi-file analysis. The given files are the files that
     * were changed or removed. Other files in the context might also be reanalyzed.
     * 
     * @param files
     *      the files that were changed
     * @param print
     *      if the output should be printed
     * 
     * @return
     *      the results of the analysis
     * 
     * @throws MetaborgException
     *      If any of the analysis results are not valid. 
     */
    public ISpoofaxAnalyzeResults analyzeAll(Iterable<ISpoofaxParseUnit> files, boolean print) throws MetaborgException {
        ISpoofaxAnalyzeResults results;
        
        try (IClosableLock lock = context.write()) {
            results = S.analysisService.analyzeAll(files, context);
        }
        
        if (print && printer != null) {
            for (ISpoofaxAnalyzeUnit analysisUnit : results.results()) {
                for (IMessage message : analysisUnit.messages()) {
                    printer.print(message, false);
                }
            }
        }
        
        for (ISpoofaxAnalyzeUnit analysisUnit : results.results()) {
            if (!analysisUnit.valid()) {
                throw new MetaborgException("Analysis of " + analysisUnit.source() + " failed.");
            }
        }
        
        for (ISpoofaxAnalyzeUnit analysisUnit : results.results()) {
            if (!analysisUnit.success()) {
                logger.info("{} has type errors.", analysisUnit.source());
            }
        }
        
        return results;
    }
    
    public boolean hasErrors(ISpoofaxAnalyzeResults results) {
        for (ISpoofaxAnalyzeUnit unit : results.results()) {
            if (!unit.success()) return false;
        }
        
        return true;
    }
}
