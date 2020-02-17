package scaldi.play

import org.scalatest.{Matchers, WordSpec}
import ScaldiApplicationBuilder._
import play.api.Environment
import scaldi.Module
import scaldi.play.condition._
import scaldi.Injectable._
import play.api.Mode._

class PlayConditionSpec extends WordSpec with Matchers {

  "Play Condition" should {
    "inject bindings based on the correct mode" in {
      class TestModule extends Module {
        bind [String] identifiedBy Symbol("test") when inTestMode to "test"
        bind [String] identifiedBy Symbol("test") when inDevMode to "dev"
        bind [String] identifiedBy Symbol("test") when inProdMode to "prod"
      }

      withScaldiInj(modules = Seq(new TestModule), environment = Environment.simple(mode = Test)) { implicit inj =>
        inject[String](Symbol("test")) should be ("test")
      }

      withScaldiInj(modules = Seq(new TestModule), environment = Environment.simple(mode = Dev)) { implicit inj =>
        inject[String](Symbol("test")) should be ("dev")
      }

      withScaldiInj(modules = Seq(new TestModule), environment = Environment.simple(mode = Prod)) { implicit inj =>
        inject[String](Symbol("test")) should be ("prod")
      }
    }

    "not initialize eager bindings id false" in {
      var testInit = false
      var devInit = false
      var prodInit = false

      class TestModule extends Module {
        bind [String] identifiedBy Symbol("test") when inTestMode toNonLazy "test" initWith (_ => testInit = true)
        bind [String] identifiedBy Symbol("test") when inDevMode toNonLazy "dev" initWith (_ => devInit = true)
        bind [String] identifiedBy Symbol("test") when inProdMode toNonLazy "prod" initWith (_ => prodInit = true)
      }

      withScaldiApp(modules = Seq(new TestModule), environment = Environment.simple(mode = Dev)) {
        testInit should be (false)
        devInit should be (true)
        prodInit should be (false)
      }
    }
  }

}
