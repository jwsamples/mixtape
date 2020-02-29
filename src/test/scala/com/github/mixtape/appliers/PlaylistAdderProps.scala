package com.github.mixtape.appliers

import com.github.mixtape.model.Change.AddPlaylist
import com.github.mixtape.model.Database
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

class PlaylistAdderProps extends Properties("Playlist Adder") {
  import PlaylistAdderProps._
  import com.github.mixtape.appliers.instances.playlistAdder
  import com.github.mixtape.generators.Generators._

  //`Valid AddPlaylist` is a function call
  property("inserts a valid playlist") =
    forAll(genDatabaseWith(`Valid AddPlaylist`)) {
      case (db, change) =>
        val updatedDb = playlistAdder.applyChange(change, db)
        playlistAdded(change, updatedDb)
    }

}

object PlaylistAdderProps {
  def playlistAdded(change: AddPlaylist, db: Database): Boolean =
    db.playlists
      .find(_.id == change.id)
      .exists(
        actual =>
          actual.userId == change.userId && actual.songIds == change.songs
      )
}
