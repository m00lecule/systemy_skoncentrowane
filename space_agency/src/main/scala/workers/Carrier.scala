package workers

import com.rabbitmq.client._
import workers.settings.{Exchange, Settings}


class Carrier(override val connection: Connection, val tasks: List[String], val exchange: String, override val routes: Map[Exchange, List[String]]) extends Mailbox {

  val channels: Map[String, Channel] = tasks.foldLeft(Map[String, Channel]()) { (m, t) => m + (t -> connection.createChannel()) }

  channels foreach { case (t, ch) => createListener(t, ch) }

  def createListener(queueName: String, channel: Channel): Unit = {
    channel.queueDeclare(queueName, true, false, false, null)
    channel.basicQos(1)
    channel.exchangeDeclare(exchange, "topic")
    channel.queueBind(queueName, exchange, queueName)

    val deliverCallback: DeliverCallback = (consumerTag: String, delivery: Delivery) => {
      val message = new String(delivery.getBody, Settings.encoding)
      println(" [x] Received '" + message + "'")
      channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
      channel.basicPublish(exchange, message, MessageProperties.PERSISTENT_TEXT_PLAIN, "1".getBytes(Settings.encoding))
    }

    channel.basicConsume(queueName, false, deliverCallback, (_: String) => {})
  }
}


object Carrier {

  @throws[Exception]
  def main(argv: Array[String]): Unit = {
    val factory = new ConnectionFactory
    factory.setHost(Settings.IP)
    val connection = factory.newConnection

    val _: Carrier = new Carrier(connection, Settings.tasks, Settings.productionLine.name, Map())

  }
}