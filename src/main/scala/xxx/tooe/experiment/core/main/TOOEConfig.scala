package xxx.tooe.experiment.core.main

//import xxx.tooe.experiment.core.main.EnvironmentConfig

//import com.mongodb.casbah.MongoConnection
//import com.mongodb.casbah.MongoCollection
//import com.mongodb.casbah.MongoDB

case class TOOEEnvironment(mode: String)

object TOOEConfig extends EnvironmentConfig {

  var mode: TOOEEnvironment = _
  var env: String = _

//  var mongoDB: MongoDB = _
//
//  def collection(name: String): MongoCollection = {
//    mongoDB(name)
//  }

  def dev() {
    mode = new TOOEEnvironment("Development environment")
    env = "dev"
    // Connect to default - localhost, 27017
//    mongoDB = MongoConnection().apply("salat_test")
    // connect to "mongodb01" host, default port
    //val mongoConn = MongoConnection("mongodb01")
    // connect to "mongodb02" host, port 42001
    //val mongoConn = MongoConnection("mongodb02", 42001)
  }

  def test() {
    mode = new TOOEEnvironment("Integration test environment")
    env = "test"
    // Connect to default - localhost, 27017
//    mongoDB = MongoConnection().apply("test")
    // connect to "mongodb01" host, default port
    //val mongoConn = MongoConnection("mongodb01")
    // connect to "mongodb02" host, port 42001
    //val mongoConn = MongoConnection("mongodb02", 42001)
  }

  def prod() {
    mode = new TOOEEnvironment("Production environment")
    env = "prod"
    // Connect to default - localhost, 27017
//    mongoDB = MongoConnection().apply("tooe_prod")
    // connect to "mongodb01" host, default port
    //val mongoConn = MongoConnection("mongodb01")
    // connect to "mongodb02" host, port 42001
    //val mongoConn = MongoConnection("mongodb02", 42001)
  }

}