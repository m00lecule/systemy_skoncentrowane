package actor.server

import java.util

import actor.shop.ShopActor
import message._
import akka.actor.{Actor, ActorRef, Props}
import scala.language.postfixOps

import scala.concurrent.duration._

class ServerActor(val shopsCount: Int) extends Actor {

  import context.dispatcher

  val threads = 10
  val shops: List[ActorRef] = List.tabulate(shopsCount)(n => context.actorOf(ShopActor(threads, n)));
  var id = 0
  val requests = new util.HashMap[Int, (Option[Float], String, ActorRef)]();

  def receive = {
    case ClientRequest(name) => {
      requests.put(id, (None, name, sender()))
      val scheduledRequest = ServerRequest(id, name)
      shops.foreach(s => s ! scheduledRequest)
      context.system.scheduler.scheduleOnce(300 milliseconds, self, ServerTimeout(id))
      id += 1
    }

    case ServerTimeout(id) => {
      val (value, name, sender) = requests.get(id);
      sender ! ClientResponse(name, value ,counter = None)
      requests.remove(id)
    }

    case ServerResponse(id, name, price) =>
      if (requests.containsKey(id)) {
        val (r_value, r_name, r_sender) = requests.get(id);
        r_value match {
          case Some(p) if p.compareTo(price) > 0 => requests.put(id, (Some(price), r_name, r_sender))
          case _ => requests.put(id, (Some(price), r_name, r_sender))
        }
      }
  }
}

object ServerActor {
  def apply(count: Int): Props = Props(new ServerActor(count))
}