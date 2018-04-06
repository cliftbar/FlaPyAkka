package com.cliftbar.flapyakka.app

// Scala
import scala.collection.mutable

// Config
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}

// Java
import java.io.{FileWriter, File}

class User(){
    var id: Int = -1
    var username = ""
    def loadConfig(configDir: String): Unit ={
        val username = configDir.split(File.separatorChar).last
        val conf_fi = new File(configDir + File.separator + username + ".conf")
        val conf = ConfigFactory.parseFile(conf_fi)
        this.id = conf.getInt("id")
        this.username = conf.getString("username")
    }
    def saveConfig(rootSaveDir: String) {
        val userDir = rootSaveDir + this.username + File.separator
        val fi = new File(userDir + username + ".conf")
        fi.getParentFile.mkdirs()

        var conf = ConfigFactory.empty
        conf = conf.withValue("id", ConfigValueFactory.fromAnyRef(this.id))
        conf = conf.withValue("username", ConfigValueFactory.fromAnyRef(this.username))

        val fiWr = new FileWriter(fi)
        fiWr.write(conf.root().render(ConfigRenderOptions.defaults().setOriginComments(false)))
        fiWr.close
    }
}

class UserStore {
    private val userNames: mutable.HashMap[String, Int] = new mutable.HashMap[String, Int]
    private var userIds: Seq[User] = Seq()
    private val userDir: String = ConfigFactory.load().getConfig("app").getString("userDir")

    this.initUsers()

    private def initUsers(): Unit ={
        val userDir = new File(this.userDir)
        for (fi <- userDir.listFiles){
            if (fi.isDirectory) {
                val user = new User()
                user.loadConfig(fi.getPath)
                this.userIds = this.userIds :+ user
                this.userNames.put(user.username, user.id)
                println(s"loading user: ${user.username}")
            }
        }
    }
    def validateUser(id: Int): Boolean = {
        return (0 <= id && id < userIds.length)
    }

    def addUser(username: String): Int = {
        val id = userIds.length
        val u = new User()
        u.id = id
        u.username = username
        u.saveConfig(this.userDir)
        this.userIds = this.userIds :+ u
        this.userNames.put(username, id)
        return id
    }

    def getUserId(username: String): Option[Int] = {
        return this.userNames.get(username)
    }

    def getUser(id: Int): Option[User] = {
        val ret: Option[User] = if (this.validateUser(id)){
            Some(this.userIds(id))
        } else {
            None
        }

        return ret
    }

    private def saveUser(id:Int) {
        userIds(id).saveConfig(this.userDir)
    }

    def printUsers(): Unit ={
        println(userNames.toString())
    }
}
