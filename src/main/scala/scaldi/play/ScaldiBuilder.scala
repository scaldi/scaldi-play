package scaldi.play

import java.io.File

import com.google.inject.CreationException
import play.api._
import play.api.inject.{Binding => PlayBinding, Injector => PlayInjector, Module => PlayModule, _}

import scaldi._
import scaldi.jsr330.{AnnotationBinding, AnnotationIdentifier, OnDemandAnnotationInjector}
import scaldi.util.ReflectionHelper

import javax.inject._

import scala.concurrent.Future
import scala.reflect.runtime.universe.{typeOf, Type}
import scala.reflect.ClassTag

abstract class ScaldiBuilder[Self] protected (
  environment: Environment,
  configuration: Configuration,
  modules: Seq[CanBeScaldiInjector],
  disabled: Seq[Class[_]]
) {

  /**
   * Set the environment.
   */
  final def in(env: Environment): Self =
    copyBuilder(environment = env)

  /**
   * Set the environment path.
   */
  final def in(path: File): Self =
    copyBuilder(environment = environment.copy(rootPath = path))

  /**
   * Set the environment mode.
   */
  final def in(mode: Mode): Self =
    copyBuilder(environment = environment.copy(mode = mode))

  /**
   * Set the environment class loader.
   */
  final def in(classLoader: ClassLoader): Self =
    copyBuilder(environment = environment.copy(classLoader = classLoader))

  /**
   * Add additional configuration.
   */
  final def configure(conf: Configuration): Self =
    copyBuilder(configuration = configuration ++ conf)

  /**
   * Add additional configuration.
   */
  final def configure(conf: Map[String, Any]): Self =
    configure(Configuration.from(conf))

  /**
   * Add additional configuration.
   */
  final def configure(conf: (String, Any)*): Self =
    configure(conf.toMap)
  
  
  /*** Disable modules by class.
   */
  final def disable(moduleClasses: Class[_]*): Self =
    copyBuilder(disabled = disabled ++ moduleClasses)

  /**
   * Disable module by class.
   */
  final def disable[T](implicit tag: ClassTag[T]): Self = disable(tag.runtimeClass)

  final def appendModule(ms: CanBeScaldiInjector*): Self =
    copyBuilder(modules = modules ++ ms)

  final def prependModule(ms: CanBeScaldiInjector*): Self =
    copyBuilder(modules = ms ++ modules)
  
  protected final def createInjector: (Injector, PlayInjector) = {
    import ScaldiBuilder._

    try {
      val commonBindings = new Module {
        bind [PlayInjector] to new ScaldiInjector(false, environment.classLoader)
      }

      val enabledModules = modules.map(_.disable(disabled))
      val scaldiInjectors = enabledModules flatMap (_.toScaldi(environment, configuration))

      implicit val injector = createScaldiInjector(scaldiInjectors :+ commonBindings, configuration)

      injector -> Injectable.inject[PlayInjector]
    } catch {
      case e: CreationException => e.getCause match {
        case p: PlayException => throw p
        case _ => throw e
      }
    }
  }

  /**
   * Internal copy method with defaults.
   */
  private def copyBuilder(
      environment: Environment = environment,
      configuration: Configuration = configuration,
      modules: Seq[CanBeScaldiInjector] = modules,
      disabled: Seq[Class[_]] = disabled): Self =
    newBuilder(environment, configuration, modules, disabled)

  /**
   * Create a new Self for this immutable builder.
   * Provided by builder implementations.
   */
  protected def newBuilder(
      environment: Environment,
      configuration: Configuration,
      modules: Seq[CanBeScaldiInjector],
      disabled: Seq[Class[_]]): Self
}

