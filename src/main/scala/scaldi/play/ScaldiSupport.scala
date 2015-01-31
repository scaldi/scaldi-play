package scaldi.play

import play.api.{Application, GlobalSettings, Mode, Configuration}
import scala.collection.concurrent.TrieMap
import scala.reflect.runtime
import scaldi._

/**
 * Adds Scaldi support to the `Global`.
 *
 * If you mix-in `ScaldiSupport` in the `Global`, then you need to implement `applicationModule` method:
 *
 * <pre class="prettyprint">
 * override def applicationModule = new MyAppModule :: new AnotherModule
 * </pre>
 *
 * Implicit `Injector` would be available in scope so you can use it in different play callbacks like `onStart`
 * and `onStop` (`ScaldiSupport` also extends `Injectable`, so you can use `inject` without any additional setup):
 *
 * <pre class="prettyprint">
 * override def onStart(app: Application) = {
 *   super.onStart(app)
 *   val service = inject [Service]
 *   ...
 * }
 * </pre>
 *
 * `ScaldiSupport` provides following pre-defined bindings:
 *
 * <ul>
 *   <li> '''playApp''' - Current play application
 *   <li> '''playMode''' - Current play application's mode (`Dev`, `Prod` or `Test`)
 *   <li> '''config''' - Current play application's configuration (all properties of configuration are also available as bindings)
 * </ul>
 */
trait ScaldiSupport extends GlobalSettings with Injectable {
  /**
   * The current injector if the application is running.
   */
  private var currentInjector: Option[Injector with LifecycleManager] = None

  /**
   * @return the application module to use
   */
  def applicationModule: Injector

  /**
   * The current injector when the application is running.
   *
   * This should only be used directly in legacy code and tests.
   */
  implicit def injector: Injector = currentInjector.getOrElse {
    throw new IllegalStateException("No injector found. Is application running?")
  }

  private def createApplicationInjector(currentApplication: Application): Injector with LifecycleManager =
    applicationModule ::
      TypesafeConfigInjector(currentApplication.configuration.underlying) ::
      new PlayAppModule(currentApplication)

  abstract override def onStart(app: Application) {
    super.onStart(app)

    currentInjector = Some(createApplicationInjector(app))
  }

  abstract override def onStop(app: Application) {
    controllerCache.clear()

    currentInjector foreach (_.destroy())
    currentInjector = None
    super.onStop(app)
  }

  /**
   * For each entry, V.getClass == K
   */
  private val controllerCache = TrieMap[Class[_], Any]()

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    controllerCache.getOrElseUpdate(controllerClass, {
      getBoundInstance(controllerClass).fold(msg => throw new IllegalStateException(msg), a => a)
    }).asInstanceOf[A]
  }

  /**
   * As this involves a little bit of reflection, calls should be cached.
   */
  protected def getBoundInstance[A](controllerClass: Class[A]): Either[String, A] = {
    val runtimeMirror = runtime.universe.runtimeMirror(controllerClass.getClassLoader)
    val identifier = TypeTagIdentifier(runtimeMirror.classSymbol(controllerClass).toType)
    injector.getBinding(List(identifier)).map {
      binding => binding.get.map {
        bound => Right(bound.asInstanceOf[A])
      }.getOrElse {
        Left(s"Controller for class $controllerClass is explicitly un-bound!")
      }
    } getOrElse {
      Left(s"Controller for class $controllerClass not found!")
    }
  }
}

class PlayAppModule(app: Application) extends Module {
  bind [Application] identifiedBy 'playApp to app
  bind [Mode.Mode] identifiedBy 'playMode to inject[Application].mode
  bind [Configuration] identifiedBy 'config to inject[Application].configuration
}