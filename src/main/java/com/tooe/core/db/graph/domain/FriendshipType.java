package com.tooe.core.db.graph.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.Transformer;

public enum FriendshipType {
	FRIEND, 
	MYFRIEND, 
	BESTFRIEND, 
	FAMILY, 
	COLLEAGUE;
	
	public static FriendshipType[] getFriendshipTypes(String... ftypes){
		FriendshipType[] friendshipTypes = new FriendshipType[ftypes.length];
		for(int i=0; i<ftypes.length; i++) {
			friendshipTypes[i] = FriendshipType.valueOf(ftypes[i]);
		}
		return friendshipTypes;
	}	
	
	public static FriendshipType[] usergroupValues() {
		List<FriendshipType> uValues = ListUtils.removeAll(Arrays.asList(FriendshipType.values()), Arrays.asList(FRIEND));
		return uValues.toArray(new FriendshipType[uValues.size()]);
	}
	
//	public static String[] usergroupNames() {
//		List<FriendshipType> uValues = ListUtils.removeAll(Arrays.asList(FriendshipType.values()), Arrays.asList(FRIEND));
//		CollectionUtils.transform(uValues, new Transformer() {
//			@Override
//			public Object transform(Object input) {
//				return ((FriendshipType)input).name();
//			}
//		});
//		return uValues.toArray(new String[uValues.size()]);
//	}

//	public static String[] allUsergroupNames() {
//		List<FriendshipType> uValues = new ArrayList<FriendshipType>();
//		uValues.addAll(Arrays.asList(FriendshipType.values())); //some strange problems with transformation of Arrays.ArrayList<E>
//		CollectionUtils.transform(uValues, new Transformer() {
//			@Override
//			public Object transform(Object input) {
//				return ((FriendshipType)input).name();
//			}
//		});
//		return uValues.toArray(new String[uValues.size()]);
//	}

	public static String[] getUsergroupNames(FriendshipType... ftypes) {
		List<FriendshipType> uValues = new ArrayList<FriendshipType>();
		uValues.addAll(Arrays.asList(ftypes)); //some strange problems with transformation of Arrays.ArrayList<E>
		CollectionUtils.transform(uValues, new Transformer() {
			@Override
			public Object transform(Object input) {
				return ((FriendshipType)input).name();
			}
		});
		return uValues.toArray(new String[uValues.size()]);
	}	
}
