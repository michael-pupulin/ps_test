import org.apache.pekko
import pekko.stream._
import pekko.stream.scaladsl._
import pekko.{ Done, NotUsed }
import pekko.util.ByteString
import scala.concurrent._
import scala.concurrent.duration._
import java.nio.file.Paths
import pekko.actor.typed.ActorSystem
import pekko.actor.typed.ActorRef
import pekko.actor.typed.Behavior
import pekko.actor.typed.scaladsl.AbstractBehavior
import pekko.actor.typed.scaladsl.ActorContext
import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.receptionist.Receptionist
import pekko.actor.typed.receptionist.ServiceKey



object AuctionActors extends App {

  sealed trait AuctionWord
  case class Bid(Impression: String, Amount: Int, Bidder: String) extends AuctionWord
  case class AuctionItem(Impression: String) extends AuctionWord
  case class AuctionItemWithCloser(Impression: String, CloserReference: ActorRef[AuctionActors.AuctionWord]) extends AuctionWord
  case class AuctionStart(BidderCount: Int) extends AuctionWord

  object Auctioneer {
    def apply(): Behavior[AuctionWord] =
      Behaviors.setup(context => new Auctioneer(context))
  }


  class Auctioneer(context: ActorContext[AuctionWord]) extends AbstractBehavior[AuctionWord](context) {

    var BidderPool:List[ActorRef[AuctionActors.AuctionWord]] = Nil
    var CloserRef:ActorRef[AuctionActors.AuctionWord] = _

    override def onMessage(msg: AuctionWord): Behavior[AuctionWord] =
      msg match {
        case AuctionItem(impression) => 
          BidderPool.map(bidder => bidder ! AuctionItemWithCloser(impression, CloserRef))
          this
        case AuctionStart(bidders) =>
          println(s"Starting auction house. Creating ${bidders} bidders and a closer.")
          BidderPool = (1 to bidders).toList.map(potentialbidder => context.spawn(Buyer(), s"Bidder$potentialbidder"))
          CloserRef = context.spawn(Closer(), "AuctionCloser")
          this
        case _ =>
          throw new Exception("Unexpected Message")
          this
      }
  }


  object Buyer {
    def apply(): Behavior[AuctionWord] =
      Behaviors.setup(context => new Buyer(context))
  }

  class Buyer(context: ActorContext[AuctionWord]) extends AbstractBehavior[AuctionWord](context) {
    val rand = new scala.util.Random
    override def onMessage(msg: AuctionWord): Behavior[AuctionWord] =
      msg match {
        case AuctionItemWithCloser(impression, closerref) =>  
          closerref ! Bid(impression, rand.nextInt(100), context.self.toString)
          this
        case _ =>
          throw new Exception("Unexpected Message")
          this
      }
  }


  object Closer {
    def apply(): Behavior[AuctionWord] =
      Behaviors.setup(context => new Closer(context))
  }

  class Closer(context: ActorContext[AuctionWord]) extends AbstractBehavior[AuctionWord](context) {
    var MaxBids:List[(AuctionActors.Bid,Int)] = List[(AuctionActors.Bid,Int)]()
    override def onMessage(msg: AuctionWord): Behavior[AuctionWord] =
      msg match {
        case Bid(imp, amt, bidder) =>  {
          if(MaxBids.filter((x,y) => x.Impression == imp).isEmpty) MaxBids = (Bid(imp, amt, bidder),1) :: MaxBids 
          else MaxBids = MaxBids.map((x,y) => 
            if (x.Impression != imp) (x,y)
            else if (x.Impression == imp && x.Amount >= amt) (x, y+1)
            else (Bid(imp, amt, bidder),y+1) 
            )

          {
            if( MaxBids.filter((x,y) => x.Impression == imp && y == 10).isEmpty) 
              println(s"Bid won with bid ${msg}")
          }

          this
        }
        case _ =>
          throw new Exception("Unexpected Message")
          this
      }
  }



  implicit val AuctionHouse: ActorSystem[AuctionWord] = ActorSystem(Auctioneer(), "Auctioneer")
  val houseref: ActorRef[AuctionActors.AuctionWord] = AuctionHouse
  AuctionHouse ! AuctionStart(100)

  val impressionstream: Source[Int, NotUsed] = Source(1 to 10000)
  val done: Future[Done] = impressionstream.map(_.toString).runForeach(i => AuctionHouse ! AuctionItem(i))
  
}


