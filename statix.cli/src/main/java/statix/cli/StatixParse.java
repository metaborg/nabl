package statix.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.IMessagePrinter;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ParseContrib;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * Class for parsing functionalities for the Statix CLI.
 */
public class StatixParse {
    private static final ILogger logger = LoggerUtils.logger(StatixParse.class);
    
    private final Spoofax S;
    private final ILanguageImpl lang;
    private final IMessagePrinter printer;
    
    public StatixParse(Spoofax S, ILanguageImpl lang, IMessagePrinter printer) {
        this.S = S;
        this.lang = lang;
        this.printer = printer;
    }
    
    /**
     * Creates parse units for the given files (keys in the map).
     * If the value is a string, the string is parsed and used for the parse unit.
     * If the value is a file, the contents of that file are parsed and used for the parse unit.
     * If the value is a stratego term, that term is used as AST for the parse unit.
     * If the value is an object (new Object), the contents of the file itself are used.
     * Otherwise, an exception is thrown.
     * 
     * @param files
     *      the files and corresponding content
     * 
     * @return
     *      a list of parse units
     * 
     * @throws MetaborgException
     *      If parsing any of the contents fails.
     * 
     * @see #parse(String)
     *      Parse normally
     * @see #parse(String, File)
     *      Parse contents of separate file
     * @see #parse(String, String)
     *      Parse supplied string
     * @see #parse(String, IStrategoTerm)
     *      Parse with AST supplied
     */
    public List<ISpoofaxParseUnit> parseFiles(Map<String, ?> files) throws MetaborgException {
        List<ISpoofaxParseUnit> list = new ArrayList<>();
        for (Entry<String, ?> e : files.entrySet()) {
            ISpoofaxParseUnit unit;
            Object o = e.getValue();
            if (o instanceof String) {
                unit = parse(e.getKey(), (String) o);
            } else if (o instanceof File) {
                unit = parse(e.getKey(), (File) o);
            } else if (o instanceof IStrategoTerm) {
                unit = parse(e.getKey(), (IStrategoTerm) o);
            } else if (o.getClass() == Object.class){
                unit = parse(e.getKey());
            } else {
                throw new IllegalArgumentException("The value must be a string, file, stratego term or a new object.");
            }
            
            list.add(unit);
        }
        return list;
    }
    
    /**
     * Creates a parse unit for the given file.
     * 
     * @param file
     *      the file to create the parse unit for
     * 
     * @return
     *      the parsed file
     * 
     * @throws MetaborgException
     *      If parsing of the contents of the given file fails.
     */
    public ISpoofaxParseUnit parse(String file) throws MetaborgException {
        final FileObject resource = S.resourceService.resolve(file);
        final String text;
        try {
            text = S.sourceTextService.text(resource);
        } catch(IOException e) {
            throw new MetaborgException("Cannot find " + file, e);
        }
        final ISpoofaxInputUnit inputUnit = S.unitService.inputUnit(resource, text, lang, null);
        final Optional<ISpoofaxParseUnit> parseUnit = parse(inputUnit);
        return parseUnit.orElseThrow(() -> new MetaborgException("Parsing of " + file + " failed: " + file + " contains syntax errors (1)."));
    }
    
    /**
     * Creates a parse unit for the given file with the contents of the second file.
     * 
     * @param file
     *      the file to create the parse unit for
     * @param contents
     *      the file to use the contents of
     * 
     * @return
     *      the parsed file
     * 
     * @throws MetaborgException
     *      If parsing of the given contents fails.
     */
    public ISpoofaxParseUnit parse(String file, File contents) throws MetaborgException {
        String fileContents;
        try {
            fileContents = IOUtils.toString(new FileInputStream(contents), Charset.defaultCharset());
        } catch (IOException e) {
            throw new MetaborgException("Unable to read file contents of " + contents, e);
        }
        
        final FileObject resource = S.resourceService.resolve(file);
        final ISpoofaxInputUnit inputUnit = S.unitService.inputUnit(resource, fileContents, lang, null);
        final Optional<ISpoofaxParseUnit> parseUnit = parse(inputUnit);
        return parseUnit.orElseThrow(() -> new MetaborgException("Parsing of " + file + " failed: file " + contents + " has syntax errors (2)."));
    }
    
    /**
     * Creates a parse unit for the given file with the given contents.
     * 
     * @param file
     *      the file to create the parse unit for
     * @param contents
     *      the contents of the file
     * 
     * @return
     *      the parsed file
     * 
     * @throws MetaborgException
     *      If parsing of the given contents fails.
     */
    public ISpoofaxParseUnit parse(String file, String contents) throws MetaborgException {
        final FileObject resource = S.resourceService.resolve(file);
        final ISpoofaxInputUnit inputUnit = S.unitService.inputUnit(resource, contents, lang, null);
        final Optional<ISpoofaxParseUnit> parseUnit = parse(inputUnit);
        return parseUnit.orElseThrow(() -> new MetaborgException("Parsing of " + file + " failed: passed contents have syntax errors (3)."));
    }
    
    /**
     * Creates a parse unit for a file with the given ast.
     * 
     * @param file
     *      the file to create the parse unit for
     * @param ast
     *      the ast to associate with the parsing
     * 
     * @return
     *      the parse unit
     */
    public ISpoofaxParseUnit parse(String file, IStrategoTerm ast) {
        final FileObject resource = S.resourceService.resolve(file);
        final ISpoofaxInputUnit inputUnit = S.unitService.emptyInputUnit(resource, lang, null);
        final ISpoofaxParseUnit parseUnit = S.unitService.parseUnit(inputUnit, new ParseContrib(ast));
        return parseUnit;
    }
    
    /**
     * Creates a parse unit for the given file which represents that the given file has been
     * removed.
     * 
     * @param file
     *      the file
     * 
     * @return
     *      the parse unit for the file
     */
    public ISpoofaxParseUnit removedFile(String file) {
        final FileObject resource = S.resourceService.resolve(file);
        final ISpoofaxInputUnit inputUnit = S.unitService.emptyInputUnit(resource, lang, null);
        final ISpoofaxParseUnit parseUnit = S.unitService.emptyParseUnit(inputUnit);
        return parseUnit;
    }
    
    /**
     * Parses the given input unit. If the given input unit has syntax errors, this method returns
     * an empty optional. If the given input unit does not have a parseable content, this method
     * throws a MetaborgException. Otherwise, the parse unit is returned.
     * 
     * @param inputUnit
     *      the input unit
     * 
     * @return
     *      an optional parse result
     * 
     * @throws MetaborgException
     *      If the language is not available, or if the input does not contain parseable content.
     */
    public Optional<ISpoofaxParseUnit> parse(ISpoofaxInputUnit inputUnit) throws MetaborgException {
//        final ILanguageImpl lang = context.language();
        if (!S.syntaxService.available(lang)) {
            throw new MetaborgException("Parsing not available (parser for " + lang + "not available).");
        }
        
        final ISpoofaxParseUnit parseUnit = S.syntaxService.parse(inputUnit);
        for (IMessage message : parseUnit.messages()) {
            printer.print(message, false);
        }
        
        if (!parseUnit.valid()) {
            throw new MetaborgException("Parsing for " + inputUnit.source() + " failed.");
        }
        
        if (!parseUnit.success()) {
            logger.info("The content for {} has syntax errors", inputUnit.source());
            return Optional.empty();
        }
        
        return Optional.of(parseUnit);
    }
}
