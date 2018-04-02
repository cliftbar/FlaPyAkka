package com.cliftbar.flapyakka.app

import com.typesafe.config.ConfigFactory
import scala.collection.mutable

class User(id: Int, username: String){

}

class UserStore {
    private val userNames: mutable.HashMap[String, Int] = new mutable.HashMap[String, Int]
    private val userIds: Seq[User] = Seq()
    private val userDir: String = ConfigFactory.load().getConfig("app").getString("userDir")

    def addUser(username: String) {
        val id = userIds.length
        this.userIds :+ new User(id, username)
        this.userNames.put(username, id)
    }

    def getUserId(username: String): Option[Int] = {
        return this.userNames.get(username)
    }

    def getUser(id: Int): Option[User] = {
        val ret: Option[User] = if (id < userIds.length){
            Some(this.userIds(id))
        } else {
            None
        }
        return ret
    }

    private def saveUser(id:Int) {

    }
}
