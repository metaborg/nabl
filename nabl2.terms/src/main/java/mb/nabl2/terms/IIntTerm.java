package mb.nabl2.terms;

public interface IIntTerm extends ITerm {

    int getValue();

    @Override
    IIntTerm withAttachments(IAttachments value);

    @Override default Tag termTag() {
        return Tag.IIntTerm;
    }

}
