package scaldi.play

import org.scalatest.{Matchers, WordSpec}
import scaldi.{Injectable, Module, Injector}
import play.api.mvc.Controller
import play.api.Environment

class ControllerInjectorTest extends WordSpec with Matchers with Injectable {

  class UserModule extends Module {
    binding to new TestController2 {
      override val name = "in user module"
    }

    bind [String] identifiedBy 'dep to "dep"

    bind [Environment] to Environment.simple()
  }

  "ControllerInjector" should {
    implicit val module = new UserModule :: new ControllerInjector

    "create controllers with implicit injector" in {
      inject[TestController1].dep should be ("dep-c1")
    }

    "use explicitly defined bindings" in {
      val c2 = inject[TestController2]

      c2.dep should be ("dep-c2")
      c2.name should be ("in user module")
    }
  }

}

class TestController1(implicit inj: Injector) extends Controller with Injectable {
  val dep = inject[String]('dep) + "-c1"
}
class TestController2(implicit inj: Injector) extends Controller with Injectable  {
  val dep = inject[String]('dep) + "-c2"
  val name = "test"
}