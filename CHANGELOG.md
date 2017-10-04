## v0.5.17 (2017-10-04)

* Updated to play v2.6.5
* Cross-compile for scala 2.11 and 2.12

## v0.5.16 (2017-09-28)

* Updated to play v2.6.3

## v0.5.15 (2016-04-02)

* Updated to play v2.5.1

## v0.5.14 (2016-03-10)

* Updated to play v2.5.0

## v0.5.13-play-2.5.0-M2 (2016-02-12)

* Updated to play v2.5.0-M2

## v0.5.13 (2016-01-28)

* Updated to play v2.4.6

## v0.5.12 (2015-12-11)

* Updated scaldi-jsr330 v0.5.8
* `toNonLazy` is initialized even if it has a condition (#26)

## v0.5.11 (20.11.2015)

* Fixes singleton and eager `ConstructionTarget`s (now play-binding scope is considered during the binding phase)

## v0.5.10 (11.10.2015)

* Fixes circular dependency where `Mode` depends on a play `Application`. `Mode` binding is no longer depends on it.

## v0.5.9 (26.09.2015)

* Fixes bug in module loading order (bug caused inconsistent behaviour in some edge-cases)
* Updated scala and play deps

## v0.5.8 (14.06.2015)

* Updated to scaldi-jsr330 v0.5.7

## v0.5.7 (05.06.2015)

* Fixed #22 - IllegalArgumentException when using default assets route from Play 2.4 template
* Context classloader issue during the startup (playframework/playframework#4616)
* ControllerInjector does not create bindings for no-arg constructors anymore and does not throws exceptions (this task is now implicitly delegated to JSR 330 injector)

## v0.5.6 (30.05.2015)

* Play 2.4.0 support :star2:
  * Added `ScaldiApplicationLoader` which can be configured in `application.conf` to enable scaldi support. Fo example:
    ```
    play.application.loader = scaldi.play.ScaldiApplicationLoader

    play.modules.enabled += "modules.UserModule"
    play.modules.enabled += "modules.ServerModule"
    play.modules.enabled += "scaldi.play.ControllerInjector"
    ```
  * Added `ScaldiApplicationBuilder`, `ScaldiBuilder` and `FakeRouterModule` for testing support
* `ScaldiSupport` is deprecated in favour of new 2.4.0 DI configuration method through `application.conf`
* Updated to scaldi v0.5.6 and added new scaldi-jsr330 v0.5.6 dependency in order to support play 2.4.0 JSR 330 bindings and DI mechanism

## v0.5.6-RC1 (29.05.2015)

* Merged play-2.4 branch with all 2.4.0 related changes
* Updated to Play 2.4.0

## v0.5.5 (29.04.2015)

* Updated to scaldi version 0.5.5

## v0.5.4 (11.04.2015)

* Updated to scaldi version 0.5.4
* Controller cache is smarter now and takes `isCacheable` binding property into a consideration. This means, for example, that
  controllers bound with `toProvider` are no longer cached.

## v0.5.3 (03.02.2015)

* Updated to scaldi version 0.5.3

## v0.5.2 (02.02.2015)

* Updated to scaldi version 0.5.2

## v0.5.1 (01.02.2015)

* Updated to scaldi version 0.5.1

## v0.5 (31.01.2015)

* Updated to play version 2.3.7
* Updated to scaldi version 0.5
* Using `TypesafeConfigInjector` now
* Deprecated `PlayConfigurationInjector` in favour of `TypesafeConfigInjector`

## v0.4.1 (06.07.2014)

* Added `ControllerInjector` which creates controller bindings on the fly, so that you don't need to define them explicitly anymore

## v0.4 (22.06.2014)

* Updated to play version 2.3.0
* Updated to scaldi version 0.4

## v0.3.3 (20.05.2014)

* Updated to play version 2.3.0-RC1
* Cross-compiling to Scala version 2.10.4 and 2.11.0

## v0.3.2 (24.04.2014)

* Updated to scaldi version 0.3.2

## v0.3.1 (15.03.2014)

* Correct Play application shutdown handling (big thanks to [Peter Kolloch](https://github.com/kolloch) for this contribution)

## v0.3 (02.03.2014)

* Updated Scaldi v0.3
* Documentation for all public API.
