package com.cliftbar.flapyakka.hurricane

import java.io.{File, FileWriter}
import java.nio.file._
import java.time.{Duration, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.util.concurrent.ForkJoinPool

import cliftbar.disastermodeling.hurricane.nws23.nws23
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}
import spray.json.{JsNumber, JsObject, JsValue, _}

import scala.collection.JavaConverters._
import scala.collection.parallel.ForkJoinTaskSupport
//import scala.concurrent.forkjoin.ForkJoinPool

object EventType extends Enumeration {
    type EventType = Value
    val UNISYS, HURDAT, MODEL = Value
}

object Event{
    // Output headers
    val modelHeaders = Seq("identifier", "name", "timestamp", "latY_deg", "lonX_deg", "maxWind_kts", "minCp_mb", "fspeed_kts", "heading", "is_landfall_point", "sequence", "rmax_nmi", "gwaf")

    def buildFromHurdat(){}

    def buildFromUnisys(unisysFileLines: Seq[String], eventIdentifier: Option[String] = None): Event ={
        val year: Int = unisysFileLines(0).trim.split(' ').last.toInt
        val name: String = eventIdentifier.getOrElse(unisysFileLines(1).trim.split(' ').last.toString)
        val identifier: String = eventIdentifier.getOrElse(name + "_" + year.toString)
        val dataRows: Seq[String] = unisysFileLines.drop(3)
        val tpsTemp: Seq[TrackPoint] = dataRows.zipWithIndex.map{case (r, i) => this.parseUnisysRow(year, r, i)}
        val tpsFiltered: Seq[(TrackPoint, Int)] = tpsTemp.map(x => x.timestamp -> x).toMap.values.toSeq.sortBy(x => x.sequence).zipWithIndex
        tpsFiltered.foreach{case (r, i) => r.sequence = i}
        val tps: Seq[TrackPoint] = tpsFiltered.map(x => x._1)

        val eventTemp = new Event(identifier, name, year, tps)
        eventTemp.timeInterpolateTrackPoints()
        eventTemp.headingFromPoints()
        return eventTemp
    }

