package com.tooe.core.db.mongo.domain

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.annotation.Id
import java.util.Date

// --- TESTS with mongoDB --- !!! keep for tests here

case class Account(name:String, username:String, password:String)

@Document( collection = "person" )
case class Person(@Id _id: ObjectId, name:String, username:String, account:Account)

@Document( collection = "basic_user" )
case class BasicUser(@Id _id: ObjectId, uname:String, age:Int)



