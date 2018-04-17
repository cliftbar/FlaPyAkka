package com.cliftbar.flapyakka.routes

import akka.http.scaladsl.model.HttpHeader
import com.cliftbar.flapyakka.models.FlaPyAkkaModel

object UserValidator {
    def validateUser(headers: Seq[HttpHeader], model: FlaPyAkkaModel): Option[Int] = {
        val userIDheader: Seq[HttpHeader] = headers.filter(x => x.name() == "user-id")
        return model.validateUser(Some(userIDheader(0).value.toInt))
    }
}
