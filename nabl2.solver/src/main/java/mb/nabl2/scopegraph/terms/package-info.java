// @formatter:off
@Value.Style(
    typeAbstract = { "A*" },
    typeImmutable = "*",
    get = { "is*", "get*" },
    with = "with*",
    defaults = @Value.Immutable(builder = false, copy = true, prehash = true),
    // prevent generation of javax.annotation.*; bogus entry, because empty list = allow all
    allowedClasspathAnnotations = {Override.class}
)
// @formatter:on
package mb.nabl2.scopegraph.terms;

import org.immutables.value.Value;