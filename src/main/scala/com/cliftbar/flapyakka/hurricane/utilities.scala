package com.cliftbar.flapyakka.hurricane

// import xcb_app.{hurricaneNws23 => nws}

import java.awt.Color
import java.awt.image._
import java.io._
import java.util.concurrent.atomic.AtomicLong

import cliftbar.disastermodeling.hurricane.nws23.{HurricaneUtilities, model}
import javax.imageio.ImageIO

//import scala.collection.mutable._
import scala.collection.parallel._
import scala.concurrent.forkjoin._

package object Utilities {

  def CalcBearingNorthZero(latRef:Double, lonRef:Double, latLoc:Double, lonLoc:Double):Double = {
    val lonDelta = lonLoc - lonRef
    val latDelta = latLoc - latRef

    val angleDeg = math.toDegrees(math.atan2(lonDelta, latDelta))
    return (angleDeg + 360) % 360
  }

  def calc_bearing_great_circle(latRef:Double, lonRef:Double, latLoc:Double, lonLoc:Double):Double = {
    val y = math.sin(lonLoc - lonRef) * math.cos(latLoc)
    val x = math.cos(latRef) * math.sin(latLoc) - math.sin(latRef) * math.cos(latLoc) * math.cos(lonLoc - lonRef)
    val brng = math.toDegrees(math.atan2(y, x))
    return (brng + 360) % 360
  }

  def haversine_degrees_to_meters(lat_1:Double, lon_1:Double, lat_2:Double, lon_2:Double):Double = {
    val r = 6371000
    val delta_lat = math.toRadians(lat_2 - lat_1)
    val delta_lon = math.toRadians(lon_2 - lon_1)

    val a = math.pow(math.sin(delta_lat / 2), 2) + math.cos(math.toRadians(lat_1)) * math.cos(math.toRadians(lat_2)) * math.pow(math.sin(delta_lon / 2), 2)
    val c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return r * c
  }
}

class BoundingBox (val topLatY_deg:Double, val botLatY_deg:Double, val leftLonX_deg:Double, val rightLonX_deg:Double) {
//  val topLatY:Double = topLatY
//  val botLatY:Double = Double.NaN
//  val leftLonX:Double = Double.NaN
//  val rightLonX:Double = Double.NaN

  def Update(side:String, value:Double):BoundingBox = {
    return side match {
      case "top" => new BoundingBox(value, this.botLatY_deg, this.leftLonX_deg, this.rightLonX_deg)
      case "bottom" => new BoundingBox(this.topLatY_deg, value, this.leftLonX_deg, this.rightLonX_deg)
      case "left" => new BoundingBox(this.topLatY_deg, this.botLatY_deg, value, this.rightLonX_deg)
      case "right" => new BoundingBox(this.topLatY_deg, this.botLatY_deg, this.leftLonX_deg, value)
      case _ => throw new Exception("Unsupported Update")
    }
  }

  def GetWidth:Double = {return math.abs(this.rightLonX_deg - this.leftLonX_deg)}
  def GetHeight:Double = {return math.abs(this.topLatY_deg - this.botLatY_deg)}
}

class LatLonGrid(topLatY:Double, botLatY:Double, leftLonX:Double, rightLonX:Double, val BlockPerDegreeX:Int, val BlockPerDegreeY:Int) extends BoundingBox (topLatY, botLatY, leftLonX, rightLonX) {

  override def Update(item: String, value: Double): BoundingBox = {
    return item match {
      case "blocksX" => new LatLonGrid(this.topLatY, this.botLatY, this.leftLonX, this.rightLonX, value.toInt, this.BlockPerDegreeY)
      case "blocksY" => new LatLonGrid(this.topLatY, this.botLatY, this.leftLonX, this.rightLonX, value.toInt, this.BlockPerDegreeY)
      case _ => super.Update(item, value)
    }
  }

