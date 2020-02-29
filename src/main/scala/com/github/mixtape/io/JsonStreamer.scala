package com.github.mixtape.io

import java.nio.file.Path

import cats.effect.{Blocker, ContextShift, Sync}
import fs2.{Stream, text, io => fs2io}
import io.circe.Json
import io.circe.fs2.stringStreamParser

object JsonStreamer {

  /**
   * Takes a UTF-8 or ASCII-encoded file containing a set of newline-separated JSON's and produces a lazy
   * stream of JSON's.
   */
  def stream[F[_]: Sync: ContextShift](path: Path): Stream[F, Json] = {
    val chunkSize = 4096

    Stream
      .resource(Blocker[F]) // execution context suitable for blocking IO
      .flatMap { blocker =>
        fs2io
          .file
          .readAll[F](path, blocker, chunkSize) // lazily stream file contents
      }
      .through(text.utf8Decode)
      .through(stringStreamParser) // parse into a stream of JSON's separated by newline
  }
}
