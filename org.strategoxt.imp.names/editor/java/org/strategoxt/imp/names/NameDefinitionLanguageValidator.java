package org.strategoxt.imp.names;

import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.services.MetaFileLanguageValidator;

public class NameDefinitionLanguageValidator extends MetaFileLanguageValidator 
{ 
  @Override public Descriptor getDescriptor()
  { 
    return NameDefinitionLanguageParseController.getDescriptor();
  }
}