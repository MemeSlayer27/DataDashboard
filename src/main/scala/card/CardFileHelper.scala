package card

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.syntax._

import java.io.{File, FileWriter}
import scala.io.Source
import scala.util.{Failure, Success, Try}

/// Class for interfacing with files
object CardFileHelper:

  implicit val valueRangeEncoder: Encoder[ValueRange] = deriveEncoder[ValueRange]
  implicit val valueRangeDecoder: Decoder[ValueRange] = deriveDecoder[ValueRange]

  implicit val plotEncoder: Encoder[Plot] = deriveEncoder[Plot]
  implicit val plotDecoder: Decoder[Plot] = deriveDecoder[Plot]

  implicit val timeSeriesPlotEncoder: Encoder[TimeSeriesPlot] = deriveEncoder[TimeSeriesPlot]
  implicit val timeSeriesPlotDecoder: Decoder[TimeSeriesPlot] = deriveDecoder[TimeSeriesPlot]

  implicit val pieChartNumericEncoder: Encoder[PieChartNumeric] = deriveEncoder[PieChartNumeric]
  implicit val pieChartNumericDecoder: Decoder[PieChartNumeric] = deriveDecoder[PieChartNumeric]

  implicit val pieChartCategoricEncoder: Encoder[PieChartCategoric] = deriveEncoder[PieChartCategoric]
  implicit val pieChartCategoricDecoder: Decoder[PieChartCategoric] = deriveDecoder[PieChartCategoric]

  implicit val barChartNumericEncoder: Encoder[BarChartNumeric] = deriveEncoder[BarChartNumeric]
  implicit val barChartNumericDecoder: Decoder[BarChartNumeric] = deriveDecoder[BarChartNumeric]

  implicit val barChartCategoricEncoder: Encoder[BarChartCategoric] = deriveEncoder[BarChartCategoric]
  implicit val barChartCategoricDecoder: Decoder[BarChartCategoric] = deriveDecoder[BarChartCategoric]

  implicit val bigNumberEncoder: Encoder[BigNumber] = deriveEncoder[BigNumber]
  implicit val bigNumberDecoder: Decoder[BigNumber] = deriveDecoder[BigNumber]

  implicit val chartEncoder: Encoder[Chart] = Encoder.instance {
    case p: Plot =>
      Json.obj(
        "type" -> Json.fromString("Plot"),
        "fields" -> p.fields.asJson,
        "names" -> p.names.asJson,
      )
    case tsp: TimeSeriesPlot =>
      Json.obj(
        "type" -> Json.fromString("TimeSeriesPlot"),
        "fields" -> tsp.fields.asJson,
        "names" -> tsp.names.asJson,
      )
    case pcn: PieChartNumeric =>
      Json.obj(
        "type" -> Json.fromString("PieChartNumeric"),
        "intervals" -> pcn.intervals.asJson,
        "field" -> pcn.field.asJson,
      )
    case pcc: PieChartCategoric =>
      Json.obj(
        "type" -> Json.fromString("PieChartCategoric"),
        "field" -> pcc.field.asJson,
      )
    case bcn: BarChartNumeric =>
      Json.obj(
        "type" -> Json.fromString("BarChartNumeric"),
        "intervals" -> bcn.intervals.asJson,
        "field" -> bcn.field.asJson,
      )
    case bcc: BarChartCategoric =>
      Json.obj(
        "type" -> Json.fromString("BarChartCategoric"),
        "field" -> bcc.field.asJson,
      )
    case bn: BigNumber =>
      Json.obj(
        "type" -> Json.fromString("BigNumber"),
        "field" -> bn.field.asJson,
      )
  }


  implicit val chartDecoder: Decoder[Chart] = Decoder.instance { cursor =>
    cursor.get[String]("type").flatMap {
      case "Plot" => cursor.as[Plot]
      case "TimeSeriesPlot" => cursor.as[TimeSeriesPlot]
      case "PieChartNumeric" => cursor.as[PieChartNumeric]
      case "PieChartCategoric" => cursor.as[PieChartCategoric]
      case "BarChartNumeric" => cursor.as[BarChartNumeric]
      case "BarChartCategoric" => cursor.as[BarChartCategoric]
      case "BigNumber" => cursor.as[BigNumber]
      case _ => Left(DecodingFailure("Invalid Chart type", cursor.history))
    }
  }

  implicit val cardEncoder: Encoder[Card] = deriveEncoder[Card]
  implicit val cardDecoder: Decoder[Card] = deriveDecoder[Card]


  /// Takes the path to the file containing the card's configuration file,
  // builds the card based on that, then returns it
  def buildCardFromFile(path: String): Card =
    val fileContent = Try {
      val source = Source.fromFile(path)
      try
        source.mkString("")
      finally
        source.close()
    }

    fileContent match
      case Failure(exception) => throw exception
      case Success(value) =>

        val json = value
        val card = decode[Card](json)

        card match
          case Left(error) => throw error
          case Right(card) => card


  /**
   * The path to the card
   * */

  def saveCard(card: Card, path: String) =

    val file = File(path)
    val fileWriter = Try(FileWriter(file))

    fileWriter match
      case Success(writer) =>
        val json = cardEncoder(card)

        writer.write(json.toString)
        writer.close()
      case Failure(exception) =>
        println(s"Error creating file writer: ${exception.getMessage}")
        throw exception

  // Function for deleting a card from the file system
  def deleteCard(card: Card, path: String) =
    val file = new File(path)
    file.delete()


end CardFileHelper

