package mb.nabl2.terms;

public interface IBlobTerm extends ITerm {

    Object getValue();

    @Override
    IBlobTerm withAttachments(IAttachments value);

}
