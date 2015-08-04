package org.metaborg.meta.lang.nabl2;

import org.strategoxt.imp.runtime.dynamicloading.Descriptor;
import org.strategoxt.imp.runtime.services.MetaFileLanguageValidator;

public class NameBindingLanguage2Validator extends MetaFileLanguageValidator 
{ 
  @Override public Descriptor getDescriptor()
  { 
    return NameBindingLanguage2ParseController.getDescriptor();
  }
}