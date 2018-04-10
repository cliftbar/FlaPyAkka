package com.cliftbar.flapyakka.hurricane

import java.io.File
import java.nio.file._
import java.time.{Duration, LocalDateTime}
import java.time.format.DateTimeFormatter

object EventType extends Enumeration {
    type EventType = Value
    val UNISYS, HURDAT, MODEL = Value
}

object Event{
    // Output headers
    val outputHeaders = Seq("identifier", "name", "timestamp", "latY_deg", "lonX_deg", "maxWind_kts", "minCp_mb", "fspeed_kts", "heading", "is_landfall_point", "sequence", "rmax_nmi", "gwaf")

    def buildFromHurdat(){}
    def buildFromUnisys(unisysFileLines: Seq[String], eventIdentifier: Option[String] = None): Event ={
        val year: Int = unisysFileLines(0).trim.split(' ').last.toInt
        val name: String = eventIdentifier.getOrElse(unisysFileLines(1).trim.split(' ').last.toString)
        val identifier: String = eventIdentifier.getOrElse(name + "_" + year.toString)
        val dataRows: Seq[String] = unisysFileLines.drop(3)
        val tps: Seq[TrackPoint] = dataRows.zipWithIndex.map{case (r, i) => this.parseUnisysRow(year, r, i)}

        new Event(identifier, name, year, tps)
    }
    private def parseUnisysRow(year: Int, row: String, sequence: Int, fSpeed_kts: Int = 15): TrackPoint = {
        val splitRow: Seq[String] = row.trim.split("\\s+").map(x => x.trim)
        val latY: Float = splitRow(1).toFloat
        val lonX: Float = splitRow(2).toFloat
        //val fmt = new DateTimeFormatterBuilder().appendPattern("YYYY/MM/dd/HH").parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0).toFormatter()

        //val timeSeq = splitRow(3).dropRight(1).split('/').toSeq
        val time: LocalDateTime = LocalDateTime.parse(year.toString + '/' + splitRow(3).dropRight(1), DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"))
        val maxWind: Int = splitRow(4).toInt
        val minCp: Option[Float] = if (splitRow(4) == '-') None else Some(splitRow(4).toFloat)

        new TrackPoint(time, latY, lonX, maxWind, minCp, fSpeed_kts, sequence)
    }
    def buildFromSaveEvent(){}
}

class Event (var identifier: String, val name: String, val year: Int, var tps: Seq[TrackPoint], var fSpeed_kts: Int = 15){
    // Event info
    var fSpeedFromPoints: Boolean = false
    var trackTimeInterpolated: Boolean = false
    var bounds: Option[(Float, Float, Float, Float)] = None

    // Model Inputs
    var resolution_pxPerDeg: Int = 10
    var maxCalcDistance_nmi = 360
    var rMax_nmi: Int = 15
    var gwaf: Float = 0.9f

    // Footprint Raster Options
    var rasterBands: Int = 1
    var rasterOutputBand: Int = 1

    def fillTrackPointHeading(){}
    def fillTrackPointForwardSpeed(){}
    def timeInterpolateTrackPoints(timestep_h: Int = 1) = {
        this.tps = tps.sliding(2).flatMap(x => interpolateTrackPoints(x(0), x(1), timestep_h)).toSeq :+ tps.last
        //this.tps = this.tps.zipWithIndex{case (x: TrackPoint, i: Int) => x.sequence = i}
    }

    private def interpolateTrackPoints(point1: TrackPoint, point2: TrackPoint, timestep_h: Int): Seq[TrackPoint] = {
        val ret: Seq[TrackPoint] = Seq(point1)
        val steps:Int =  (Duration.between(point1.timestamp, point2.timestamp).toHours / timestep_h).toInt

        val latYStep: Float = (point2.latY_deg - point1.latY_deg) / steps
        val lonXStep: Float = (point2.lonX_deg - point1.lonX_deg) / steps
        val maxWindStep: Int = ((point2.maxWindSpeed_kts - point1.maxWindSpeed_kts) / steps)
        val minCpStep: Option[Float] = if (point2.minCentralPressure_mb.nonEmpty && point1.minCentralPressure_mb.nonEmpty){
            Some(point2.minCentralPressure_mb.get - point1.minCentralPressure_mb.get / steps)
        } else {
            None
        }

        val interpolated = point1 +: (0 to steps).map{ i =>
            val newLatY: Float = point1.latY_deg + (latYStep * i)
            val newLonX: Float = point1.lonX_deg + (lonXStep * i)
            val newTime: LocalDateTime = point1.timestamp.plusHours(timestep_h * i)
            val newMaxWind: Int = point1.maxWindSpeed_kts + (maxWindStep * i)
            val newMinCp: Option[Float] = if (minCpStep.nonEmpty) Some(point1.minCentralPressure_mb.get + (minCpStep.get * i)) else None
            new TrackPoint(newTime, newLatY, newLonX, newMaxWind, newMinCp, point1.forwardSpeed_kts, point1.sequence + steps)
        }
        return interpolated
    }

    def boundingBoxFromTrack(){}
    def calculateFootprint(){}

    def getTrackXyz(){}
    def getTrackGeojson(){}
    def getGridXyz(){}
    def getGridGeojson(){}

    def saveEvent(rootSaveDir: String) = {
        val eventDir: Path = Paths.get(rootSaveDir + File.separator + "events" + File.separator + this.identifier + File.separator)
        Files.createDirectories(eventDir)
        this.saveEventConfig(eventDir)
        this.saveEventRaster(eventDir)
        val baseUri = Paths.get(eventDir.toString, "baseData.txt")
        this.saveBaseData(baseUri)
    }
    private def saveEventConfig(saveDir: Path){}
    private def saveEventRaster(saveDir: Path){}
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

        fi.write(Event.outputHeaders.mkString(sep))
        fi.newLine()
        for (line <- this.tps) {
            val out = (dataFront.map(x => x.toString) ++ line.pointAsPrintSeq.map(x => x.toString) ++ dataBack.map(x => x.toString)).mkString(sep)
            fi.write(out)
            fi.newLine()
        }
        fi.close()
    }
}
