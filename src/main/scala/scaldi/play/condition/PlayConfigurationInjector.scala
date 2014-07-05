package scaldi.play.condition

import scaldi.RawInjector
import play.api.Application

/**
 * Play configuration Injector.
 */
class PlayConfigurationInjector(app: => Application) extends RawInjector {
  def getRawValue(name: String) = app.configuration.getString(name)
}
