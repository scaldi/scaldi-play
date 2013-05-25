package scaldi.play

import languageFeature.postfixOps

import scaldi.{Condition, Injector}
import scaldi.Injectable._
import play.api.Mode._

package object condition {
  /**
   * Play application is started in Dev mode
   */
  def inDevMode(implicit inj: Injector) =
    Condition(mode == Dev)

  /**
   * Play application is started in Test mode
   */
  def inTestMode(implicit inj: Injector) =
    Condition(mode == Test)

  /**
   * Play application is started in Prod mode
   */
  def inProdMode(implicit inj: Injector) =
    Condition(mode == Prod)

  private def mode(implicit inj: Injector) =
    inject [Mode] ('playMode)
}
