package com.github.mixtape

import java.nio.file.{Path, Paths}

import cats.effect.{ContextShift, ExitCode, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import com.github.mixtape.io.{ChangeStreamer, DatabaseDumper, DatabaseLoader}
import com.github.mixtape.model.{Change, Database}
import monix.eval.{Task, TaskApp}
import org.slf4j.LoggerFactory

case class Options(dbPath: Path, changesPath: Path)

object App extends TaskApp {
  private val logger = LoggerFactory.getLogger(getClass)
  private val OutputPath = "output.json"

  /**
   * Entry point
   */
  override def run(args: List[String]): Task[ExitCode] = {
    for {
      opts <- parseCommandLineOptions[Task](args)
      _ <- run[Task](opts.dbPath, opts.changesPath)
    } yield {
      logger.info(s"Processing complete.  Final db dumped to: $OutputPath")
      ExitCode.Success
    }
  }

  private def run[F[_]: Sync: ContextShift](
      databasePath: Path,
      changePath: Path
    ): F[Unit] =
    for {
      db <- DatabaseLoader.load[F](databasePath)
      updatedDb <- applyChanges[F](db, changePath)
      _ <- DatabaseDumper.dump[F](Paths.get(OutputPath), updatedDb)
    } yield ()

  /**
   * Load changes from disk, apply them to database one by one, return updated database.
   */
  private def applyChanges[F[_]: Sync: ContextShift](
      db: Database,
      changePath: Path
    ): F[Database] =
    ChangeStreamer
      .stream[F](changePath)
      .fold(db) {
        case (curDb, change) => applySingleChange(curDb, change)
      }
      .compile // turn Stream into an F
      .lastOrError // when program is executed, pull every element of the stream

  private def applySingleChange(curDb: Database, change: Change): Database =
    ChangeHandler.handleChange(change, curDb) match {
      case Right(updatedDb) =>
        import DiffResult.instances.show
        logger.info(
          s"Processed $change. Changes: ${DatabaseDiff.diff(updatedDb, curDb).show}"
        )
        updatedDb
      case Left(e) =>
        logger.warn(
          s"Ignored invalid change $change (Validation Error: $e)"
        )
        curDb
    }

  // Should really be an either or something, but this makes for easier composition
  private def parseCommandLineOptions[F[_]: Sync](
      args: List[String]
    ): F[Options] =
    args match {
      case dbPath :: changesPath :: Nil =>
        Sync[F].delay {
          Options(Paths.get(dbPath), Paths.get(changesPath))
        }
      case _ =>
        Sync[F].raiseError(
          new IllegalArgumentException(
            "Invalid command line arguments. Usage: app <database file> <changes file>"
          )
        )
    }
}
