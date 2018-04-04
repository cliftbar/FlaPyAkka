package com.cliftbar.flapyakka.app

// Scala
import scala.collection.mutable

// Config
import com.typesafe.config.{ConfigFactory, ConfigRenderOptions, ConfigValueFactory}

// Java
import java.io.{FileWriter, File}

class User(id: Int, username: String){
    def saveConfig(rootSaveDir: String) {
        val userDir = rootSaveDir + this.username + "/"
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
    private val userIds: Seq[User] = Seq()
    private val userDir: String = ConfigFactory.load().getConfig("app").getString("userDir")

    def validateUser(id: Int): Boolean = {
        return (0 <= id && id < userIds.length)
    }

    def addUser(username: String) {
        val id = userIds.length
        this.userIds :+ new User(id, username)
        this.userNames.put(username, id)
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
}
