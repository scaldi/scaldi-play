package scaldi.play

import play.api._
import play.api.inject.{Injector => PlayInjector, Module => _, _}
import play.core.{DefaultWebCommands, WebCommands}

import scaldi.{Injectable, Injector, NilInjector, Module}

/**
 * A builder for creating Applications using Scaldi.
 */
final class ScaldiApplicationBuilder(
  environment: Environment = Environment.simple(),
  configuration: Configuration = Configuration.empty,
  modules: Seq[CanBeScaldiInjector] = Seq.empty,
  disabled: Seq[Class[_]] = Seq.empty,
  loadConfiguration: Environment => Configuration = Configuration.load,
  global: Option[GlobalSettings] = None,
  loadModules: (Environment, Configuration) => Seq[CanBeScaldiInjector] = ScaldiBuilder.loadModules
) extends ScaldiBuilder[ScaldiApplicationBuilder](environment, configuration, modules, disabled) {
  def this() = this(environment = Environment.simple())

  /**
   * Set the initial configuration loader.
   * Overrides the default or any previously configured values.
   */
  def loadConfig(loader: Environment => Configuration): ScaldiApplicationBuilder =
    copy(loadConfiguration = loader)

  /**
   * Set the initial configuration.
   * Overrides the default or any previously configured values.
   */
  def loadConfig(conf: Configuration): ScaldiApplicationBuilder =
    loadConfig(env => conf)

  /**
   * Set the global settings object.
   * Overrides the default or any previously configured values.
   */
  def global(globalSettings: GlobalSettings): ScaldiApplicationBuilder =
    copy(global = Option(globalSettings))

  /**
   * Set the module loader.
   * Overrides the default or any previously configured values.
   */
  def load(loader: (Environment, Configuration) => Seq[CanBeScaldiInjector]): ScaldiApplicationBuilder =
    copy(loadModules = loader)

  /**
   * Override the module loader with the given modules.
   */
  def load(modules: CanBeScaldiInjector*): ScaldiApplicationBuilder =
    load((env, conf) => modules)

  protected def realInjector: (Injector, PlayInjector) = {
    val initialConfiguration = loadConfiguration(environment)
    val globalSettings = global getOrElse GlobalSettings(initialConfiguration, environment)
    val loadedConfiguration = globalSettings.onLoadConfig(initialConfiguration, environment.rootPath, environment.classLoader, environment.mode)
    val appConfiguration = loadedConfiguration ++ configuration

    // TODO: Logger should be application specific, and available via dependency injection.
    //       Creating multiple applications will stomp on the global logger configuration.
    Logger.configure(environment)

    if (appConfiguration.underlying.hasPath("logger"))
      Logger.warn("Logger configuration in conf files is deprecated and has no effect. Use a logback configuration file instead.")

    val loadedModules = loadModules(environment, appConfiguration)
    val cacheControllers = configuration.getBoolean("scaldi.controller.cache") getOrElse true

    val globalInjector = globalSettings match {
      case s: ScaldiSupport => s.applicationModule
      case _ => NilInjector
    }

    copy(configuration = appConfiguration)
        .appendModule(globalInjector)
        .appendModule(loadedModules: _*)
        .appendModule(new BuiltinScaldiModule(globalSettings, cacheControllers))
        .createInjector
  }

  /**
   * Create a new Play Application using this configured builder. In order ti get the underlying injector instead, please use `buildInj`
   * or `buildPlayInj` instead.
   */
  def build(): Application = realInjector._2.instanceOf[Application]

  /**
   * Create a new Scaldi Injector for an Application using this configured builder.
   */
  def buildInj(): Injector = realInjector._1

  /**
   * Create a new Play Injector for an Application using this configured builder.
   */
  def buildPlayInj(): PlayInjector = realInjector._2

  /**
   * Internal copy method with defaults.
   */
  private def copy(
      environment: Environment = environment,
      configuration: Configuration = configuration,
      modules: Seq[CanBeScaldiInjector] = modules,
      disabled: Seq[Class[_]] = disabled,
      loadConfiguration: Environment => Configuration = loadConfiguration,
      global: Option[GlobalSettings] = global,
      loadModules: (Environment, Configuration) => Seq[CanBeScaldiInjector] = loadModules): ScaldiApplicationBuilder =
    new ScaldiApplicationBuilder(environment, configuration, modules, disabled, loadConfiguration, global, loadModules)

  override protected def newBuilder(
      environment: Environment,
      configuration: Configuration,
      modules: Seq[CanBeScaldiInjector],
      disabled: Seq[Class[_]]): ScaldiApplicationBuilder =
    copy(environment, configuration, modules, disabled)
}

class BuiltinScaldiModule(global: GlobalSettings, cacheControllers: Boolean) extends Module {
  bind [GlobalSettings] to global
  bind [OptionalSourceMapper] to new OptionalSourceMapper(None)
  bind [WebCommands] to new DefaultWebCommands
  bind [PlayInjector] to new ScaldiInjector(cacheControllers)

  binding identifiedBy 'playMode to inject[Application].mode
}

object ScaldiApplicationBuilder {
  /**
   * Helper function that allows to construct a Play `Application` and execute a function while it's running.
   */
  def withScaldiApp[T](environment: Environment = Environment.simple(),
                       configuration: Configuration = Configuration.empty,
                       modules: Seq[CanBeScaldiInjector] = Seq.empty,
                       disabled: Seq[Class[_]] = Seq.empty,
                       loadConfiguration: Environment => Configuration = Configuration.load,
                       global: Option[GlobalSettings] = None,
                       loadModules: (Environment, Configuration) => Seq[CanBeScaldiInjector] = ScaldiBuilder.loadModules)(fn: => T) = {
    val app = new ScaldiApplicationBuilder(environment, configuration, modules, disabled, loadConfiguration, global, loadModules).build()

    try {
      Play.start(app)
      fn
    } finally Play.stop(app)
  }

  /**
   * Helper function that allows to construct a Play `Application` and execute a function while it's running. This variation allows you to also
   * get scaldi `Injector` of this application.
   */
  def withScaldiInj[T](environment: Environment = Environment.simple(),
                       configuration: Configuration = Configuration.empty,
                       modules: Seq[CanBeScaldiInjector] = Seq.empty,
                       disabled: Seq[Class[_]] = Seq.empty,
                       loadConfiguration: Environment => Configuration = Configuration.load,
                       global: Option[GlobalSettings] = None,
                       loadModules: (Environment, Configuration) => Seq[CanBeScaldiInjector] = ScaldiBuilder.loadModules)(fn: Injector => T) = {
    implicit val inj = new ScaldiApplicationBuilder(environment, configuration, modules, disabled, loadConfiguration, global, loadModules).buildInj()
    val app = Injectable.inject[Application]

    try {
      Play.start(app)
      fn(inj)
    } finally Play.stop(app)
  }
}