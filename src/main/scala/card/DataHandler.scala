package card

import io.circe.parser.*
import io.circe.{Error, Json, *}
import requests.*

import java.io.File
import scala.collection.mutable.Buffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}

/** card.DataHandler
 * Takes in an url, if the url is a path to some local file, fetches data from there. If live
 * fetches some json from the interwebs.
 *
 * */

class DataHandler(url: String, val isLive: Boolean):


  private def tryParseNumeric(json: Json): Option[Any] = {
    json.asNumber.flatMap { num =>
      num.toInt.orElse(num.toLong).orElse(Some(num.toDouble))
    }
  }


  // Recursive function that returns a map with keys representing atomic field names
  // and values representing the actual values of the fields
  def getAtomicFieldValues(json: Json, parentKey: Option[String] = None, separator: String = "."): Map[String, Any] = {
    json.asObject match {
      case Some(obj) =>
        obj.toVector.flatMap {
          case (key, value) =>
            // add the key to the parent key, if it exists
            val fullKey = parentKey.map(_ + separator + key).getOrElse(key)

            value.asObject match
              case Some(_) => getAtomicFieldValues(value, Some(fullKey), separator)
              case None if value.isArray =>
                value.asArray.get.zipWithIndex.flatMap {
                  case (arrayElem, index) =>
                    getAtomicFieldValues(arrayElem, Some(fullKey + s"[$index]"), separator)
                }
              case _ =>
                val actualValue = tryParseNumeric(value)
                  .orElse(value.asString.map(s => s))
                  .getOrElse(value)



                Map(fullKey -> actualValue)

        }.toMap
      case None => Map.empty
    }
  }

  // this should maybe take a function as an argument, that is executed when the future is complete
  def getLiveDataStructure(onCompleteCallback: Vector[(String, String)] => Unit, onFailureCallback: () => Unit): Unit =
    if this.isLive then
      this.getJSONFromAPI().onComplete {

        case Success(Right(json)) =>
          val jsonAsMap = getAtomicFieldValues(json)
          val fields = jsonAsMap.keys.toVector
          val types =
            fields.map(
              field =>
                jsonAsMap(field) match {
                  case _: Int | _: Long | _: Double => "numeric"
                  case _ => "categoric"
                }
            )

          val fieldsAndTypes = (fields zip types).toVector

          onCompleteCallback(fieldsAndTypes)

        case Success(Left(error)) =>
          onFailureCallback()
        case Failure(_) => onFailureCallback()
      }
    else // If the datahandler is not live, throw an exception
      throw new Exception("card.DataHandler is not live")

  // Returns the names and corresponding types of all fields with atomic data-types
  // If current source is not functional, returns None
  def getLocalDataStructure: Option[Vector[(String, String)]] =
    if this.isLive then
      None
    else
      val data = this.loadLocalData()
      val fieldNames = data.split("\n")(0).split(",")
      val types = data.split("\n")(1).split(",").map( field => if Try(field.toDouble).isSuccess then "numeric" else "categoric")

      val fields = (fieldNames zip types).toVector
      Some(fields)

  def loadLocalData() =
    val fileContent = Try {
      val source = Source.fromFile(this.url)
      try
          source.mkString("")
      finally
          source.close()
    }
    fileContent match
      case Success(content) => content
      case Failure(exception) => throw exception

  def getAllDataFromCSVColumn(fieldName: String) =
    
    val data = this.loadLocalData()

    // Get the index of the field
    val tuples = data.split("\n")
    val index = tuples(0).split(",").indexOf(fieldName)

    // If field is not found, throw exception
    index match
      case -1 => throw new Exception("Field not found")
      case _ =>

        tuples.tail.map( t => t.split(",")(index) ).toSeq

  def getPlotDataFromCSV(fields: (String, String)): Seq[(Double,Double)] =

    val data = this.loadLocalData()

    val tuples = data.split("\n")
    val indexX = tuples(0).split(",").indexOf(fields._1)
    val indexY = tuples(0).split(",").indexOf(fields._2)
    
    
    // Ineed to make sure that the data is numeric or a date
    val xData = tuples.tail.map( t => t.split(",")(indexX) ).toSeq
    val yData = tuples.tail.map( t => t.split(",")(indexY) ).toSeq
    
    
    try 
      val xDataNumeric = xData.map( x => x.toDouble )
      val yDataNumeric = yData.map( y => y.toDouble )
      xDataNumeric zip yDataNumeric
    catch
      case e: Exception => throw e


  private def isValidUrl(url: String): Boolean = {
    Try(new java.net.URL(url)).isSuccess
  }

  /** API response model
   * 1. Canvashandler calls liveUpdate on all livecards when it is time to update
   * 2. each card calls getJSONFromAPI on its datahandler(s)
   * 3. updateData returns the futureresponse to canvas.CanvasHandler
   * 4. canvas.CanvasHandler calls .onComplete on the future response,
   *    then updates the card with data from the json when complete
   *
   *
   * */
  def getJSONFromAPI()(implicit ec: ExecutionContext): Future[Either[Error, Json]] =
    if (isValidUrl(url)) then
      Future {
        val response = requests.get(url)
        parse(response.text())
      }
    else
      Future.successful(Left(ParsingFailure("Invalid URL", null)))

end DataHandler
