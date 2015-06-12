package com.juliasoft.pinger

import akka.actor.{ActorRef, Actor}
import spray.routing._
import spray.http._
import MediaTypes._

class PingerServiceActor(val manager: ActorRef) extends Actor with PingerService {

  def actorRefFactory = context

  def receive = runRoute(pingerRoute)
}

trait PingerService extends HttpService {

  val pingerRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <h1>Say hello to <i>spray-routing</i> on <i>spray-can</i>!</h1>
              </body>
            </html>
          }
        }
      }
    }
}
