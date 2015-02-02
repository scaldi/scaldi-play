package scaldi.play

import org.scalatest.{WordSpec, Matchers}
import play.api.{Application, Play, GlobalSettings}
import play.api.test.FakeApplication
import scaldi.{Module, Injector}
import play.api.test.Helpers._

object ScaldiSupportTest {
  object DummySevice {
    var instanceCount: Int = 0
    var stopCount: Int = 0
  }

  class DummyService {
    import DummySevice._

    instanceCount += 1

    var stopped: Boolean = false

    def hi: String = {
      if (!stopped) {
        "hello"
      } else {
        "stopped"
      }
    }

    def stop() {
      stopCount += 1
      stopped = true
    }
  }

  object Global extends GlobalSettings with ScaldiSupport with Matchers {
    var startCount: Int = 0
    var stopCount: Int = 0

    override def applicationModule: Injector = new Module {
      binding to new DummyService destroyWith(_.stop())
    }

    override def onStart(app: Application): Unit = {
      super.onStart(app)

      startCount += 1

      inject[DummyService].hi should equal("hello")
    }

    override def onStop(app: Application) = {
      super.onStop(app)

      stopCount += 1
    }
  }
}

class ScaldiSupportTest extends WordSpec with Matchers {
  import ScaldiSupportTest._

  "ScaldiSupport" should {
    "reinit with Global object" in {
      val app = FakeApplication(
        withGlobal = Some(Global)
      )

      Global.startCount should equal (0)
      Global.stopCount should equal (0)
      DummySevice.instanceCount should equal (0)
      DummySevice.stopCount should equal (0)

      withClue("first run") {
        running(app) {/* do nothing */}

        Global.startCount should equal (1)
        Global.stopCount should equal (1)
        DummySevice.instanceCount should equal (1)
        DummySevice.stopCount should equal (1)
      }


      withClue("second run") {
        running(app) {/* do nothing */}

        Global.startCount should equal (2)
        Global.stopCount should equal (2)
        DummySevice.instanceCount should equal (2)
        DummySevice.stopCount should equal (2)
      }
    }
  }
}
