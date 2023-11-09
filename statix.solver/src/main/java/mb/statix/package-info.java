// @formatter:off
@Value.Style(
    typeAbstract = { "A*" },
    typeImmutable = "*",
    get = { "is*", "get*" },
    with = "with*",
    defaults = @Value.Immutable(builder = true, copy = true, lazyhash = true),
    // prevent generation of jakarta.annotation.*; bogus entry, because empty list = allow all
    allowedClasspathAnnotations = {Override.class},
    jdkOnly = true
)
// @formatter:on
package mb.statix;

import org.immutables.value.Value;