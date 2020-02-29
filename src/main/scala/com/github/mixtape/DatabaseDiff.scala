package com.github.mixtape

import cats.Show
import com.github.mixtape.model.{Database, Playlist, Song, User}
import _root_.io.circe._
import _root_.io.circe.generic.extras.semiauto._

sealed trait Diff {
  def playlists: List[Playlist]
  def songs: List[Song]
  def users: List[User]
}
case class Removed(
    playlists: List[Playlist],
    songs: List[Song],
    users: List[User])
    extends Diff

case class Added(
    playlists: List[Playlist],
    songs: List[Song],
    users: List[User])
    extends Diff

case class DiffResult(added: Added, removed: Removed)

object DiffResult {
  object encoders {
    import Database.encoders._
    implicit val removedEncoder: Encoder[Removed] =
      deriveConfiguredEncoder[Removed]
    implicit val addedEncoder: Encoder[Added] = deriveConfiguredEncoder[Added]
    implicit val diffResultEncoder: Encoder[DiffResult] =
      deriveConfiguredEncoder[DiffResult]
  }

  object instances {
    import encoders._
    import _root_.io.circe.syntax._

    implicit val show: Show[DiffResult] = _.asJson.spaces2
  }
}

object DatabaseDiff {

  /**
   * Returns the elements added and removed from left database (when compared to right).
   *
   * "Added" == in left and not right.
   */
  def diff(left: Database, right: Database): DiffResult = {
    val added =
      Added(
        playlists = left.playlists.diff(right.playlists),
        songs = left.songs.toList.diff(right.songs.toList),
        users = left.users.toList.diff(right.users.toList)
      )
    val removed =
      Removed(
        playlists = right.playlists.diff(left.playlists),
        songs = right.songs.toList.diff(left.songs.toList),
        users = right.users.toList.diff(left.users.toList)
      )
    DiffResult(added, removed)
  }
}
