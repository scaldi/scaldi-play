package scaldi.play

import condition.PlayConfigurationInjector
import play.api.{Application, GlobalSettings}
import scaldi.{StaticModule, DynamicModule, Injector, ClassIdentifier}

/**
 * Adds Scaldi support to the GlobalSettings
 *
 * @author Oleg Ilyenko
 */
trait ScaldiSupport extends GlobalSettings {

  def applicationModule: Injector

  private var currentApplication: Application = _

  private lazy val appInjector = applicationModule :: new PlayConfigurationInjector(currentApplication) ::
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
    appInjector.getBinding(List(ClassIdentifier(controllerClass))) match {
      case Some(binding) => binding.get map (_.asInstanceOf[A]) getOrElse
        (throw new IllegalStateException("Controller for class " + controllerClass + " is explicitly un-bound!"))
      case None =>
        throw new IllegalStateException("Controller for class " + controllerClass + " not found!")
    }

}
