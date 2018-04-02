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

import com.cliftbar.flapyakka.FlaPyAkkaModel

object AppRoutes {
    // Routes that this WebServer must handle are defined here
    def getRoutes(serverId: Int, model: => FlaPyAkkaModel ): Route =
        pathEndOrSingleSlash {
            get {
                complete("FlaPyAkka Home")
            }
            //    respondWithHeaders(RawHeader("id", server_id.toString), RawHeader("another_header", "hi")) {
            //      complete("Server " + server_id.toString + " up and running") // Completes with some text
            //    }
        } ~
          path("health") { // health check the server, gives some info and such
              get { // Listens only to GET requests

                  respondWithHeaders(RawHeader("serverId", serverId.toString)) {
                      complete("health-check")
                  }
              }
          } ~
          path("app") {
              // reset the application.
              path("reset") {
                  post {
                      entity(as[String]) {
                          json =>
                              val parsedJson = JsonParser(json).asJsObject
                              val resetType: String = parsedJson.fields("resetType").convertTo[String]
                              respondWithHeader(RawHeader("server id", serverId.toString))
                              complete("success")
                      }
                  }
              }
          }
}
