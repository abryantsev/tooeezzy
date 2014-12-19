package com.tooe.core.db.graph.msg;

import java.io.Serializable;
import java.util.Collection;

public abstract class GraphIds<T> implements Serializable {

	private static final long serialVersionUID = -8696965337058693837L;

	protected final Collection<T> ids;

	protected GraphIds(Collection<T> ids) {
		this.ids = ids;
	}

	public Collection<T> getIds() {
		return ids;
	}

	@Override
	public abstract String toString();
	
}
