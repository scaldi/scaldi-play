package scaldi.play

import play.utils.Threads
import play.api.inject.{BindingKey, Injector => PlayInjector}
import scaldi.Injectable.noBindingFound
import scaldi._

import scala.collection.concurrent.TrieMap
import scala.reflect.ClassTag

class ScaldiInjector(useCache: Boolean, classLoader: ClassLoader)(implicit inj: Injector) extends PlayInjector {
  private val cache = TrieMap[BindingKey[_], () => Any]()

  def instanceOf[T](implicit ct: ClassTag[T]): T =
    instanceOf(ct.runtimeClass.asInstanceOf[Class[T]])

  def instanceOf[T](clazz: Class[T]): T =
    instanceOf(BindingKey(clazz))

  def instanceOf[T](key: BindingKey[T]): T =
    if (useCache) {
      cache.getOrElse(key, {
        val (actual, allowedToCache, ids) = getActualBinding(key)
        val valueFn = () => actual getOrElse noBindingFound(ids)

        if (allowedToCache)
          cache(key) = valueFn

        valueFn
      })().asInstanceOf[T]
    } else {
      val (actual, _, ids) = getActualBinding(key)

      actual map (_.asInstanceOf[T]) getOrElse noBindingFound(ids)
    }

  private def getActualBinding(key: BindingKey[_]): (Option[Any], Boolean, List[Identifier]) =
    Threads.withContextClassLoader(classLoader) {
      val (_, identifiers) = ScaldiBuilder.identifiersForKey(key)
      val binding = inj getBinding identifiers

      binding map (b => (b.get, b.isCacheable, identifiers)) getOrElse noBindingFound(identifiers)
    }
}
