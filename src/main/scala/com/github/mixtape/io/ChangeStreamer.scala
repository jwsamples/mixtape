package com.github.mixtape.io

import java.nio.file.Path

import cats.effect.{ContextShift, Sync}
import com.github.mixtape.model.Change
import fs2.Stream
import io.circe.Json

object ChangeStreamer {

  /**
   * Given a file containing newline-separated JSON's representing change requests, produce a lazy stream of
   * Change's.
   */
  def stream[F[_]: Sync: ContextShift](path: Path): Stream[F, Change] =
    JsonStreamer
      .stream[F](path)
      .evalMap(asChange[F])

  private def asChange[F[_]: Sync](json: Json): F[Change] = {
    val F = Sync[F]
    import Change.decoders._
    json
      .as[Change]
      .fold(
        e =>
          F.raiseError[Change](
            new IllegalArgumentException(s"Error decoding change $e")
          ),
        F.delay(_)
      )
  }
}
