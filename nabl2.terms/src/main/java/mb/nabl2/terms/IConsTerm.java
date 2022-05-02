package mb.nabl2.terms;

public interface IConsTerm extends IListTerm {

    ITerm getHead();

    IListTerm getTail();

    @Override
    IConsTerm withAttachments(IAttachments value);

    @Override default ITerm.Tag termTag() {
        return ITerm.Tag.IConsTerm;
    }

    @Override default IListTerm.Tag listTermTag() {
        return IListTerm.Tag.IConsTerm;
    }
}