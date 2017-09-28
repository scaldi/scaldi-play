package scaldi.play

import java.io.StringReader

import com.typesafe.config.{ConfigFactory}
import org.scalatest.{Matchers, WordSpec}
import play.api.Configuration
import play.api.db.slick.DatabaseConfigProvider
import scaldi.Injectable
import scaldi.play.ScaldiApplicationBuilder._
import slick.jdbc.JdbcProfile

class SlickTest extends WordSpec with Matchers with Injectable {
  "Slick Plugin" should {
    "initialized and injected correctly" in {
      val config = Configuration(ConfigFactory.parseReader(new StringReader(
        """
          |slick.dbs.default.driver="slick.driver.H2Driver$"
          |slick.dbs.default.db.driver=org.h2.Driver
          |slick.dbs.default.db.url="jdbc:h2:mem:play;MODE=MYSQL;TRACE_LEVEL_SYSTEM_OUT=2"
          |slick.dbs.default.db.user=sa
          |slick.dbs.default.db.password=""
          |slick.dbs.default.db.connectionPool=disabled
          |slick.dbs.default.db.keepAliveConnection=true
          |
          |play.evolutions.db.default.autoApply = true
          |play.evolutions.db.default.autoApplyDowns = true
          |play.evolutions.autocommit=true
        """.stripMargin)))

      withScaldiInj(configuration = config) { implicit inj =>
        inject[DatabaseConfigProvider].get[JdbcProfile]
      }
    }
  }
}
