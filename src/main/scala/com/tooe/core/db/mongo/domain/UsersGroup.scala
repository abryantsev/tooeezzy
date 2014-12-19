package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.annotation.Id
import com.tooe.core.domain.UsersGroupId


@Document( collection = "usersgroup" )
case class UsersGroup(
    id: UsersGroupId,
    name : ObjectMap[String]
)