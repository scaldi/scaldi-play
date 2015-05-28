package scaldi.play

import com.typesafe.config.{ConfigFactory, Config}
import org.scalatest.{Matchers, WordSpec}
import play.api.test.FakeApplication
import play.api.{Configuration, Play, Application, GlobalSettings}
import scaldi.{Injectable, Module, Injector}
import play.api.test.Helpers._

class PlayConfigInjectorSpec extends WordSpec with Matchers {
  class DummyService(implicit inj: Injector) extends Injectable {
    val strProp = inject [String] (identified by "test.stringProp")
    val intProp = inject [Int] (identified by "test.intProp")

    def hi = s"Hi, str = $strProp, int = ${intProp + 1}"
  }

  "Play configuration injector" should {
    "inject strings and ints" in {
      val global = new ScaldiSupport {
        override def applicationModule: Injector = new Module {
          binding to new DummyService
        }

        override def configuration = Configuration(ConfigFactory.load())
      }

      running(FakeApplication(withGlobal = Some(global))) {
        import Injectable._
        implicit val injector = global.injector

        inject[DummyService].hi should be ("Hi, str = 123, int = 457")
      }
    }
  }


}
