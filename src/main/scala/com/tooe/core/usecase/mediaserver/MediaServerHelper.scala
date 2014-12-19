package com.tooe.core.usecase

import org.apache.http.HttpEntity
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpPost
import java.io.{BufferedReader, InputStreamReader}
import spray.json._
import com.tooe.core.util.ActorHelper
import com.fasterxml.jackson.annotation.JsonProperty
import org.bson.types.ObjectId
import com.tooe.core.main.SharedActorSystem
import com.tooe.extensions.scala.Settings

trait MediaServerHelper {

  self: ActorHelper =>

  private val mediaServerUrl = settings.MediaServer.Host

  private def getMediaServerResponse(entity: HttpEntity, imageType: ImageType.Value) = {
      val httpClient: HttpClient = new DefaultHttpClient
      val httpPost = new HttpPost(getHostUrl(imageType))
      httpPost.setEntity(entity)
      var responseString: String = ""
      var is: InputStreamReader = null
      try {
        val result = httpClient.execute(httpPost)
        is = new InputStreamReader(result.getEntity.getContent)
        val br: BufferedReader = new BufferedReader(is)
        responseString = br.readLine()
      }
      finally {
        if(is != null) try { is.close() }
        httpPost.releaseConnection()
      }
      responseString
  }

  def getMediaServerJSONResponse(entity: HttpEntity, imageType: ImageType.Value) = {
    getMediaServerResponse(entity, imageType).asJson
  }

  def getHostUrl(imageType: ImageType.Value) = {
    s"${mediaServerUrl}${settings.MediaServer.getUrlSuffix(imageType.toString)}"
  }

}

case class ActionParams(filename: String, @JsonProperty("imgtype") imageType: String,width: Int,height: Int, @JsonProperty("userid") userId: String,  exactly: Boolean = false)

case class Action(action: String, params: ActionParams)

case class ImageSize(height: Int, width: Int)

case class ImageInfo(name: String, imageType: ImageType.Value, ownerId: ObjectId)

case class AccessCode(@JsonProperty("accesscode") accessCode: String)

object ImageType extends Enumeration {
  val photo = Value("photo")
  val avatar = Value("avatar")
  val banner = Value("banner")
  val locationPhoto = Value("locationPhoto")
  val product = Value("product")
  val location = Value("location")
  val company = Value("company")
}


trait MediaServerTimeout {
  implicit val timeout = Settings(SharedActorSystem.sharedMainActorSystem).MediaServer.DEFAULT_TIMEOUT
}