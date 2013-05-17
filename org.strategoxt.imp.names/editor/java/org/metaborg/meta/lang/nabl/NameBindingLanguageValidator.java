package org.metaborg.meta.lang.nabl;

import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.services.MetaFileLanguageValidator;

public class NameBindingLanguageValidator extends MetaFileLanguageValidator 
{ 
  @Override public Descriptor getDescriptor()
  { 
    return NameBindingLanguageParseController.getDescriptor();
  }
}