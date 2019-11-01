package statix.cli;

import java.io.File;
import java.io.OutputStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.NameScope;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.IMessagePrinter;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.context.constraint.IConstraintContext;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import statix.cli.messages.CompactMessagePrinter;

public class StatixData {
    public static final ILogger logger = LoggerUtils.logger(StatixData.class);
    
    public Spoofax S;
    public CLIUtils cli;
    public IProject project;
    public IMessagePrinter messagePrinter;
    private int nameCounter = 0;
    
    public StatixData(Spoofax S, String folder, OutputStream messageOutput) throws MetaborgException {
        //TODO
        this.S = S;
        this.cli = new CLIUtils(S);
        this.messagePrinter = new CompactMessagePrinter(S.sourceTextService, S.projectService, messageOutput); //WithLocationStreamMessagePrinter
        
        loadLanguagesFromPath();
        loadStatix();
        try {
            this.project = cli.getOrCreateProject(cli.getCWD().resolveFile(folder, NameScope.FILE_SYSTEM));
        } catch (FileSystemException e) {
            throw new MetaborgException(e);
        }
    }
    
    /**
     * Loads the given language from the given location.
     * 
     * @param file
     *      the directory / language file
     * 
     * @return
     *      the loaded language
     * 
     * @throws MetaborgException 
     *      If the given location is not a valid spoofax language location.
     */
    public ILanguageImpl loadLanguage(String file) throws MetaborgException {
        final FileObject spoofaxPathComponent;
        try {
            spoofaxPathComponent = S.resourceService.resolve(file);
        } catch (MetaborgRuntimeException ex) {
            throw new MetaborgException("Invalid language location " + file, ex);
        }
        
        return cli.loadLanguage(spoofaxPathComponent);
    }
    
    /**
     * Loads languages from the path.
     * 
     * @throws MetaborgException
     * 
     */
    private void loadLanguagesFromPath() throws MetaborgException {
        logger.info("Loading languages from $" + CLIUtils.SPOOFAXPATH + " environment variable: " + System.getenv(CLIUtils.SPOOFAXPATH));
        cli.loadLanguagesFromPath();
    }
    
    /**
     * Loads statix.lang if it was not already loaded.
     * 
     * @return
     *      the statix language implementation
     * 
     * @throws MetaborgException
     *      If statix could not be loaded.
     */
    private ILanguageImpl loadStatix() throws MetaborgException {
        //Check if statix was already loaded from the path.
        ILanguageImpl lang;
        ILanguage L = S.languageService.getLanguage("StatixLang");
        lang = L != null ? L.activeImpl() : null;
        if (lang != null) return lang;
        
        //Otherwise, try loading statix from resources
        final FileObject langResource;
        try {
            langResource = S.resourceService.resolve("res:statix.lang.spoofax-language");
        } catch (MetaborgRuntimeException ex) {
            throw new MetaborgException("Failed to load language statix.lang.", ex);
        }
        lang = cli.loadLanguage(langResource);
        if (lang != null) return lang;

        throw new MetaborgException("Failed to load language statix.lang from path or resources.");
    }
    
    /**
     * @param name
     *      the name of the language
     * 
     * @return
     *      the language with the given name, or null if not found
     */
    public ILanguageImpl getLanguage(String name) {
        ILanguage L = S.languageService.getLanguage(name);
        if (L == null) return null;
        
        return L.activeImpl();
    }
    
    /**
     * Creates a new context for the given language.
     * 
     * @param language
     *      the language
     * 
     * @return
     *      the created context, can be a cached context
     * 
     * @throws MetaborgException
     *      If the language cannot be found.
     */
    public IConstraintContext createContext(String language) throws MetaborgException {
        ILanguageImpl lang = getLanguage(language);
        if (lang == null) throw new MetaborgException("Unable to find language " + language);
        
        IContext context = S.contextService.get(project.location(), project, lang);
        if (!(context instanceof IConstraintContext)) throw new IllegalStateException("Expected context to be a constraint context!");
        return (IConstraintContext) context;
    }
    
    /**
     * Loads the context state from the given file into the given context.
     * 
     * @param context
     *      the context
     * @param file
     *      the file to load from
     * 
     * @throws MetaborgException 
     *      If loading the context fails.
     */
    public void loadFrom(IConstraintContext context, File file) throws MetaborgException {
        StatixUtil.loadFrom(S, context, file);
    }
    
    /**
     * Saves the given context to the given file.
     * 
     * @param context
     *      the context
     * @param file
     *      the file to save to
     * 
     * @throws MetaborgException 
     *      If writing the context fails.
     */
    public void saveTo(IConstraintContext context, File file) throws MetaborgException {
        StatixUtil.saveTo(S, context, file);
    }
    
    /**
     * @return
     *      a freshly created name
     */
    public String freshName() {
        return "FreshName" + nameCounter++;
    }
    
    /**
     * Resets the fresh names.
     */
    public void resetFreshNames() {
        nameCounter = 0;
    }
    
    /**
     * Loads and analyzes the specification.
     * 
     * @param file
     *      the file to load the specification from
     * 
     * @return
     *      the analyzed specification
     * 
     * @throws MetaborgException
     *      If the file cannot be found, the spec cannot be parsed or cannot be analyzed. 
     */
    @Deprecated
    public ISpoofaxAnalyzeUnit loadSpec(String file) throws MetaborgException {
        ILanguage L = S.languageService.getLanguage("StatixLang");
        if (L == null) throw new NullPointerException("Cannot find the Statix language!");
        
        ILanguageImpl lang = L.activeImpl();
        IContext context = S.contextService.get(project.location(), project, lang);
        StatixParse parse = new StatixParse(S, lang, messagePrinter);
        
        ISpoofaxParseUnit specParsed = parse.parse(file);
        
        StatixAnalyze analyze = new StatixAnalyze(S, (IConstraintContext) context, messagePrinter);
        ISpoofaxAnalyzeUnit specAnalyzed = analyze.analyzeSingle(specParsed);
        return specAnalyzed;
    }
    
    public IMessagePrinter getMessagePrinter() {
        return messagePrinter;
    }
    
    /**
     * @param message
     *      the message to print
     * @param pardoned
     *      if the (error or warning) message is pardoned, i.e. the message is acceptable even if
     *      it indicates a problem
     * 
     * @see IMessagePrinter#print(IMessage, boolean)
     */
    public void print(IMessage message, boolean pardoned) {
        messagePrinter.print(message, pardoned);
    }
}
