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

package quasar.lib.jdbc.datasource

import quasar.lib.jdbc._
import quasar.lib.jdbc.implicits._

import scala.{Boolean, None, Option, Some}
import scala.util.{Left, Right}

import java.lang.Throwable

import cats.Defer
import cats.data.{Ior, NonEmptyList}
import cats.effect.{Bracket, Resource}
import cats.implicits._

import doobie._
import doobie.implicits._

import fs2.Stream

import quasar.api.resource.{ResourceName, ResourcePath, ResourcePathType => RPT}
import quasar.api.datasource.DatasourceType
import quasar.connector.QueryResult
import quasar.connector.datasource.{DatasourceModule, Loader}
import quasar.qscript.InterpretedRead

import shims.equalToCats

final class JdbcDatasource[F[_]: Bracket[?[_], Throwable]: Defer] private (
    xa: Transactor[F],
    discovery: JdbcDiscovery,
    val kind: DatasourceType,
    val loaders: NonEmptyList[Loader[Resource[F, ?], InterpretedRead[ResourcePath], QueryResult[F]]])
    extends DatasourceModule.DS[F] {

  def pathIsResource(path: ResourcePath): Resource[F, Boolean] =
    resourcePathRef(path).fold(false.pure[Resource[F, ?]]) {
      case Left(table) =>
        Resource.eval(discovery.tableExists(table, None).transact(xa))

      case Right((schema, table)) =>
        Resource.eval(discovery.tableExists(table, Some(schema)).transact(xa))
    }

  def prefixedChildPaths(prefixPath: ResourcePath): Resource[F, Option[Stream[F, (ResourceName, RPT.Physical)]]] = {
    type Out[X[_]] = Stream[X, (ResourceName, RPT.Physical)]

    if (prefixPath === ResourcePath.Root)
      discovery.topLevel
        .map(_.fold(
          s => (ResourceName(s.asString), RPT.prefix),
          t => (ResourceName(t.asString), RPT.leafResource)))
        .transact(xa)
        .some
        .pure[Resource[F, ?]]
    else
      resourcePathRef(prefixPath).fold((None: Option[Out[F]]).pure[Resource[F, ?]]) {
        case Right((schema, table)) =>
          Resource.eval {
            discovery.tableExists(table, Some(schema))
              .map(p => if (p) Some(Stream.empty: Out[F]) else None)
              .transact(xa)
          }

        case Left(ident) =>
          for {
            c <- xa.strategicConnection

            isTable <- Resource.eval(xa.runWith(c).apply(discovery.tableExists(ident, None)))

          } yield if (isTable) {
            (Stream.empty: Out[F]).some
          } else {
            discovery.tables(Ior.left(ident))
              .map(m => (ResourceName(m.table.asString), RPT.leafResource))
              .translate(xa.runWith(c))
              .some
          }
      }
  }
}

object JdbcDatasource {
  def apply[F[_]: Bracket[?[_], Throwable]: Defer](
      xa: Transactor[F],
      discovery: JdbcDiscovery,
      datasourceType: DatasourceType,
      loaders: NonEmptyList[Loader[Resource[F, ?], InterpretedRead[ResourcePath], QueryResult[F]]])
      : DatasourceModule.DS[F] =
    new JdbcDatasource(xa, discovery, datasourceType, loaders)
}
