package scaldi.play

import play.api.Environment
import play.api.mvc._
import scaldi._
import scaldi.util.ReflectionHelper._

import java.lang.reflect.InvocationTargetException
import scala.reflect.runtime.universe.{runtimeMirror, typeTag, Type}

/** <p>Injector for the Play applications that creates controller bindings on the fly. The preferred way to use it is by
  * adding it to the module composition at the very end, so that it would be possible to override default instantiation
  * strategy in user-defined modules.
  *
  * <p>Here is an example:
  *
  * <pre class="stHighlight"> object Global extends GlobalSettings with ScaldiSupport { def applicationModule = new
  * UserModule :: new DbModule :: new ControllerInjector } </pre>
  */
class ControllerInjector
    extends MutableInjectorUser
    with InjectorWithLifecycle[ControllerInjector]
    with ShutdownHookLifecycleManager {

  private var bindings: Set[(List[Identifier], Option[BindingWithLifecycle])] = Set.empty

  def getBindingInternal(identifiers: List[Identifier]): Option[BindingWithLifecycle] = identifiers match {
    case TypeTagIdentifier(tpe) :: Nil
        if (tpe safe_<:< typeTag[InjectedController].tpe) || (tpe safe_<:< typeTag[AbstractController].tpe) =>
      bindings.find(b => Identifier.sameAs(b._1, identifiers)) map (_._2) getOrElse {
        this.synchronized {
          bindings.find(b => Identifier.sameAs(b._1, identifiers)) map (_._2) getOrElse {
            val binding = createBinding(tpe, identifiers)
            bindings = bindings + binding
            binding._2
          }
        }
      }
    case _ => None
  }

  private def createBinding(
      tpe: Type,
      identifiers: List[Identifier]
  ): (List[Identifier], Option[BindingWithLifecycle]) = {
    val controller =
      tpe.decls
        .filter(_.isMethod)
        .map(_.asMethod)
        .find(m =>
          m.isConstructor && (m.paramLists match {
            case List(Nil, List(injectorType, paramType))
                if (tpe safe_<:< typeTag[AbstractController].tpe)
                  && injectorType.isImplicit
                  && (injectorType.typeSignature safe_<:< typeTag[Injector].tpe)
                  && injectorType.isImplicit && (paramType.typeSignature safe_<:< typeTag[ControllerComponents].tpe) =>
              true
            case List(Nil, List(injectorType))
                if (tpe safe_<:< typeTag[InjectedController].tpe)
                  && injectorType.isImplicit
                  && (injectorType.typeSignature safe_<:< typeTag[Injector].tpe) =>
              true
            case _ => false
          })
        )
        .map { constructor =>
          import Injectable._

          val env               = inject[Environment]
          val mirror            = runtimeMirror(env.classLoader)
          val constructorMirror = mirror.reflectClass(tpe.typeSymbol.asClass).reflectConstructor(constructor)

          try
            constructor.paramLists match {
              case List(Nil, List(_, _)) => constructorMirror(injector, inject[ControllerComponents])
              case List(Nil, List(_)) =>
                val instance = constructorMirror(injector)
                instance.asInstanceOf[InjectedController].setControllerComponents(inject[ControllerComponents])
                instance
              case List(Nil) => constructorMirror()
            } catch {
            case e: InvocationTargetException => throw e.getCause
          }
        }

    identifiers -> controller.map(c => LazyBinding(Some(() => c), identifiers))
  }

  def getBindingsInternal(identifiers: List[Identifier]): List[BindingWithLifecycle] =
    getBindingInternal(identifiers).toList

  protected def init(lifecycleManager: LifecycleManager): () => Unit = () => ()
}
