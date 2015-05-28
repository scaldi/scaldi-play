package scaldi.play

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}
import play.api.Configuration
import scaldi.play.ScaldiApplicationBuilder._
import scaldi.{Injectable, Module, Injector}

class PlayConfigInjectorSpec extends WordSpec with Matchers {
  class DummyService(implicit inj: Injector) extends Injectable {
    val strProp = inject [String] (identified by "test.stringProp")
    val intProp = inject [Int] (identified by "test.intProp")

    def hi = s"Hi, str = $strProp, int = ${intProp + 1}"
  }

  "Play configuration injector" should {
    "inject strings and ints" in {
      val testModule = new Module {
        binding to new DummyService
      }

      withScaldiInj(modules = Seq(testModule), configuration = Configuration(ConfigFactory.load())) { implicit inj =>
        import Injectable._

        inject[DummyService].hi should be ("Hi, str = 123, int = 457")
      }
    }
  }


}
