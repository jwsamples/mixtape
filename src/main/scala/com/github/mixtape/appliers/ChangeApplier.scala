package com.github.mixtape.appliers

import com.github.mixtape.model.Change.{AddPlaylist, AddSong, RemovePlaylist}
import com.github.mixtape.model.{Change, Database, Playlist}

object ChangeApplier {
  def applyTo[C <: Change](
      implicit instance: ChangeApplier[C]
    ): ChangeApplier[C] = instance
}

trait ChangeApplier[C <: Change] {
  def applyChange(change: C, database: Database): Database
}

/**
 * These implement ChangeApplier behavior for various types.
 *
 * The practical upshot of these properties is that, when imported along with Syntax, they
 * add an extra applyTo method to every instance of Change that has an instance defined here.
 */
trait Instances {

  // This right-hand side of this expression is actually a class.
  // In scala, you can implement a one method class as an anonymous function
  implicit val playlistAdder: ChangeApplier[AddPlaylist] =
    (change, database) =>
      database.copy(
        playlists = database.playlists :+ Playlist(
          change.id,
          change.userId,
          change.songs
        )
      )

  implicit val playlistRemover: ChangeApplier[RemovePlaylist] =
    (change, database) =>
      database.copy(playlists = database.playlists.filterNot(_.id == change.id))

  implicit val songAdder: ChangeApplier[AddSong] =
    new ChangeApplier[AddSong] {
      private def mutatePlaylist(
          playlists: List[Playlist],
          id: String,
          mutation: Playlist => Playlist
        ): List[Playlist] =
        playlists
          .map {
            case p if p.id == id => mutation(p)
            case p               => p
          }

      override def applyChange(change: AddSong, database: Database): Database =
        database.copy(
          playlists = mutatePlaylist(
            database.playlists,
            change.playlistId,
            p => p.copy(songIds = p.songIds.add(change.songId))
          )
        )
    }

  implicit val universalChangeApplier: ChangeApplier[Change] =
    (change, database) => {
      import syntax._
      change match {
        case c: AddPlaylist    => c.applyTo(database)
        case c: RemovePlaylist => c.applyTo(database)
        case c: AddSong        => c.applyTo(database)
      }
    }
}

/**
 * By convention, "syntax" refers to extension method definitions.
 */
trait Syntax {

  /**
   * Adds the applyTo method to every Change instance whose type has a ChangeApplier
   */
  implicit class ChangeApplierOps[C <: Change](change: C) {
    def applyTo(
        database: Database
      )(implicit applier: ChangeApplier[C]
      ): Database =
      applier.applyChange(change, database)
  }
}
