package com.cliftbar.flapyakka.hurricane

import java.io.{File, FileWriter}
import java.nio.file.Path

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}

import scala.collection.JavaConverters._
import scala.collection.mutable

object Catalog {
    def load(userId: Int, catalogName: String): Unit ={

    }

    def getUserCatalogs(catalogPath: Path): Seq[String] = {
        val catalogDir = new File(catalogPath.toString)
        val userCatalogs: Seq[String] = catalogDir.listFiles().filter(x => x.isDirectory).map(x => x.getName).toSeq
        return userCatalogs
    }

    def getCatalogEvents(catalogConf: Path): Seq[String] ={
        val confFi = new File(catalogConf.toString)
        val conf = ConfigFactory.parseFile(confFi)
        return conf.getStringList("catalog.events").asScala.toList
    }
}

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
