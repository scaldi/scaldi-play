package scaldi.play

import play.api.GlobalSettings
import scaldi.Injector

trait ScaldiSupport extends GlobalSettings {
  def applicationModule: Injector
}
