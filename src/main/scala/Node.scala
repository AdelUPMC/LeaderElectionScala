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
               
               var index = 0
               terminaux.foreach(n => { 
                    if (n.id != id) {
                         val remote = context.actorSelection("akka.tcp://LeaderSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Node")
                         // Mise a jour de la liste des nodes
                         this.allNodes = this.allNodes:::List(remote)
                         idToindex+=(n.id-> index)
                         index+=1
                    }
               })
               println(idToindex)
          }

          // Envoi de messages (format texte)
          case Message (content) => {
               displayActor ! Message (content)
          }

          case BeatLeader (nodeId) => {
               this.allNodes.foreach(n => {
                    n ! IsAliveLeader(nodeId)
               })
               checkerActor ! IsAliveLeader(nodeId)
          }

          case Beat (nodeId) => {
               this.allNodes.foreach(n => {
                    n ! IsAlive(nodeId)
               })
               checkerActor ! IsAlive(nodeId)
          }

          // Messages venant des autres nodes : pour nous dire qui est encore en vie ou mort
          case IsAlive (id) => {
               this.checkerActor ! IsAlive(id)
          }

          case IsAliveLeader (id) => {
               this.checkerActor ! IsAliveLeader(id)
          }

          // Message indiquant que le leader a change
          case LeaderChanged (nodeId) => {
               this.beatActor ! LeaderChanged(nodeId)
          }

          case SendLeaderMessage(msg, dest) =>{
               println("SendLeaderMessage(" + msg.toString() + ", " + dest + ")")
               var index:Int = idToindex.getOrElse(dest, 0)
               this.allNodes(index) ! msg

          }
          case ALG (list, nodeId) => {
               println("node: ALG("+nodeId+")")
               electionActor ! ALG(list, nodeId)
          }
          case AVS (list, nodeId) => {
               println("node: AVS("+nodeId+")")               
               electionActor ! AVS(list, nodeId)
          }
          case AVSRSP(list ,nodeId) => {
               println("node: AVSRSP("+nodeId+")")
               electionActor ! AVSRSP(list, nodeId)
          }

     }

}
