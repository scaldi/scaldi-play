## v0.5.6 (TBD)

* TODO
* Play 2.4.0

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
