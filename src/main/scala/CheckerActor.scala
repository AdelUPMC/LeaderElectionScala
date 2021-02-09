package upmc.akka.leader

import java.util
import java.util.Date

import akka.actor._

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

     var maxTicks = 30;
     var curTicks = 0;
     var leader : Int = -1

     def receive = {

          // Initialisation
          case Start => {
               self ! CheckerTick
          }

          // A chaque fois qu'on recoit un Beat : on met a jour la liste des nodes
          case IsAlive (nodeId) => {
               var contains = false;
               nodesAlive.foreach(n => {
                    if(n == nodeId) contains = true;
               })
               if(!contains){
                    this.nodesAlive = this.nodesAlive:::List(nodeId)
               }
               
          }

          case IsAliveLeader (nodeId) => {
               var contains = false;
               nodesAlive.foreach(n => {
                    if(n == nodeId) contains = true;
               })
               if(!contains){
                    this.nodesAlive = this.nodesAlive:::List(nodeId)
               }
               this.leader = nodeId;
          }

           
          // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
          // Objectif : lancer l'election si le leader est mort
          case CheckerTick => {
               // Check if this node is leader
               var contains = this.id == this.leader
               // Check if leader in list alive
               if(!contains){
                    this.nodesAlive.foreach( n=>{
                         if(n == this.leader) contains = true
                    })
               }
               // We haven't leader's message in maxTicks, we confirm that leader was dead
               if(curTicks == maxTicks - 1) {
                    var msg = "leader=" + this.leader + ", nodesAlive: " + nodesAlive.toString()
                    println(msg);
                    if(!contains) electionActor ! StartWithNodeList(this.nodesAlive)
                    this.nodesAlive = List()
               }
               curTicks = (curTicks + 1) % maxTicks
               Thread.sleep(time)
               self ! CheckerTick
          }

     }


}
