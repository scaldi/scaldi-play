package scaldi.play

import condition.PlayConfigurationInjector
import play.api.{Application, GlobalSettings}
import scaldi._
import scala.Some
import scaldi.ClassIdentifier

/**
 * Adds Scaldi support to the `Global`.
 *
 * If you mix-in `ScaldiSupport` in the `Global`, then you need to implement `applicationModule` method:
 *
 * <pre class="prettyprint">
 * override def applicationModule = new MyAppModule :: new AnotherModule
 * </pre>
 *
 * Implicit `Injector` would be available in scope so you can use it in different play callbacks like `onStart`
 * and `onStop` (`ScaldiSupport` also extends `Injectable`, so you can use `inject` without any additional setup):
 *
 * <pre class="prettyprint">
 * override def onStart(app: Application) = {
 *   super.onStart(app)
 *   val service = inject [Service]
 *   ...
 * }
 * </pre>
 *
 * `ScaldiSupport` provides following pre-defined bindings:
 *
 * <ul>
 *   <li> '''playApp''' - Current play application
 *   <li> '''playMode''' - Current play application's mode (`Dev`, `Prod` or `Test`)
 *   <li> '''config''' - Current play application's configuration (all properties of configuration are also available as bindings)
 * </ul>
 */
trait ScaldiSupport extends GlobalSettings with Injectable {

  def applicationModule: Injector

  private var currentApplication: Application = _

  protected implicit lazy val applicationInjector = applicationModule :: new PlayConfigurationInjector(currentApplication) ::
    new StaticModule {
      lazy val playApp = currentApplication
      lazy val playMode = playApp.mode
      lazy val config = playApp.configuration
    }

  abstract override def onStart(app: Application) {
    super.onStart(app)

    currentApplication = app
  }

  override def getControllerInstance[A](controllerClass: Class[A]) =
    applicationInjector.getBinding(List(ClassIdentifier(controllerClass))) match {
      case Some(binding) => binding.get map (_.asInstanceOf[A]) getOrElse
        (throw new IllegalStateException("Controller for class " + controllerClass + " is explicitly un-bound!"))
      case None =>
        throw new IllegalStateException("Controller for class " + controllerClass + " not found!")
    }

}