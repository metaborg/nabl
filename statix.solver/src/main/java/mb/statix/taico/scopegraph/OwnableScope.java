package mb.statix.taico.scopegraph;

import java.util.Objects;

import com.google.common.collect.ImmutableClassToInstanceMap;

import mb.nabl2.scopegraph.terms.Scope;
import mb.statix.taico.module.IModule;

public class OwnableScope extends Scope implements IOwnableScope {
    private final IModule owner;
    private final String resource;
    private final String name;
    private final ImmutableClassToInstanceMap<Object> attachments;

    public OwnableScope(IModule owner, String resource, String name) {
        this.owner = owner;
        this.resource = Objects.requireNonNull(resource, "resource");
        this.name = Objects.requireNonNull(name, "name");
        this.attachments = Objects.requireNonNull(super.getAttachments(), "attachments");
    }

    public OwnableScope(
            IModule owner,
            String resource,
            String name,
            ImmutableClassToInstanceMap<Object> attachments) {
        this.owner = owner;
        this.resource = resource;
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
      return new OwnableScope(this.owner, this.resource, this.name, newValue);
    }
    
    @Override
    public String toString() {
        return "Scope<name: " + name + ", resource: " + resource + ", owner: " + owner + ">";
    }
}
