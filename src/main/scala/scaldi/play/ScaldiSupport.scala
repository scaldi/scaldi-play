package scaldi.play

import play.api.GlobalSettings
import scaldi.Injector

@deprecated("GlobalSettings is deprecated so is ScaldiSupport trait", "02.02.2015")
trait ScaldiSupport extends GlobalSettings {
  def applicationModule: Injector
}
