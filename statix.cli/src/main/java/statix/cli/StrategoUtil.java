package statix.cli;

import org.metaborg.spoofax.core.Spoofax;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.flowspec.terms.M;
import statix.cli.incremental.changes.NotApplicableException;
import statix.cli.util.ITransformation;

public class StrategoUtil {
    //---------------------------------------------------------------------------------------------
    //General purpose
    //---------------------------------------------------------------------------------------------
    
    /**
     * Effeciently creates a new list with the element at the given index replaced with the
     * given element. Annotations are thrown away.
     * 
     * @param S
     *      the Spoofax instance
     * @param original
     *      the original list
     * @param index
     *      the index to replace at
     * @param newElement
     *      the new element to put at index
     * 
     * @return
     *      the new list
     * 
     * @throws IndexOutOfBoundsException
     *      If the given index is negative or >= the size of the list.
     */
    public static IStrategoList replaceItemInList(Spoofax S, IStrategoList original, int index, IStrategoTerm newElement) {
        if (index < 0) throw new IndexOutOfBoundsException("Index " + index + " is invalid, should be positive.");
        if (index >= original.size()) throw new IndexOutOfBoundsException("Index " + index + " is out of bounds, size " + original.size());
        
        final ITermFactory factory = S.termFactoryService.getGeneric();
        
        //We rebuild only the front of the list
        IStrategoList list = original;
        IStrategoTerm[] headerItems = new IStrategoTerm[index + 1];
        for (int i = 0; i < index; i++) {
            headerItems[i] = list.getSubterm(0);
            list = list.tail();
        }
        headerItems[index] = newElement;
        
        //Skip the replaced element
        list = list.tail();
        
        //Now build the list backwards
        IStrategoList result = list;
        int i = headerItems.length - 1;
        while (i > 0) {
            IStrategoTerm head = headerItems[i--];
            result = factory.makeListCons(head, result);
        }
        
        if (i == 0) {
            IStrategoTerm head = headerItems[i];
            result = factory.makeListCons(head, result, original.getAnnotations());
        }
        result.putAttachment(original.getAttachment(null));
        return result;
    }
    
    /**
     * Effeciently removes an element from the given list.
     * 
     * @param S
     *      the Spoofax instance
     * @param original
     *      the original list
     * @param index
     *      the index to replace at
     * 
     * @return
     *      the new list
     * 
     * @throws IndexOutOfBoundsException
     *      If the given index is negative or >= the size of the list.
     */
    public static IStrategoList removeItemFromList(Spoofax S, IStrategoList original, int index) {
        if (index < 0) throw new IndexOutOfBoundsException("Index " + index + " is invalid, should be positive.");
        if (index >= original.size()) throw new IndexOutOfBoundsException("Index " + index + " is out of bounds, size " + original.size());
        
        final ITermFactory factory = S.termFactoryService.getGeneric();
        
        //We rebuild only the front of the list
        IStrategoList list = original;
        IStrategoTerm[] headerItems = new IStrategoTerm[index];
        for (int i = 0; i < index; i++) {
            headerItems[i] = list.getSubterm(0);
            list = list.tail();
        }
        
        //Skip the removed element
        list = list.tail();
        
        //Now build the list backwards
        IStrategoList result = list;
        int i = headerItems.length - 1;
        while (i > 0) {
            IStrategoTerm head = headerItems[i--];
            result = factory.makeListCons(head, result);
        }
        
        if (i == 0) {
            IStrategoTerm head = headerItems[i];
            result = factory.makeListCons(head, result, original.getAnnotations());
        }
        result.putAttachment(original.getAttachment(null));
        return result;
    }
    
    /**
     * Adds the given item to the list at the given position.
     * 
     * @param S
     *      the spoofax instance
     * @param original
     *      the original list
     * @param index
     *      the index to insert the item at
     * @param newElement
     *      the item to insert
     * 
     * @return
     *      the new list
     * 
     * @throws IndexOutOfBoundsException
     *      If the given index is negative, or greater than the size of the list.
     */
    public static IStrategoList addItemToList(Spoofax S, IStrategoList original, int index, IStrategoTerm newElement) {
        if (index < 0) throw new IndexOutOfBoundsException("Index " + index + " is invalid, should be positive.");
        if (index > original.size()) throw new IndexOutOfBoundsException("Index " + index + " is out of bounds, size " + original.size());
        
        final ITermFactory factory = S.termFactoryService.getGeneric();
        
        //We rebuild only the front of the list
        IStrategoList list = original;
        IStrategoTerm[] headerItems = new IStrategoTerm[index + 1];
        for (int i = 0; i < index; i++) {
            headerItems[i] = list.getSubterm(0);
            list = list.tail();
        }
        
        headerItems[index] = newElement;
        
        //Now build the list backwards
        IStrategoList result = list;
        int i = headerItems.length - 1;
        while (i > 0) {
            IStrategoTerm head = headerItems[i--];
            result = factory.makeListCons(head, result);
        }
        
        if (i == 0) {
            IStrategoTerm head = headerItems[i];
            result = factory.makeListCons(head, result, original.getAnnotations());
        }
        result.putAttachment(original.getAttachment(null));
        return result;
    }
    
