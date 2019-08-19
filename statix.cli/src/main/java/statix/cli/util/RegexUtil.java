package statix.cli.util;

import java.util.regex.Pattern;

public class RegexUtil {
    public static final String FILE =
            //DO NOT ADD ANY CAPTURE GROUPS, that will break the method body matching bracket matching
            "\\s*package (?<package>[^;]*)\\s*;\\s*"
            + "\\s*(?<imports>(?:import [^;]*;\\s*)*)\\s*"
            + "\\s*(?<classes>"
                + "(?:(?:\\w+ ){0,3}class\\s+\\w+" //modifiers class name
                    + "\\s*(?:\\<\\w+(?:\\s*,\\s*\\w+)*\\>)?" //generics
                    + "\\s*(?:(?:implements|extends)\\s+\\w+(?:\\s*,\\s*\\w+)*\\s*){0,2}\\s*" //implements/extends
                + "\\{\\s*"
                + "(?<classcontent>"
                    + "(?:"
                        //At least 2 words, space separated, e.g. `int name;`
                        + "(?:\\s*(?<field>(?:\\w+\\s+){1,6}\\w+\\s*;\\s*))"
                        + "|"
                        + "(?:\\s*(?<method>"
                            + "(?:\\w+[ ]+){1,4}\\w+\\s*"
                                + "\\("
                                + "(?:(?:\\w+\\s+\\w+\\s*,\\s*)*\\w+\\s+\\w+\\s*)?"
                                + "\\)\\s*"
                                //Magic for the body
                                + "(?=\\{)(?:(?=.*?\\{(?!.*?\\7)(.*\\}(?!.*\\8).*))(?=.*?\\}(?!.*?\\8)(.*)).)+?.*?(?=\\7)[^\\{]*(?=\\8$)"
                        + "))"
                    + ")*"
                + ")"
                + "\\s*\\}"
                + ")+"
            + ")\\s*";
    public static final Pattern FILE_PATTERN = Pattern.compile(FILE, Pattern.DOTALL);
    
    
    
}
