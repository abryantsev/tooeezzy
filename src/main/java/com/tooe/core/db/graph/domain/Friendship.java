package com.tooe.core.db.graph.domain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tinkerpop.blueprints.Vertex;
import com.tooe.core.domain.UserId;
import org.bson.types.ObjectId;

public class Friendship {

	private Vertex userA;

	private Vertex userB;
	
	private Set<FriendshipType> usergroups;

	public Friendship(Vertex userA, Vertex userB) {
		this.userA = userA;
		this.userB = userB;
		this.usergroups = new HashSet<FriendshipType>();
	}
	
	public Friendship(Vertex userA, Vertex userB, Set<FriendshipType> usergroups) {
		this.userA = userA;
		this.userB = userB;
		this.usergroups = usergroups;
	}

	public Vertex getUserA() {
		return userA;
	}

	public Vertex getUserB() {
		return userB;
	}

	public void addUsergroup(FriendshipType usergroup) {
		usergroups.add(usergroup);
	}
	
	public Set<FriendshipType> getUsergroups() {
		return usergroups;
	}

	public Set<String> getUsergroupsNames() {
		Set<String> names = new HashSet<String>();
		for(FriendshipType ftype : usergroups) {
			names.add(ftype.name());
		}
		return names;
	}

	public List<UserId> getFriendsIds() {
		return Arrays.asList(new UserId(new ObjectId(userA.getProperty(UserProps.OBJECTID.name()).toString())),
                new UserId(new ObjectId(userB.getProperty(UserProps.OBJECTID.name()).toString())));
	}

}
