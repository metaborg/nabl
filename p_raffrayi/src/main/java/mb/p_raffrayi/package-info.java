/**
 * Deep in the forest of Malaysia is a spider hard at work to ensure the survival of the colony. She determinedly builds
 * her---by itself far too small for the intended prey. However, all around her are other, building their little webs,
 * and connecting them to webs of the others. A big communal web is built that serves these P. raffrayi well.
 * 
 * Masumoto, Toshiya. “The Composition of a Colony of Philoponella Raffrayi (Uloboridae) in Peninsular Malaysia.” Acta
 * Arachnologica 41, no. 1 (1992): 1–4. https://doi.org/10.2476/asjaa.41.1.
 */
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
package mb.p_raffrayi;

import org.immutables.value.Value;