  def GetBlockIndex(latY:Double, lonX:Double):(Int, Int) = {
    val blockX = (lonX - this.leftLonX) * this.BlockPerDegreeX
    val blockY = (latY - this.botLatY) * this.BlockPerDegreeY

    return (blockY.toInt, blockX.toInt)
  }
  def GetBlockLatLon(blockX:Int, blockY:Int):(Double, Double) = {
    return (GetBlockLatY(blockY),GetBlockLonX(blockX))
  }

  def GetBlockLatY(blockY:Int):Double = {return this.botLatY + (blockY / this.BlockPerDegreeY.toDouble)}
  def GetBlockLonX(blockX:Int):Double = {this.leftLonX + (blockX / this.BlockPerDegreeX.toDouble)}

  def GetWidthInBlocks:Int = {(this.GetWidth * this.BlockPerDegreeX).round.toInt}
  def GetHeightInBlocks:Int = {(this.GetHeight * this.BlockPerDegreeY).round.toInt}

  def GetLatLonList:Seq[(Double,Double)] = {
    val height = this.GetHeightInBlocks
    val width = this.GetWidthInBlocks
    val grid = Array.fill(height)(Array.range(0,width)).zipWithIndex.flatMap(x => x._1.map(y => (y, x._2)).reverse).reverse
    return grid.map(x => this.GetBlockLatLon(x._1, x._2)).toSeq
  }

}
///**
//  * Created by cameron.barclift on 5/12/2017.
//  */
//class HurricaneEvent (val grid:LatLonGrid, val trackPoints:List[TrackPoint], val rMax_nmi:Double, var CalcedResults:List[Tuple3[Double,Double,Int]] = List.empty) {
//  var calcPos: AtomicLong = new AtomicLong()
//
//  def AddTrackPoint(tp:TrackPoint):HurricaneEvent = {
//    return new HurricaneEvent(this.grid, this.trackPoints ::: List(tp), this.rMax_nmi)
//  }
//
//  def AddGrid(grid: LatLonGrid):HurricaneEvent = {
//    return new HurricaneEvent(grid, this.trackPoints, this.rMax_nmi)
//  }
//
//  def CalcTrackpointHeadings():Unit = {
//    if (this.trackPoints.length == 1) {
//      this.trackPoints(0).heading = Some(0)
//    } else {
//      for (i <- 0 until this.trackPoints.length - 1) {
//        val next_lat = this.trackPoints(i+1).eyeLat_y
//        val next_lon = this.trackPoints(i+1).eyeLon_x
//        val curr_lat = this.trackPoints(i).eyeLat_y
//        val curr_lon = this.trackPoints(i).eyeLon_x
//
//        val heading = HurricaneUtilities.CalcBearingNorthZero(curr_lat, curr_lon, next_lat, next_lon)
//        this.trackPoints(i).heading = Some(heading)
//      }
//
//      this.trackPoints.last.heading = this.trackPoints(this.trackPoints.length - 2).heading
//    }
//  }
//
//  def DoCalcs(maxDist:Int, levelParallelism:Int, imageFileUri: String = "", textFileUri: String = "", printDebug:Boolean = false):Unit = {
//    val latLonList = this.grid.GetLatLonList
//
//    val parallel = if (levelParallelism == -1) false else true
//
//    val totalCalcLen = latLonList.length
//
//    CalcedResults = if (parallel) {
//      val latLonPar = latLonList.toParArray
//      latLonPar.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(levelParallelism))
//      latLonPar.map(x => TrackMap(x._1, x._2, maxDist, totalCalcLen, printDebug)).toList
//    } else {
//      latLonList.map(x => TrackMap(x._1, x._2, maxDist, totalCalcLen, printDebug))
//    }
//    if (printDebug){
//      println()
//    }
//
//    if (imageFileUri != "") {
//      this.WriteToImage(CalcedResults, this.grid.GetWidthInBlocks, this.grid.GetHeightInBlocks, imageFileUri)
//    }
//
//    if (textFileUri != "") {
//      val writer = new FileWriter(textFileUri)
//      writer.write("LatY\tLonX\twind_kts\n")
//
//      for (x <- CalcedResults) {
//        writer.write(s"${x._1}\t${x._2}\t${x._3}\n")
//      }
//
//      writer.close()
//    }
//  }
//
//  def TrackMap(pointLatY:Double, pointLonX:Double, maxDist:Int, totalCalcLen:Long, printProgress:Boolean = false):(Double, Double, Int) = {
//    val ret = this.trackPoints.toArray.map(tp => PointMap(tp, pointLatY, pointLonX, maxDist)).maxBy(x => x._3)
//    if (printProgress) {
//      val pct = (calcPos.incrementAndGet() / totalCalcLen.toDouble) * 100.0
//      println(s"progress: $pct")
//    }
//    return ret
//  }
//
//  def PointMap(tp:TrackPoint, pointLatY:Double, pointLonX:Double, maxDist:Int):(Double, Double, Int) = {
//    val distance_nmi = HurricaneUtilities.haversine_degrees_to_meters(pointLatY, pointLonX, tp.eyeLat_y, tp.eyeLon_x) / 1000 * 0.539957
//
//    val maxWind = if (distance_nmi <= maxDist) {
//      val angleToCenter = HurricaneUtilities.CalcBearingNorthZero(tp.eyeLat_y, tp.eyeLon_x, pointLatY, pointLonX)
//      model.calcWindspeed(distance_nmi, tp.eyeLat_y, tp.fSpeed_kts, this.rMax_nmi.toInt, angleToCenter, tp.heading.getOrElse(0.0), tp.maxWind_kts.get, tp.gwaf)
//    } else {
//      0
//    }
//
//    return (pointLatY, pointLonX, math.min(math.max(maxWind, 0), 255).round.toInt)
//  }
//
//  def WriteToImage(outList: List[(Double, Double, Int)], width: Int, height: Int, fileUri: String):Unit = {
//    val pixels = outList.map(x => new Color(x._3, 0, 0, 255).getRGB)
//
//    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
//    val raster = image.getRaster
//    raster.setDataElements(0, 0, width, height, pixels.toArray)
//    ImageIO.write(image, "PNG", new File(fileUri))
//  }
//
//  def ConvertGridToContours(gridList: List[(Double, Double, Int)], width: Int, height: Int, contourValue: Int):Unit = {
//    println("hello")
//    val gridList_twoD_width = gridList.sortBy(x => x._1).grouped(width).toList
//    val gridList_twoD_height = gridList.sortBy(x => x._2).grouped(height).toList
//    println(gridList_twoD_width(0))
//    println("Output")
//    println("x,y,z")
//    for (line <- gridList_twoD_width){
//      line.map(x => println("%s,%s,%s".format(x._1, x._2, x._3)))
//    }
//
//    println("Contour")
//    val contourWidth = gridList_twoD_width.flatMap(line => FindContourInList(line, contourValue))
//    val contourHeight = gridList_twoD_height.flatMap(line => FindContourInList(line, contourValue))
//
//    println("x,y,z")
//    (contourWidth ++ contourHeight).map(x => println("%s,%s,%s".format(x._1, x._2, x._3)))
//  }
//
//  def FindContourInList(line: List[(Double, Double, Int)], contourValue: Int): List[(Double, Double, Int)] = {
//    var contourPoints:Set[(Double, Double, Int)] = Set.empty
//
//    // main loop
//    for (i <- 0 until (line.length - 1)){
//      val slope = line(i + 1)._3 - line(i)._3
//
//      if (slope > 0) {
//        if (line(i + 1)._3 >= contourValue && line(i)._3 < contourValue) {
//          contourPoints.add(line(i + 1))
//        }
//      } else if (slope < 0) {
//        if (line(i)._3 >= contourValue && line(i + 1)._3 < contourValue) {
//          contourPoints.add(line(i))
//        }
//      }
//    }
//    if (line.last._3 >= contourValue) {
//      contourPoints.add(line.last)
//    }
//
//    if (line.head._3 >= contourValue) {
//      contourPoints.add(line.head)
//    }
//    return contourPoints.toList.sortBy(x => x._2)
//  }
//}
