package com.tooe.core.db.graph.data;

import org.bson.types.ObjectId;

public class GraphTestData implements GraphData {

  public final static String[] vertsUserIds = { "iam", "he", "she" };
  public final static String[] vertsUserKeys = { "type", "objectid", "name" };  
  public final static Object[][] vertsUserData =  
    { {"user", "51197909444fddfecf2a668f" },
      {"user", "51197909444fddfecf2a6690" },
      {"user", "51197909444fddfecf2a6691" } };

  public final static String[] vertsLocIds = { "restaurant", "saloon", "hotel", "fitness", "cinema", "shop" };
  public final static String[] vertsLocKeys = { "type", "objectid", "lon", "lat", "promotions", "city", "cafe",
                                  "rest", "beauty", "children", "courses", "shops", "flowers",
                                  "service", "name" };  
  public final static Object[][] vertsLocData =  
    { {"location", "51197909444fddfecf2a6692", 40.739037, 73.992964, true, ObjectId.get().toString(), true,
       true, true, true, true, true, true, true },
      {"location", "51197909444fddfecf2a6693", 41.739037, 70.992964, null, ObjectId.get().toString(), null,
       null, null, null, null, null, null, null },
      {"location", "51197909444fddfecf2a6694", 38.739037, 71.992964, null, ObjectId.get().toString(), true,
       null, true, null, true, null, null, null },
      {"location", "51197909444fddfecf2a6695", 39.739037, 72.992964, null, ObjectId.get().toString(), null,
       null, true, null, null, null, true, true },
      {"location", "51197909444fddfecf2a6696", 39.739037, 72.992964, null, ObjectId.get().toString(), null,
       true, null, null, true, null, true, null },
      {"location", "51197909444fddfecf2a6697", 37.739037, 69.992964, true, ObjectId.get().toString(), true,
       null, null, null, null, null, true, true } };
  
  public final static String[] edgesUserIds = { "iam-he", "he-iam", "he-she", "she-he" };
  public final static String[] edgesUserKeys = { "friends", "family" };  
  public final static Object[][] edgesUserData =  
    { { true, null }, 
      { null, true },
      { true, true },
      { null, null } };

  public final static String[] edgesLocIds = { "iam-restaurant", "iam-saloon", "iam-fitness", "she-fitness",
                                 "she-shop", "she-cinema", "he-cinema", "he-restaurant"};
  public final static String[] edgesLocKeys = { "favorite", "checkin" };  
  public final static Object[][] edgesLocData =  
    { { true, true }, 
      { null, true },
      { null, true },
      { true, true },
      { true, null },
      { true, null },
      { true, null },
      { null, true } };	
}
