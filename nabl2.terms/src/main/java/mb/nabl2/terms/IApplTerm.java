package mb.nabl2.terms;

import org.metaborg.util.collection.ImList;

public interface IApplTerm extends ITerm {

    String getOp();

    int getArity();

    ImList.Immutable<ITerm> getArgs();

    @Override IApplTerm withAttachments(IAttachments value);

}
