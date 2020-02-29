package com.github.mixtape.model

import cats.data.NonEmptySet

import cats.implicits._
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration

sealed trait Change {
  final def operation: String = this.getClass.getSimpleName
}

object Change {
  case class AddPlaylist(id: String, userId: String, songs: NonEmptySet[String])
      extends Change
  case class RemovePlaylist(id: String) extends Change
  case class AddSong(playlistId: String, songId: String) extends Change

  object decoders {
    implicit val snakeCaseConfig: Configuration =
      Configuration
        .default
        .withSnakeCaseMemberNames
        .withDiscriminator("operation")

    implicit val addPlaylistDecoder: Decoder[AddPlaylist] =
      deriveConfiguredDecoder[AddPlaylist]
    implicit val removePlayListDecoder: Decoder[RemovePlaylist] =
      deriveConfiguredDecoder[RemovePlaylist]
    implicit val addSongDecoder: Decoder[AddSong] =
      deriveConfiguredDecoder[AddSong]

    implicit val changeDecoder: Decoder[Change] =
      deriveConfiguredDecoder[Change]
  }
}
