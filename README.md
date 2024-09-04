## Testing out Apache Pekko

Just playing around with Pekko. I wanted to write something quick that used both Pekko actors and Pekko streams together. The program is supposed to work like an auction. A stream of auction items is sent to the Auctioneer, which forwards them to Buyers. Buyers send their random bids to a Closer, which determines who wins the auction. 

Things I should add:
- Auctions usually depend on a response time, so if I add that then I need to send messages to Buyer actors in parallel.
- Once an auction is complete, I should remove it from the MaxBids list.

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.


