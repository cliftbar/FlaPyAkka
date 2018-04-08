package com.cliftbar.flapyakka.hurricane

import java.io.{FileWriter, File}

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable

class Catalog(name: String, username: String) {
    val events = new mutable.HashMap[String, String]

    def getNames() = {}

    def addEvent() = {}

    def removeEvent() = {}

    def loadFromFile(fileUri: String) {}

    def importFromFile(fileUri: String) {}

    def importHurdatCatalog(fileName: String) {}

    def saveCatalog(rootSaveDir: String): Unit = {

        val userDir = rootSaveDir + this.username + File.separator + "catalogs" + File.separator  + this.name + File.separator
        val fi = new File(userDir + this.name + ".conf")
        fi.getParentFile.mkdirs()

        val eventNames = events.keys.asJava
        var conf = ConfigFactory.empty
        conf = conf.withValue("catalog.name", ConfigValueFactory.fromAnyRef(this.name))
        conf = conf.withValue("catalog.events", ConfigValueFactory.fromIterable(eventNames))

        val fiWr = new FileWriter(fi)
        fiWr.write(conf.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
        fiWr.close
    }
}
