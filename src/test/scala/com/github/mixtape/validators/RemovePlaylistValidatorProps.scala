package com.github.mixtape.validators

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

class RemovePlaylistValidatorProps
    extends Properties("RemovePlaylist Validator") {
  import com.github.mixtape.generators.Generators._
  import com.github.mixtape.validators.instances.playlistRemoverValidator

  // `RemovePlaylist with invalid id` is a function call
  property("ignores request to delete playlist that doesn't exist") =
    forAll(genDatabaseWith(`RemovePlaylist with invalid id`)) {
      case (db, invalidReq) =>
        playlistRemoverValidator.validate(invalidReq, db).isValid
    }

}
