package statix.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.context.constraint.ConstraintContext;
import org.metaborg.spoofax.core.context.constraint.IConstraintContext;
import org.metaborg.util.concurrent.IClosableLock;

public class StatixUtil {
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
    public static void loadFrom(Spoofax S, IConstraintContext context, File file) throws MetaborgException {
        FileObject contextFile = S.resourceService.resolve(file);
        try {
            if (!contextFile.exists()) {
                System.err.println("The given context file does not exist!");
            }
        } catch (FileSystemException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        try {
            Method m = ConstraintContext.class.getDeclaredMethod("readContext", FileObject.class);
            m.setAccessible(true);
            Field f = ConstraintContext.class.getDeclaredField("state");
            f.setAccessible(true);
            
            //Read the state from the file
            Object state = m.invoke(context, contextFile);
            
            //We have to get the lock before unloading the context, otherwise the state will be filled
            try (IClosableLock lock = context.write()) {
                context.unload();
                f.set(context, state);
            }
        } catch (Exception ex) {
            System.err.println("Unable to load context from file " + file);
            ex.printStackTrace();
            throw new MetaborgException("Unable to load context from file " + file, ex);
        }
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
    public static void saveTo(Spoofax S, IConstraintContext context, File file) throws MetaborgException {
        FileObject contextFile = S.resourceService.resolve(file);
        
        try {
            Method m = ConstraintContext.class.getDeclaredMethod("writeContext", FileObject.class);
            m.setAccessible(true);
            
            try(IClosableLock lock = context.read()) {
                m.invoke(context, contextFile);
            }
        } catch (Exception ex) {
            System.err.println("Unable to write context to file " + file);
            ex.printStackTrace();
            throw new MetaborgException("Unable to write context to file " + file, ex);
        }
    }
    
    public static void initContext(IConstraintContext context) throws MetaborgException {
        try {
            Field f = ConstraintContext.class.getDeclaredField("state");
            f.setAccessible(true);
            Method m = ConstraintContext.class.getDeclaredMethod("initState");
            m.setAccessible(true);
            
            try(IClosableLock lock = context.read()) {
                f.set(context, m.invoke(context));
            }
        } catch (Exception ex) {
            System.err.println("Unable to init context");
            ex.printStackTrace();
            throw new MetaborgException("Unable to init context", ex);
        }
    }
    
    /**
     * Reads the given file as a string.
     * 
     * @param file
     *      the file to read
     * 
     * @return
     *      the contents of the file
     * 
     * @throws MetaborgException
     *      If the file cannot be read.
     */
    public static String readFile(File file) throws MetaborgException {
        try {
            return IOUtils.toString(new FileInputStream(file), Charset.defaultCharset());
        } catch (IOException e) {
            throw new MetaborgException("Unable to read file contents of " + file, e);
        }
    }
    
    /**
     * Resolves a file that is a String, File, Path or FileObject.
     * 
     * @param S
     *      Spoofax
     * @param file
     *      the file
     * 
     * @return
     *      the file object
     * 
     * @throws IllegalArgumentException
     *      If the given object is not a String, File, Path or FileObject.
     */
    public static FileObject resolve(Spoofax S, Object file) {
        if (file instanceof String) {
            return S.resourceService.resolve((String) file);
        } else if (file instanceof File) {
            return S.resourceService.resolve((File) file);
        } else if (file instanceof Path) {
            return S.resourceService.resolve(((Path) file).toFile());
        } else if (file instanceof FileObject) {
            return (FileObject) file;
        }
        
        throw new IllegalArgumentException("Expected String, File, Path or FileObject.");
    }
}
