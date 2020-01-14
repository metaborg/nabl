package mb.nabl2.terms.build;

import java.util.Arrays;
import java.util.LinkedList;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Lists;

import mb.nabl2.terms.IConsList;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.IListVar;
import mb.nabl2.terms.INilList;
import mb.nabl2.terms.ITerm;

public class ListBuild {

    public static final LB LB = new LB();

    public static class LB {

        public final IListTerm EMPTY_LIST = newNil();

        public IListTerm newList(ITerm... elems) {
            return newList(Arrays.asList(elems));

        }

        public IListTerm newList(Iterable<? extends ITerm> elems) {
            return newList(elems, null);
        }

        public IListTerm newList(Iterable<? extends ITerm> elems,
                @Nullable Iterable<ImmutableClassToInstanceMap<Object>> attachments) {
            LinkedList<ITerm> elemsQueue = Lists.newLinkedList(elems);
            LinkedList<ImmutableClassToInstanceMap<Object>> attachmentsQueue =
                    attachments != null ? Lists.newLinkedList(attachments) : null;
            if(attachmentsQueue != null && attachmentsQueue.size() != elemsQueue.size() + 1) {
                throw new IllegalArgumentException(
                        "Number of attachments does not correspond to number of elements in the list.");
            }
            IListTerm list = newNil();
            if(attachmentsQueue != null) {
                list = list.withAttachments(attachmentsQueue.removeLast());
            }
            return newListTail(elemsQueue, list, attachmentsQueue);
        }

        public IListTerm newListTail(Iterable<? extends ITerm> elems, IListTerm list) {
            return newListTail(elems, list, null);
        }

        public IListTerm newListTail(Iterable<? extends ITerm> elems, IListTerm list,
                @Nullable Iterable<ImmutableClassToInstanceMap<Object>> attachments) {
            LinkedList<ITerm> elemsQueue = Lists.newLinkedList(elems);
            LinkedList<ImmutableClassToInstanceMap<Object>> attachmentsQueue =
                    attachments != null ? Lists.newLinkedList(attachments) : null;
            if(attachmentsQueue != null && attachmentsQueue.size() != elemsQueue.size()) {
                throw new IllegalArgumentException(
                        "Number of attachments does not correspond to number of elements in the list.");
            }
            return newListTail(elemsQueue, list, attachmentsQueue);
        }

        private IListTerm newListTail(LinkedList<? extends ITerm> elems, IListTerm list,
                @Nullable LinkedList<ImmutableClassToInstanceMap<Object>> attachments) {
            while(!elems.isEmpty()) {
                list = newCons(elems.removeLast(), list);
                if(attachments != null) {
                    list = list.withAttachments(attachments.removeLast());
                }
            }
            return list;
        }

        public INilList newNil() {
            return newNil(null);
        }

        public INilList newNil(ImmutableClassToInstanceMap<Object> attachments) {
            final INilList term = ImmutableNilList.of();
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        public IConsList newCons(ITerm head, IListTerm tail) {
            return newCons(head, tail, null);
        }

        public IConsList newCons(ITerm head, IListTerm tail, ImmutableClassToInstanceMap<Object> attachments) {
            final IConsList term = ImmutableConsList.of(head, tail);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

        public IListVar newVar(String resource, String name) {
            return newVar(resource, name, null);
        }

        public IListVar newVar(String resource, String name, ImmutableClassToInstanceMap<Object> attachments) {
            final IListVar term = ImmutableListVar.of(resource, name);
            return attachments != null ? term.withAttachments(attachments) : term;
        }

    }

}