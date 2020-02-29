package com.github.mixtape.model

import cats.Show
import cats.data.{NonEmptyList, NonEmptySet}
import cats.instances.string._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

case class Playlist(id: String, userId: String, songIds: NonEmptySet[String])
case class Song(id: String, artist: String, title: String)
case class User(id: String, name: String)

case class Database(
    playlists: List[Playlist],
    songs: NonEmptyList[Song],
    users: NonEmptyList[User]) {
  lazy val userIds: NonEmptySet[String] = users.map(_.id).toNes
  lazy val playlistIds: Set[String] = playlists.map(_.id).toSet
  lazy val songIds: NonEmptySet[String] = songs.map(_.id).toNes
}

object Database {

  object decoders {
    implicit val snakeCaseConfig: Configuration =
      Configuration.default.withSnakeCaseMemberNames

    implicit val playlist: Decoder[Playlist] = deriveConfiguredDecoder[Playlist]
    implicit val song: Decoder[Song] = deriveConfiguredDecoder[Song]
    implicit val user: Decoder[User] = deriveConfiguredDecoder[User]

    implicit val database: Decoder[Database] = deriveConfiguredDecoder[Database]
  }

  object encoders {
    implicit val snakeCaseConfig: Configuration =
      Configuration.default.withSnakeCaseMemberNames

    implicit val playlist: Encoder[Playlist] = deriveConfiguredEncoder[Playlist]
    implicit val song: Encoder[Song] = deriveConfiguredEncoder[Song]
    implicit val user: Encoder[User] = deriveConfiguredEncoder[User]

    implicit val database: Encoder[Database] = deriveConfiguredEncoder[Database]
  }

  // Type class instances.  Among other things, these enable enhanced behavior like someDbInstance.show
  object instances {
    import encoders._
    import io.circe.syntax._
    implicit val show: Show[Database] = (db) => db.asJson.spaces2

  }
}
