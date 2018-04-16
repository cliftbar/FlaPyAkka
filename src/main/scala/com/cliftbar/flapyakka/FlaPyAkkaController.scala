package com.cliftbar.flapyakka

// Akka
import akka.http.scaladsl.model.HttpHeader
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

    override def routes =
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
                pathEndOrSingleSlash {
                    get {
                        model.printUsers()
                        respondWithHeaders(RawHeader("server_id", this.serverId.toString)) {
                            complete("success")
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
            extractRequestContext { ctx =>
                val valid = UserValidator.validateUser(ctx.request.headers, this.model)
                println(valid)
                validate(valid.nonEmpty, "Invalid User") {
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
                //            }
            }
        } ~ pathPrefix("dev"){
            extractRequestContext { ctx =>
                val valid = UserValidator.validateUser(ctx.request.headers, this.model)
                println(valid)
                validate(valid.nonEmpty, "Invalid User") {
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
    def logAccess(innerRoute: Route, userId: Option[Int]): Route = {
        def toLogEntry(marker: String, f: Any => String) = (r: Any) => LogEntry(marker + f(r), akka.event.Logging.InfoLevel)
        extractRequest { request =>
            val id: String = userId.flatMap(s => Try(s.toString).toOption).getOrElse("unauthorized")
            logResult(toLogEntry(s"${id} ${request.method.name} ${request.uri} ==> ", {
                case c: RouteResult.Complete => c.response.status.toString()
                case x => s"unknown response part of type ${x.getClass}"
            }))
            return innerRoute
        }
    }

    def routingValidateAccess(innerRoute: Route): Route = {
        extractRequestContext { ctx =>
            val userIDheader: Seq[HttpHeader] = ctx.request.headers.filter(x => x.name() == "user-id")
            val userId: Option[Int] = this.model.validateUser(userIDheader(0).value.toInt)
            return logAccess(innerRoute, userId)
        }
    }

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
