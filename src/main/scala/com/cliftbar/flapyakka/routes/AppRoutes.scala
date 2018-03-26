package com.cliftbar.flapyakka.routes

// Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route

// JSON
import spray.json.JsonParser
import spray.json.DefaultJsonProtocol._

// Java
import java.time.LocalDateTime

object AppRoutes {
  var serverId: Int = -1
  // Routes that this WebServer must handle are defined here
  def getRoutes(serverId: Int): Route =
    pathEndOrSingleSlash {
      get {
        complete("home response")
      }
      //    respondWithHeaders(RawHeader("id", server_id.toString), RawHeader("another_header", "hi")) {
      //      complete("Server " + server_id.toString + " up and running") // Completes with some text
      //    }
    } ~
    path("health") { // health check the server, gives some info and such
      get { // Listens only to GET requests
        respondWithHeaders(RawHeader("server_id", this.serverId.toString), RawHeader("QueryTime", LocalDateTime.now().toString)) {
          complete("healthCheck")
        }
      }
    } ~
    path("app" / "reset") {
    // reset the application.
      post {
        entity (as[String] ) {
          json =>
          val parsedJson = JsonParser (json).asJsObject
          val resetType: String = parsedJson.fields ("resetType").convertTo[String]
          respondWithHeader (RawHeader ("server id", this.serverId.toString) )
          complete ("success")
        }
      }
    }
}
