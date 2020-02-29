package com.github.mixtape.io

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import cats.effect.Sync
import com.github.mixtape.model.Database

object DatabaseDumper {

  /**
   * Saves a JSON representation of the given database
   */
  def dump[F[_]: Sync](outputPath: Path, db: Database): F[Unit] =
    Sync[F].catchNonFatal {
      import Database.encoders._
      import io.circe.syntax._
      Files.write(
        outputPath,
        db.asJson.spaces2.getBytes(StandardCharsets.UTF_8)
      )
      () // Ignore Files.write return.  Return Unit instead.  (Files.write returns outputPath, which is not helpful.)
    }
}
