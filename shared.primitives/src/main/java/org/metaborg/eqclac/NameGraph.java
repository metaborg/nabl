package org.metaborg.eqclac;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;

public final class NameGraph {
	
	private final Set<Set<NameIndex>> clusters;
	
	public NameGraph() {
		clusters = new HashSet<>();
	}
	
	public NameGraph(List<IStrategoTerm> resolutionRelation) {
		clusters = new HashSet<>();
		for(IStrategoTerm pair: resolutionRelation) {
			addResolutionPair(new ResolutionPair(pair));
		}
	}
	
	public void addResolutionPair(ResolutionPair pair) {
		Optional<Set<NameIndex>> decCluster = find(pair.getDeclaration());
		Optional<Set<NameIndex>> refCluster = find(pair.getReference());
		
		if(!(decCluster.isPresent() || refCluster.isPresent())) {
			Set<NameIndex> cluster = new HashSet<>();
			cluster.add(pair.getDeclaration());
			cluster.add(pair.getReference());
			clusters.add(cluster);
		} else if (decCluster.isPresent() && !refCluster.isPresent()) {
			decCluster.get().add(pair.getReference());
		} else if (!decCluster.isPresent() && refCluster.isPresent()) {
			refCluster.get().add(pair.getDeclaration());
		} else if (decCluster.isPresent() && refCluster.isPresent()) {
			union(decCluster.get(), refCluster.get());
		}
	}

	public Optional<Set<NameIndex>> find(NameIndex index) {
		return clusters.stream()
				.filter(cluster -> cluster.contains(index))
				.findFirst();
	}
	
	public void union(Set<NameIndex> decCluster, Set<NameIndex> refCluster) {
		if (decCluster != refCluster) {
			decCluster.addAll(refCluster);
			clusters.remove(refCluster);
		}
	}

}
