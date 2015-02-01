package scaldi.play

import play.api.inject.{BindingKey, Injector => PlayInjector}
import scaldi.Injectable.{injectWithDefault, noBindingFound}
import scaldi._

import scala.reflect.ClassTag

class ScaldiInjector(implicit inj: Injector) extends PlayInjector {
  def instanceOf[T](implicit ct: ClassTag[T]) =
    instanceOf(ct.runtimeClass.asInstanceOf[Class[T]])

  def instanceOf[T](clazz: Class[T]) =
    instanceOf(BindingKey(clazz))

  def instanceOf[T](key: BindingKey[T]) = {
    val (_, identifiers) = ScaldiApplicationLoader.identifiersForKey(key)

    injectWithDefault[T](inj, noBindingFound(identifiers))(identifiers)
  }
}