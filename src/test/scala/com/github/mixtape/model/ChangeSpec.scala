package com.github.mixtape.model

import com.github.mixtape.model.Change._
import com.github.mixtape.model.Change.decoders._
import io.circe.Json
import org.scalacheck.Prop.forAll
import org.scalacheck.{Gen, Properties}

class ChangeSpec extends Properties("Change Decoders") {
  property("decodes AddPlaylist without error") = forAll(addPlaylistJson) {
    (json: Json) =>
      json.as[AddPlaylist].isRight
  }
  property("decodes AddSong without error") = forAll(addSongJson) {
    (json: Json) =>
      json.as[AddSong].isRight
  }
  property("decodes RemovePlaylist without error") =
    forAll(removePlaylistJson) { (json: Json) =>
      json.as[RemovePlaylist].isRight
    }

//  TODO repeated
  private val positiveInt: Gen[Int] = Gen.choose(1, Int.MaxValue)
  private val positiveIntStr: Gen[String] = positiveInt.map(i => s"$i")

  private val addPlaylistJson: Gen[Json] =
    for {
      id <- positiveIntStr
      userId <- positiveIntStr
      songs <- Gen.nonEmptyListOf(positiveIntStr)
    } yield
      Json.obj(
        ("id" -> Json.fromString(id)),
        ("operation" -> Json.fromString("AddPlaylist")),
        ("user_id" -> Json.fromString(userId)),
        ("songs" -> Json.arr(songs.map(Json.fromString): _*))
      )

  private val removePlaylistJson: Gen[Json] =
    for {
      id <- positiveIntStr
    } yield
      Json.obj(
        ("id" -> Json.fromString(id)),
        ("operation" -> Json.fromString("RemovePlaylist"))
      )

  private val addSongJson: Gen[Json] =
    for {
      songId <- positiveIntStr
      playlistId <- positiveIntStr
    } yield
      Json.obj(
        ("song_id" -> Json.fromString(songId)),
        ("playlist_id" -> Json.fromString(playlistId)),
        ("operation" -> Json.fromString("AddSong"))
      )
}
