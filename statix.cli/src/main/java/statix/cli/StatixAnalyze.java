package statix.cli;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.IMessagePrinter;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalyzeResults;
import org.metaborg.spoofax.core.context.constraint.IConstraintContext;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import mb.statix.solver.ISolverResult;

public class StatixAnalyze {
    private static final ILogger logger = LoggerUtils.logger(StatixAnalyze.class);
    
    private Spoofax S;
    private IConstraintContext context;
    private IMessagePrinter printer;
    
    public StatixAnalyze(Spoofax S, IContext context, IMessagePrinter printer) throws MetaborgException {
        this.S = S;
        this.context = (IConstraintContext) context;
        this.printer = printer;
        
        //Verify that the analysisService is available
        if (!S.analysisService.available(context.language())) {
            throw new MetaborgException("No analysis service available for language " + context.language() + ".");
        }
    }
    
    /**
     * Clears all analysis results so far, i.e. performs a clean run.
     */
    public void clearAnalysisResults() {
        context.clear();
    }
    
    /**
     * Performs a clean analysis run.
     * 
     * @param files
     *      the files that were changed
     * 
     * @return
     *      the results of the analysis
     * 
     * @throws MetaborgException
     *      If any of the analysis results are not valid. 
     */
    public ISpoofaxAnalyzeResults cleanAnalysis(Iterable<ISpoofaxParseUnit> files) throws MetaborgException {
        clearAnalysisResults();
        return analyzeAll(files);
    }
    
    public ISolverResult getAnalysisResult(String resource) {
        IStrategoTerm term = context.get(resource);
        System.out.println(term);
        
        //TODO
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
     * 
     * @return
     *      the results of the analysis
     * 
     * @throws MetaborgException
     *      If any of the analysis results are not valid. 
     */
    public ISpoofaxAnalyzeResults analyzeAll(Iterable<ISpoofaxParseUnit> files) throws MetaborgException {
        ISpoofaxAnalyzeResults results;
        
        try (IClosableLock lock = context.write()) {
            results = S.analysisService.analyzeAll(files, context);
        }
        
        if (printer != null) {
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
