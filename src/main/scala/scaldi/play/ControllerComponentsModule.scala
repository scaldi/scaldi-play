package scaldi.play

import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc.{ControllerComponents, DefaultActionBuilder, DefaultControllerComponents, PlayBodyParsers}
import scaldi.Module

import scala.concurrent.ExecutionContext

/**
  * Created by dsarosi on 30/6/2017.
  */
class ControllerComponentsModule extends Module {
  bind[ControllerComponents] to DefaultControllerComponents(
    inject[DefaultActionBuilder],
    inject[PlayBodyParsers],
    inject[MessagesApi],
    inject[Langs],
    inject[FileMimeTypes],
    inject[ExecutionContext]
  )
}
