package mb.nabl2.terms;

import java.util.List;

public interface IApplTerm extends ITerm {

    String getOp();

    int getArity();

    List<ITerm> getArgs();

    @Override IApplTerm withAttachments(IAttachments value);

}
