package mb.nabl2.terms;

public interface IConsTerm extends IListTerm {

    ITerm getHead();

    IListTerm getTail();

    @Override
    IConsTerm withAttachments(IAttachments value);

}