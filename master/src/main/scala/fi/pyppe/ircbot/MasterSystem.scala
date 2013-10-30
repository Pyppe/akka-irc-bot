package fi.pyppe.ircbot

import AkkaUtil.remoteActorSystemConfiguration

import fi.pyppe.ircbot.event._

import akka.actor._
import org.pircbotx.{Configuration, PircBotX}
import org.pircbotx.hooks.{ListenerAdapter, Listener}
import org.pircbotx.hooks.events.{PrivateMessageEvent, MessageEvent}
import org.joda.time.DateTime
import com.typesafe.config.{ConfigFactory, Config}
import fi.pyppe.ircbot.CommonConfig._
import fi.pyppe.ircbot.event.Message
import fi.pyppe.ircbot.event.PrivateMessage

object MasterSystem {

  val RemoteActorSystem = ActorSystem(actorSystemName, remoteActorSystemConfiguration(host, masterPort, secureCookie))
  val slaveLocation = s"akka.tcp://$actorSystemName@$host:$slavePort/user/$slaveName"

  def main(args: Array[String]) {

    val configuration =
      new Configuration.Builder()
        .setName("maunobot")
        .setLogin("maunobot")
        .setAutoNickChange(true)
        .setCapEnabled(true)
        .setRealName("maunobot")
        //.addListener(new PircBotXExample()) //This class is a listener, so add it to the bots known listeners
        .addListener(new IrcListener(slaveLocation))
        .setServerHostname("open.ircnet.net")
        .addAutoJoinChannel("#pyppe-testaa")
        .buildConfiguration()

    try {
      val bot = new PircBotX(configuration)
      RemoteActorSystem.actorOf(Props(classOf[MasterBroker], slaveLocation, bot), masterName)
      bot.startBot()
    } catch {
      case e: Exception => e.printStackTrace
    }
  }

}

class IrcListener[T <: PircBotX](slaveLocation: String) extends ListenerAdapter[T] {

  val slave = MasterSystem.RemoteActorSystem.actorSelection(slaveLocation)

  override def onMessage(event: MessageEvent[T]): Unit = {
    val message = Message(new DateTime(event.getTimestamp),
                          event.getChannel.getName,
                          event.getUser.getNick,
                          event.getUser.getLogin,
                          event.getUser.getServer,
                          event.getMessage)
    slave ! message
  }

  override def onPrivateMessage(event: PrivateMessageEvent[T]) = {
    val privateMessage = PrivateMessage(new DateTime(event.getTimestamp),
                                        event.getUser.getNick,
                                        event.getUser.getLogin,
                                        event.getUser.getServer,
                                        event.getMessage)
    slave ! privateMessage
  }

}

class MasterBroker(slaveLocation: String, bot: PircBotX) extends Actor with LoggerSupport {
  import fi.pyppe.ircbot.action._

  protected implicit val ec = context.dispatcher
  protected val slave = context.actorSelection(slaveLocation)

  override def receive = {
    case say: SayToChannel =>
      logger.debug(s"Got $say from $sender")
      bot.getUserChannelDao.getChannel(say.channel).send.message(say.message)
  }

}
