package com.github.mixtape.generators

import cats.data.{NonEmptyList, NonEmptySet}
import cats.instances.string._
import com.github.mixtape.model.Change.{AddPlaylist, AddSong, RemovePlaylist}
import com.github.mixtape.model.Database
import com.github.mixtape.model.{Playlist, Song, User}
import org.scalacheck.Gen

import scala.collection.immutable.SortedSet

object Generators {

  lazy val genPositiveInt: Gen[Int] = Gen.choose(1, Int.MaxValue)

  lazy val genPositiveIntStr: Gen[String] = genPositiveInt.map(i => s"$i")

  lazy val genAlphaNumSpaceStr: Gen[String] =
    Gen
      .listOf(Gen.frequency((1, Gen.const[Char](' ')), (9, Gen.alphaNumChar)))
      .map(_.mkString)

  def nonExistentUser(database: Database): Gen[User] =
    genUser.retryUntil(u => !database.userIds.contains(u.id))

  lazy val genUser: Gen[User] =
    for {
      id <- genPositiveIntStr
      name <- genAlphaNumSpaceStr
    } yield User(id, name)

  lazy val genSong: Gen[Song] =
    for {
      id <- genPositiveIntStr
      artist <- genAlphaNumSpaceStr
      title <- genAlphaNumSpaceStr
    } yield Song(id, artist, title)

  private def genValidPlaylist(
      possibleUsers: NonEmptyList[User],
      possibleSongs: NonEmptyList[Song]
    ): Gen[Playlist] =
    for {
      id <- genPositiveIntStr
      userId <- Gen.oneOf(possibleUsers.toList).map(_.id)
      songIds <- Gen
        .nonEmptyListOf(Gen.oneOf(possibleSongs.toList))
        .map(_.map(_.id))
        .map(SortedSet[String](_: _*))
        .map(NonEmptySet.fromSetUnsafe)
    } yield Playlist(id, userId, songIds)

  lazy val genDatabase: Gen[Database] =
    for {
      users <- Gen.nonEmptyListOf(genUser).map(NonEmptyList.fromListUnsafe)
      songs <- Gen.nonEmptyListOf(genSong).map(NonEmptyList.fromListUnsafe)
      playlists <- Gen.nonEmptyListOf(genValidPlaylist(users, songs))
    } yield Database(playlists, songs, users)

  def genDatabaseWith[T](genItem: Database => Gen[T]): Gen[(Database, T)] =
    genDatabase.flatMap(db => genItem(db).map(db -> _))

  lazy val genAddPlaylist: Gen[AddPlaylist] =
    for {
      id <- genPositiveIntStr
      userId <- genPositiveIntStr
      songs <- Gen
        .nonEmptyListOf(genPositiveIntStr)
        .map(SortedSet[String](_: _*))
        .map(NonEmptySet.fromSetUnsafe)
    } yield AddPlaylist(id, userId, songs)

  /*
    Generators with friendly names because they're effectively part of the title of a property.
    When combined with genDatabaseWith, they represent scenarios: a database and a request with some behavior-relevant
    attribute.
   */
  def `Valid AddPlaylist`(database: Database): Gen[AddPlaylist] =
    genValidPlaylist(database.users, database.songs)
      .map { playlist =>
        AddPlaylist(playlist.id, playlist.userId, playlist.songIds)
      }

  def `Valid RemovePlaylist`(database: Database): Gen[RemovePlaylist] =
    for {
      id <- Gen.oneOf(database.playlistIds)
    } yield RemovePlaylist(id)

  def `Valid AddSong`(database: Database): Gen[AddSong] =
    for {
      songId <- Gen.oneOf(database.songIds.toSortedSet)
      playlistId <- Gen.oneOf(database.playlistIds)
    } yield AddSong(playlistId, songId)

  def `AddPlaylist with invalid user`(database: Database): Gen[AddPlaylist] =
    `Valid AddPlaylist`(database)
      .flatMap(
        playlist =>
          nonExistentUser(database)
            .map(badUser => playlist.copy(userId = badUser.id))
      )

  def `AddPlaylist with duplicate id`(database: Database): Gen[AddPlaylist] =
    `Valid AddPlaylist`(database)
      .map(playlist => playlist.copy(id = database.playlists.head.id))

  def `AddPlaylist with invalid song id`(database: Database): Gen[AddPlaylist] =
    `Valid AddPlaylist`(database)
      .flatMap { playlist =>
        genPositiveIntStr
          .retryUntil(id => !database.songIds.contains(id))
          .flatMap { invalidSongId =>
            genSong
              .map(_.copy(id = invalidSongId))
              .map(
                invalidSong =>
                  playlist.copy(songs = NonEmptySet.of(invalidSong.id))
              )
          }
      }

  def `AddSong with invalid playlist id`(database: Database): Gen[AddSong] =
    `Valid AddSong`(database)
      .flatMap(
        req =>
          genPositiveIntStr
            .map(newId => req.copy(playlistId = newId))
            .retryUntil(
              newReq => !database.playlistIds.contains(newReq.playlistId)
            )
      )

  def `AddSong with invalid song id`(database: Database): Gen[AddSong] =
    `Valid AddSong`(database)
      .flatMap(
        req =>
          genPositiveIntStr
            .map(newId => req.copy(songId = newId))
            .retryUntil(newReq => !database.songIds.contains(newReq.songId))
      )

  def `RemovePlaylist with invalid id`(
      database: Database
    ): Gen[RemovePlaylist] =
    `Valid RemovePlaylist`(database)
      .flatMap(
        playlist =>
          genPositiveIntStr
            .map(newId => playlist.copy(id = newId))
            .retryUntil(playlist => !database.playlistIds.contains(playlist.id))
      )

}
