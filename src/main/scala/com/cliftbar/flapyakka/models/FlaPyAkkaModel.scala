package com.cliftbar.flapyakka.models

import java.nio.file.Paths

import com.cliftbar.flapyakka.hurricane

// FlapyDisaster
import cliftbar.disastermodeling.hurricane.TrackPoint
import cliftbar.disastermodeling.hurricane.nws23._
import com.cliftbar.flapyakka.app.UserStore

class FlaPyAkkaModel {
    val users = UserStore
    val hurricaneModel = new HurricaneModel(this)

    def validateUser(id: Option[Int]): Option[Int] ={
        return users.validateUser(id)
    }

    def addUser(username: String): Int ={
        return users.addUser(username)
    }

    def printUsers(): Unit ={
        users.printUsers()
    }

    // old
//    def CalculateHurricane(trackPoints: Seq[TrackPoint], bBox: hurricane.BoundingBox, fSpeed_kts: Option[Double], rMax_nmi: Double, pxPerDegree: (Int, Int), maxDist: Int, par: Int = -1): Map[String, String] = {
//
//        //println("Start Calculate Hurricane with NWS23")
//        //println(time.LocalDateTime.now())
//        val grid = new hurricane.LatLonGrid(bBox.topLatY_deg, bBox.botLatY_deg, bBox.leftLonX_deg, bBox.rightLonX_deg, pxPerDegree._1, pxPerDegree._2)
//        val event = new hurricane.HurricaneEvent(grid, trackPoints.toList, rMax_nmi)
//
//        val imageName = "test_image.png"
//        event.DoCalcs(maxDist, par, imageName, "test_text.txt")
//
//        //println(time.LocalDateTime.now())
//        //println("Did test")
//
//        val retPath = Paths.get(System.getProperty("user.dir"), imageName)
//        //val retPath = System.getProperty("user.dir") + "\\" + imageName
//        val ret = Map("imageUri" -> retPath.toAbsolutePath.toString)
//
//        return ret
//    }
}
