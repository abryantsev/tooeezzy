package com.tooe.core.db.mongo.converters

object DBObjectConverters 
  extends IdentityConverters
  with BasicConverters
  with IdConverters
{
  final val IdField = "_id"

}