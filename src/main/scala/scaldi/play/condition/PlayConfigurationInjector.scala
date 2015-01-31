package scaldi.play.condition

import scaldi.RawInjector
import play.api.Application

/**
 * Play configuration Injector.
 */
@deprecated("Use TypesafeConfigInjector instead", "31.01.2015")
class PlayConfigurationInjector(app: => Application) extends RawInjector {
  def getRawValue(name: String) = app.configuration.getString(name)
}
