package com.cliftbar.flapyakka.hurricane

import java.time._
import java.time.format.DateTimeFormatter

class TrackPoint(
    val timestamp: LocalDateTime
    ,var latY_deg: Float
    ,var lonX_deg: Float
    ,val maxWindSpeed_kts: Int
    ,val minCentralPressure_mb: Option[Float]
    ,var sequence: Int
    ,var forwardSpeed_kts: Int
    ,var headingToNextPoint: Option[Float] = None
    ,var isInterpolated: Boolean = false
    ,var isLandfall: Boolean = false
) {
    def pointToXyz(): (Float, Float, Int) ={
        val xy = this.pointLatLon()
        return (xy._1, xy._2, this.maxWindSpeed_kts)
    }
    def pointLatLon(): (Float, Float) ={
        return (this.latY_deg, this.lonX_deg)
    }
    def pointAsGeojson(){}
    def pointAsPrintSeq(): Seq[Any] = {
        return Seq(
            timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ,pointLatLon()._1
            ,pointLatLon()._2
            ,maxWindSpeed_kts
            ,minCentralPressure_mb.getOrElse(Float.NaN)
            ,forwardSpeed_kts
            ,headingToNextPoint.getOrElse(Float.NaN)
            ,if (this.isLandfall) 1 else 0
            ,sequence
        )
    }
}

class HurdatTrackPoint(
    override val timestamp: LocalDateTime
    ,latY_deg: Float
    ,lonX_deg: Float
    ,maxWindSpeed_kts: Int
    ,minCentralPressure_mb: Option[Float]
    ,sequence: Int
    ,forwardSpeed_kts: Int
    ,headingToNextPoint: Float
    ,recordIdentifier: String
    ,status: String
    ,hemisphere_ns: String
    ,hemisphere_ew: String
    ,r34_ne_nmi: Int
    ,r34_se_nmi: Int
    ,r34_sw_nmi: Int
    ,r34_nw_nmi: Int
    ,r50_ne_nmi: Int
    ,r50_se_nmi: Int
    ,r50_sw_nmi: Int
    ,r50_nw_nmi: Int
    ,r64_ne_nmi: Int
    ,r64_se_nmi: Int
    ,r64_sw_nmi: Int
    ,r64_nw_nmi: Int
    ,isInterpolated: Boolean = false
) extends TrackPoint(
    timestamp: LocalDateTime
    ,latY_deg: Float
    ,lonX_deg: Float
    ,maxWindSpeed_kts: Int
    ,minCentralPressure_mb: Option[Float]
    ,forwardSpeed_kts: Int
    ,sequence: Int
) {
    val year: Int = timestamp.getYear
    val month: Int = timestamp.getMonthValue
    val day: Int = timestamp.getDayOfMonth
    val hour: Int = timestamp.getHour
    val minute: Int = timestamp.getMinute
    isLandfall = this.recordIdentifier == 'L'

    override def pointLatLon(): (Float, Float) = {
        val lat = if (this.hemisphere_ns == 'S') {
            math.abs(this.latY_deg) * -1
        } else {
            math.abs(this.latY_deg)
        }

        val lon = if (this.hemisphere_ns == 'W') {
            math.abs(this.lonX_deg) * -1
        } else {
            math.abs(this.lonX_deg)
        }

        return (lat, lon)
    }
    def pointAsHurdatList(){}
}
