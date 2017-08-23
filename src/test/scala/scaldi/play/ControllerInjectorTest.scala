package scaldi.play

import org.scalatest.{Matchers, WordSpec}
import scaldi.{Injectable, Injector, Module}
import play.api.mvc._
import play.api.Environment
import play.api.Mode.Test
import scaldi.Injectable.inject
import scaldi.play.ScaldiApplicationBuilder.withScaldiInj

class ControllerInjectorTest extends WordSpec with Matchers with Injectable {

  class UserModule extends Module {

    binding to new TestInjectedController2 {
      override val name = "in user module"
    }

    bind [String] identifiedBy 'dep to "dep"

    bind [Environment] to Environment.simple()
  }

  "ControllerInjector" should {

    withScaldiInj(modules = Seq(new UserModule, new ControllerComponentsModule, new ControllerInjector), environment = Environment.simple(mode = Test)) { implicit inj ⇒
      "create injected controllers with implicit injector" in {
        inject[TestInjectedController1].dep should be ("dep-ic1")
      }

      "use explicitly defined bindings for injected controllers" in {
        val c2 = inject[TestInjectedController2]

        c2.dep should be ("dep-ic2")
        c2.name should be ("in user module")
      }

      "create abstract controllers with implicit injector" in {
        inject[TestAbstractController1].dep should be ("dep-ac1")
      }

    }
  }

}

class TestInjectedController1(implicit inj: Injector) extends InjectedController with Injectable {
  val dep = inject[String]('dep) + "-ic1"
}
class TestInjectedController2(implicit inj: Injector) extends InjectedController with Injectable  {
  val dep = inject[String]('dep) + "-ic2"
  val name = "test"
}

class TestAbstractController1(implicit inj: Injector, controllerComponents: ControllerComponents) extends AbstractController(controllerComponents) with Injectable {
  val dep = inject[String]('dep) + "-ac1"
}