package xxx.tooe.experiment.core.main

import collection.mutable
import reflect.{ClassTag, classTag}

private object ConfigurationStore {
  val entries = mutable.Map[String, AnyRef]()

  def put(key: String, value: AnyRef) {
    entries += ((key, value))
  }

  def get[A: ClassTag] = {
    val erasure = classTag[A].runtimeClass
    entries.values.find(x => erasure.isAssignableFrom(x.getClass)) match {
      case Some(v) => Some(v.asInstanceOf[A])
      case None => None
    }
  }
}

trait Configured {

  def configured[A: Manifest, U](f: A => U) = f(ConfigurationStore.get[A].get)

  def configured[A: Manifest] = ConfigurationStore.get[A].get

}

trait Configuration {

  final def configure[A <: AnyRef](tag: String)(f: => A) = {
    val a = f
    ConfigurationStore.put(tag, a)
    a
  }

  final def configure[A <: AnyRef](f: => A) = {
    val a = f
    ConfigurationStore.put(a.getClass.getName, a)
    a
  }

}