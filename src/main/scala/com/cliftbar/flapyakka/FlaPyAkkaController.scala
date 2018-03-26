package com.cliftbar.flapyakka

import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.{HttpApp, Route}
import com.cliftbar.flapyakka.routes.AppRoutes

// FlaPyDisaster application server
object FlaPyAkkaController extends HttpApp with App {

  val model = new FlaPyAkkaModel
  val serverId = (math.random() * 1000).toInt


  override def routes: Route = AppRoutes.getRoutes(serverId)

  // This will start the server until the return key is pressed
  val httpConfig = ConfigFactory.load().getConfig("akka.http")
  val interface = httpConfig.getString("server.interface")
  val port = httpConfig.getInt("server.port")
  //println(interface)
  //println(port)
  println("ServerID " + serverId.toString + " at " + interface.toString + ":" + port.toString)
  startServer(interface, port)
}
