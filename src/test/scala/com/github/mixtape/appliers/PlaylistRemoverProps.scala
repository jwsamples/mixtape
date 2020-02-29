package com.github.mixtape.appliers

import com.github.mixtape.model.Change.RemovePlaylist
import com.github.mixtape.model.Database
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

class PlaylistRemoverProps extends Properties("Playlist Remover") {
  import com.github.mixtape.generators.Generators._
  import com.github.mixtape.appliers.instances.playlistRemover
  import PlaylistRemoverProps.playlistRemoved

  //`Valid RemovePlaylist` is a function call
  property("removes correct playlist") =
    forAll(genDatabaseWith(`Valid RemovePlaylist`)) {
      case (db, validReq) =>
        val updatedDb = playlistRemover.applyChange(validReq, db)
        playlistRemoved(validReq, updatedDb)

    }
}

object PlaylistRemoverProps {
  def playlistRemoved(change: RemovePlaylist, db: Database): Boolean =
    db.playlistIds
      .contains(change.id) == false
}
