package com.tooe.core.db.graph.msg;

import java.util.Collection;
import java.util.Collections;

import com.tooe.core.db.graph.domain.FriendshipType;
import com.tooe.core.domain.UserId;
import org.apache.commons.collections.CollectionUtils;

public class GraphFriendship extends GraphIds<UserId> implements GraphFriendsIds {

	private static final long serialVersionUID = -2338909763949749740L;

	protected final Collection<FriendshipType> usergroups;
	
	public GraphFriendship(Collection<UserId> friendsIds) {
		super(friendsIds);
		this.usergroups = Collections.emptySet();
	}

	public GraphFriendship(Collection<UserId> friendsIds, Collection<FriendshipType> usergroups) {
		super(friendsIds);
		this.usergroups = usergroups;
	}

	@Override
	public Collection<UserId> getFriends() {
		return this.ids;
	}


	public Collection<FriendshipType> getUsergroups() {
		return this.usergroups;
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+" {friendsIds=" + ids.toString() + ", usergroups=" + usergroups.toString() + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GraphFriendship)) {
			return false;
		}
		GraphFriendship that = (GraphFriendship) obj;
		return CollectionUtils.isEqualCollection(this.getFriends(), that.getFriends()) 
				&& CollectionUtils.isEqualCollection(this.getUsergroups(), that.getUsergroups());		
	}

}
