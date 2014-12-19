package com.tooe.core.usecase

import com.tooe.core.application.Actors
import com.tooe.core.domain.{MediaUrl, UserId, WishId}
import com.tooe.core.db.mongo.domain.{User, WishLike}
import com.tooe.core.usecase.wish.WishLikeDataActor.{CountLikes, SelectWishLikes}
import com.tooe.core.usecase.wish.WishLikeDataActor
import com.tooe.api._
import com.tooe.core.usecase.WishLikeReadActor.GetLikes
import com.tooe.core.usecase.user.UserDataActor
import com.tooe.core.usecase.user.UserDataActor.GetUsers
import com.tooe.api.service.{ExecutionContextProvider, SuccessfulResponse, OffsetLimit}
import com.tooe.core.util.Images

object WishLikeReadActor {
  final val Id = Actors.WishLikeRead

  case class GetLikes(currentUserId: UserId, wishId: WishId, offsetLimit: OffsetLimit)
}

class WishLikeReadActor extends AppActor with ExecutionContextProvider {

  lazy val wishLikeDataActor = lookup(WishLikeDataActor.Id)
  lazy val userDataActor = lookup(UserDataActor.Id)

  def receive = {
    case GetLikes(currentUserId, wishId, offsetLimit) => {for {
        wishLikes <- (wishLikeDataActor ? SelectWishLikes(wishId, offsetLimit)).mapTo[Seq[WishLike]]
        likers <- (userDataActor ? GetUsers(wishLikes.map(_.userId))).mapTo[Seq[User]].map(s => s.map(u => LikeAuthorItem(u)))
        likesQty <- (wishLikeDataActor ? CountLikes(wishId)).mapTo[Long]
      }yield {
        val selfLiked = likers.map(_.id).contains(currentUserId)
        val responseSelfLiked = if (selfLiked) Some(selfLiked) else None
                                          
        GetWishLikesResponse(likesQty, 
                             responseSelfLiked,
                             likers map(GetResponseWishLikeItem(_)))
      }
    } pipeTo sender
  }
}

case class GetWishLikesResponse(@JsonProp("likescount")likesCount: Long,
                                @JsonProp("selfliked")selfLiked: Option[Boolean],
                                @JsonProp likes: Seq[GetResponseWishLikeItem]) extends SuccessfulResponse

case class GetResponseWishLikeItem(author: LikeAuthorItem)

case class LikeAuthorItem(id: UserId,
                          name: String,
                          @JsonProp("lastname") lastName: String,
                          media: MediaUrl)

case object LikeAuthorItem{
  def apply (liker: User): LikeAuthorItem = {
    LikeAuthorItem(id = liker.id,
                   name = liker.name,
                   lastName = liker.lastName,
                   media = liker.getMainUserMediaUrl(Images.Wishlikes.Full.Author.Media))
  }
}