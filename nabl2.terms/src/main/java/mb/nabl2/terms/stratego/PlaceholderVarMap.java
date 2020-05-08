package mb.nabl2.terms.stratego;

import java.util.HashMap;

import javax.annotation.Nullable;

import mb.nabl2.terms.IApplTerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.build.TermBuild;

/**
 * A mapping between placeholders and term variables.
 */
public final class PlaceholderVarMap {

    private final String resourceName;
    private final HashMap<TermVarWrapper, IApplTerm> varToPlhdr = new HashMap<>();
    private final HashMap<IApplTerm, TermVarWrapper> plhdrToVar = new HashMap<>();

    /**
     * Initializes a new instance of the {@link PlaceholderVarMap} class.
     *
     * @param resourceName the resource name to use for generated variables
     */
    public PlaceholderVarMap(String resourceName) {
        this.resourceName = resourceName;
    }

    /**
     * Adds a mapping from a variable with the specified name to the specified placeholder term.
     *
     * If one or both of the arguments are already part of a mapping, the old mappings are replaced.
     *
     * @param termVar the term variable
     * @param placeholderTerm the placeholder term
     * @return {@code true} if the mapping was added without replacing any existing mappings;
     * otherwise, {@code false}
     */
    public boolean add(ITermVar termVar, IApplTerm placeholderTerm) {
        @Nullable IApplTerm oldPlaceholderTerm = this.varToPlhdr.put(new TermVarWrapper(termVar), placeholderTerm);
        @Nullable TermVarWrapper oldTermVar = this.plhdrToVar.put(placeholderTerm, new TermVarWrapper(termVar));
        boolean replacedAnything = false;
        if (oldPlaceholderTerm != null && oldPlaceholderTerm != placeholderTerm) {
            this.varToPlhdr.remove(oldTermVar);
            replacedAnything = true;
        }
        if (oldTermVar != null && oldTermVar.termVar != termVar) {
            this.plhdrToVar.remove(oldPlaceholderTerm);
            replacedAnything = true;
        }
        assert this.plhdrToVar.size() == this.varToPlhdr.size();
        return !replacedAnything;
    }

    /**
     * Gets the term variable associated with the specified placeholder term.
     *
     * @param placeholderTerm the placeholder term
     * @return the associated term var; or {@code null} when none is associated
     */
    public @Nullable ITermVar getVar(IApplTerm placeholderTerm) {
        TermVarWrapper termVarWrapper = plhdrToVar.get(placeholderTerm);
        return termVarWrapper != null ? termVarWrapper.termVar : null;
    }

    /**
     * Gets the placeholder term associated with the specified term variable.
     *
     * @param varTerm the term variable
     * @return the associated placeholder term; or {@code null} when none is associated
     */
    public @Nullable IApplTerm getPlaceholder(ITermVar varTerm) {
        return varToPlhdr.get(new TermVarWrapper(varTerm));
    }

    /**
     * Determines whether the map has a variable with the specified name.
     *
     * @param name the name to check
     * @return {@code true} when a variable with the specified name exists in this map;
     * otherwise, {@code false}
     */
    public boolean hasVarWithName(String name) {
        return varToPlhdr.keySet().stream().anyMatch(v -> v.termVar.getName().equals(name));
    }

    /**
     * Adds a mapping for the specified placeholder, or returns an existing mapping.
     *
     * @param placeholderTerm the placeholder to add
     * @return the new mapping; or an existing mapping
     */
    public ITermVar addPlaceholderMapping(IApplTerm placeholderTerm) {
        @Nullable ITermVar termVar = getVar(placeholderTerm);
        if (termVar == null) {
            String newVarName = generateName(placeholderTerm);
            termVar = TermBuild.B.newVar(this.resourceName, newVarName);
            boolean unique = add(termVar, placeholderTerm);
            assert unique : "Apparently the added term variable and/or placeholder was not unique.";
        }
        return termVar;
    }

    /**
     * Generates a name for the variable corresponding to the given placeholder term.
     *
     * @param placeholderTerm the placeholder term
     * @return the generated name, which is unique
     */
    private String generateName(IApplTerm placeholderTerm) {
        String prefix = "$" + placeholderTerm.getOp().substring(0, placeholderTerm.getOp().length() - "-Plhdr".length());
        int index = 0;
        String name = prefix + index;
        while (hasVarWithName(name)) {
            index += 1;
            name = prefix + index;
        }
        return name;
    }

    /** Wraps an ITermVar such that the equality check includes the annotations. */
    private static class TermVarWrapper {
        private final ITermVar termVar;

        private TermVarWrapper(ITermVar termVar) {
            this.termVar = termVar;
        }

        @Override
        public int hashCode() {
            return this.termVar.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof TermVarWrapper)) return false;
            return this.termVar.equals(((TermVarWrapper)obj).termVar, true);
        }

        @Override
        public String toString() {
            return this.termVar.toString();
        }
    }

}
