package com.github.mixtape.validators

import cats.data.ValidatedNel
import cats.syntax.validated._
import com.github.mixtape.model.Change.{AddPlaylist, AddSong, RemovePlaylist}
import com.github.mixtape.model.{Change, Database}

object ChangeValidator {
  def apply[C <: Change](
      implicit instance: ChangeValidator[C]
    ): ChangeValidator[C] = instance
}

trait ChangeValidator[C <: Change] {
  def validate(
      change: C,
      database: Database
    ): ValidatedNel[String, (C, Database)]
}

/**
 * These implement ChangeValidator behavior for various types.
 *
 * The practical upshot of these properties is that, when imported along with Syntax, they
 * add an extra validateChange method to every instance of Change that has an instance defined here.
 */
trait Instances {

  // This right-hand side of this expression is actually a class.
  // In scala, you can implement a one method class as an anonymous function
  implicit val addPlaylistValidator: ChangeValidator[AddPlaylist] =
    (change, database) =>
      if (!database.userIds.contains(change.userId)) {
        s"Invalid user id ${change.userId}".invalidNel
      } else if (database.playlistIds.contains(change.id)) {
        s"Playlist id ${change.id} already exists".invalidNel
      } else if (change.songs.exists(id => !database.songIds.contains(id))) {
        s"At least one song does not exist in db".invalidNel
      } else {
        (change, database).validNel
      }

  implicit val addSongValidator: ChangeValidator[AddSong] =
    (change, database) =>
      if (!database.playlistIds.contains(change.playlistId)) {
        "Invalid playlist id".invalidNel
      } else if (!database.songIds.contains(change.songId)) {
        "Invalid song id".invalidNel
      } else {
        (change, database).validNel
      }

  implicit val playlistRemoverValidator: ChangeValidator[RemovePlaylist] =
    (change, database) => (change, database).validNel

  implicit val universalValidator: ChangeValidator[Change] =
    (change, database) => {
      import syntax._
      change match {
        case c: AddPlaylist    => c.validateChange(database)
        case c: RemovePlaylist => c.validateChange(database)
        case c: AddSong        => c.validateChange(database)
      }
    }
}

/**
 * By convention, "syntax" refers to extension method definitions.
 */
trait Syntax {

  /**
   * Adds the validateChange method to every Change instance whose type has a ChangeValidator
   */
  implicit class ChangeValidatorOps[C <: Change](change: C) {
    def validateChange(
        database: Database
      )(implicit validator: ChangeValidator[C]
      ): ValidatedNel[String, (C, Database)] =
      validator.validate(change, database)
  }
}