    public static IStrategoAppl replaceTermInAppl(Spoofax S, IStrategoAppl original, int index, IStrategoTerm newTerm) {
        if (index < 0) throw new IndexOutOfBoundsException("Index " + index + " is invalid, should be positive.");
        if (index >= original.getSubtermCount()) throw new IndexOutOfBoundsException("Index " + index + " is out of bounds, size " + original.getSubtermCount());
        
        final ITermFactory factory = S.termFactoryService.getGeneric();
        
        //Replace the child, keeping the attachment
        IStrategoTerm[] children = original.getAllSubterms();
        newTerm.putAttachment(children[index].getAttachment(null));
        children[index] = newTerm;
        
        //Replace the application, keeping the attachment
        IStrategoAppl newAppl = factory.makeAppl(original.getConstructor(), children, original.getAnnotations());
        newAppl.putAttachment(original.getAttachment(null));
        return newAppl;
    }
    
    //---------------------------------------------------------------------------------------------
    //CompilationUnit
    //---------------------------------------------------------------------------------------------
    
    //CompilationUnit(Some(PackageDeclaration), ?, List(ClassLikes))
    
//    public static IStrategoAppl alterCompilationUnit(Spoofax S, IStrategoTerm ast, Function<IStrategoAppl, IStrategoAppl> transformation) {
//        IStrategoAppl compUnit = getCompilationUnit(ast);
//        return transformation.apply(compUnit);
//    }
    
    public static IStrategoAppl getCompilationUnit(IStrategoTerm ast) {
        IStrategoAppl compUnit = M.appl(ast, "CompilationUnit", 3);
        return compUnit;
    }
    
    public static IStrategoList getClasses(IStrategoTerm ast) {
        IStrategoAppl compUnit = getCompilationUnit(ast);
        IStrategoList classes = M.list(M.at(compUnit, 2));
        return classes;
    }
    
    public static IStrategoAppl replaceClasses(Spoofax S, IStrategoTerm ast, IStrategoList newClasses) {
        IStrategoAppl compUnit = M.appl(ast, "CompilationUnit", 3);
        IStrategoAppl newCompUnit = replaceTermInAppl(S, compUnit, 2, newClasses);
        return newCompUnit;
    }
    
    //---------------------------------------------------------------------------------------------
    //Classes
    //---------------------------------------------------------------------------------------------
    
    //ClassDeclaration(List(Modifier), Id, Some(Generics), Some(ParentClass), Some(ParentInterfaces), List(ClassContent))
    //ClassContent = BodyDecl
    //BodyDecl     = MemberDecl or Instance, Static, Constructor
    //MemberDecl   = Field/Method/Class/Interface
    
    /**
     * Attempts to apply the given transformation to one of the classes in the given AST. If there
     * are no classes in the given AST or if the given transformation fails to apply on all of the
     * classes in the given AST, this method throws a {@link NotApplicableException}.
     * 
     * Keep in mind that the given transformation is always applied to the classes in the file in
     * order, stopping whenever one application succeeds.
     * 
     * @param S
     *      the spoofax instance
     * @param ast
     *      the ast of a file
     * @param classChange
     *      the transformation to apply to the class
     * 
     * @return
     *      the given ast with the given transformation applied
     * 
     * @throws NotApplicableException
     *      If there are no classes in the given AST or if the given transformation fails to apply
     *      on all of the classes in the given AST.
     */
    public static IStrategoAppl alterClass(Spoofax S, IStrategoTerm ast, ITransformation classChange) {
        IStrategoList classes = getClasses(ast);
        
        IStrategoList newClasses = null;
        int i = -1;
        for (IStrategoTerm clazzTerm : classes) {
            i++;
            IStrategoAppl clazz = M.appl(clazzTerm);
            
            //Only accept classes
            if (!"ClassDeclaration".equals(clazz.getName())) continue;
            
            try {
                //If we are applicable, replace the item in the list and stop looking
                IStrategoAppl newClass = classChange.apply(clazz);
                if (newClass != null) {
                    newClass.putAttachment(clazz.getAttachment(null));
                    newClasses = replaceItemInList(S, classes, i, newClass);
                } else {
                    newClasses = removeItemFromList(S, classes, i);
                }
                break;
            } catch (NotApplicableException ex) {
                continue;
            }
        }
        
        if (newClasses == null) throw new NotApplicableException("No class found or none of the classes were applicable");
        return replaceClasses(S, ast, newClasses);
    }
    
