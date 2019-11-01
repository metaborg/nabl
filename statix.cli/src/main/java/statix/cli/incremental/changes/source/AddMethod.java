package statix.cli.incremental.changes.source;

import java.util.regex.Matcher;

import statix.cli.StatixData;
import statix.cli.incremental.changes.IIncrementalSourceChange;
import statix.cli.incremental.changes.NotApplicableException;
import statix.cli.util.RegexUtil;

public class AddMethod extends IIncrementalSourceChange {
    public static final String FILE =
            //DO NOT ADD ANY CAPTURE GROUPS, that will break the method body matching bracket matching
            "(?<pre>\\s*package [^;]*\\s*;\\s*"
            + "\\s*(?:import [^;]*;\\s*)*\\s*"
            + "\\s*)"
                + "(?:(?:\\w+ ){0,3}class \\w+" //modifiers class name
                    + "\\s*(?:\\<\\w+(?:\\s*,\\s*\\w+)*\\>)?" //generics
                    + "\\s*(?:(?:implements|extends)\\s+\\w+(?:\\s*,\\s*\\w+)*\\s*){0,2}\\s*" //implements/extends
                + "\\{\\s*"
                + "(?<insertion>)"
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
                            + "(?=\\{)(?:(?=.*?\\{(?!.*?\\5)(.*\\}(?!.*\\6).*))(?=.*?\\}(?!.*?\\6)(.*)).)+?.*?(?=\\5)[^\\{]*(?=\\6$)"
                    + "))"
                + ")*"
                + "\\s*\\}"
                + ")+"
            + "\\s*";
    public AddMethod() {
        super("method", "add");
    }

    @Override
    public String apply(StatixData data, String original) throws NotApplicableException {
        throw new NotApplicableException(this, "UNSUPPORTED YET");
        
//        Matcher m = RegexUtil.FILE_PATTERN.matcher(original);
//        
//        if (!m.matches()) throw new NotApplicableException(this, "File is not a recognized class");
//        
//        
//        // TODO Auto-generated method stub
//        return null;
    }
}
