package upmc.akka.leader

import akka.actor._
import scala.concurrent.duration._


abstract class NodeStatus
case object Passive extends NodeStatus
case object Candidate  extends NodeStatus
case object Dummy  extends NodeStatus
case object Waiting  extends NodeStatus
case object Leader extends NodeStatus

abstract class LeaderAlgoMessage
case class Initiate () extends LeaderAlgoMessage
case class ALG (list:List[Int], nodeId:Int) extends LeaderAlgoMessage
case class AVS (list:List[Int], nodeId:Int) extends LeaderAlgoMessage
case class AVSRSP (list:List[Int], nodeId:Int) extends LeaderAlgoMessage

case class SendLeaderMessage(msg:LeaderAlgoMessage, dest:Int)

case class StartWithNodeList (list:List[Int])

class ElectionActor (val id:Int, val terminaux:List[Terminal]) extends Actor {
     // import context.system.dispatcher
     // var scheduler = context.system.scheduler

     val father = context.parent
     var nodesAlive:List[Int] = List(id)

     var candSucc:Int = -1
     var candPred:Int = -1
     var status:NodeStatus =  Passive

     def getSucc(i:Int):Int={
          var n = i
          do{
               n = (n + 1) % 4
          }while(!nodesAlive.contains(n))
          return n
     }

     def startLeader(){
          father ! LeaderChanged(id)
          context.stop(self)
     }

     def receive = {

          // Initialisation
          case Start => {
               father ! Message("Leader election start")
               self ! Initiate
          }

          case StartWithNodeList (list) => {
               father ! Message("Leader election start, alive: " + list.toString())
               if (list.isEmpty) {
                    this.nodesAlive = this.nodesAlive:::List(id)
               }
               else {
                    this.nodesAlive = list
               }

               // Debut de l'algorithme d'election
               self ! Initiate
          }

          case Initiate => {
               println("Initiate")
               this.status = Candidate;
               var next = getSucc(this.id)
               father ! SendLeaderMessage(ALG(this.nodesAlive, this.id), next)
          }

          case ALG (list, init) => {
               println("ALG(" + init + ")")
               if (this.status ==  Passive){
                    this.status =  Dummy
                    father ! SendLeaderMessage(ALG(list, init), getSucc(id))
               } else if(this.status == Candidate){
                    candPred = init
                    if(this.id > init){
                         if(candSucc == -1){
                              this.status = Waiting
                              father ! SendLeaderMessage(AVS(list, id), init)
                         }else{
                              father ! SendLeaderMessage(AVSRSP(list, candPred), candSucc)
                              status = Dummy
                         }
                    } else if (id == init){
                         status = Leader
                         startLeader()
                    }
               }
          }

          case AVS (list, j) => {
               println("AVS(" + j + ")")
               if(status == Candidate) {
                    if(candPred == -1){
                         candSucc = j
                         // scheduler.scheduleOnce(100 milliseconds, self, AVS(list, j))   
                    } else{
                         father != SendLeaderMessage(AVSRSP(list, candPred), j)
                         status = Dummy
                    }
               } else if( status == Waiting) {
                    candSucc = j
               }
          }

          case AVSRSP (list, k) => {
               println("AVSRSP(" + k + ")")
               if (status == Waiting) {
                    if(id == k) {
                         status = Leader
                         startLeader()
                    } else{
                         candPred = k
                         if(candSucc == -1){
                              if(k < id){
                                   status = Waiting
                                   father ! SendLeaderMessage(AVS(list, id), k)
                              }
                         } else{
                              status = Dummy
                              father ! SendLeaderMessage(AVSRSP(list, k), candSucc)
                         }
                    }
               }
          }

     }

}
