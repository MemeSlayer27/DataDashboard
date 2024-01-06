package card

import scala.util.{Failure, Success, Try}
import concurrent.ExecutionContext.Implicits.global

/**
 * Goes like this: circe can decode this from json. All fields are simple and atomic except for graph
 * Graph is also decoded by circe. Cards are immutable for simplicity's sake and it makes working with
 * circe a whole lot easier. When something gets changed, a new card gets constructed,
 * and the config file rewritten.
 * The dataSource can either be a file path or an URL
 *
 * The savelocation should just be named with id. That way, name and filename won't be tied up
 * and it's easier to find the file and move them between different dashboards
 *
 * */
case class Card(
                 val id: Int,
                 val title: String,
                 val x: Double,
                 val y: Double,
                 val width: Double,
                 val height: Double,
                 val bgColor: String,
                 val dataSource: Vector[String],
                 val xRange: Option[ValueRange] = None,
                 val yRange: Option[ValueRange] = None,
                 val live: Boolean,
                 val chart: Chart
               ):

  // Stores a dataHandler for each datasource. Usually there will be only one, but with plot, there can be many
  private val dataHandlers =
    for source <- dataSource yield
      DataHandler(source, live)

  def timeSeriesPlotLiveUpdate(uiObjectUpdateCallback: (Double, String) => Unit) =
    val newDataPoint =
      dataHandlers.foreach(
        dh =>
          dh.getJSONFromAPI().onComplete {
            case Success(Right(json)) =>

              val index = dataHandlers.indexOf(dh)

              val (fieldName, seriesName) =
                this.chart match
                  case TimeSeriesPlot(fields, name) => (fields(index), name(index))
                  case _ => throw Exception("susss")


              // what if there are multiple fields
              val jsonAsMap = dh.getAtomicFieldValues(json)
              val valueY = jsonAsMap.getOrElse(fieldName, 0).toString.toDouble

              yRange match
                case Some(range) =>
                  if range.inRange(valueY) then
                    uiObjectUpdateCallback(valueY, seriesName)
                case None =>
                  uiObjectUpdateCallback(valueY, seriesName)

            case Success(Left(error)) => None
            case Failure(_) => None
          }
      )

  // Funtion for updating the data of a plot. The callback is used to update the UI
  def plotLiveUpdate(uiObjectUpdataCallback: ((Double, Double), String) => Unit) =
    val newDataPoint =

      dataHandlers.foreach(
        dh =>
          dh.getJSONFromAPI().onComplete {
            case Success(Right(json)) =>

              val index = dataHandlers.indexOf(dh)

              val (fieldNames, seriesName) =
                this.chart match
                  case Plot(fields, name) => (fields(index), name(index))
                  case _ => throw Exception("susss")

              // extract the values from the json
              val jsonAsMap = dh.getAtomicFieldValues(json)

              val valueX = jsonAsMap.getOrElse(fieldNames._1, 0).toString.toDouble
              val valueY = jsonAsMap.getOrElse(fieldNames._2, 0).toString.toDouble
              
              // Filter out data that is out of the provided range
              (xRange, yRange) match
                case (Some(xRange), Some(yRange)) =>
                  if xRange.inRange(valueX) && yRange.inRange(valueY) then
                    uiObjectUpdataCallback((valueX, valueY), seriesName)
                case (Some(xRange), None) =>
                  if xRange.inRange(valueX) then
                    uiObjectUpdataCallback((valueX, valueY), seriesName)
                case (None, Some(yRange)) =>
                  if yRange.inRange(valueY) then
                    uiObjectUpdataCallback((valueX, valueY), seriesName)
                case (None, None) =>
                  uiObjectUpdataCallback((valueX, valueY), seriesName)

            case Success(Left(error)) => None
            case Failure(_) => None
          }
      )


  // Get the data from the API and update the datahandlers. Return options of JSONs for each.
  // These should be in the same order as the datasources
  def nonPlotLiveUpdate(uiObjectUpdateCallback: ((String, Int)) => Unit) = // This should be
    val dh = dataHandlers.headOption match
      case Some(dh) => dh
      case None => throw Exception("No data source")
    val newDataPoint =
      dh.getJSONFromAPI().onComplete {
        case Success(Right(json)) =>

          val fieldName =
            this.chart match
              case PieChartCategoric(field) => field
              case BarChartCategoric(field) => field
              case BarChartNumeric(_, field) => field
              case BigNumber(field) => field
              case _ => throw Exception("susss")


          // what if there are multiple fields

          val jsonAsMap = dh.getAtomicFieldValues(json)
          val value = jsonAsMap.getOrElse(fieldName, "0").toString
          
          this.chart match
            case BarChartNumeric(_, _) => 
              
              this.xRange match
                case Some(range) =>
                  if range.inRange(value.toDouble) then
                    uiObjectUpdateCallback((value, 1))
                case None =>
                  uiObjectUpdateCallback((value, 1))
            case _ => uiObjectUpdateCallback((value, 1))
            
        case Success(Left(error)) => None
        case Failure(_) => None
      }

  def addNewSeriesToPlot(path: String, seriesName: String, fieldNames: (String, String), dataSource: String) =
    val newCard = this.copy(
      dataSource = this.dataSource :+ dataSource,
      chart = this.chart match
        case Plot(fields, names) => Plot(fields :+ fieldNames, names :+ seriesName)
        case TimeSeriesPlot(fields, names) => TimeSeriesPlot(fields :+ fieldNames._1, names :+ seriesName)
        case _ => throw Exception("susss")
    )

    val fileHandler = CardFileHelper

    fileHandler.saveCard(newCard, path) // save the new card to the file. using filehandler


    newCard // returning newCard so that it can be saved into the uiCardObject that calls this function

  end addNewSeriesToPlot


  // Load data for plots. This and loadNonPlotData because of different different required returnvalues
  def loadPlotData: Map[String, Seq[(Double, Double)]] = 
    this.chart match
      case Plot(fields, names) =>
        val nameZipFields = names zip fields
        // zip the names and fields together, then zip them with the datahandlers, then map them to the data provided by the datahandler
        val data = (nameZipFields zip dataHandlers).map((nzfs, dh) => nzfs._1 -> dh.getPlotDataFromCSV(nzfs._2))
        
        // Filter out data that is out of the provided range
        (xRange, yRange) match
          case (Some(xRange), Some(yRange)) =>
            data.map((name, data) => name -> data.filter((x, y) => xRange.inRange(x) && yRange.inRange(y))).toMap
          case (Some(xRange), None) =>
            data.map((name, data) => name -> data.filter((x, y) => xRange.inRange(x))).toMap
          case (None, Some(yRange)) =>
            data.map((name, data) => name -> data.filter((x, y) => yRange.inRange(y))).toMap
          case (None, None) => data.toMap
          
      case _ => Map[String, Seq[(Double, Double)]]()


  def loadNonPlotData: Seq[(String, Int)] =
    this.chart match
      case BarChartNumeric(intervals, field) =>
        // 5. get occurrences per interval 6. name the intervals 7. zip these
        val data = dataHandlers.headOption match
          case Some(dh) => dh.getAllDataFromCSVColumn(field)
          case None => throw Exception("No data source")

        // make sure that the data is actually numeric
        val numericData = data.map(x => Try(x.toDouble) match
          case Success(d) => d
          case Failure(_) => throw Exception("Data is not numeric")
        )

        //prune out data out of range
        val prunedData =
          numericData.filter(
            x => xRange match
              case Some(range) => range.inRange(x)
              case None => true
          )

        // figure out max & min
        val max = prunedData.max
        val min = prunedData.min

        // Divide it into n intervals, get interval size
        val intervalSize = (max - min) / intervals

        // Get occurrences per interval
        val occurrences = prunedData.groupBy(x => (x - min) / intervalSize).view.mapValues(_.length).toSeq

        // Sort occurrences by interval start value
        val sortedOccurrences = occurrences.sortBy(_._1)

        // Name and format the intervals
        val namedIntervals = sortedOccurrences.map { case (intervalStart, count) =>
          val start = min + intervalStart * intervalSize
          val end = min + (intervalStart + 1) * intervalSize
          val intervalName = f"$start%1.2f - $end%1.2f" // You can adjust the format here
          intervalName -> count
        }

        namedIntervals

      case BarChartCategoric(field) =>
        // get all data from the field
        val data = dataHandlers.headOption match
          case Some(dh) => dh.getAllDataFromCSVColumn(field)
          case None => throw Exception("No data source")

        // Get occurences of each category
        data.groupBy(x => x).view.mapValues(_.length).toSeq

      case PieChartCategoric(field) =>
        val data = dataHandlers.headOption match
          case Some(dh) => dh.getAllDataFromCSVColumn(field)
          case None => throw Exception("no data source")

        data.groupBy(x => x).view.mapValues(_.length).toSeq

end Card
