package com.cliftbar.flapyakka.routes

// Akka
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.cliftbar.flapyakka.FlaPyAkkaController.{extractRequestContext, validate}

//FlaPyAkka
import com.cliftbar.flapyakka.FlaPyAkkaModel
import com.cliftbar.flapyakka.routes.UserValidator

object HurricaneRoutes {
    // Routes that this WebServer must handle are defined here
    def getRoutes(serverId: Int, model: FlaPyAkkaModel): Route =
        pathPrefix("hurricane") {
            extractRequestContext { ctx =>
                val valid = UserValidator.validateUser(ctx.request.headers, model)
                validate(valid, "Invalid User") {
                    pathEndOrSingleSlash {
                        get {
                            complete("hello hurricane")
                        }
                    } ~ pathPrefix("catalog") {
                        pathEndOrSingleSlash {
                            get {
                                complete("success")
                            } ~ post {
                                complete("success")
                            } ~ delete {
                                complete("success")
                            }
                        } ~ pathPrefix("add-event") {
                            complete("success")
                        } ~ pathPrefix("remove-event") {
                            complete("success")
                        } ~ pathPrefix("get-event-names") {
                            complete("success")
                        }
                    } ~ pathPrefix("event") {
                        pathEndOrSingleSlash {
                            get {
                                complete("success")
                            } ~ post {
                                complete("success")
                            } ~ delete {
                                complete("success")
                            }
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
