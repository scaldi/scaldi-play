package scaldi.play

import play.api.GlobalSettings
import scaldi.Injector

@deprecated("GlobalSettings is deprecated so is ScaldiSupport trait", "2015-02-02")
trait ScaldiSupport extends GlobalSettings {
  def applicationModule: Injector
}

@deprecated("GlobalSettings is deprecated so is ScaldiSupport trait", "2015-02-02")
object ScaldiSupport {
  private[play] var currentInj: Option[Injector] = None

  @deprecated("`currentInjector` was introduced **only for backwards compatibility purposes**. It would be removed as soon as plugins stop using `Play.current`.", "2015-04-25")
  implicit def currentInjector: Injector = currentInj getOrElse sys.error("There is no injector available for the application. You are probably accessing it outside of the current application's scope.")
}