    public static IStrategoAppl alterClasslike(Spoofax S, IStrategoTerm ast, ITransformation classlikeChange) {
        IStrategoList classes = getClasses(ast);
        
        IStrategoList newClasses = null;
        int i = -1;
        for (IStrategoTerm clazzTerm : classes) {
            i++;
            IStrategoAppl clazz = M.appl(clazzTerm);
            
            try {
                //If we are applicable, replace the item in the list and stop looking
                IStrategoAppl newClass = classlikeChange.apply(clazz);
                if (newClass != null) {
                    newClass.putAttachment(clazz.getAttachment(null));
                    newClasses = replaceItemInList(S, classes, i, newClass);
                } else {
                    newClasses = removeItemFromList(S, classes, i);
                }
                break;
            } catch (NotApplicableException ex) {
                continue;
            }
        }
        
        if (newClasses == null) throw new NotApplicableException("No classlike found or none of the classes were applicable");
        return replaceClasses(S, ast, newClasses);
    }
    
    public static IStrategoAppl getClass(IStrategoTerm term) {
        IStrategoAppl clazz = M.appl(term, "ClassDeclaration", 6);
        return clazz;
    }
    
    public static String getClassName(IStrategoAppl appl) {
        return idToString(M.at(appl, 1));
    }
    
    public static IStrategoAppl setClassName(Spoofax S, IStrategoAppl original, String name) {
        IStrategoAppl newId = stringToId(S, name);
        return replaceTermInAppl(S, original, 1, newId);
    }
    
    public static IStrategoList getDeclarations(IStrategoAppl clazz) {
        return M.list(M.at(clazz, 5));
    }
    
    public static IStrategoAppl replaceDeclarations(Spoofax S, IStrategoAppl clazz, IStrategoList newDeclarations) {
        IStrategoAppl newClass = replaceTermInAppl(S, clazz, 5, newDeclarations);
        return newClass;
    }
    
    //---------------------------------------------------------------------------------------------
    //Methods
    //---------------------------------------------------------------------------------------------
    
    //MethodDecl(List(Modifier), MethodHeader(ReturnType, Id, Params, List(Annotation), Some(List(ThrowsDec))), Body)
    //ConstrDecl(List(Modifier), Some(Generics), Id, Params, Some(List(ThrowsDec)), Some(SuperOrThis), List(Statement))
    
    /**
     * Attempts to apply the given transformation to one of the methods in the given class. If
     * there are no methods in the given class or if the given transformation fails to apply on all
     * of the methods in the given class, this method throws a {@link NotApplicableException}.
     * 
     * Keep in mind that the given transformation is always applied to the classes in the file in
     * order, stopping whenever one application succeeds.
     * 
     * @param S
     *      the spoofax instance
     * @param clazz
     *      the class
     * @param methodChange
     *      the transformation to apply to the method
     * 
     * @return
     *      the given class with the given transformation applied
     * 
     * @throws NotApplicableException
     *      If there are no methods in the given class or if the given transformation fails to
     *      apply on each of the methods in the given class.
     */
    public static IStrategoAppl alterMethod(Spoofax S, IStrategoAppl clazz, ITransformation methodChange) {
        IStrategoList declarations = getDeclarations(clazz);
        
        IStrategoList newDeclarations = null;
        int i = -1;
        for (IStrategoTerm declarationTerm : declarations) {
            i++;
            IStrategoAppl method = M.appl(declarationTerm);
            
            //Only accept methods
            if (!"MethodDecl".equals(method.getName())) continue;
            
            try {
                //If we are applicable, replace the item in the list and stop looking
                IStrategoAppl newMethod = methodChange.apply(method);
                if (newMethod != null) {
                    newMethod.putAttachment(method.getAttachment(null));
                    newDeclarations = replaceItemInList(S, declarations, i, newMethod);
                } else {
                    newDeclarations = removeItemFromList(S, declarations, i);
                }
                break;
            } catch (NotApplicableException ex) {
                continue;
            }
        }
        
        if (newDeclarations == null) throw new NotApplicableException("No method found or none of the methods were applicable");
        return replaceDeclarations(S, clazz, newDeclarations);
    }
    
