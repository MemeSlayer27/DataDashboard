import card.Card.*

import scala.io.Source
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.*

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import card.DataHandler


class APItests extends AnyFlatSpec with Matchers:
  "Method getResponseFromAPI" should "work" in {

    withClue("didn't work") {

      val dataHandler = DataHandler(url = "https://deckofcardsapi.com/api/deck/new/draw/?count=2", isLive = true)

      // get the data from the API
      val data = dataHandler.getJSONFromAPI()

      // check if the data is a valid json. If it is, then the test passes
      data.onComplete( x =>
        println(x.getOrElse("no data"))
        x.get.isInstanceOf[io.circe.Json] shouldBe true

      )


      Await.result(data, scala.concurrent.duration.Duration.Inf)
    }

  }

end APItests

