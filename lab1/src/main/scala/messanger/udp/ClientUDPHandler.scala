package messanger.udp

import java.io.{ByteArrayInputStream, ObjectInputStream}
import java.net.{DatagramPacket, DatagramSocket, InetAddress}

import messanger.Server
import messanger.messages.{LogoutMessage, Message}


class ClientUDPHandler(override val socket: DatagramSocket) extends Runnable with DatagramRead {

  var registeredClients: scala.collection.mutable.Set[(Int, InetAddress)] = scala.collection.mutable.Set()

  override def run(): Unit = {
    while (true)
      processMessages
  }

  private def forwardPacket(packet: DatagramPacket): Unit = {
    val clientTuple = (packet.getPort, packet.getAddress)
    registeredClients += clientTuple

    registeredClients.filterNot(_ equals clientTuple).foreach {
      case (port, address) =>
        val dp = new DatagramPacket(packet.getData, packet.getLength, address, port)
        socket.send(dp)
    }
  }

  def processMessages(): Unit = {
    socket.receive(packet)
    val received: AnyRef = new ObjectInputStream(new ByteArrayInputStream(packet.getData)).readObject()

    received match {
      case mess: Message =>
        println(mess)
        forwardPacket(packet)
      case _: LogoutMessage =>
        registeredClients remove(packet.getPort, packet.getAddress)
    }

  }
}