object ScaldiBuilder extends Injectable {
  def loadModules(environment: Environment, configuration: Configuration): Seq[CanBeScaldiInjector] = {
    def doLoadModules(modules: Seq[Any]) =
      modules.map {
        case playModule: PlayModule => CanBeScaldiInjector.fromPlayModule(playModule)
        case inj: Injector => CanBeScaldiInjector.fromScaldiInjector(inj)
        case unknown =>
          throw new PlayException("Unknown module type", s"Module [$unknown] is not a Play module or a Scaldi module")
      }

    val (lowPrio, highPrio) = Modules.locate(environment, configuration).partition(m => m.isInstanceOf[BuiltinModule] || m.isInstanceOf[ControllerInjector])

    doLoadModules(highPrio) ++ doLoadModules(lowPrio)
  }

  def createScaldiInjector(injectors: Seq[Injector], config: Configuration) = {
    val standard = Seq(TypesafeConfigInjector(config.underlying), new OnDemandAnnotationInjector)
    val allInjectors = injectors ++ standard

    implicit val injector = new MutableInjectorAggregation(allInjectors.toList)

    injector match {
      case init: Initializeable[_] =>
        init.initNonLazy()
      case _ => // there is nothing to init
    }

    injector match {
      case lm: LifecycleManager =>
        inject [ApplicationLifecycle] addStopHook { () =>
          lm.destroy()

          Future.successful(())
        }
      case _ => // there is nothing to destroy
    }

    injector
  }

  def convertToScaldiModule(env: Environment, conf: Configuration, playModule: PlayModule): Injector =
    toScaldiBindings(playModule.bindings(env, conf))

