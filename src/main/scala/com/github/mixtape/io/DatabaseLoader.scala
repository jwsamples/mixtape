package com.github.mixtape.io

import java.nio.file.Path

import cats.effect.{Resource, Sync}
import cats.syntax.functor._
import cats.syntax.flatMap._
import com.github.mixtape.model.Database
import io.circe.{Error => CirceError}

import scala.io.{BufferedSource, Source}

object DatabaseLoader {

  /**
   * Given a file containing a JSON representation of database state, load database into memory.
   */
  def load[F[_]: Sync](databasePath: Path): F[Database] = {
    val F = Sync[F]

    // I went a bit overboard breaking this out in functions in an attempt to make the key logic clear at a glance
    asSource(databasePath)
      .use { source =>
        readToString(source).map(deserializeToDatabase)
      }
      .flatMap {
        case Left(e) =>
          F.raiseError[Database](
            new Exception(s"Unable to parse database json $e")
          )
        case Right(db) => F.delay(db)
      }
  }

  private def deserializeToDatabase(
      str: String
    ): Either[CirceError, Database] = {
    import Database.decoders._
    import io.circe.parser.parse
    parse(str).flatMap(_.as[Database])
  }

  private def readToString[F[_]: Sync](source: Source): F[String] =
    Sync[F].catchNonFatal {
      source.mkString
    }

  /**
   * Build a Resource containing a source we can use to read data from given UTF-8-encoded file.
   *
   * Resource automatically closes source after use.
   */
  private def asSource[F[_]: Sync](path: Path): Resource[F, BufferedSource] =
    Resource
      .fromAutoCloseable[F, BufferedSource](
        Sync[F].delay(
          Source.fromFile(path.toUri, "UTF-8")
        )
      )

}
