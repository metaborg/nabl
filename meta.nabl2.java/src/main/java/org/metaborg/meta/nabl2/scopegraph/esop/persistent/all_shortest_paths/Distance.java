package org.metaborg.meta.nabl2.scopegraph.esop.persistent.all_shortest_paths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;

public class Distance<L extends ILabel> implements java.io.Serializable {

    private static final long serialVersionUID = 42L;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static final Distance ZERO = new Distance(Collections.EMPTY_LIST);

    @SuppressWarnings({ "rawtypes", "unchecked" })
    
    public static final Distance INFINITE = new Distance((List) null);

    @SuppressWarnings("unchecked")
    public static final <L extends ILabel> Distance<L> zero() {
        return ZERO;
    }
    
    @SuppressWarnings("unchecked")
    public static final <L extends ILabel> Distance<L> infinite() {
        return INFINITE;
    }
    
    private final List<L> labels;

    public static final <L extends ILabel> Distance<L> of(L label) {
        return new Distance<L>(label);
    }
    
    public Distance(L label) {
        this.labels = Collections.singletonList(label);
    }

    public Distance(List<L> labels) {
        this.labels = labels;
    }        

    public List<L> getLabels() {
        if (labels == null) {
            return Collections.EMPTY_LIST;
        } else {
            return labels;
        }
    }
    
    @SuppressWarnings("unchecked")
    public static final <L extends ILabel> Distance<L> concat(final IRegExpMatcher<L> wellFormednessExpression,
            final Distance<L> one, final Distance<L> two) {
        if (one == Distance.ZERO) {
            return two;
        }
        if (one == Distance.INFINITE) {
            return Distance.INFINITE;
        }
        if (two == Distance.ZERO) {
            return one;
        }
        if (two == Distance.INFINITE) {
            return Distance.INFINITE;
        }

        final List<L> mergedLabels = new ArrayList<>(one.labels.size() + two.labels.size());
        mergedLabels.addAll(one.labels);
        mergedLabels.addAll(two.labels);
        assert mergedLabels.size() >= 2;
        
        final Distance<L> concatenation = new Distance<>(mergedLabels);

        if (!Distance.isValid(concatenation)) {
            return INFINITE;
        }
        
        // @formatter:off
        final List<L> filteredLabels = mergedLabels.stream()
                .filter(label -> !label.equals(Label.R))
                .filter(label -> !label.equals(Label.D))
                .collect(Collectors.toList());
        // @formatter:on            

        final IRegExpMatcher<L> matcherResult = wellFormednessExpression.match(filteredLabels);

        if (matcherResult.isEmpty()) {
            return INFINITE;
        } else {
            return concatenation;
        }
    }

    /*
     * Checks that paths contain at most one reference label and at most one
     * declaration label. If present, a declaration occur at the first
     * position, and a declaration at the last position.
     */
    public static final <L extends ILabel> boolean isValid(Distance<L> distance) {
        List<L> mergedLabels = distance.labels;

        if (mergedLabels.size() == 1) {
            return true;
        }

        long countOfLabelR = mergedLabels.stream().filter(label -> label.equals(Label.R)).count();
        long countOfLabelD = mergedLabels.stream().filter(label -> label.equals(Label.D)).count();

        if (countOfLabelR == 1 && !mergedLabels.get(0).equals(Label.R)) {
            return false;
        }

        if (countOfLabelD == 1 && !mergedLabels.get(mergedLabels.size() - 1).equals(Label.D)) {
            return false;
        }

        return true;
    }        
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((labels == null) ? 0 : labels.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Distance other = (Distance) obj;
        if (labels == null) {
            if (other.labels != null)
                return false;
        } else if (!labels.equals(other.labels))
            return false;
        return true;
    }

    @Override
    public String toString() {
        if (this == INFINITE) {
            return "∞";
        } else if (this == ZERO) {
            return "∅";
        } else {
            return labels.toString();
        }
    }

}