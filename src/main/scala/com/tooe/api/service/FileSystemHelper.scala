package com.tooe.api.service

import sun.misc.BASE64Decoder
import java.util.UUID
import java.io.{File, FileOutputStream}
import spray.routing.StandardRoute
import com.tooe.core.exceptions.ApplicationException

trait FileSystemHelper {
  val decoder = new BASE64Decoder()

  /**
   * Store file to temp folder and return file path
   */
  def storeFileToTmpDirectory(base64String: String): Option[String] = {
    val imgBytes = decoder.decodeBuffer(base64String)
    val fileName = UUID.randomUUID().toString
    var stream: FileOutputStream = null
    var file: File = null
    try {
      file = File.createTempFile(fileName, "")
      stream = new FileOutputStream(file)
      stream.write(imgBytes)
      stream.flush()
    }
    catch { case _: Throwable => return None }
    finally {
      if(stream != null) stream.close()
    }
    Some(file.getAbsolutePath)
  }

  def storeFile(base64String: String)(fire: String => StandardRoute): StandardRoute = {
    storeFileToTmpDirectory(base64String).map(fire).getOrElse(throw new ApplicationException(message = "Cannon write file to temporary folder"))
  }

}