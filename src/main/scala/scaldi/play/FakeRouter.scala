package scaldi.play

import play.api.inject.RoutesProvider
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router
import play.api.routing.Router.Routes
import scaldi.Module

import scala.runtime.AbstractPartialFunction

class FakeRouterModule(fakeRoutes: PartialFunction[(String, String), Handler]) extends Module {
  bind [Router] to new FakeRouter(inject[RoutesProvider].get)(fakeRoutes)
}

object FakeRouterModule {
  def apply(fakeRoutes: PartialFunction[(String, String), Handler]) = new FakeRouterModule(fakeRoutes)
}

class FakeRouter(fallback: Router)(fakeRoutes: PartialFunction[(String, String), Handler]) extends Router {
  val routes: Routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
      fakeRoutes.applyOrElse((rh.method, rh.path), (_: (String, String)) => default(rh))
    def isDefinedAt(rh: RequestHeader) = fakeRoutes.isDefinedAt((rh.method, rh.path))
  } orElse new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) =
      fallback.routes.applyOrElse(rh, default)
    def isDefinedAt(x: RequestHeader) = fallback.routes.isDefinedAt(x)
  }

  def documentation: Seq[(String, String, String)] = fallback.documentation
  def withPrefix(prefix: String) = new FakeRouter(fallback.withPrefix(prefix))(fakeRoutes)
}
