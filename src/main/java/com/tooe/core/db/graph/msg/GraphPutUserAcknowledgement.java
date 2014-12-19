package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.UserId;

public class GraphPutUserAcknowledgement extends GraphPutUser {

	private static final long serialVersionUID = 1163505824925570166L;

	public GraphPutUserAcknowledgement(UserId userId) {
		super(userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GraphPutUserAcknowledgement)) {
			return false;
		}
		GraphPutUserAcknowledgement that = (GraphPutUserAcknowledgement) obj;
		return this.getUserId().equals(that.getUserId());
	}
}