    public static IStrategoAppl getMethodHeader(IStrategoAppl method) {
        return M.appl(M.at(method, 1));
    }
    
    public static IStrategoAppl setMethodHeader(Spoofax S, IStrategoAppl method, IStrategoAppl header) {
        return replaceTermInAppl(S, method, 1, header);
    }
    
    public static String getMethodName(IStrategoAppl method) {
        return getMethodNameFromHeader(getMethodHeader(method));
    }
    
    public static IStrategoAppl setMethodName(Spoofax S, IStrategoAppl method, String name) {
        IStrategoAppl methodHeader = getMethodHeader(method);
        IStrategoAppl newMethodHeader = setMethodNameOnHeader(S, methodHeader, name);
        
        return setMethodHeader(S, method, newMethodHeader);
    }
    
    private static String getMethodNameFromHeader(IStrategoAppl methodHeader) {
        if ("MethodHeader".equals(methodHeader.getName())) {
            //MethodHeader.MethodHeader = <
            //<Result> <Id>(<FormalParams>) <AnnotatedDimsEmpty> <Throws?>>
            return idToString(M.at(methodHeader, 1));
        } else if ("MethodHeaderTypeParameters".equals(methodHeader.getName())) {
            //MethodHeader.MethodHeaderTypeParameters = <
            //<TypeParameters> <{Annotation " "}*> <Result> <Id>(<FormalParams>) <Throws?>>
            return idToString(M.at(methodHeader, 3));
        }
        
        throw new IllegalStateException("Unknown method header type: " + methodHeader.getName());
    }
    
    private static IStrategoAppl setMethodNameOnHeader(Spoofax S, IStrategoAppl methodHeader, String name) {
        if ("MethodHeader".equals(methodHeader.getName())) {
            //MethodHeader.MethodHeader = <
            //<Result> <Id>(<FormalParams>) <AnnotatedDimsEmpty> <Throws?>>
            return replaceTermInAppl(S, methodHeader, 1, stringToId(S, name));
        } else if ("MethodHeaderTypeParameters".equals(methodHeader.getName())) {
            //MethodHeader.MethodHeaderTypeParameters = <
            //<TypeParameters> <{Annotation " "}*> <Result> <Id>(<FormalParams>) <Throws?>>
            return replaceTermInAppl(S, methodHeader, 3, stringToId(S, name));
        }
        
        throw new IllegalStateException("Unknown method header type: " + methodHeader.getName());
    }
    
    /**
     * Creates a new method
     * <pre>
     * public type name() {}
     * </pre>
     * 
     * @param S
     *      the spoofax instance
     * @param name
     *      the name of the method
     * 
     * @return
     */
    public static IStrategoAppl createMethod(Spoofax S, String name, IStrategoAppl type) {
        ITermFactory factory = factory(S);
        
        IStrategoAppl params = factory.makeAppl("NoParams");
        IStrategoList mods = factory.makeList(factory.makeAppl("Public"));
        IStrategoAppl header = factory.makeAppl("MethodHeader", type, stringToId(S, name), params, factory.makeList(), factory.makeAppl("None"));
        IStrategoAppl method = factory.makeAppl("MethodDecl", mods, header, factory.makeAppl("Block", factory.makeList()));
        return method;
    }
    
    //---------------------------------------------------------------------------------------------
    //Types
    //---------------------------------------------------------------------------------------------
    
    public static IStrategoAppl typeInt(Spoofax S) {
        ITermFactory factory = factory(S);
        return factory.makeAppl("NumericType", factory.makeAppl("Int"));
    }
    
    public static IStrategoAppl typeVoid(Spoofax S) {
        return factory(S).makeAppl("Void");
    }
    
    //---------------------------------------------------------------------------------------------
    //Convenience methods
    //---------------------------------------------------------------------------------------------
    
    public static String idToString(IStrategoTerm term) {
        return M.string(M.at(M.appl(term, "Id", 1), 0));
    }
    
    public static IStrategoAppl stringToId(Spoofax S, String string) {
        ITermFactory factory = factory(S);
        return factory.makeAppl("Id", factory.makeString(string));
    }
    
    public static ITermFactory factory(Spoofax S) {
        return S.termFactoryService.getGeneric();
    }
}
