package upmc.akka.leader

import akka.actor._

import scala.collection.mutable.HashMap
import scala.collection.mutable.Map

case class Start ()

sealed trait SyncMessage
case class Sync (nodes:List[Int]) extends SyncMessage
case class SyncForOneNode (nodeId:Int, nodes:List[Int]) extends SyncMessage

sealed trait AliveMessage
case class IsAlive (id:Int) extends AliveMessage
case class IsAliveLeader (id:Int) extends AliveMessage



class Node (val id:Int, val terminaux:List[Terminal]) extends Actor {

     // Les differents acteurs du systeme
     val electionActor = context.actorOf(Props(new ElectionActor(this.id, terminaux)), name = "electionActor")
     val checkerActor = context.actorOf(Props(new CheckerActor(this.id, terminaux, electionActor)), name = "checkerActor")
     val beatActor = context.actorOf(Props(new BeatActor(this.id)), name = "beatActor")
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")

     var idToindex:Map[Int,Int]= new HashMap[Int, Int]()
     var allNodes:List[ActorSelection] = List()

     def receive = {
          // Initialisation
          case Start => {
               displayActor ! Message ("Node " + this.id + " is created")
               checkerActor ! Start
               beatActor ! Start
               // Initilisation des autres remote, pour communiquer avec eux
               terminaux.foreach(n => {
                    val remote = context.actorSelection("akka.tcp://LeaderSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Node")
                    this.allNodes = this.allNodes:::List(remote)
               })
          }

          // Envoi de messages (format texte)
          case Message (content) => {
               displayActor ! Message (content)
          }

          case BeatLeader (nodeId) => {
               allNodes.foreach(actor => actor ! IsAliveLeader(nodeId))
          }
          case Beat (nodeId) => {
               allNodes.foreach(actor => actor ! IsAlive(nodeId))
          }

          // Messages venant des autres nodes : pour nous dire qui est encore en vie ou mort
          case IsAlive (id) => {
               checkerActor ! IsAlive(id)
          }
          case IsAliveLeader (id) => {
               checkerActor ! IsAliveLeader (id)
          }

          // Message indiquant que le leader a change
          case LeaderChanged (nodeId) => {
               beatActor ! LeaderChanged(nodeId)
          }

          case SendLeaderMessage(msg, destId) => {
               this.allNodes(destId) ! msg
          }
          case ALG(list, nodeId) => {
               electionActor ! ALG(list, nodeId)
          }
          case AVS (list, j) => {
               electionActor ! AVS(list, j)
          }
          case AVSRSP (list, k) => {
               electionActor ! AVSRSP(list, k)
          } 
     }
}
