package org.metaborg.eqclac;

import java.util.Objects;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

public final class NameIndex {
	
	private final int numIndex;
	private final String path;
	private IStrategoTerm termIndex;
	
	public NameIndex(IStrategoTerm termIndex) {
		this.termIndex = termIndex;
		this.path = TermUtils.toJavaString(termIndex.getSubterm(0));
		this.numIndex = TermUtils.toJavaInt(termIndex.getSubterm(1));
	}
	
	public NameIndex(int numIndex, String path) {
		if (numIndex < 0 || path == null) {
			throw new IllegalArgumentException("Invalid constructor arguments.");
		}
		this.numIndex = numIndex;
		this.path = path;
		this.termIndex = null;
	}
	
	public IStrategoTerm getTermIndex() {
		return this.termIndex;
	}

	public int getNumIndex() {
		return numIndex;
	}

	public String getPath() {
		return path;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof NameIndex)) {
			return false;
		}
		NameIndex other = (NameIndex) obj;
		
		if(other.getNumIndex() != this.numIndex) {
			return false;
		}
		
		if(!other.getPath().equals(this.path)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(numIndex, path);
	}
	
	@Override
	public String toString() {
		return "NameIndex(" + path + ", " + numIndex + ")";
	}

}
