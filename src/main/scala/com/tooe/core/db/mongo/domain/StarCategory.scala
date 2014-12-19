package com.tooe.core.db.mongo.domain

import org.springframework.data.mongodb.core.mapping.Document
import com.tooe.core.domain.StarCategoryId

@Document(collection = "star_category")
case class StarCategory(id: StarCategoryId,
                        name: ObjectMap[String] = ObjectMap.empty,
                        description: ObjectMap[String] = ObjectMap.empty,
                        categoryMedia: String,
                        starsCount: Int,
                        parentId: Option[StarCategoryId] = None)