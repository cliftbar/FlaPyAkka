package com.cliftbar.flapyakka.routes

import akka.http.scaladsl.model.HttpHeader
import com.cliftbar.flapyakka.FlaPyAkkaModel

object UserValidator {
    def validateUser(headers: Seq[HttpHeader], model: FlaPyAkkaModel): Boolean = {
        val userIDheader: Seq[HttpHeader] = headers.filter(x => x.name() == "user-id")
        return model.validateUser(userIDheader(0).value.toInt)
    }
}
