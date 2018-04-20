package com.cliftbar.flapyakka.models

import java.nio.file.{Files, Path, Paths}

import com.cliftbar.flapyakka.hurricane.{Catalog, Event}
import spray.json.{JsObject, JsValue}

class HurricaneModel(model: FlaPyAkkaModel) {
    /*****************************
    ** Hurricane Config Methods **
    *****************************/
    def setUserHurricaneConfig(userId: Int){}

    def createHurricaneConfig(userId: Int){}

    /********************
    ** Catalog Methods **
    ********************/
    def createCatalog(userId: Int, catalogName: String): Unit ={
        val username = this.model.users.getUsername(userId)
        val catalog = new Catalog(catalogName, username)
        catalog.saveCatalog(this.model.users.userDir)
    }

    def createCatalogFromHurdat(userId: Int){}

    //def loadCatalogFromFile(userId: Int){}

    def deleteCatalog(userId: Int, catalogName: String): Unit ={
        val userDir = model.users.getUserDir(userId)
        if (userDir.nonEmpty) {
            val catalogPath = Paths.get(userDir.get, "catalogs", catalogName)
            Files.deleteIfExists(catalogPath)
        }
    }

    def addEventToCatalog(userId: Int, catalogName: String, eventName: String): Unit ={
        val userDir = this.model.users.getUserDir(userId)
        if (userDir.nonEmpty) {
            val catTempOpt = Catalog.loadFromSaved(catalogName, userDir.get)
            if (catTempOpt.nonEmpty) {
                val catTemp = catTempOpt.get
                catTemp.addEvent(eventName)
                catTemp.saveCatalog(userDir.get)
            }
        }
    }

    def removeEventFromCatalog(userId: Int, catalogName: String, eventName: String) = {
        val userDir = this.model.users.getUserDir(userId)
        if (userDir.nonEmpty) {
            val catTempOpt = Catalog.loadFromSaved(catalogName, userDir.get)
            if (catTempOpt.nonEmpty) {
                val catTemp = catTempOpt.get
                catTemp.removeEvent(eventName)
                catTemp.saveCatalog(userDir.get)
            }
        }
    }

    def getUserCatalogs(userId: Int): Seq[String] = {
        val userName = this.model.users.getUsername(userId)
        val catalogPath = Paths.get(this.model.users.userDir, userName, "catalogs")
        Catalog.getUserCatalogs(catalogPath)
    }

    def getCatalogEvents(userId: Int, catalogName: String): Seq[String] = {
        val userName = this.model.users.getUsername(userId)
        val catalogPath = Paths.get(this.model.users.userDir, userName, "catalogs", catalogName, catalogName + ".conf")
        return Catalog.getCatalogEvents(catalogPath)
    }

    def getCatalogStats(userId: Int){}

    /******************
    ** Event Methods **
    ******************/
    def getAllSavedEvents(userId: Int) {}

    def saveEventToDisk(userId: Int, eventName: String) {}

    // Load event methods
    def buildFromUnysis(userId: Int, eventName: String, unisysFileLines: Seq[String]): Unit ={
        val event: Event = Event.buildFromUnisys(unisysFileLines)
        val saveDir = this.model.users.getUserDir(userId)
        if (saveDir.nonEmpty) {
            event.saveEvent(saveDir.get)
        }
    }

    def loadFromFile(userId: Int, eventName: String): Unit ={
        val testEvent = Event.buildFromSaveEvent("users/xcb/events/MATTHEW_2016/MATTHEW_2016_EventData.conf")
        val test = "test"
    }

    def getEventConfigFile(userId: Int, eventName: String): Option[Path] = {
        val userName = this.model.users.getUsername(userId)
        val testPath = Paths.get(this.model.users.userDir, userName, "events", eventName, eventName + "_EventData.conf")
        val fiExists = Files.exists(testPath)

        return if (fiExists) Some(testPath) else None
    }

    def getEventConfig(userId: Int, eventName: String): Option[JsObject] = {
        val eventPath: Path = Paths.get(model.users.getUserDir(userId).get, "events", eventName, eventName + "_EventData.conf")
        val ret = if (Files.exists(eventPath)) {
            Some(Event.buildFromSaveEvent(eventPath.toString).getConfig)
        } else {
            None
        }

        return ret
    }

    def getTrackAsJsonArray(userId: Int, eventName: String): Option[JsValue] = {
        val eventPath: Path = Paths.get(model.users.getUserDir(userId).get, "events", eventName, eventName + "_EventData.conf")
        val ret = if (Files.exists(eventPath)) {
            val eventTemp = Event.buildFromSaveEvent(eventPath.toString)
            Some(eventTemp.getTrackJsonArray())
        } else {
            None
        }
        return ret
    }

    def buildFromHurdat(userId: Int, eventName: String){}

    // Event footprint methods
    def calculateEventFootprint(userId: Int, eventName: String){}

    def getEventFootprintDataArray(userId: Int, eventName: String){}

    def getEventFootprintAsGeojson(userId: Int, eventName: String){}

    // Event track methods
    def interpolateTrack(userId: Int, eventName: String){}

    def getEventTrackAsGeojson(userId: Int, eventName: String){}
}
