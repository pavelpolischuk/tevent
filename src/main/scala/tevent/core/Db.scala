package tevent.core

import slick.SlickException
import slick.basic.BasicBackend
import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import slick.util.ClassLoaderUtil
import zio.{IO, RIO, Task, URIO, URLayer, ZIO, ZLayer}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

object Db {
  trait Service {
    def db: BasicBackend#DatabaseDef
    def profile: JdbcProfile
  }

  def fromConfig: URLayer[Config, Db] =
    ZLayer.fromService[Config.DbConfig, Db.Service](c => new Service {
      override val db: BasicBackend#DatabaseDef = Database.forURL(url = c.url, user = c.user, password = c.password, driver = c.driver)
      override val profile = loadProfile(c.profile)
    })

  private def loadProfile(classPath: String): JdbcProfile = {
    val classLoader = ClassLoaderUtil.defaultClassLoader
    val untypedP = try {
      if (classPath.endsWith("$")) classLoader.loadClass(classPath).getField("MODULE$").get(null)
      else classLoader.loadClass(classPath).getConstructor().newInstance()
    } catch {
      case NonFatal(ex) =>
        throw new SlickException(s"""Error getting instance of profile "$classPath"""", ex)
    }
    val pClass = implicitly[ClassTag[JdbcProfile]].runtimeClass
    if (!pClass.isInstance(untypedP))
      throw new SlickException(s"Configured profile $classPath does not conform to requested profile ${pClass.getName}")
    untypedP.asInstanceOf[JdbcProfile]
  }

  def profile: URIO[Db, JdbcProfile] = ZIO.access(_.get.profile)

  def db: URIO[Db, BasicBackend#DatabaseDef] = ZIO.access(_.get.db)


  implicit class ZIOOps[R](private val dbio: DBIO[R]) extends AnyVal {
    def toZIO: RIO[Db.Service, R] =
      for {
        db <- ZIO.access[Db.Service](_.db)
        r  <- ZIO.fromFuture(implicit ec => db.run(dbio))
      } yield r
  }

  implicit class TaskOps[T](private val task: Task[T]) extends AnyVal {
    def refineRepositoryError: IO[RepositoryError, T] = task.refineOrDie {
      case e: Exception => RepositoryError(e)
    }
  }
}
