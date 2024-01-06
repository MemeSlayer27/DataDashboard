import card.{Card, CardFileHelper, PieChartNumeric, Plot, ValueRange}
import card.Card.*
import card.ValueRange.*

import scala.io.Source
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.*

class JSONtests extends AnyFlatSpec with Matchers:
  "Method buildCardFromFile" should "build the correct card.Card from json" in {

    withClue("test_resources/read1.json didn't decode properly") {


      val ree = CardFileHelper
      val yee = ree.buildCardFromFile("test_resources/read1.json")

      yee shouldBe Card(
        1,
        "Temperature Data",
        0.0,
        0.0,
        300.0,
        200.0,
        "#FFFFFF",
        Vector("test_resources/sensor_readings.csv"),
        Some(ValueRange(Some(0.0), Some(100.0))),
        Some(ValueRange(Some(-10.0), Some(40.0))),
        false,
        PieChartNumeric(intervals = 10, field =  "temperature")
      )

    }

  }


  "Method saveCard" should "encode the object correctly into json and write the json to the given file, then read the file and decode it properly" in {

    withClue("PCN should encode & decode properly") {
      val card = Card(
          1,
          "Temperature Data",
          0.0,
          0.0,
          300.0,
          200.0,
          "#FFFFFF",
          Vector("test_resources/sensor_readings.csv"),
          Some(ValueRange(Some(0.0), Some(100.0))),
          Some(ValueRange(Some(-10.0), Some(40.0))),
          false,
          PieChartNumeric(intervals = 10, field =  "temperature")
        )

      val path = "test_resources/write1.json"

      val ree = CardFileHelper
      val yee = ree.saveCard(card, path)

      val dee = ree.buildCardFromFile(path)

      dee shouldBe Card(
          1,
          "Temperature Data",
          0.0,
          0.0,
          300.0,
          200.0,
          "#FFFFFF",
          Vector("test_resources/sensor_readings.csv"),
          Some(ValueRange(Some(0.0), Some(100.0))),
          Some(ValueRange(Some(-10.0), Some(40.0))),
          false,
          PieChartNumeric(intervals = 10, field =  "temperature")
        )

    }

    withClue("PCN should encode & decode properly") {
      val card = Card(
          1,
          "Price Data",
          0.0,
          0.0,
          300.0,
          200.0,
          "#FFFFFF",
          Vector("data/stockpricedata.csv"),
          Some(ValueRange(Some(0.0), Some(100.0))),
          Some(ValueRange(Some(-10.0), Some(40.0))),
          false,
          Plot(
            fields = Vector(("Date", "High")),
            names = Vector("PriceData"),
          )

        )

      val path = "test_resources/write2.json"

      val ree = CardFileHelper
      val yee = ree.saveCard(card, path)

      val dee = ree.buildCardFromFile(path)

      dee shouldBe Card(
          1,
          "Price Data",
          0.0,
          0.0,
          300.0,
          200.0,
          "#FFFFFF",
          Vector("data/stockpricedata.csv"),
          Some(ValueRange(Some(0.0), Some(100.0))),
          Some(ValueRange(Some(-10.0), Some(40.0))),
          false,
          Plot(
            fields = Vector(("Date", "High")),
            names = Vector("PriceData"),
          )

        )

    }

  }

end JSONtests

