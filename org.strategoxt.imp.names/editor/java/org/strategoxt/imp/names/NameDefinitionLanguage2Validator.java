package org.strategoxt.imp.names;

import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.services.MetaFileLanguageValidator;

public class NameDefinitionLanguage2Validator extends MetaFileLanguageValidator 
{ 
  @Override public Descriptor getDescriptor()
  { 
    return NameDefinitionLanguage2ParseController.getDescriptor();
  }
}