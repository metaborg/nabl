package mb.statix.spec;

import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;

public class Fresh {

    private final Set.Transient<String> vars;

    public Fresh() {
        this(Iterables2.empty());
    }

    public Fresh(Iterable<String> vars) {
        this.vars = Set.Transient.of();
        vars.forEach(this.vars::__insert);
    }

    public String fresh(String name) {
        String fresh = name;
        if(vars.contains(name)) {
            name = name.replaceAll("-?[0-9]*$", "");
            int i = 0;
            while((vars.contains((fresh = name + "-" + Integer.toString(i++)))))
                ;
            vars.__insert(name);
            return fresh;
        }
        vars.__insert(fresh);
        return fresh;
    }

}