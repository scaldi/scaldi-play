package scaldi.play

import play.api.Mode

import languageFeature.postfixOps

import scaldi.{Identifier, Condition, Injector}
import scaldi.Injectable._
import play.api.Mode._

/**
 * Provides some Play-specific conditions that can be used in the mappings:
 *
 * <pre class="stHighlight">
 * bind [MessageService] when (inDevMode or inTestMode) to new SimpleMessageService
 * bind [MessageService] when inProdMode to new OfficialMessageService
 * </pre>
 */
package object condition {
  /**
   * Play application is started in Dev mode
   */
  def inDevMode(implicit inj: Injector) = ModeCondition(Dev)

  /**
   * Play application is started in Test mode
   */
  def inTestMode(implicit inj: Injector) = ModeCondition(Test)

  /**
   * Play application is started in Prod mode
   */
  def inProdMode(implicit inj: Injector) = ModeCondition(Prod)

  case class ModeCondition(mode: Mode)(implicit inj: Injector) extends Condition {
    lazy val m = inject [Mode] ('playMode)

    override def satisfies(identifiers: List[Identifier]) = m == mode
    override val dynamic = false
  }
}
