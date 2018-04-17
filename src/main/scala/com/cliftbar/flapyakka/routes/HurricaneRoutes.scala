package com.cliftbar.flapyakka.routes

// Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.cliftbar.flapyakka.FlaPyAkkaController.{authorize, extractRequestContext, optionalHeaderValueByName, validate}
import com.cliftbar.flapyakka.models.FlaPyAkkaModel
import spray.json.JsonParser
import spray.json.DefaultJsonProtocol._

import scala.io.Source.fromURL
import scala.util.Try

object HurricaneRoutes {
    // Routes that this WebServer must handle are defined here
    def getRoutes(serverId: Int, model: FlaPyAkkaModel): Route =
        pathPrefix("hurricane") {
            optionalHeaderValueByName("user-id") { userIdHeader =>
                val userId: Option[Int] = Try(userIdHeader.get.toInt).toOption
                authorize(model.validateUser(userId).nonEmpty) {
                    pathEndOrSingleSlash {
                        get {
                            complete("hello hurricane")
                        }
                    } ~ pathPrefix("catalog") {
                        pathEndOrSingleSlash {
                            get {
                                complete("success")
                            } ~ post {
                                entity(as[String]) {
                                    json =>
                                        val parsedJson = JsonParser(json).asJsObject
                                        val catalogName: String = parsedJson.fields("catalogName").convertTo[String]
                                        model.hurricaneModel.createCatalog(userId.get, catalogName)
                                        complete("success")
                                }
                            } ~ delete {
                                complete("success")
                            }
                        } ~ pathPrefix("add-event") {
                            entity(as[String]) {
                                json =>
                                    println("test")
                                    val parsedJson = JsonParser(json).asJsObject
                                    val catalogName: String = parsedJson.fields("catalogName").convertTo[String]
                                    val eventName: String = parsedJson.fields("eventName").convertTo[String]

                                    model.hurricaneModel.addEventToCatalog(userId.get, catalogName, eventName)
                                    complete("unisys success")
                            }
                        } ~ pathPrefix("remove-event") {
                            complete("success")
                        } ~ pathPrefix("get-event-names") {
                            complete("success")
                        }
                    } ~ pathPrefix("event") {
                        pathPrefix("unisys") {
                            pathEndOrSingleSlash {
                                post {
                                    entity(as[String]) {
                                        json =>
                                            println("test")
                                            val parsedJson = JsonParser(json).asJsObject
                                            val eventName: String = parsedJson.fields("eventName").convertTo[String]
                                            val unisysUrl: String = parsedJson.fields("unisysUrl").convertTo[String]

                                            val unisysFileLines: Seq[String] = fromURL(unisysUrl).mkString.split('\n').toSeq
                                            model.hurricaneModel.buildFromUnysis(userId.get, eventName, unisysFileLines)
                                            complete("unisys success")
                                    }
                                }
                            }
                        } ~ pathPrefix("from-saved") {
                            complete("unisys success")
                        } ~ pathPrefix("hurdat") {
                            complete("success")
                        } ~ pathPrefix("track") {
                            pathEndOrSingleSlash {
                                get {
                                    complete("success")
                                } ~ post {
                                    complete("success")
                                }
                            } ~ pathPrefix("as-geojson") {
                                get {
                                    complete("success")
                                }
                            } ~ pathPrefix("as-array") {
                                get {
                                    complete("success")
                                }
                            } ~ pathPrefix("interpolate") {
                                complete("success")
                            }
                        } ~ pathPrefix("footprint") {
                            pathEndOrSingleSlash {
                                get {
                                    complete("success")
                                } ~ delete {
                                    complete("success")
                                }
                            } ~ pathPrefix("as-geojson") {
                                get {
                                    complete("success")
                                }
                            } ~ pathPrefix("as-array") {
                                get {
                                    complete("success")
                                }
                            } ~ pathPrefix("calculate") {
                                complete("success")
                            }
                        } ~ pathPrefix("save") {
                            complete("success")
                        }
                    } ~ pathPrefix("config") {
                        pathEndOrSingleSlash {
                            get {
                                complete("success")
                            } ~ post {
                                complete("success")
                            }
                        }
                    }
                }
            }
        }
}