    private def parseUnisysRow(year: Int, row: String, sequence: Int, fSpeed_kts: Int = 15): TrackPoint = {
        val splitRow: Seq[String] = row.trim.split("\\s+").map(x => x.trim)
        val latY: Double = splitRow(1).toDouble
        val lonX: Double = splitRow(2).toDouble
        //val fmt = new DateTimeFormatterBuilder().appendPattern("YYYY/MM/dd/HH").parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0).toFormatter()

        //val timeSeq = splitRow(3).dropRight(1).split('/').toSeq
        val time: LocalDateTime = LocalDateTime.parse(year.toString + '/' + splitRow(3).dropRight(1), DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"))
        val maxWind: Int = splitRow(4).toInt
        val minCp: Option[Double] = if (splitRow(4) == '-') None else Some(splitRow(4).toDouble)

        new TrackPoint(time, latY, lonX, maxWind, minCp, sequence, fSpeed_kts)
    }

    def buildFromSaveEvent(confFileUri: String): Event ={
        val confFi = new File(confFileUri)
        val conf = ConfigFactory.parseFile(confFi)

        val baseDataUri: Path = Paths.get(conf.getString("files.baseDataUri"))

        val newTps: Seq[TrackPoint] = this.parseSavedEvent(baseDataUri)

        new Event(
            conf.getString("info.identifier")
            ,conf.getString("info.name")
            ,conf.getInt("info.year")
            ,newTps
            ,conf.getInt("info.fSpeed_kts")
        )
    }

    private def parseSavedEvent(confFileUri: Path): Seq[TrackPoint] = {
        val fiLines: Seq[String] = Files.readAllLines(confFileUri).asScala

        val tpsRet = fiLines.drop(2).map(x => parseSavedEventRow(x))

        return tpsRet
    }

    private def parseSavedEventRow(row: String, sep: Char = '\t'): TrackPoint = {
        val splitLine = row.split('\t')
        val newTp = new TrackPoint(
            LocalDateTime.parse(splitLine(2), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ,splitLine(3).toDouble
            ,splitLine(4).toDouble
            ,splitLine(5).toInt
            ,if (splitLine(6).toDouble.isNaN) None else Some(splitLine(6).toDouble)
            ,splitLine(7).toInt
            ,splitLine(10).toInt
            ,if (splitLine(8).toDouble.isNaN) None else Some(splitLine(8).toDouble)
            //,try {Some(splitLine(7).toDouble)} catch {case : Throwable => None}
            ,false
            ,if (splitLine(9).toInt == 0) false else true
        )
        return newTp
    }
}

class Event (var identifier: String, val name: String, val year: Int, var tps: Seq[TrackPoint], var fSpeed_kts: Int = 15){
    // Event info
    var fSpeedFromPoints: Boolean = false
    var trackTimeInterpolated: Boolean = false

    var bounds: Option[BoundingBox] = None
    var latLonGrid: Option[LatLonGrid] = None

    // Model Inputs
    var resolution_pxPerDeg: Int = 10
    var maxCalcDistance_nmi = 360
    var rMax_nmi: Int = 15
    var gwaf: Double = 0.9

    // Footprint Raster Options
    var rasterBands: Int = 1
    var rasterOutputBand: Int = 1
    var hasFootprintCalcd: Boolean = false

    def fillTrackPointHeading(){}
    def fillTrackPointForwardSpeed(){}
    def timeInterpolateTrackPoints(timestep_h: Int = 1): Unit = {
        val tpsTempZipped: Seq[(TrackPoint, Int)] = (tps.sliding(2).flatMap(x => interpolateTrackPoints(x(0), x(1), timestep_h)).toSeq :+ tps.last).zipWithIndex
        tpsTempZipped.foreach(x => x._1.sequence = x._2)
        this.tps = tpsTempZipped.map(x => x._1)
        //this.tps = this.tps.zipWithIndex{case (x: TrackPoint, i: Int) => x.sequence = i}
    }

    private def interpolateTrackPoints(point1: TrackPoint, point2: TrackPoint, timestep_h: Int): Seq[TrackPoint] = {
        val ret: Seq[TrackPoint] = Seq(point1)
        val steps:Int =  (Duration.between(point1.timestamp, point2.timestamp).toHours / timestep_h).toInt

        val latYStep: Double = (point2.latY_deg - point1.latY_deg) / steps
        val lonXStep: Double = (point2.lonX_deg - point1.lonX_deg) / steps
        val maxWindStep: Int = ((point2.maxWindSpeed_kts - point1.maxWindSpeed_kts) / steps)
        val minCpStep: Option[Double] = if (point2.minCentralPressure_mb.nonEmpty && point1.minCentralPressure_mb.nonEmpty){
            Some(point2.minCentralPressure_mb.get - point1.minCentralPressure_mb.get / steps)
        } else {
            None
        }

        val interpolated = point1 +: (1 to steps-1).map{ i =>
            val newLatY: Double = point1.latY_deg + (latYStep * i)
            val newLonX: Double = point1.lonX_deg + (lonXStep * i)
            val newTime: LocalDateTime = point1.timestamp.plusHours(timestep_h * i)
            val newMaxWind: Int = point1.maxWindSpeed_kts + (maxWindStep * i)
            val newMinCp: Option[Double] = if (minCpStep.nonEmpty) Some(point1.minCentralPressure_mb.get + (minCpStep.get * i)) else None
            new TrackPoint(newTime, newLatY, newLonX, newMaxWind, newMinCp, point1.sequence + i, point1.forwardSpeed_kts)
        }
        return interpolated
    }

    private def headingFromPoints() = {
        tps.sliding(2).map(x => x(0).headingToNextPoint = Some(headingToNextPoint(x(0), x(1))))
        tps.last.headingToNextPoint = tps(tps.length - 1).headingToNextPoint
    }

    private def headingToNextPoint(reference: TrackPoint, next: TrackPoint): Double = {
        val curr_lat = reference.latY_deg
        val curr_lon = reference.lonX_deg
        val next_lat = next.latY_deg
        val next_lon = next.lonX_deg

        return Utilities.CalcBearingNorthZero(curr_lat, curr_lon, next_lat, next_lon)
    }

    def boundingBoxFromTrack(): Unit ={
        val lats = this.tps.map(y => y.latY_deg)
        val lons = this.tps.map(x => x.lonX_deg)

        this.bounds = Some(new BoundingBox(lats.max, lats.min, lons.min, lons.max))
    }

    def calculateFootprint(levelParallelism: Option[Int] = None): Unit = {
        val grid = new LatLonGrid(this.bounds.get.topLatY_deg, this.bounds.get.botLatY_deg, this.bounds.get.leftLonX_deg, this.bounds.get.rightLonX_deg, resolution_pxPerDeg, resolution_pxPerDeg)

        val latLonList = grid.GetLatLonList

        val parallel = if (levelParallelism == -1) false else true

        val totalCalcLen = latLonList.length

        val calcedResults = if (parallel) {
          val latLonPar = latLonList.toParArray
          latLonPar.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(levelParallelism.get))
          latLonPar.map(x => trackFunc(x._1, x._2, this.maxCalcDistance_nmi, totalCalcLen))
        } else {
          latLonList.map(x => trackFunc(x._1, x._2, this.maxCalcDistance_nmi, totalCalcLen))
        }

//        if (imageFileUri != "") {
//          this.WriteToImage(CalcedResults, this.grid.GetWidthInBlocks, this.grid.GetHeightInBlocks, imageFileUri)
//        }
//
//        if (textFileUri != "") {
//          val writer = new FileWriter(textFileUri)
//          writer.write("LatY\tLonX\twind_kts\n")
//
//          for (x <- CalcedResults) {
//            writer.write(s"${x._1}\t${x._2}\t${x._3}\n")
//          }
//
//          writer.close()
//        }
    }

    def trackFunc(pointLatY:Double, pointLonX:Double, maxDist:Int, totalCalcLen:Long, printProgress:Boolean = false):(Double, Double, Int) = {
        val ret = this.tps.toArray.map(tp => PointMap(tp, pointLatY, pointLonX, maxDist)).maxBy(x => x._3)
        return ret
    }

    def PointMap(tp:TrackPoint, pointLatY:Double, pointLonX:Double, maxDist:Int):(Double, Double, Int) = {
        val distance_nmi = Utilities.haversine_degrees_to_meters(pointLatY, pointLonX, tp.latY_deg, tp.lonX_deg) / 1000 * 0.539957

        val maxWind = if (distance_nmi <= maxDist) {
            val angleToCenter = Utilities.CalcBearingNorthZero(tp.latY_deg, tp.lonX_deg, pointLatY, pointLonX)
            nws23.calcWindspeed(distance_nmi, tp.latY_deg, tp.forwardSpeed_kts, this.rMax_nmi, angleToCenter, tp.headingToNextPoint.getOrElse(0.0), tp.maxWindSpeed_kts, this.gwaf)
        } else {
          0
        }

        return (pointLatY, pointLonX, math.min(math.max(maxWind, 0), 255).round.toInt)
    }

    def getConfig(): JsObject = {
        val jsonBounds: JsValue = if(this.bounds.nonEmpty){
                JsObject(
                    "topLatY_deg" -> JsNumber(this.bounds.get.topLatY_deg)
                )
            } else JsNull

        val json = JsObject(
            "info" -> JsObject(
                "identifier" -> JsString(this.identifier)
                ,"name" -> JsString(this.name)
                ,"year" -> JsNumber(this.year)
            )
            ,"inputs" -> JsObject(
                "gwaf" -> JsNumber(this.gwaf)
                ,"maxCalcDistance_nmi" -> JsNumber(this.maxCalcDistance_nmi)
                ,"rMax_nmi" -> JsNumber(this.rMax_nmi)
                ,"resolution_pxPerDeg" -> JsNumber(this.resolution_pxPerDeg)
                ,"rasterBands" -> JsNumber(this.rasterBands)
                ,"rasterOutputBand" -> JsNumber(this.rasterOutputBand)
            )
            ,"bounds" -> jsonBounds
        )

        return json
    }

    def getTrackXyz(){}
    def getTrackGeojson(){}
    def getTrackJsonArray(): JsValue = {
        return JsArray(this.tps.map(x => x.pointAsJsonArray(this.rMax_nmi, this.gwaf)).toVector)
    }
    def getGridXyz(){}
    def getGridGeojson(){}

    def saveEvent(rootSaveDir: String) = {
        val eventDir: Path = Paths.get(rootSaveDir + File.separator + "events" + File.separator + this.identifier + File.separator)
        Files.createDirectories(eventDir)
        val eventDataUri = Paths.get(eventDir.toString, this.identifier + "_EventData.conf")
        val baseDataUri = Paths.get(eventDir.toString, this.identifier + "_BaseData.txt")
        val rasterUri: Option[Path] = if (hasFootprintCalcd) Some(Paths.get(eventDir.toString, this.identifier + "_RasterData.png")) else None

        this.saveBaseData(baseDataUri)
        if (!rasterUri.isEmpty) {
            this.saveEventRaster(rasterUri.get)
        }
        this.saveEventConfig(eventDataUri, baseDataUri, rasterUri)
    }

    private def saveBaseData(saveUri: Path, sep: String = "\t"): Unit ={
        val dataFront: Seq[Any] = Seq(
            identifier
            ,name
        )

        val dataBack = Seq(
            rMax_nmi
            ,gwaf
        )

        val fi = Files.newBufferedWriter(saveUri, StandardOpenOption.WRITE, StandardOpenOption.CREATE)

        fi.write(Event.modelHeaders.mkString(sep))
        fi.newLine()
        for (line <- this.tps) {
            val out = (dataFront.map(x => x.toString) ++ line.pointAsSeq.map(x => x.toString) ++ dataBack.map(x => x.toString)).mkString(sep)
            fi.write(out)
            fi.newLine()
        }
        fi.close()
    }
    private def saveEventRaster(saveDir: Path){}
    private def saveEventConfig(eventDataUri: Path, baseDataUri: Path, rasterUri: Option[Path]): Unit = {
        var conf = ConfigFactory.empty
        //conf = conf.withOnlyPath("user")

        // Event Info
        conf = conf.withValue("info.identifier", ConfigValueFactory.fromAnyRef(this.identifier))
        conf = conf.withValue("info.name", ConfigValueFactory.fromAnyRef(this.name))
        conf = conf.withValue("info.year", ConfigValueFactory.fromAnyRef(this.year))
        conf = conf.withValue("info.identifier", ConfigValueFactory.fromAnyRef(this.identifier))
        conf = conf.withValue("info.trackTimeInterpolated", ConfigValueFactory.fromAnyRef(this.trackTimeInterpolated))

        if (!this.bounds.isEmpty) {
            conf = conf.withValue("info.topLatY_deg", ConfigValueFactory.fromAnyRef(this.bounds.get.topLatY_deg))
            conf = conf.withValue("info.botLatY_deg", ConfigValueFactory.fromAnyRef(this.bounds.get.botLatY_deg))
            conf = conf.withValue("info.leftLonX_deg", ConfigValueFactory.fromAnyRef(this.bounds.get.leftLonX_deg))
            conf = conf.withValue("info.rightLonX_deg", ConfigValueFactory.fromAnyRef(this.bounds.get.rightLonX_deg))
        } else {
            conf = conf.withValue("info.topLatY_deg", ConfigValueFactory.fromAnyRef(Double.NaN))
            conf = conf.withValue("info.botLatY_deg", ConfigValueFactory.fromAnyRef(Double.NaN))
            conf = conf.withValue("info.leftLonX_deg", ConfigValueFactory.fromAnyRef(Double.NaN))
            conf = conf.withValue("info.rightLonX_deg", ConfigValueFactory.fromAnyRef(Double.NaN))
        }

        // Model Inputs
        conf = conf.withValue("inputs.resolution_pxPerDeg", ConfigValueFactory.fromAnyRef(this.resolution_pxPerDeg))
        conf = conf.withValue("inputs.maxCalcDistance_nmi", ConfigValueFactory.fromAnyRef(this.maxCalcDistance_nmi))
        conf = conf.withValue("inputs.rMax_nmi", ConfigValueFactory.fromAnyRef(this.rMax_nmi))
        conf = conf.withValue("inputs.gwaf", ConfigValueFactory.fromAnyRef(this.gwaf))
        conf = conf.withValue("inputs.fSpeed_kts", ConfigValueFactory.fromAnyRef(this.fSpeed_kts))

        // Footprint Options
        conf = conf.withValue("inputs.rasterBands", ConfigValueFactory.fromAnyRef(this.rasterBands))
        conf = conf.withValue("inputs.rasterOutputBand", ConfigValueFactory.fromAnyRef(this.rasterOutputBand))

        // Other Files
        conf = conf.withValue("files.baseDataUri", ConfigValueFactory.fromAnyRef(baseDataUri.toString))
        if (!rasterUri.isEmpty) {
            conf = conf.withValue("files.rasterUri", ConfigValueFactory.fromAnyRef(rasterUri.toString))
        }

        val fi = Files.newBufferedWriter(eventDataUri, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
        fi.write(conf.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
        fi.close()
    }
}
