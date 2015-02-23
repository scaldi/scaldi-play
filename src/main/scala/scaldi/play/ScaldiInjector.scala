package scaldi.play

import play.api.inject.{BindingKey, Injector => PlayInjector}
import scaldi.Injectable.{injectWithDefault, noBindingFound}
import scaldi._

import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag

class ScaldiInjector(useCache: Boolean)(implicit inj: Injector) extends PlayInjector {
  private val cache = TrieMap[BindingKey[_], () => Any]()

  def instanceOf[T](implicit ct: ClassTag[T]) =
    instanceOf(ct.runtimeClass.asInstanceOf[Class[T]])

  def instanceOf[T](clazz: Class[T]) =
    instanceOf(BindingKey(clazz))

  def instanceOf[T](key: BindingKey[T]) =
    if (useCache)
      cache.get(key).getOrElse {
        val (actual, allowedToCache, ids) = getActualBinding(key)
        val valueFn = () => actual getOrElse noBindingFound(ids)

        if (allowedToCache)
          cache(key) = valueFn

        valueFn
      }().asInstanceOf[T]
    else {
      val (actual, _, ids) = getActualBinding(key)

      actual map (_.asInstanceOf[T]) getOrElse noBindingFound(ids)
    }

  private def getActualBinding(key: BindingKey[_]): (Option[Any], Boolean, List[Identifier]) = {
    val (_, identifiers) = ScaldiApplicationLoader.identifiersForKey(key)

    val binding = inj getBinding identifiers

    binding map (b => (b.get, b.isCacheable, identifiers)) getOrElse noBindingFound(identifiers)
  }
}