package com.github.mixtape.appliers

import com.github.mixtape.model.Change.AddSong
import com.github.mixtape.model.Database
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

class SongAdderProps extends Properties("Song Adder") {
  import com.github.mixtape.generators.Generators._
  import com.github.mixtape.appliers.instances.songAdder
  import SongAdderProps.songAdded

  //`Valid AddSong` is a function call
  property("inserts a valid song to a valid playlist correctly") =
    forAll(genDatabaseWith(`Valid AddSong`)) {
      case (db, validReq) =>
        val updatedDb = songAdder.applyChange(validReq, db)
        songAdded(validReq, updatedDb)
    }
}

object SongAdderProps {
  def songAdded(change: AddSong, db: Database): Boolean =
    db.playlists
      .find(_.id == change.playlistId)
      .exists(actual => actual.songIds.contains(change.songId))
}
