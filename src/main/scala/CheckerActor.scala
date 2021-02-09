package upmc.akka.leader

import java.util
import java.util.Date

import akka.actor._
import akka.util.Timeout

import scala.util._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Tick
case class CheckerTick () extends Tick

class CheckerActor (val id:Int, val terminaux:List[Terminal], electionActor:ActorRef) extends Actor {

     var time : Int = 200
     val father = context.parent

     var nodesAlive:List[Int] = List()
     var datesForChecking:List[Date] = List()
     var lastDate:Date = null

     var leader : Int = -1
     val scheduler = context.system.scheduler
     var allNodes:List[ActorSelection] = List()

     def receive = {

          // Initialisation
          case Start => {
               implicit val timeout = Timeout(5 seconds)
               terminaux.foreach(n => {
                    val remote = context.actorSelection("akka.tcp://LeaderSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Node")
                    this.allNodes = this.allNodes:::List(remote)
               })
               var actorId = 0;
               for(actorId <- 0 to this.allNodes.length - 1){
                    this.allNodes(actorId).resolveOne().onComplete{
                         case Success(act) =>{
                              if(!this.nodesAlive.contains(actorId))
                                   this.nodesAlive = this.nodesAlive:::List(actorId)
                         }
                         case Failure(ex) =>
                              this.nodesAlive =  this.nodesAlive.filter(_ != actorId)
                    }
               }
               println(allNodes.toString())
               self ! CheckerTick
          }

          // A chaque fois qu'on recoit un Beat : on met a jour la liste des nodes
          case IsAlive (nodeId) => {
               if (!this.nodesAlive.contains(nodeId))
               {
                    this.nodesAlive = this.nodesAlive:::List(nodeId)
                    father ! Message ("Nodes Alive : (" + this.nodesAlive.mkString(", ") + ")")
               }
          }

          case IsAliveLeader (nodeId) => this.leader = nodeId

          // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
          // Objectif : lancer l'election si le leader est mort
          case CheckerTick => {
               var actorId = 0
               for(actorId <- 0 to this.allNodes.length - 1) {
                    if (actorId != id && this.nodesAlive.contains(actorId)) {
                         implicit val timeout = Timeout(5 seconds)
                         this.allNodes(actorId).resolveOne().onComplete {                            
                              case Success(actor) => 

                              case Failure(ex) => {
                                   this.nodesAlive =  this.nodesAlive.filter(_ != actorId)
                                   father ! Message ("Leader : "+ this.leader + "; Nodes Alive : (" + this.nodesAlive.mkString(", ") + ")")
                                   
                                   if(actorId == leader)
                                   {
                                        electionActor ! StartWithNodeList (this.nodesAlive)
                                   }
                              }
                         }    
                    }
               }
               scheduler.scheduleOnce(time milliseconds , self, CheckerTick) 
          }
     }


}
