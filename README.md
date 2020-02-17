## dipendi-play

[![Build Status](https://travis-ci.org/protenus/dipendi-play.svg?branch=master)](https://travis-ci.org/protenus/dipendi-play)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.protenus/dipendi-play_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.protenus/dipendi-play_2.13)

[Dipendi](https://github.com/protenus/dipendi) integration for Play framework

Dipendi-play is a fork of [scaldi-play](https://github.com/scaldi/scaldi-play),
created to continue the library's development in lieu of a new maintainer who can access
the Scaldi repository (see [scaldi/scaldi#81](https://github.com/scaldi/scaldi/issues/81)).
See the [main Dipendi repo](https://github.com/protenus/dipendi) for more information.

The Scaldi documentation is still being migrated. Until that is complete,
you may need to rely on the original project's documentation.

You can find an archive of the original project's homepage
[here](https://web.archive.org/web/20190616212058/http://scaldi.org/), or jump directly
to the Play Integration documentation
[here](https://web.archive.org/web/20190618005634/http://scaldi.org/learn/#play-integration). Due to it
being an archived website, some of the links on it may not work properly.

## Adding Dipendi-play in Your Build

SBT Configuration (Play 2.7.x):

```sbtshell
libraryDependencies += "com.protenus" %% "dipendi-play" % "0.6.0"
```

For older versions of Play, please use [scaldi-play](https://github.com/scaldi/scaldi-play).

## License

**dipendi-play** is licensed under [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
