package com.github.mixtape.validators

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

class AddSongValidatorProps extends Properties("Add Song Validator") {
  import com.github.mixtape.generators.Generators._
  import com.github.mixtape.validators.instances.addSongValidator

  //`AddSong with invalid playlist id` is a function call
  property("fails when playlist doesn't exist") =
    forAll(genDatabaseWith(`AddSong with invalid playlist id`)) {
      case (db, invalidReq) =>
        addSongValidator.validate(invalidReq, db).isInvalid
    }

  property("fails when song doesn't exist") =
    forAll(genDatabaseWith(`AddSong with invalid song id`)) {
      case (db, invalidReq) =>
        addSongValidator.validate(invalidReq, db).isInvalid
    }
}
