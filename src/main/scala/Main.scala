import scala.collection.mutable.ArrayBuffer
import java.text.DecimalFormat;

import scala.concurrent.duration._
import akka.util.ByteString
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.http.scaladsl.model.ws.{ BinaryMessage, Message, TextMessage }
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import scala.io.StdIn



object HttpServerRoutingMinimal {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "my-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext


      def greeter: Flow[Message, Message, Any] =
        Flow[Message].mapConcat {
          case tm: TextMessage =>
            TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
          case bm: BinaryMessage =>
            // ignore binary messages but drain content to avoid the stream being clogged
            bm.dataStream.runWith(Sink.ignore)
            Nil  
        }
      val websocketRoute = path("greeter") {
          handleWebSocketMessages(greeter)
        }

    val bindingFuture = Http().newServerAt("localhost", 8080).bind(websocketRoute)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

//
//object Main extends App {
//
//  println("Hello, World!")
//
//
//  //#greeter-service
//  def greeter: Flow[Message, Message, Any] =
//    Flow[Message].mapConcat {
//      case tm: TextMessage =>
//        TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
//      case bm: BinaryMessage =>
//        // ignore binary messages but drain content to avoid the stream being clogged
//        bm.dataStream.runWith(Sink.ignore)
//        Nil
//    }
//  val websocketRoute =
//    path("greeter") {
//      handleWebSocketMessages(greeter)
//    }
//
//}