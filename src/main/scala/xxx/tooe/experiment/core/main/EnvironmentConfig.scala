package xxx.tooe.experiment.core.main

object EnvironmentConfig extends Enumeration {
	
  type Environment = Value
  val Dev, Test, Prod = Value

}

trait EnvironmentConfig {

  import EnvironmentConfig._

  def load(env: Environment) {
    env match {
      case Dev =>
        dev()
      case Test =>
        test()
      case Prod =>
        prod()
    }
  }

  def dev()

  def test()

  def prod()
}
//
//trait EnvironmentConfig {
//
//  import EnvironmentConfig._
//
//  def getByName(envName: String) {
//    envName match {
//      case "Dev" =>
//        Dev
//      case "Test" =>
//        Test
//      case "Prod" =>
//        Prod
//    }
//  }
//
//  def load(env: Environment) {
//    env match {
//      case Dev =>
//        dev()
//      case Test =>
//        test()
//      case Prod =>
//        prod()
//    }
//  }
//
//  def dev()
//
//  def test()
//
//  def prod()
//}