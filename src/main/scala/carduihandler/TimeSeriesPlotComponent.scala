package carduihandler

import canvas.CanvasHandler
import card.{Card, TimeSeriesPlot}
import javafx.application.Platform
import javafx.scene.control.ColorPicker
import javafx.scene.input.MouseButton
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.chart.{LineChart, NumberAxis, PieChart, XYChart}
import scalafx.util.StringConverter
import scalafx.Includes.jfxXYChartData2sfx

import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.Includes.jfxXYChartSeries2sfx
import scalafx.scene.control.{ContextMenu, MenuItem}
import scalafx.scene.input.MouseEvent
import scalafx.Includes.jfxNode2sfx
import scalafx.Includes.jfxColorPicker2sfx
import scalafx.Includes.eventClosureWrapperWithZeroParam
import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxMouseEvent2sfx



class TimeSeriesPlotComponent(initialCard: Card, canvas: CanvasHandler, path: String) extends CardUIHandler(initialCard, canvas, path) with CommonPlotFunctionality:

  override def getChartComponent: Node =

    val card = getCurrentCard
    val bgColor = card.bgColor

    val (fields, names) = card.chart match
      case c: TimeSeriesPlot => (c.fields, c.names)
      case _ => throw new Exception("No chart data found")

    val xAxis = new NumberAxis {
      label = "Time"
      autoRanging = false
    }

    var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    def updateFormatter(minX: Double, maxX: Double): Unit = {
      val minDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(minX.toLong), ZoneId.systemDefault())
      val maxDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(maxX.toLong), ZoneId.systemDefault())

      if (ChronoUnit.DAYS.between(minDateTime.toLocalDate, maxDateTime.toLocalDate) >= 1) {
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      } else {
        formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
      }
    }

    xAxis.tickLabelFormatter = new StringConverter[Number] {
      def toString(number: Number): String = {
        val instant = Instant.ofEpochMilli(number.longValue)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        localDateTime.format(formatter)
      }

      def fromString(string: String): Number = {
        throw new UnsupportedOperationException("Cannot parse date/time strings.")
      }
    }

    val yAxis = new NumberAxis {
      label = fields.head
    }

    val dataSeries =
      (fields zip names)
        .map(
          (f, n) =>
            javafx.scene.chart.XYChart.Series[Number, Number](
              n,
              ObservableBuffer[javafx.scene.chart.XYChart.Data[Number, Number]]()
            )
        )



    def updateAxisBounds(series: javafx.scene.chart.XYChart.Series[Number, Number]): Unit =
      val data = series.getData
      if (data.nonEmpty) {

        val minX = data.map(_.getXValue.doubleValue()).min
        val maxX = data.map(_.getXValue.doubleValue()).max

        updateFormatter(minX, maxX)

        xAxis.setLowerBound(minX)
        xAxis.setUpperBound(maxX)
        xAxis.setTickUnit((maxX - minX) / 10)
      }

    dataSeries.head.getData.onChange(updateAxisBounds(dataSeries.head))

    updateAxisBounds(dataSeries.head)

    val chart = new LineChart[Number, Number](xAxis, yAxis) {
      title = card.title
      data = dataSeries.map( _.delegate )
      style = s"-fx-background-radius: 20px; -fx-background-color: $bgColor;"
    }

    // Add color change listeners to each series
    dataSeries.foreach(addSeriesColorChangeListener(chart, _))

    chart


  end getChartComponent


  def addDataToTimeSeriesChart(dataPoint: Double, seriesName: String): Unit =
    Platform.runLater(() => {
      getCurrentChart match {
        case lineChart: LineChart[Number, Number] =>
          lineChart.data.get().find(_.getName == seriesName) match {
            case Some(series) =>
              val newDataPoint = XYChart.Data[Number, Number](Instant.now().toEpochMilli, dataPoint)
              series.getData.add(newDataPoint)

              newDataPoint.nodeProperty.addListener((_, _, newNode) => {
                if (newNode != null) {
                  val seriesNodeStyle = series.getNode.lookup(".chart-series-line").getStyle
                  val seriesColor = if (seriesNodeStyle.contains("-fx-stroke:")) seriesNodeStyle.split("-fx-stroke:")(1).trim() else ""
                  if (seriesColor.nonEmpty) newDataPoint.getNode.setStyle(s"-fx-background-color: $seriesColor, white;")
                  addDeleteContextMenu(newNode, series, newDataPoint)
                }
              })

              // Check if the node has already been created and style it accordingly
              if (newDataPoint.getNode != null) then
                val seriesNodeStyle = series.getNode.lookup(".chart-series-line").getStyle
                val seriesColor = if (seriesNodeStyle.contains("-fx-stroke:")) seriesNodeStyle.split("-fx-stroke:")(1).trim() else ""
                newDataPoint.getNode.setStyle(s"-fx-background-color: $seriesColor, white;")
                addDeleteContextMenu(newDataPoint.getNode, series, newDataPoint)

              if (series.getData.size == 1) then
                series.getNode.setOnMouseClicked((e: MouseEvent) => {
                  if (e.getButton != MouseButton.SECONDARY) {
                    val colorPicker = new ColorPicker()
                    colorPicker.setOnAction(() => {
                      val hexColor = colorToHex(colorPicker.value())
                      series.getNode.setStyle(s"-fx-stroke: $hexColor")
                      series.getData.forEach(data => data.getNode.setStyle(s"-fx-background-color: $hexColor, white;"))
                      findLegendItem(lineChart, series.getName).foreach { legendItem =>
                        legendItem.lookup(".chart-legend-item-symbol").setStyle(s"-fx-background-color: ${hexColor};")
                      }

                    })

                    val contextMenu = new ContextMenu {
                      items ++= Seq(
                        new MenuItem(s"Series: ${series.getName}") {
                          disable = true
                        },
                        new MenuItem("Change color") {
                          graphic = colorPicker
                        }
                      )
                    }
                    contextMenu.show(series.getNode.scene().getWindow, e.getScreenX, e.getScreenY)
                  }
                })


            case None => println("Series not found") // this should never happen as the series should be created when the chart is created
          }
        case _ => println("Unsupported chart type")
      }
    })

  end addDataToTimeSeriesChart

  
  def addNewSeriesPlotChart(updatedCard: Card, seriesName: String, color: String): Unit =
    Platform.runLater {() =>
      getCurrentChart match {
        case lineChart: LineChart[Number, Number] =>
          val newSeriesData =
            if updatedCard.live then
              new ObservableBuffer[javafx.scene.chart.XYChart.Data[Number, Number]]()
            else
              ObservableBuffer.from(
                updatedCard.loadPlotData(seriesName)
                  .map( data => XYChart.Data[Number, Number](data._1, data._2) )
              )


          val newSeries = XYChart.Series[Number, Number](seriesName, newSeriesData)

          // Add the series to the chart before setting the style
          lineChart.data.get().add(newSeries)

          // Now set the style since the node has been created
          newSeries.getNode.setStyle(s"-fx-stroke: ${color};")

          changeCurrentCard(updatedCard)

        case _ => println("Unsupported chart type")
      }
    }
  end addNewSeriesPlotChart
  



end TimeSeriesPlotComponent

