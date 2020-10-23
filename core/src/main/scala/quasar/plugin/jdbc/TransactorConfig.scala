/*
 * Copyright 2020 Precog Data
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quasar.plugin.jdbc

import scala._
import scala.concurrent.duration._

/** Configuration for a `doobie.Transactor`.
  *
  * @param driverConfig describes how to configure and load the JDBC driver
  * @param connectionMaxConcurrency the maximum number of concurrent connections to allow to the database
  * @param connectionReadOnly whether to obtain `Connection`s in read-only mode
  * @param connectionTimeout how long to await a database connection before erroring (default: 30s)
  * @param connectionValidationTimeout how long to await validation of a connection (default: 5s)
  * @param connectionMaxLifetime the maximum lifetime of a database connection,
  *                              idle connections exceeding their lifetime will be replaced with
  *                              new ones. Make sure to set this several seconds shorter than any
  *                              database or infrastructure imposed connection time limit. (default: 30min)
  * @param connectionInitFailTimeout the time before the pool initialization fails, or 0 to
  *                                  validate connection setup but continue with pool start, or less than
  *                                  zero to skip all initialization checks and start the pool without delay.
  *                                  (default: 1ms)
  */
final case class TransactorConfig(
    driverConfig: JdbcDriverConfig,
    connectionMaxConcurrency: Int,
    connectionReadOnly: Boolean,
    connectionTimeout: FiniteDuration,
    connectionValidationTimeout: FiniteDuration,
    connectionMaxLifetime: FiniteDuration,
    connectionInitFailTimeout: FiniteDuration)

object TransactorConfig {
  val DefaultConnectionTimeout: FiniteDuration = 30.seconds
  val DefaultConnectionValidationTimeout: FiniteDuration = 5.seconds
  val DefaultConnectionMaxLifetime: FiniteDuration = 30.minutes
  val DefaultInitFailTimeout: FiniteDuration = 1.milli

  def withDefaultTimeouts(
      driverConfig: JdbcDriverConfig,
      connectionMaxConcurrency: Int,
      connectionReadOnly: Boolean)
      : TransactorConfig =
    TransactorConfig(
      driverConfig = driverConfig,
      connectionMaxConcurrency = connectionMaxConcurrency,
      connectionReadOnly = connectionReadOnly,
      connectionTimeout = DefaultConnectionTimeout,
      connectionValidationTimeout = DefaultConnectionValidationTimeout,
      connectionMaxLifetime = DefaultConnectionMaxLifetime,
      connectionInitFailTimeout = DefaultInitFailTimeout)
}
