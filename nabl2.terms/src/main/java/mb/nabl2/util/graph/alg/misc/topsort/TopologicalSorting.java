/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package mb.nabl2.util.graph.alg.misc.topsort;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import mb.nabl2.util.graph.igraph.IGraphDataSource;

/**
 * @since 1.6
 */
public class TopologicalSorting {

    private TopologicalSorting() {/*Utility class constructor*/}
    
    private static final class Pair<T> {
        public T element;
        public boolean isParent;

        public Pair(final T element, final boolean isParent) {
            this.element = element;
            this.isParent = isParent;
        }
    }

    /**
     * Returns a topological ordering for the given graph data source. 
     * Output format: if there is an a -> b (transitive) reachability, then node <code>a</code> will come before node <code>b</code> in the resulting list.  
     * 
     * @param gds the graph data source
     * @return a topological ordering
     */
    public static <T> List<T> compute(final IGraphDataSource<T> gds) {
        final Set<T> visited = new HashSet<>();
        final LinkedList<T> result = new LinkedList<>();
        final Stack<Pair<T>> dfsStack = new Stack<>();

        for (final T node : gds.getAllNodes()) {
            if (!visited.contains(node)) {
                dfsStack.push(new Pair<>(node, false));                
            }
            
            while (!dfsStack.isEmpty()) {
                final Pair<T> head = dfsStack.pop();
                final T source = head.element;
                
                if (head.isParent) {
                    // we have already seen source, push it to the resulting stack
                    result.addFirst(source);
                } else {
                    // first time we see source, continue with its children
                    visited.add(source);
                    dfsStack.push(new Pair<>(source, true));
                    
                    for (final T target : gds.getTargetNodes(source).distinctValues()) {
                        if (!visited.contains(target)) {
                            dfsStack.push(new Pair<>(target, false));
                        }
                    }
                }
            }
        }

        return result;
    }
}
