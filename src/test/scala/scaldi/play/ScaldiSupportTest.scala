package scaldi.play

import org.scalatest.{WordSpec, Matchers}
import play.api.{Application, Play, GlobalSettings}
import play.api.test.FakeApplication
import scaldi.{Injectable, Module, Injector}

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

  object Global extends GlobalSettings with ScaldiSupport with Matchers with Injectable {
    var startCount: Int = 0

    override def applicationModule = new Module {
      binding toNonLazy new DummyService destroyWith(_.stop())
    }

    override def onStart(app: Application): Unit = {
      super.onStart(app)

      startCount += 1
    }
  }
}

class ScaldiSupportTest extends WordSpec with Matchers {
  import ScaldiSupportTest._

  "ScaldiSupport" should {
    "reinit with Global object" in {
      def createApp = FakeApplication(
        withGlobal = Some(Global),
        additionalConfiguration = Map(
          "play.application.loader" -> "scaldi.play.ScaldiApplicationLoader")) // TODO: improve... it looks terrible

      Global.startCount should equal(0)
      DummySevice.instanceCount should equal(0)
      DummySevice.stopCount should equal(0)

      withClue("first run") {
        val app = createApp

        Play.start(app)
        Play.stop(app)

        Global.startCount should equal(1)
        DummySevice.instanceCount should equal(1)
        DummySevice.stopCount should equal(1)
      }

      withClue("second run") {
        val app = createApp

        Play.start(app)
        Play.stop(app)

        Global.startCount should equal(2)
        DummySevice.instanceCount should equal(2)
        DummySevice.stopCount should equal(2)
      }
    }
  }
}
