package com.cliftbar.flapyakka.hurricane

import java.io.{File, FileWriter}
import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}

import scala.collection.JavaConverters._
//import scala.collection.mutable

object Catalog {
    def loadFromSaved(catalogName: String, rootUserDir: String): Option[Catalog] ={
        val catalogConfPath = Paths.get(rootUserDir, "catalogs", catalogName, catalogName + ".conf")
        val ret: Option[Catalog] = if (Files.exists(catalogConfPath)) {
            val confFi = new File(catalogConfPath.toString)
            val conf = ConfigFactory.parseFile(confFi)

            val catTemp = new Catalog(conf.getString("catalog.name"), conf.getString("catalog.owner"))
            val eventsTemp: Seq[String] = conf.getStringList("catalog.events").asScala
            eventsTemp.map(x => catTemp.addEvent(x))
            Some(catTemp)
        } else {
            None
        }
        return ret
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
    private var events: Set[String] = Set[String]()

    def getNames(): Seq[String] = {
        this.events.toSeq
    }

    def addEvent(eventName: String) = {
        this.events = this.events + eventName
    }

    def removeEvent(eventName: String) = {
        this.events = this.events - eventName
    }

    def loadFromFile(fileUri: String) {}

    def importFromFile(fileUri: String) {}

    def importHurdatCatalog(fileName: String) {}

    def saveCatalog(rootSaveDir: String): Unit = {
        val userDir = rootSaveDir + this.username + File.separator + "catalogs" + File.separator  + this.name + File.separator
        val fi = new File(userDir + this.name + ".conf")
        fi.getParentFile.mkdirs()

        val eventNames = events.asJava
        var conf = ConfigFactory.empty
        conf = conf.withValue("catalog.owner", ConfigValueFactory.fromAnyRef(this.username))
        conf = conf.withValue("catalog.name", ConfigValueFactory.fromAnyRef(this.name))
        conf = conf.withValue("catalog.events", ConfigValueFactory.fromIterable(eventNames))

        val fiWr = new FileWriter(fi)
        fiWr.write(conf.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
        fiWr.close
    }
}