  def identifiersForKey[T](key: BindingKey[T]): (Type, List[Identifier]) = {
    val mirror = ReflectionHelper.mirror
    val keyType = mirror.classSymbol(key.clazz).toType

    val qualifier: Option[Identifier] = key.qualifier map {
      case QualifierInstance(a: Named) =>
        StringIdentifier(a.value())
      case QualifierInstance(a) if ReflectionHelper.hasAnnotation[Qualifier](a) =>
        AnnotationIdentifier.forAnnotation(a)
      case QualifierClass(clazz) =>
        AnnotationIdentifier(ReflectionHelper.classToType(clazz))
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
        case Some(BindingKeyTarget(key)) if binding.eager =>
          NonLazyBinding(Some(() => injectWithDefault[Any](inj, noBindingFound(identifiers))(identifiersForKey(key)._2)), identifiers)
        case Some(BindingKeyTarget(key)) if singleton =>
          LazyBinding(Some(() => injectWithDefault[Any](inj, noBindingFound(identifiers))(identifiersForKey(key)._2)), identifiers)
        case Some(BindingKeyTarget(key)) =>
          ProviderBinding(() => injectWithDefault[Any](inj, noBindingFound(identifiers))(identifiersForKey(key)._2), identifiers)

        case Some(ProviderTarget(provider)) =>
          AnnotationBinding(
            instanceOrType = Left(provider),
            injector = () => inj,
            identifiers = identifiers,
            eager = binding.eager,
            forcedScope = scope,
            bindingConverter = Some(_.asInstanceOf[Provider[AnyRef]].get()))

        case Some(ProviderConstructionTarget(provider)) if binding.eager =>
          val providerIds = List[Identifier](ReflectionHelper.classToType(provider))

          NonLazyBinding(Some(() => injectWithDefault[Provider[_]](inj, noBindingFound(providerIds))(providerIds).get()), identifiers)
        case Some(ProviderConstructionTarget(provider)) if singleton =>
          val providerIds = List[Identifier](ReflectionHelper.classToType(provider))

          LazyBinding(Some(() => injectWithDefault[Provider[_]](inj, noBindingFound(providerIds))(providerIds).get()), identifiers)
        case Some(ProviderConstructionTarget(provider)) =>
          val providerIds = List[Identifier](ReflectionHelper.classToType(provider))

          ProviderBinding(() => injectWithDefault[Provider[_]](inj, noBindingFound(providerIds))(providerIds).get(), identifiers)

        case Some(ConstructionTarget(impl)) if binding.eager =>
          val implIds = List[Identifier](ReflectionHelper.classToType(impl))

          NonLazyBinding(Some(() => injectWithDefault[Any](inj, noBindingFound(implIds))(implIds)), identifiers)
        case Some(ConstructionTarget(impl)) if singleton =>
          val implIds = List[Identifier](ReflectionHelper.classToType(impl))

          LazyBinding(Some(() => injectWithDefault[Any](inj, noBindingFound(implIds))(implIds)), identifiers)
        case Some(ConstructionTarget(impl)) =>
          val implIds = List[Identifier](ReflectionHelper.classToType(impl))

          ProviderBinding(() => injectWithDefault[Any](inj, noBindingFound(implIds))(implIds), identifiers)
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

/**
 * Default empty builder for creating Scaldi-backed Injectors.
 */
final class ScaldiInjectorBuilder(
  environment: Environment = Environment.simple(),
  configuration: Configuration = Configuration.empty,
  modules: Seq[CanBeScaldiInjector] = Seq.empty,
  disabled: Seq[Class[_]] = Seq.empty
) extends ScaldiBuilder[ScaldiInjectorBuilder](environment, configuration, modules, disabled) {
  def this() = this(environment = Environment.simple())

  /**
   * Create a Play Injector backed by Scaldi using this configured builder.
   */
  def build(): PlayInjector = createInjector._2

  /**
   * Create a Scaldi Injector using this configured builder.
   */
  def buildInj(): Injector = createInjector._1

  protected def newBuilder(
      environment: Environment,
      configuration: Configuration,
      modules: Seq[CanBeScaldiInjector] = Seq.empty,
      disabled: Seq[Class[_]]): ScaldiInjectorBuilder =
    new ScaldiInjectorBuilder(environment, configuration, modules, disabled)
}

trait CanBeScaldiInjector {
  def toScaldi(env: Environment, conf: Configuration): Seq[Injector]
  def disable(classes: Seq[Class[_]]): CanBeScaldiInjector
}

object CanBeScaldiInjector {
  import scala.language.implicitConversions

  implicit def fromScaldiInjector(inj: Injector): CanBeScaldiInjector = fromScaldiInjectors(Seq(inj))

  implicit def fromScaldiInjectors(injectors: Seq[Injector]): CanBeScaldiInjector = new CanBeScaldiInjector {
    def toScaldi(env: Environment, conf: Configuration): Seq[Injector] = injectors
    def disable(classes: Seq[Class[_]]): CanBeScaldiInjector = fromScaldiInjectors(filterOut(classes, injectors))
    override def toString = s"CanBeScaldiInjector(${injectors.mkString(", ")})"
  }

  implicit def fromPlayModule(playModule: PlayModule): CanBeScaldiInjector = fromPlayModules(Seq(playModule))

  implicit def fromPlayModules(playModules: Seq[PlayModule]): CanBeScaldiInjector = new CanBeScaldiInjector {
    def toScaldi(env: Environment, conf: Configuration): Seq[Injector] = playModules.map(ScaldiBuilder.convertToScaldiModule(env, conf, _))
    def disable(classes: Seq[Class[_]]): CanBeScaldiInjector = fromPlayModules(filterOut(classes, playModules))
    override def toString = s"CanBeScaldiInjector(${playModules.mkString(", ")})"
  }

  implicit def fromPlayBinding(binding: PlayBinding[_]): CanBeScaldiInjector = fromPlayBindings(Seq(binding))

  implicit def fromPlayBindings(bindings: Seq[PlayBinding[_]]): CanBeScaldiInjector = new CanBeScaldiInjector {
    def toScaldi(env: Environment, conf: Configuration): Seq[Injector] = Seq(ScaldiBuilder.toScaldiBindings(bindings))
    def disable(classes: Seq[Class[_]]): CanBeScaldiInjector = this // no filtering
    override def toString = s"CanBeScaldiInjector(${bindings.mkString(", ")})"
  }

  private def filterOut[A](classes: Seq[Class[_]], instances: Seq[A]): Seq[A] =
    instances.filterNot(o => classes.exists(_.isAssignableFrom(o.getClass)))
}