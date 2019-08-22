package statix.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Calculator {
    private static final Pattern CLASS_NAME = Pattern.compile("(?:class|interface|enum)\\s+(?<name>\\w+)");
    private static final Pattern PACKAGE = Pattern.compile("package\\s+(?<name>\\w+(?:\\s*\\.\\s*\\w+)*)\\s*;");
    public static final Pattern STAR_IMPORT = Pattern.compile("import\\s+(?<star>\\w+(?:\\s*\\.\\s*\\w+)*)\\.\\s*\\*\\s*;");
    
    public static LinkedHashMap<File, List<File>> getOptionsForFileUsages(List<File> files, int usages) {
        Map<File, String> fileContents = readFiles(files);
        LinkedHashMap<File, List<File>> options = new LinkedHashMap<>();
        for (Entry<File, String> entry : fileContents.entrySet()) {
            File file = entry.getKey();
            String content = entry.getValue();
            String pack;
            try {
                pack = getPackage(content);
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot find package in " + file + ": ", ex);
            }
            List<String> classes = getClassNames(pack, content);
            List<File> allUsages = new ArrayList<>();
            for (String fqn : classes) {
                allUsages.addAll(findUsages(fqn, fileContents, file, true));
            }
            if (allUsages.size() == usages) {
                Collections.sort(allUsages);
                options.put(file, allUsages);
            }
        }
        
        return options;
    }
    
    public static LinkedHashMap<String, Entry<File, List<File>>> getOptionsForClassUsages(List<File> files, int usages) {
        Map<File, String> fileContents = readFiles(files);
        LinkedHashMap<String, Entry<File, List<File>>> options = new LinkedHashMap<>();
        for (Entry<File, String> entry : fileContents.entrySet()) {
            File file = entry.getKey();
            String content = entry.getValue();
            String pack;
            try {
                pack = getPackage(content);
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot find package in " + file + ": ", ex);
            }
            List<String> classes = getClassNames(pack, content);
            for (String fqn : classes) {
                List<File> classUsages = findUsages(fqn, fileContents, file, true);
                if (classUsages.size() != usages) continue;
                
                options.put(fqn, new AbstractMap.SimpleEntry<>(file, classUsages));
            }
        }
        
        return options;
    }
    
    /**
     * @param file
     *      the file to find the usages of
     * @param fileContents
     *      a map from file to file content
     * 
     * @return
     *      a list of all the files that use the given file
     */
    public static List<File> getUsages(File file, Map<File, String> fileContents) {
        String content = fileContents.get(file);
        if (content == null) throw new IllegalArgumentException("The given file should be in the list of all files!");
        String pack;
        try {
            pack = getPackageFromFileContents(content);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot find package in " + file + ": ", ex);
        }
        List<String> classes = getClassNames(pack, content);
        List<File> usages = new ArrayList<>();
        for (String fqn : classes) {
            usages.addAll(findUsages(fqn, fileContents, file, true));
        }
        Collections.sort(usages);
        return usages;
    }
    
    /**
     * @param file
     *      the file to find the usages of
     * @param allFiles
     *      all the files to search in
     * 
     * @return
     *      a list of all the files that use the given file
     */
    public static List<File> getUsages(File file, List<File> allFiles) {
        Map<File, String> fileContents = readFiles(allFiles);
        return getUsages(file, fileContents);
    }
    
    /**
     * @param fqn
     *      the fully qualified name of the class
     * @param fileContents
     *      the contents of all the files in the project
     * 
     * @return
     *      the files referencing the given file
     */
    public static List<File> findUsages(String fqn, Map<File, String> fileContents, File toRemove, boolean ignoreMethods) {
        String name = getName(fqn);
        String pack = getPackage(fqn);
        
        //Escape
        String efqn = fqn.replace(".", "\\s*\\.\\s*");
        String epack = pack.replace(".", "\\s*\\.\\s*");
        
        //Precompile patterns
        Pattern nameRef = Pattern.compile("\\b" + name + "\\b");
        Pattern fqnNameRef = Pattern.compile("\\b" + efqn + "\\b");
        Pattern fqnImport = Pattern.compile("import\\s+" + efqn + "\\s*;");
        Pattern starImport = Pattern.compile("import\\s+" + epack + "\\.\\*\\s*;");
        
        List<File> users = new ArrayList<>();
        for (Entry<File, String> entry : fileContents.entrySet()) {
            String oldValue = entry.getValue();
            //Check normal import
            if (fqnImport.matcher(oldValue).find()) {
                users.add(entry.getKey());
                continue;
            }
            
            ignore: if (ignoreMethods) {
                int index = oldValue.indexOf('{');
                if (index == -1) break ignore;
                oldValue = oldValue.substring(0, index);
            }
            
            //No match, does not even contain the name
            if (!nameRef.matcher(oldValue).find()) continue;
            
            
            //Check star import to import the file
            if (starImport.matcher(oldValue).find()) {
                users.add(entry.getKey());
                continue;
            }
            
            //Check package equality for unqualified access
            String tpack = getPackageFromFileContents(oldValue);
            if (tpack.equals(pack)) {
                users.add(entry.getKey());
                continue;
            }
            
            if (fqnNameRef.matcher(oldValue).find()) {
                users.add(entry.getKey());
                continue;
            }
        }
        
        if (toRemove != null) users.remove(toRemove);
        Collections.sort(users);
        
        return users;
    }
    
    public static int getUsageCount(String fqn, Map<File, String> fileContents) {
        int nr = findUsages(fqn, fileContents, null, true).size();
        return nr == 0 ? 0 : nr - 1;
    }
    
    //---------------------------------------------------------------------------------------------
    //Helpers
    //---------------------------------------------------------------------------------------------
    
    private static String getName(String fqn) {
        int index = fqn.lastIndexOf('.');
        if (index == -1) return fqn;
        return fqn.substring(index + 1);
    }
    
    private static String getPackage(String fqn) {
        int index = fqn.lastIndexOf('.');
        if (index == -1) return "";
        return fqn.substring(0, index);
    }
    
    private static String getPackageFromFileContents(String content) {
        Matcher mat = PACKAGE.matcher(content);
        if (mat.find()) {
            return mat.group("name").replaceAll("\\s", "");
        } else {
            return "";
        }
    }
    
    private static List<String> getClassNames(String pack, String content) {
        Matcher mat = CLASS_NAME.matcher(content);
        List<String> tbr = new ArrayList<>();
        while (mat.find()) {
            tbr.add(pack + "." + mat.group("name"));
        }
        return tbr;
    }
    
    //---------------------------------------------------------------------------------------------
    //Reading from files
    //---------------------------------------------------------------------------------------------
    
    public static Map<File, String> readFiles(Iterable<File> allFiles) {
        try {
            Map<File, String> fileContents = new HashMap<>();
            for (File file : allFiles) {
                String content = readAllText(file);
                fileContents.put(file, content);
            }
            return fileContents;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private static String readAllText(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            return sb.toString();
        }
    }
}
