package com.tooe.core.infrastructure

import org.springframework.context.support.GenericXmlApplicationContext
import reflect.ClassTag

object AppContextTestHelper {
  final val TestMongoContext = "classpath:META-INF/spring/test/spring-data-mongo-ctxt.xml"
  final val TestMySqlContext = "classpath:META-INF/spring/test/spring-data-mysql-ctxt.xml"

  lazy val AppContext = new GenericXmlApplicationContext(TestMongoContext)

  def lookupBean[T: ClassTag]: T = AppContext.getBean(reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]])
}
