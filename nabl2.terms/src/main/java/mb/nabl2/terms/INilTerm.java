package mb.nabl2.terms;

public interface INilTerm extends IListTerm {

    @Override
    INilTerm withAttachments(IAttachments value);

    @Override default ITerm.Tag termTag() {
        return ITerm.Tag.INilTerm;
    }

    @Override default IListTerm.Tag listTermTag() {
        return IListTerm.Tag.INilTerm;
    }

}