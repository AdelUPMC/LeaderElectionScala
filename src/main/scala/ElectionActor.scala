package upmc.akka.leader

import akka.actor._
import scala.concurrent.duration._


abstract class NodeStatus
case class Passive () extends NodeStatus
case class Candidate () extends NodeStatus
case class Dummy () extends NodeStatus
case class Waiting () extends NodeStatus
case class Leader () extends NodeStatus

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
     var status:NodeStatus = new Passive ()

     def neighboor(nodeId:Int):Int={
          var n = this.nodesAlive.indexOf(nodeId)
          do{
               n = (n + 1) % terminaux.length
          }while(!nodesAlive.contains(n))
          return n
     }


     def receive = {
          // Initialisation
          case Start => {
               self ! Initiate
          }

          case StartWithNodeList (list) => {
               println("LeaderElection start ...")

               if (list.isEmpty) {
                    this.nodesAlive = this.nodesAlive:::List(id)
               }
               else {
                    this.nodesAlive = list
               }
               self ! Initiate
          }

          case Initiate => {
               this.status = new Candidate() 
               val neighboor = this.neighboor(id)
               father ! SendLeaderMessage(ALG(this.nodesAlive, id), neighboor)
          }

          case ALG (list, init) => {
               if(this.status.equals(Passive())) {
                    this.status = new Dummy()
                    val neighboor = this.neighboor(id)
                    father ! SendLeaderMessage(ALG(this.nodesAlive, id), neighboor)
               }
               else if(this.status.equals(Candidate())) {
                    this.candPred = init
                    if(id > init) {
                         if(this.candSucc == -1) {
                              this.status = new Waiting()
                              father ! SendLeaderMessage(AVS (this.nodesAlive, id), init)
                         } else {
                              father ! SendLeaderMessage(AVSRSP (this.nodesAlive, this.candPred), this.candSucc)
                              this.status = new Dummy()
                         }
                    }
                    if(init == id) {
                         this.status = new Leader()
                         father ! LeaderChanged (id)
                    }
               }
          }

          case AVS (list, j) => {
               if(this.status.equals(Candidate())) {
                    if(this.candPred == -1)
                         this.candSucc = j
                    else { 
                         father ! SendLeaderMessage(AVSRSP(this.nodesAlive, this.candPred), j)
                         this.status = new Dummy()
                    }
               } else if(this.status.equals(Waiting()))
                         this.candSucc = j
          }

          case AVSRSP (list, k) => {
               if(this.status.equals(Waiting())) {
                    if(this.id == k)
                         this.status = new Leader()
                    else {
                         this.candPred = k
                         if (this.candSucc == -1) {
                              if(k < this.id) {
                                   this.status = new Waiting()
                                   father ! SendLeaderMessage(AVS(this.nodesAlive, this.id), k)
                              }
                         } else {
                              this.status = new Dummy()
                              father ! SendLeaderMessage(AVSRSP(nodesAlive, k), this.candSucc)
                         } 
                    }
               }
          }
     }
}
