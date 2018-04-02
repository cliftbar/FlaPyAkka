package com.cliftbar.flapyakka

import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.{HttpApp, Route}
import com.cliftbar.flapyakka.routes._

// Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, _}

// JSON
import spray.json.JsonParser
import spray.json.DefaultJsonProtocol._
import spray.json._

// Java
import java.time.{LocalDateTime, Clock}
import java.time._

// FlaPyDisaster application server
object FlaPyAkkaController extends HttpApp with App {

    val model = new FlaPyAkkaModel
    val serverId = (math.random() * 1000).toInt
    val serverStartTime = LocalDateTime.now(Clock.systemUTC())

    def appRoutes: Route = AppRoutes.getRoutes(serverId, model)

    def hurricaneRoutes: Route = HurricaneRoutes.getRoutes(serverId)

    override def routes =
        pathEndOrSingleSlash {
            get {
                complete("FlaPyAkka Home")
            }
        }~ pathPrefix("health") { // health check the server, gives some info and such
            pathPrefix("check") {
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
            }
        } ~ pathPrefix("app") {
            // reset the application.
            pathPrefix("reset") {
                pathEndOrSingleSlash {
                    post {
                        entity(as[String]) {
                            json =>
                                val parsedJson = JsonParser(json).asJsObject
                                val resetType: String = parsedJson.fields("resetType").convertTo[String]
                                respondWithHeader(RawHeader("server id", this.serverId.toString))
                                complete("success")
                        }
                    }
                }
            }
        } ~ hurricaneRoutes

    // This will start the server until the return key is pressed
    val httpConfig = ConfigFactory.load().getConfig("akka.http")
    val interface = httpConfig.getString("server.interface")
    val port = httpConfig.getInt("server.port")
    //println(interface)
    //println(port)
    println("ServerID " + serverId.toString + " at " + interface.toString + ":" + port.toString)
    println(routes)
    startServer(interface, port)
}
