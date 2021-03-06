package com.cliftbar.flapyakka

// Akka
import akka.actor.ActorLogging
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.directives.LogEntry
import com.cliftbar.flapyakka.models.FlaPyAkkaModel
import com.cliftbar.flapyakka.routes._
import com.cliftbar.flapyakka.routes.UserValidator

import scala.util.Try

// Typesafe Config
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory, ConfigValue, _}

// Akka
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.model.headers.RawHeader

// JSON
import spray.json.JsonParser
import spray.json.DefaultJsonProtocol._
import spray.json._

// Java
import java.time.{LocalDateTime, Clock}
import java.time._

// FlaPyDisaster application server
object FlaPyAkkaController extends HttpApp with App {

    var model = new FlaPyAkkaModel
    val serverId = (math.random() * 1000).toInt
    val serverStartTime = LocalDateTime.now(Clock.systemUTC())
    val serverTempDir: String = ConfigFactory.load().getConfig("app").getString("tempDir")
    def hurricaneRoutes: Route = HurricaneRoutes.getRoutes(serverId, model)


    override def routes: Route = optionalHeaderValueByName("user-id") { userIdHeader =>
      val userId: Option[Int] = Try(userIdHeader.get.toInt).toOption
        pathEndOrSingleSlash {
            get {
                complete("FlaPyAkka Home")
            }
        } ~ pathPrefix("auth") {
            pathPrefix("login") {
                pathEndOrSingleSlash {
                    post {
                        entity(as[String]) {
                            json =>
                                val parsedJson = JsonParser(json).asJsObject
                                val username: String = parsedJson.fields("username").convertTo[String]

                                val respData: JsObject = JsObject(
                                    "id" -> JsNumber(-1)
                                )

                                val respEntity: HttpEntity.Strict = HttpEntity(MediaTypes.`application/json`, respData.toString())
                                respondWithHeaders(RawHeader("server_id", this.serverId.toString)) {
                                    complete(respEntity)
                                }
                        }
                    }
                }
            } ~ pathPrefix("create-user") {
                pathEndOrSingleSlash {
                    post {
                        entity(as[String]) {
                            json =>
                                val parsedJson = JsonParser(json).asJsObject
                                val username: String = parsedJson.fields("username").convertTo[String]

                                val id = model.addUser(username)
                                val respData: JsObject = JsObject(
                                    "id" -> JsNumber(id)
                                )

                                val respEntity: HttpEntity.Strict = HttpEntity(MediaTypes.`application/json`, respData.toString())
                                respondWithHeaders(RawHeader("server_id", this.serverId.toString)) {
                                    complete(respEntity)
                                }
                        }
                    }
                }
            } ~ pathPrefix("list-users") {
                authorize(this.model.validateUser(userId).nonEmpty) {
                    pathEndOrSingleSlash {
                        get {
                            model.printUsers()
                            respondWithHeaders(RawHeader("server_id", this.serverId.toString)) {
                                complete("success")
                            }
                        }
                    }
                }
            }
        } ~ pathPrefix("server") { // health check the server, gives some info and such
            pathPrefix("health") {
                pathEndOrSingleSlash {
                    get { // Listens only to GET requests
                        val respData: JsObject = JsObject(
                            "serverId" -> JsNumber(this.serverId)
                            , "uptime" -> JsNumber(Duration.between(this.serverStartTime, LocalDateTime.now(Clock.systemUTC())).getSeconds)
                        )

                        val respEntity: HttpEntity.Strict = HttpEntity(MediaTypes.`application/json`, respData.toString())
                        respondWithHeaders(RawHeader("server_id", this.serverId.toString)) {
                            complete(respEntity)
                        }
                    }
                }
            } ~ pathPrefix("info") {
                pathEndOrSingleSlash {
                    get {
                        respondWithHeaders(RawHeader("server_id", this.serverId.toString)) {
                            complete("success")
                        }
                    }
                }
            }
        } ~ pathPrefix("app") {
            authorize(this.model.validateUser(userId).nonEmpty) {
                pathPrefix("reset") { // reset the application.
                    pathEndOrSingleSlash {
                        post {
                            entity(as[String]) {
                                json =>
                                    val parsedJson = JsonParser(json).asJsObject
                                    val resetType: String = parsedJson.fields("resetType").convertTo[String]
                                    this.model = new FlaPyAkkaModel
                                    respondWithHeader(RawHeader("server id", this.serverId.toString))
                                    complete("success")
                            }
                        }
                    }
                }
            }
        } ~ pathPrefix("dev") {
            authorize(this.model.validateUser(userId).nonEmpty) {
                pathPrefix("test") {
                    pathEndOrSingleSlash {
                        get {
                            complete("test")
                        }
                    }
                }
            }
        }
    } ~ hurricaneRoutes

    /**Produces a log entry for every RouteResult. The log entry includes the request URI */

//    def routingValidateAccess(userId: Option[String]): Boolean = {
//        val checkVal: Option[Int] = Try(userId.get.toInt).toOption
//        val check = if (checkVal.isEmpty) {
//            false
//        } else {
//            this.model.validateUser(checkVal.get).nonEmpty
//        }
//        return check
//    }

    //val compoundRoute: Route = this.appRoutes ~ this.hurricaneRoutes
    //override def routes = routingValidateAccess(compoundRoute)

// This will start the server until the return key is pressed

//    var conf = ConfigFactory.empty()
//    conf = conf.withValue("test", ConfigValueFactory.fromAnyRef(1))
//    conf = conf.withValue("teststr", ConfigValueFactory.fromAnyRef("2"))
//    import java.io._
//    val fi = new FileWriter("testConfig.conf")
//
//    fi.write(conf.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
//    fi.close

val httpConfig = ConfigFactory.load().getConfig ("akka.http")
val interface = httpConfig.getString ("server.interface")
val port = httpConfig.getInt ("server.port")
println ("ServerID " + serverId.toString + " at " + interface.toString + ":" + port.toString)
startServer (interface, port)
}
