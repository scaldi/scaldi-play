package scaldi.play

import play.api._
import play.api.inject.{Injector => PlayInjector, Module => _, _}
import play.core.{DefaultWebCommands, WebCommands}

import scaldi.{NilInjector, Module}

final class ScaldiApplicationBuilder(
  environment: Environment = Environment.simple(),
  configuration: Configuration = Configuration.empty,
  modules: Seq[CanBeScaldiInjector] = Seq.empty,
  disabled: Seq[Class[_]] = Seq.empty,
  loadConfiguration: Environment => Configuration = Configuration.load,
  global: Option[GlobalSettings] = None,
  loadModules: (Environment, Configuration) => Seq[CanBeScaldiInjector] = ScaldiBuilder.loadModules
) extends ScaldiBuilder[ScaldiApplicationBuilder](environment, configuration, modules, disabled) {
  // extra constructor for creating from Java
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

  /**
   * Create a new Play Injector for an Application using this configured builder.
   */
  override def injector: PlayInjector = {
    val initialConfiguration = loadConfiguration(environment)
    val globalSettings = global getOrElse GlobalSettings(initialConfiguration, environment)
    val loadedConfiguration = globalSettings.onLoadConfig(initialConfiguration, environment.rootPath, environment.classLoader, environment.mode)
    val appConfiguration = loadedConfiguration ++ configuration

    // TODO: Logger should be application specific, and available via dependency injection.
    //       Creating multiple applications will stomp on the global logger configuration.
    Logger.configure(environment, appConfiguration)

    val loadedModules = loadModules(environment, appConfiguration)
    val cacheControllers = configuration.getBoolean("scaldi.controller.cache") getOrElse true

    val globalInjector = globalSettings match {
      case s: ScaldiSupport => s.applicationModule
      case _ => NilInjector
    }

    copy(configuration = appConfiguration)
        .appendModule(globalInjector)
        .appendModule(loadedModules: _*)
        .appendModule(new Module {
          bind [GlobalSettings] to globalSettings
          bind [OptionalSourceMapper] to new OptionalSourceMapper(None)
          bind [WebCommands] to new DefaultWebCommands
          bind [PlayInjector] to new ScaldiInjector(cacheControllers)

          binding identifiedBy 'playMode to inject[Application].mode
        })
        .createInjector
  }

  /**
   * Create a new Play Application using this configured builder.
   */
  def build(): Application = injector.instanceOf[Application]

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

object ScaldiApplicationBuilder {
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
}