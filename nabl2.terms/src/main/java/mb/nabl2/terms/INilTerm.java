package mb.nabl2.terms;

public interface INilTerm extends IListTerm {

    @Override
    INilTerm withAttachments(IAttachments value);

    @Override default Tag listTermTag() {
        return Tag.INilTerm;
    }

}