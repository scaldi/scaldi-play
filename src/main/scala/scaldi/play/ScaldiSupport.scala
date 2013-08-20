package scaldi.play

import condition.PlayConfigurationInjector
import play.api.{Application, GlobalSettings}
import scaldi._
import scala.Some
import scaldi.ClassIdentifier

/**
 * Adds Scaldi support to the GlobalSettings
 *
 * @author Oleg Ilyenko
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
