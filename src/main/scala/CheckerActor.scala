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

     var leader : Int = -1

     def receive = {

          // Initialisation
          case Start => {
               Thread.sleep(2000)
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
               var msg = "leader=" + this.leader + ", nodesAlive: " + nodesAlive.toString()
               var contains = false
               this.nodesAlive.foreach( n=>{
                    if(n == this.leader) contains = true
               })
               father ! Message(msg)
               if(!contains) {
                    electionActor ! Start
               }
               Thread.sleep(time)
               this.nodesAlive = List()
               self ! CheckerTick
          }

     }


}
