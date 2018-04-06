package com.cliftbar.flapyakka.models

class HurricaneModel(model: FlaPyAkkaModel) {
    /*****************************
    ** Hurricane Config Methods **
    *****************************/
    def setUserHurricaneConfig(userId: Int){}

    /********************
    ** Catalog Methods **
    ********************/
    def createCatalog(userId: Int){}

    def deleteCatalog(userId: Int){}

    def addEventToCatalog(userId: Int, catalogName: String, eventName: String){}

    def getEventNamesInCatalog(userId: Int){}

    def saveCatalog(userId: Int){}

    /******************
    ** Event Methods **
    ******************/
    def getAllSavedEvents(userId: Int) {}

    def saveEventToDisk(userId: Int, eventName: String) {}

    // Load event methods


    // Event footprint methods
    def calculateEventFootprint(userId: Int, eventName: String){}

    def getEventFootprintDataArray(userId: Int, eventName: String){}

    def getEventFootprintAsGeojson(userId: Int, eventName: String){}

    // Event track methods
    def interpolateTrack(userId: Int, eventName: String){}

    def getEventTrackAsArray(userId: Int, eventName: String){}

    def getEventTrackAsGeojson(userId: Int, eventName: String){}
}
