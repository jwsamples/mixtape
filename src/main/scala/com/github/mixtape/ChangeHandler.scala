package com.github.mixtape

import cats.data.NonEmptyList
import com.github.mixtape.model.{Change, Database}
import com.github.mixtape.appliers.instances._
import com.github.mixtape.appliers.syntax._
import com.github.mixtape.validators.instances._
import com.github.mixtape.validators.syntax._

object ChangeHandler {

  /**
   * Validate change request and apply it by mutating database if necessary.
   * @return Updated database
   */
  def handleChange(
      change: Change,
      database: Database
    ): Either[NonEmptyList[String], Database] =
    change
      .validateChange(database) // This and the follow line use implicit methods added to change by syntax imports
      .map {
        case (validatedChange, validatedDb) =>
          validatedChange.applyTo(validatedDb)
      }
      .toEither // Either because we will probably need to represent non-validation related failures
}
