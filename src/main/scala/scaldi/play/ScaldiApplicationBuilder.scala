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
  loadModules: (Environment, Configuration) => Seq[CanBeScaldiInjector] = ScaldiBuilder.loadModules
) extends ScaldiBuilder[ScaldiApplicationBuilder](environment, configuration, modules, disabled) {
  def this() = this(environment = Environment.simple())

  val GlobalAppConfigKey = "play.allowGlobalApplication"
  /**
    * Sets the configuration key to enable/disable global application state
    */
  def globalApp(enabled: Boolean): ScaldiApplicationBuilder =
    configure(GlobalAppConfigKey -> enabled)
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
    val appConfiguration = initialConfiguration ++ configuration

    LoggerConfigurator(environment.classLoader).foreach {
      _.configure(environment)
    }

    if (appConfiguration.underlying.hasPath("logger"))
      Logger.warn("Logger configuration in conf files is deprecated and has no effect. Use a logback configuration file instead.")

    val loadedModules = loadModules(environment, appConfiguration)
    val cacheControllers = configuration.getOptional[Boolean]("scaldi.controller.cache") getOrElse true


    copy(configuration = appConfiguration)
        .appendModule(loadedModules: _*)
        .appendModule(new BuiltinScaldiModule(cacheControllers, environment.classLoader, environment.mode))
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
      loadModules: (Environment, Configuration) => Seq[CanBeScaldiInjector] = loadModules): ScaldiApplicationBuilder =
    new ScaldiApplicationBuilder(environment, configuration, modules, disabled, loadConfiguration, loadModules)

  override protected def newBuilder(
      environment: Environment,
      configuration: Configuration,
      modules: Seq[CanBeScaldiInjector],
      disabled: Seq[Class[_]]): ScaldiApplicationBuilder =
    copy(environment, configuration, modules, disabled)
}

class BuiltinScaldiModule(cacheControllers: Boolean, classLoader: ClassLoader, mode: Mode) extends Module {
  bind [OptionalSourceMapper] to new OptionalSourceMapper(None)
  bind [WebCommands] to new DefaultWebCommands
  bind [PlayInjector] to new ScaldiInjector(cacheControllers, classLoader)

  binding identifiedBy 'playMode to mode
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
                       loadModules: (Environment, Configuration) => Seq[CanBeScaldiInjector] = ScaldiBuilder.loadModules)(fn: => T) = {
    val app = new ScaldiApplicationBuilder(environment, configuration, modules, disabled, loadConfiguration, loadModules).build()

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
                       loadModules: (Environment, Configuration) => Seq[CanBeScaldiInjector] = ScaldiBuilder.loadModules)(fn: Injector => T) = {
    implicit val inj = new ScaldiApplicationBuilder(environment, configuration, modules, disabled, loadConfiguration, loadModules).buildInj()
    val app = Injectable.inject[Application]

    try {
      Play.start(app)
      fn(inj)
    } finally Play.stop(app)
  }
}