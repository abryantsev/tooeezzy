package com.tooe.core.db.graph.msg;

import com.tooe.core.domain.UserId;

public abstract class GraphSearchLocations extends GraphGetForUser {

	private static final long serialVersionUID = 1404679755582483155L;

	protected final boolean inCheckinLocations;
	protected final boolean byFriends;
	protected final String category;
	protected final String name;
	protected final boolean hasPromotions;
	
	protected GraphSearchLocations(UserId userId, boolean inCheckinLocations,
			boolean byFriends, String category, String name, boolean hasPromotions) {
		super(userId);
		this.inCheckinLocations = inCheckinLocations;
		this.byFriends = byFriends;
		this.category = category;
		this.name = name;
		this.hasPromotions = hasPromotions;
	}

	public boolean inCheckinLocations() {
		return inCheckinLocations;
	}

	public boolean byFriends() {
		return byFriends;
	}

	public String getCategory() {
		return category;
	}

	public String getName() {
		return name;
	}

	public boolean hasPromotions() {
		return hasPromotions;
	}

	@Override
	public abstract String toString();
}
