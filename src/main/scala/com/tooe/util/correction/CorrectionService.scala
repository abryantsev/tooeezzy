package com.tooe.util.correction

import akka.actor.ActorSystem
import akka.pattern.ask
import com.tooe.api.service.{RouteContext, SuccessfulResponse, SprayServiceBaseClass2}
import spray.http.StatusCodes
import spray.routing.PathMatcher

class CorrectionService(implicit val system: ActorSystem) extends SprayServiceBaseClass2 {

  lazy val locationImageReplacer = lookup(LocationImageReplacer.Id)
  lazy val locationAlbumsAndPhotoCorrector = lookup(LocationPhotoAndAlbumCorrector.Id)

  val route = ((mainPrefix & path(PathMatcher("correction") / PathMatcher("location_images_replace"))) {
    rc: RouteContext =>
      post {
        complete(StatusCodes.Created, (locationImageReplacer ? LocationImageReplacer.FixLocationImages).mapTo[SuccessfulResponse])
      }
  }) ~ (mainPrefix & path(PathMatcher("correction") / PathMatcher("location_albums_corrector"))) {
    rc: RouteContext =>
      post {
        complete(StatusCodes.Created, (locationAlbumsAndPhotoCorrector ? LocationPhotoAndAlbumCorrector.FixLocationPhotosAndAlbums).mapTo[SuccessfulResponse])
      }
  }

}
