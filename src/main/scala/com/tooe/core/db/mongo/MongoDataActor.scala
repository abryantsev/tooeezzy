package com.tooe.core.db.mongo

import com.tooe.core.infrastructure.BeanLookup
import domain._
import akka.actor.Actor
import akka.event.Logging
import akka.util.Timeout
import org.bson.types.ObjectId
import repository.BasicUserRepository
import scala.collection.JavaConverters._

case class GetBasic()

case class SaveBasic()
case class GetOne(id: ObjectId)
case class ReturnOne(user: BasicUser)

case class ListBasicUsers(firstResult: Int = 0, maxResults: Int = Int.MaxValue)

class MongoDataActor extends Actor {
  implicit val timeout = Timeout(1000)

  val log = Logging(context.system, this)

  def receive = {
    case GetBasic() =>
      log.info("GetBasic (in MongoDataActor)........................."+ Thread.currentThread().toString())
      val repo = BeanLookup[BasicUserRepository]
      val count = repo.count()
      log.info("count........................."+count)
      val bus = repo.findAll()
      log.info("bus........................."+bus)
      val petjas = repo.findByUname("petja")
      log.info("petjas........................."+petjas.size())
      val mashas = repo.findBasicUsersWithName("masha")
      log.info("mashas........................."+mashas.size())
      val age = repo.findBasicUsersWithArgs("masha", 18, 22)
      log.info("age........................."+age.size())
      
    case GetOne(id) =>
      log.info("GetOne (in MongoDataActor)........................."+ Thread.currentThread().toString())
      val repo = BeanLookup[BasicUserRepository]
      val user = repo.findOne(id)
      log.info("user........................."+user)
      sender ! ReturnOne(user)
    
    case ListBasicUsers(firstResult, maxResults) =>
      log.info("ListBasicUsers (in MongoDataActor)........................."+ Thread.currentThread().toString())
      val repo = BeanLookup[BasicUserRepository]
      val bus = repo.findAll()
      log.info("bus........................."+bus)
      sender ! bus.asScala.toList
      
    case SaveBasic() =>
      log.info("SaveBasic (in MongoDataActor)........................."+ Thread.currentThread().toString())
      val repo = BeanLookup[BasicUserRepository]
      repo.save(new BasicUser(new ObjectId(), "kolja", 22))
      repo.save(new BasicUser(new ObjectId(), "petja", 25))
      val count = repo.count()
      log.info("count........................."+count)
  }
}