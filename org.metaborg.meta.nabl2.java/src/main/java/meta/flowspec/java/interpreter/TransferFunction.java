package meta.flowspec.java.interpreter;

import java.util.Arrays;

import org.metaborg.meta.nabl2.controlflow.terms.CFGNode;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import meta.flowspec.java.interpreter.locals.ArgToVarNode;

@TypeSystemReference(Types.class)
public class TransferFunction extends RootNode {
    private ArgToVarNode[] patternVariables;
    private Where body;

    public TransferFunction(FrameDescriptor frameDescriptor, ArgToVarNode[] patternVariables, Where body) {
        this(null, frameDescriptor, patternVariables, body);
    }

    public TransferFunction(TruffleLanguage<Context> language, FrameDescriptor frameDescriptor, ArgToVarNode[] patternVariables, Where body) {
        super(language, frameDescriptor);
        this.patternVariables = patternVariables;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        for (ArgToVarNode pv : patternVariables) {
            pv.execute(frame);
        }
        return body.execute(frame);
    }
    
    public static TransferFunction fromIStrategoTerm(TruffleLanguage<Context> language, FrameDescriptor frameDescriptor, IStrategoTerm term) {
        assert term instanceof IStrategoAppl : "Expected a constructor application term";
        final IStrategoAppl appl = (IStrategoAppl) term;
        switch (appl.getConstructor().getName()) {
            case "TransferFunction" : {
                assert appl.getSubtermCount() == 2 : "Expected TransferFunction to have 2 children";
                IStrategoTerm[] params = Tools.listAt(appl, 0).getAllSubterms();
                String[] patternVars = Arrays.stream(params).map(Tools::javaString).toArray(String[]::new);
                
                ArgToVarNode[] patternVariables = new ArgToVarNode[patternVars.length];
                for (int i = 0; i < patternVars.length; i++) {
                    FrameSlot slot = frameDescriptor.addFrameSlot(patternVars[i], FrameSlotKind.Object);
                    patternVariables[i] = new ArgToVarNode(i, slot);
                }
                
                Where body = Where.fromIStrategoTerm(appl.getSubterm(1), frameDescriptor);
                return new TransferFunction(language, frameDescriptor, patternVariables, body);
            }
            default : throw new IllegalArgumentException("Expected constructor TransferFunction");
        }
    }

    public Object call(Object arg) {
        // TODO implement call with one argument, the CFGNode from the direction
        // This means that TransFunction should already have the arguments for the pattern variables baked in, or this call method should be on a wrapper. 
        return null;
    }
}
