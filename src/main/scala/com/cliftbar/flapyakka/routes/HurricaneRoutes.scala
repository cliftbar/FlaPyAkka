package com.cliftbar.flapyakka.routes

// Akka
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.cliftbar.flapyakka.FlaPyAkkaController.{authorize, complete, extractRequestContext, optionalHeaderValueByName, respondWithHeaders, validate}
import com.cliftbar.flapyakka.models.FlaPyAkkaModel
import spray.json.{JsArray, JsObject, JsonParser, _}
import spray.json.DefaultJsonProtocol._

import scala.io.Source.fromURL
import scala.util.Try

object HurricaneRoutes {
    def catalogRoutes(serverId: Int, model: FlaPyAkkaModel, userId: Option[Int]): Route =
        pathPrefix("catalog") {
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
                    entity(as[String]) {
                        json =>
                            val parsedJson = JsonParser(json).asJsObject
                            val catalogName: String = parsedJson.fields("catalogName").convertTo[String]
                            model.hurricaneModel.deleteCatalog(userId.get, catalogName)
                            complete("success")
                    }
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
                entity(as[String]) {
                    json =>
                        val parsedJson = JsonParser(json).asJsObject
                        val catalogName: String = parsedJson.fields("catalogName").convertTo[String]
                        val eventName: String = parsedJson.fields("eventName").convertTo[String]

                        model.hurricaneModel.removeEventFromCatalog(userId.get, catalogName, eventName)

                        complete("success")
                }
            } ~ pathPrefix("get-event-names") {
                pathEndOrSingleSlash {
                    get {
                        parameter("catalogName") {
                            catalogName =>
                                println("get-event-names")

                                val eventsTemp = model.hurricaneModel.getCatalogEvents(userId.get, catalogName)

                                val respData: JsObject = JsObject(
                                    "events" -> eventsTemp.toJson
                                )
                                val respEntity: HttpEntity.Strict = HttpEntity(MediaTypes.`application/json`, respData.toString())
                                respondWithHeaders(RawHeader("server_id", serverId.toString)) {
                                    complete(respEntity)
                                }
                        }
                    }
                }
            } ~ pathPrefix("get-user-catalogs") {
                pathEndOrSingleSlash {
                    get {
                        val catalogsRet: Seq[String] = model.hurricaneModel.getUserCatalogs(userId.get)

                        val respData: JsObject = JsObject(
                            "catalogs" -> catalogsRet.toJson
                        )
                        val respEntity: HttpEntity.Strict = HttpEntity(MediaTypes.`application/json`, respData.toString())
                        respondWithHeaders(RawHeader("server_id", serverId.toString)) {
                            complete(respEntity)
                        }
                    }
                }
            }
        }

    def configRoutes(serverId: Int, model: FlaPyAkkaModel, userId: Option[Int]): Route =
        pathPrefix("config") {
            pathEndOrSingleSlash {
                get {
                    complete("success")
                } ~ post {
                    complete("success")
                }
            }
        }

    def eventRoutes(serverId: Int, model: FlaPyAkkaModel, userId: Option[Int]): Route =
        pathPrefix("event") {
            pathEndOrSingleSlash {
                get{
                    parameter("eventName") {
                        eventName =>
                            val conf = model.hurricaneModel.getEventConfig(userId.get, eventName)

                            if (conf.nonEmpty) {
                                val respData: JsObject = JsObject(
                                    "eventConfig" -> conf.get
                                )

                                val respEntity: HttpEntity.Strict = HttpEntity(MediaTypes.`application/json`, respData.toString())
                                respondWithHeaders(RawHeader("server_id", serverId.toString)) {
                                    complete(respEntity)
                                }
                            } else {
                                complete(StatusCodes.NotFound, "Event not found")
                            }
                    }
                }
            } ~ pathPrefix("unisys") {
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
                        parameter("eventName") {
                            eventName =>
                                println("test")

                                val trackRet = model.hurricaneModel.getTrackAsJsonArray(userId.get, eventName)

                                if (trackRet.nonEmpty) {
                                    val respData: JsObject = JsObject(
                                        "catalogs" -> trackRet.get
                                    )

                                    val respEntity: HttpEntity.Strict = HttpEntity(MediaTypes.`application/json`, respData.toString())
                                    respondWithHeaders(RawHeader("server_id", serverId.toString)) {
                                        complete(respEntity)
                                    }
                                } else {
                                    complete(StatusCodes.NotFound, "Track not found")
                                }
                        }
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
        }

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
                    } ~ catalogRoutes(serverId, model, userId) ~ eventRoutes(serverId, model, userId) ~ configRoutes(serverId, model, userId)
                }
            }
        }
}
