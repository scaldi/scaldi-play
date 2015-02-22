package scaldi.play

import javax.inject.{Named, Provider, Qualifier, Singleton}

import com.google.inject.CreationException
import play.api.ApplicationLoader.Context
import play.api._
import play.api.inject.{Binding => PlayBinding, Injector => PlayInjector, Module => PlayModule, _}
import play.core.WebCommands
import scaldi.Injectable.{injectWithDefault, noBindingFound}
import scaldi._
import scaldi.jsr330.{AnnotationBinding, AnnotationIdentifier, OnDemandAnnotationInjector}
import scaldi.util.ReflectionHelper

import scala.concurrent.Future
import scala.reflect.runtime.universe.typeOf

class ScaldiApplicationLoader(val additionalModules: Injector*) extends ApplicationLoader {
  import scaldi.play.ScaldiApplicationLoader._

  def this() = this(Nil: _*)

  def load(context: Context)  = {
    val env = context.environment
    val global = GlobalSettings(context.initialConfiguration, env)
    val configuration = global.onLoadConfig(context.initialConfiguration, env.rootPath, env.classLoader, env.mode)

    Logger.configure(env, configuration)

    val cacheControllers = configuration.getBoolean("scaldi.controller.cache") getOrElse true
    val globalInjector = global match {
      case s: ScaldiSupport => s.applicationModule
      case _ => NilInjector
    }

    val commonBindings = new Module {
      bind [GlobalSettings] to global
      bind [OptionalSourceMapper] to new OptionalSourceMapper(context.sourceMapper)
      bind [WebCommands] to context.webCommands
      bind [PlayInjector] to {
        if (cacheControllers) new ControllerCachingPlayInjector(new ScaldiInjector)
        else new ScaldiInjector
      }

      binding identifiedBy 'playMode to inject[Application].mode
      binding identifiedBy 'config to inject[Application].configuration
    }

    val configModules = Modules.locate(env, configuration).map(convertToScaldiModule(env, configuration, _))

    try {
      implicit val injector = createScaldiInjector(configModules ++ additionalModules ++ Seq(commonBindings, globalInjector), configuration)

      Injectable.inject [Application]
    } catch {
      case e: CreationException => e.getCause match {
        case p: PlayException => throw p
        case _ => throw e
      }
    }
  }

  override def createInjector(environment: Environment, configuration: Configuration, modules: Seq[Any]) = {
    val globalInjector = findTestGlobal(modules, environment, configuration) match {
      case Some(s: ScaldiSupport) => s.applicationModule
      case _ => NilInjector
    }
    val commonBindings = new Module {
      bind [PlayInjector] to new ScaldiInjector
    }
    val configModules = modules.map(convertToScaldiModule(environment, configuration, _))
    implicit val injector = createScaldiInjector(configModules ++ Seq(commonBindings, globalInjector), configuration)

    Some(Injectable.inject [PlayInjector])
  }

  // TODO: find better way!
  private def findTestGlobal(modules: Seq[Any], environment: Environment, configuration: Configuration): Option[GlobalSettings] =
    modules.flatMap {
      case playModule: PlayModule =>
        val globalKey = playModule.bind[GlobalSettings]

        playModule.bindings(environment, configuration).collectFirst {
          case PlayBinding(`globalKey`, Some(ProviderTarget(globalProvider)), _, _, _) =>
            globalProvider.get().asInstanceOf[GlobalSettings]
        }
      case _ => None
    }.headOption
}

object ScaldiApplicationLoader extends Injectable {
  def createScaldiInjector(injectors: Seq[Injector], config: Configuration) = {
    val standard = Seq(TypesafeConfigInjector(config.underlying), new OnDemandAnnotationInjector)

    implicit val injector = (injectors ++ standard) reduce (_ :: _)

    injector match {
      case lm: LifecycleManager =>
        inject [ApplicationLifecycle] addStopHook { () =>
          lm.destroy()

          Future.successful(())
        }
      case _ => // there is nothing to destroy
    }

    // TODO: initialisation logic for tests! Play.start(app) - too late, everything is already initialised
    injector match {
      case init: Initializeable[_] =>
        init.initNonLazy()
      case _ => // there is nothing to destroy
    }

    injector
  }

  def convertToScaldiModule(env: Environment, conf: Configuration, module: Any): Injector = module match {
    case playModule: PlayModule => toScaldiBindings(playModule.bindings(env, conf))
    case inj: Injector => inj
    case unknown =>
      throw new PlayException("Unknown module type", s"Module [$unknown] is not a Play module or a Scaldi module")
  }

  def identifiersForKey[T](key: BindingKey[T]) = {
    val mirror = ReflectionHelper.mirror
    val keyType = mirror.classSymbol(key.clazz).toType

    val qualifier = key.qualifier map {
      case QualifierInstance(a: Named) => StringIdentifier(a.value())
      /* Second pattern condition addresses https://github.com/scaldi/scaldi-play/issues/10. */
      case QualifierInstance(a) if a.getClass.getAnnotation(classOf[Qualifier]) != null || a.getClass.getInterfaces.map(_.getAnnotation(classOf[Qualifier])).filterNot(null ==).nonEmpty =>
        AnnotationIdentifier(mirror.classSymbol(a.getClass).toType)
      case QualifierClass(clazz) => AnnotationIdentifier(mirror.classSymbol(clazz).toType)
    }

    (keyType, TypeTagIdentifier(keyType) :: qualifier.toList)
  }

  def toScaldiBindings(bindings: Seq[PlayBinding[_]]): Injector = {
    val mirror = ReflectionHelper.mirror

    val scaldiBindings = (inj: Injector) => bindings.toList.map { binding =>
      val scope = binding.scope map (mirror.classSymbol(_).toType)
      val singleton = scope.exists(_ =:= typeOf[Singleton])
      val (keyType, identifiers) = identifiersForKey(binding.key)

      binding.target match {
        case Some(BindingKeyTarget(key)) if singleton && binding.eager =>
          NonLazyBinding(Some(() => injectWithDefault(inj, noBindingFound(identifiers))(identifiers)), identifiers)
        case Some(BindingKeyTarget(key)) if singleton =>
          LazyBinding(Some(() => injectWithDefault(inj, noBindingFound(identifiers))(identifiers)), identifiers)
        case Some(BindingKeyTarget(key)) =>
          ProviderBinding(() => injectWithDefault(inj, noBindingFound(identifiers))(identifiers), identifiers)

        case Some(ProviderTarget(provider)) =>
          AnnotationBinding(
            instanceOrType = Left(provider),
            injector = () => inj,
            identifiers = identifiers,
            eager = binding.eager,
            forcedScope = scope,
            bindingConverter = Some(_.asInstanceOf[Provider[AnyRef]].get()))
        case Some(ProviderConstructionTarget(provider)) =>
          AnnotationBinding(
            instanceOrType = Right(mirror.classSymbol(provider).toType),
            injector = () => inj,
            identifiers = identifiers,
            eager = binding.eager,
            forcedScope = scope,
            bindingConverter = Some(_.asInstanceOf[Provider[AnyRef]].get()))
        case Some(ConstructionTarget(implementation)) =>
          AnnotationBinding(
            instanceOrType = Right(mirror.classSymbol(implementation).toType),
            injector = () => inj,
            identifiers = identifiers,
            eager = binding.eager,
            forcedScope = scope)

        case None =>
          AnnotationBinding(
            instanceOrType = Right(keyType),
            injector = () => inj,
            identifiers = identifiers,
            eager = binding.eager,
            forcedScope = scope)
      }
    }

    new SimpleContainerInjector(scaldiBindings)
  }
}
