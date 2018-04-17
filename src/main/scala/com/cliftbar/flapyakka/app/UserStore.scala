package com.cliftbar.flapyakka.app

// Scala
import java.io.FileFilter
import java.nio.file.Paths

import scala.collection.mutable
import scala.collection.JavaConverters._

// Config
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}

// Java
import java.io.{FileWriter, File}
import java.nio.file.{Path, Files}

class User(){
    var id: Int = -1
    var username = ""
    def loadConfig(configDir: String): Unit ={
        val username: String = Paths.get(configDir).getFileName.toString
        val confFi = new File(configDir + File.separator + username + ".conf")
        val conf = ConfigFactory.parseFile(confFi)
        this.id = conf.getInt("user.id")
        this.username = conf.getString("user.username")
    }
    def saveConfig(rootSaveDir: String) {
        val userDir = rootSaveDir + this.username + File.separator
        val fi = new File(userDir + this.username + ".conf")
        fi.getParentFile.mkdirs()

        var conf = ConfigFactory.empty
        //conf = conf.withOnlyPath("user")
        conf = conf.withValue("user.id", ConfigValueFactory.fromAnyRef(this.id))
        conf = conf.withValue("user.username", ConfigValueFactory.fromAnyRef(this.username))
        //conf = conf.withValue("user.testlist", ConfigValueFactory.fromIterable(Seq("1", "2").asJava))

        val fiWr = new FileWriter(fi)
        fiWr.write(conf.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
        fiWr.close
    }
}

object UserStore {
//    private val userNames: mutable.HashMap[String, Int] = new mutable.HashMap[String, Int]
//    private var userIds: Seq[User] = Seq()
    val userDir: String = ConfigFactory.load().getConfig("app").getString("userDir")

//    this.initUsers()
//
//    private def initUsers(): Unit ={
//        val userDir = new File(this.userDir)
//        for (fi <- userDir.listFiles){
//            if (fi.isDirectory) {
//                val user = new User()
//                user.loadConfig(fi.getPath)
//                this.userIds = this.userIds :+ user
//                this.userNames.put(user.username, user.id)
//                println(s"loading user: ${user.username}")
//            }
//        }
//    }
    def validateUser(id: Option[Int]): Option[Int] = {
        val ret = if (id.nonEmpty) {
            val userDir = new File(this.userDir)
            val userFiles = userDir.listFiles().filter(x => x.isDirectory)

            val ids: Seq[Int] = userFiles.map({ x =>
                val user = new User
                user.loadConfig(this.userDir + x.getName)
                user.id
            })

            ids.find(x => id.get == x)
        } else {
            None
        }

        return ret
    }

    def addUser(username: String): Int = {
        val id = if (!this.isUser(username)) {
            val userDir = new File(this.userDir)
            val newId = userDir.listFiles().count(x => x.isDirectory)//.filter(x => x.isDirectory).length
            val u = new User()
            u.id = newId
            u.username = username
            u.saveConfig(this.userDir)
//            this.userIds = this.userIds :+ u
//            this.userNames.put(username, newId)
            newId
        } else {
            -1
        }
        return id
    }
    def isUser(username: String): Boolean = {
        val userDir = Paths.get(this.userDir + username)
        return Files.exists(userDir)
    }
    def getUserId(username: String): Option[Int] = {
        val retId = if (this.isUser(username)) {
            val user = new User()
            val userDir = this.userDir + username
            user.loadConfig(userDir)
            Some(user.id)
        } else {
            None
        }
        return retId
    }

    def getUsername(id: Int): String = {
        val userDir = new File(this.userDir)
        val userFiles = userDir.listFiles().filter(x => x.isDirectory)

        val username: String = userFiles.map({ x =>
            val user = new User
            user.loadConfig(this.userDir + x.getName)
            (user.id, user.username)
        }).filter(x => x._1 == id)(0)._2
        return username
    }

    def getUserDir(userId: Int): Option[String] = {
        val ret: Option[String] = if (validateUser(Some(userId)).nonEmpty){
            Some(this.userDir + this.getUsername(userId))
        } else {
            None
        }
        return ret
    }
//
//    def getUser(id: Int): Option[User] = {
//        val ret: Option[User] = if (this.validateUser(id).nonEmpty){
//            Some(this.userIds(id))
//        } else {
//            None
//        }
//
//        return ret
//    }

//    private def saveUser(id:Int) {
//        userIds(id).saveConfig(this.userDir)
//    }

    def printUsers(): Unit ={
        val userDir = new File(this.userDir)
        val userNames:Seq[String] = userDir.listFiles().filter(x => x.isDirectory).map(x => x.getName).toSeq
        println(userNames.toString())
    }
}
