package com.github.mixtape.validators

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

class AddPlaylistValidatorProps extends Properties("AddPlaylist Validator") {
  import com.github.mixtape.generators.Generators._
  import com.github.mixtape.validators.instances.addPlaylistValidator

  ////`AddPlaylist with invalid user` is a function call
  property("refuses to add a playlist for a user who doesn't exist") =
    forAll(genDatabaseWith(`AddPlaylist with invalid user`)) {
      case (db, invalidReq) =>
        addPlaylistValidator.validate(invalidReq, db).isInvalid
    }

  property("refuses to add a playlist that already exists") =
    forAll(genDatabaseWith(`AddPlaylist with duplicate id`)) {
      case (db, invalidReq) =>
        addPlaylistValidator.validate(invalidReq, db).isInvalid
    }

  property("refuses to add a playlist when a song on it doesn't exist") =
    forAll(genDatabaseWith(`AddPlaylist with invalid song id`)) {
      case (db, invalidReq) =>
        addPlaylistValidator.validate(invalidReq, db).isInvalid
    }
}
