package mb.statix.taico.scopegraph;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Objects;

import org.metaborg.util.functions.Function1;

import com.google.common.collect.ImmutableClassToInstanceMap;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.ModuleManager;

public class OwnableScope extends Scope implements IOwnableScope {
    private final IModule owner;
    private final String resource;
    private final String name;
    private final ImmutableClassToInstanceMap<Object> attachments;

    public OwnableScope(IModule owner, String name) {
        this.owner = owner;
        this.resource = owner.getId();
        this.name = Objects.requireNonNull(name, "name");
        this.attachments = Objects.requireNonNull(super.getAttachments(), "attachments");
    }

    public OwnableScope(
            IModule owner,
            String name,
            ImmutableClassToInstanceMap<Object> attachments) {
        this.owner = owner;
        this.resource = owner.getId();
        this.name = name;
        this.attachments = attachments;
    }

    /**
     * @return The value of the {@code resource} attribute
     */
    @Override
    public String getResource() {
        return resource;
    }

    /**
     * @return The value of the {@code name} attribute
     */
    @Override
    public String getName() {
      return name;
    }

    /**
     * @return The value of the {@code attachments} attribute
     */
    @Override
    public ImmutableClassToInstanceMap<Object> getAttachments() {
      return attachments;
    }
    
    @Override
    public IModule getOwner() {
        return owner;
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Scope#getAttachments() attachments} attribute.
     * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
     * @param value A new value for attachments
     * @return A modified copy of the {@code this} object
     */
    public final OwnableScope withAttachments(ImmutableClassToInstanceMap<Object> value) {
      if (this.attachments == value) return this;
      ImmutableClassToInstanceMap<Object> newValue = Objects.requireNonNull(value, "attachments");
      return new OwnableScope(this.owner, this.name, newValue);
    }
    
    @Override
    public String toString() {
        return "Scope<name: " + name + ", resource: " + resource + ", owner: " + owner + ">";
    }
    
    public static IMatcher<OwnableScope> ownableMatcher(Function1<String,IModule> lookup) {
        //TODO Scopes used to be immutable, do we want to have them as mutable objects now?
        //TODO Ask Hendrik if this is okay
        return M.preserveAttachments(M.appl2("Scope", M.stringValue(), M.stringValue(),
                (t, resource, name) -> new OwnableScope(lookup.apply(resource), name)));
    }
}
