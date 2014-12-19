package com.tooe.core.db.graph.msg;

import java.util.Collection;

import com.tooe.core.domain.UserId;
import org.apache.commons.collections.CollectionUtils;

public class GraphFriends extends GraphIds<UserId> implements GraphFriendsIds {

	private static final long serialVersionUID = -681022420735944249L;

	public GraphFriends(Collection<UserId> friendsIds) {
		super(friendsIds);
	}

	@Override
	public Collection<UserId> getFriends() {
		return this.ids;
	}


	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" {friendsIds=" + ids.toString() + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GraphFriends)) {
			return false;
		}
		GraphFriends that = (GraphFriends) obj;
		return CollectionUtils.isEqualCollection(this.getFriends(), that.getFriends());		
	}

}
