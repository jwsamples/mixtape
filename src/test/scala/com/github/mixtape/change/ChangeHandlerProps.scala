package com.github.mixtape.change

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

class ChangeHandlerProps extends Properties("Change Handler") {
  import com.github.mixtape.appliers.PlaylistAdderProps.playlistAdded
  import com.github.mixtape.appliers.PlaylistRemoverProps.playlistRemoved
  import com.github.mixtape.appliers.SongAdderProps.songAdded
  import com.github.mixtape.generators.Generators._
  import com.github.mixtape.ChangeHandler.handleChange

  //`Valid AddPlaylist` is a function call
  property("inserts a playlist in response to valid AddPlaylist req") =
    forAll(genDatabaseWith(`Valid AddPlaylist`)) {
      case (db, change) =>
        handleChange(change, db)
          .map(playlistAdded(change, _))
          .getOrElse(false)
    }

  property("removes playlist in response to valid RemovePlaylist req") =
    forAll(genDatabaseWith(`Valid RemovePlaylist`)) {
      case (db, change) =>
        handleChange(change, db)
          .map(playlistRemoved(change, _))
          .getOrElse(false)
    }

  property("adds song in response to valid AddSong req") =
    forAll(genDatabaseWith(`Valid AddSong`)) {
      case (db, change) =>
        handleChange(change, db)
          .map(songAdded(change, _))
          .getOrElse(false)
    }

  property("refuses to add a playlist for a user who doesn't exist") =
    forAll(genDatabaseWith(`AddPlaylist with invalid user`)) {
      case (db, invalidChange) =>
        handleChange(invalidChange, db).isLeft
    }

  property("refuses to add a playlist that already exists") =
    forAll(genDatabaseWith(`AddPlaylist with duplicate id`)) {
      case (db, invalidChange) =>
        handleChange(invalidChange, db).isLeft
    }

  property("refuses to add a playlist when a song on it doesn't exist") =
    forAll(genDatabaseWith(`AddPlaylist with invalid song id`)) {
      case (db, invalidChange) =>
        handleChange(invalidChange, db).isLeft
    }

  property("refuses to add song when playlist doesn't exist") =
    forAll(genDatabaseWith(`AddSong with invalid playlist id`)) {
      case (db, invalidChange) =>
        handleChange(invalidChange, db).isLeft
    }

  property("refuses to add song when song doesn't exist") =
    forAll(genDatabaseWith(`AddSong with invalid song id`)) {
      case (db, invalidChange) =>
        handleChange(invalidChange, db).isLeft
    }

  property("ignores request to delete playlist that doesn't exist") =
    forAll(genDatabaseWith(`RemovePlaylist with invalid id`)) {
      case (db, invalidChange) =>
        handleChange(invalidChange, db)
          .map(updatedDb => updatedDb.playlistIds.size == db.playlistIds.size)
          .getOrElse(false)
    }
}
