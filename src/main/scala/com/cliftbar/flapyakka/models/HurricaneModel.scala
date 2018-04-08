package com.cliftbar.flapyakka.models

import com.cliftbar.flapyakka.hurricane.Catalog

import scala.collection.mutable

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

    def loadCatalogFromFile(userId: Int){}

    def deleteCatalog(userId: Int){}

    def addEventToCatalog(userId: Int, catalogName: String, eventName: String){}

    def getEventNamesInCatalog(userId: Int){}

    def saveCatalog(userId: Int){}

    def getCatalogStats(userId: Int){}

    /******************
    ** Event Methods **
    ******************/
    def getAllSavedEvents(userId: Int) {}

    def saveEventToDisk(userId: Int, eventName: String) {}

    // Load event methods
    def loadUnysisEvent(userId: Int, eventName: String){}

    def loadFromFile(userId: Int, eventName: String){}

    // Event footprint methods
    def calculateEventFootprint(userId: Int, eventName: String){}

    def getEventFootprintDataArray(userId: Int, eventName: String){}

    def getEventFootprintAsGeojson(userId: Int, eventName: String){}

    // Event track methods
    def interpolateTrack(userId: Int, eventName: String){}

    def getEventTrackAsArray(userId: Int, eventName: String){}

    def getEventTrackAsGeojson(userId: Int, eventName: String){}
}
