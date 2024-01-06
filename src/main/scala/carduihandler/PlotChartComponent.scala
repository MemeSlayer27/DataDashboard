package carduihandler

import canvas.CanvasHandler
import card.{Card, Plot, TimeSeriesPlot}
import javafx.application.Platform
import javafx.scene.control.ColorPicker
import javafx.scene.input.MouseButton
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.chart.{LineChart, NumberAxis, XYChart}
import scalafx.util.StringConverter
import scalafx.Includes.eventClosureWrapperWithZeroParam
import scalafx.Includes.jfxMouseEvent2sfx
import scalafx.Includes.eventClosureWrapperWithParam
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


class PlotChartComponent(initialCard: Card, canvas: CanvasHandler, path: String) extends CardUIHandler(initialCard, canvas, path) with CommonPlotFunctionality:

  override def getChartComponent: Node =
    val card = getCurrentCard
    val bgColor = card.bgColor

    val (fields, names) = card.chart match
      case c: Plot => (c.fields, c.names)
      case _ => throw new Exception("No chart data found")


    val xAxis = new NumberAxis{
          label = fields(0)._1
        }
        val yAxis = new NumberAxis{
          label = fields(0)._2
        }


    val allSeries =

      if card.live then
        for seriesName <- names yield
           new XYChart.Series[Number, Number] {
            name = seriesName
            data = Seq[javafx.scene.chart.XYChart.Data[Number, Number]]()
          }.delegate
      else
        val allRawSeries = card.loadPlotData

        for (seriesName, seriesData) <- allRawSeries yield
          new XYChart.Series[Number, Number] {
            name = seriesName
            data = seriesData.map {
              case (x, y) => XYChart.Data[Number, Number](x, y)
            }
          }.delegate

    val chart = new LineChart[Number, Number](xAxis, yAxis) {
      title = card.title
      data = allSeries.toSeq
      style = s"-fx-background-radius: 20px; -fx-background-color: $bgColor;"
    }

    // Add context menu to each series
    for (series <- chart.data.get()) {
      series.getNode.setOnMouseClicked((e: MouseEvent) => {
        if (e.getButton != MouseButton.SECONDARY) {
          val colorPicker = new ColorPicker()
            colorPicker.setOnAction(() => {
              val hexColor = colorToHex(colorPicker.value())
              series.getNode.setStyle(s"-fx-stroke: $hexColor")
              series.getData.forEach(data => data.getNode.setStyle(s"-fx-background-color: $hexColor, white;"))
              findLegendItem(chart, series.getName()) match
                case Some(legendItem) => legendItem.lookup(".chart-legend-item-symbol").setStyle(s"-fx-background-color: ${hexColor};")
                case None => println("Legend item not found")
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

      // Add context menu to each data point
      for (data <- series.getData()) {
        data.getNode().setOnMouseClicked((e: MouseEvent) => {
          if (e.getButton() != MouseButton.SECONDARY) {
            addDeleteContextMenu(data.getNode, series, data)
          }
        })
      }

      // Add color change listeners to each series
      addSeriesColorChangeListener(chart, series)
    }
    chart
  end getChartComponent


  def addDataToPlotChart(dataPoint: (Double, Double), seriesName: String): Unit =
    Platform.runLater {() =>
      getCurrentChart match {
        case lineChart: LineChart[Number, Number] =>
          lineChart.data.get().find(_.getName == seriesName) match {
            case Some(series) =>
              val newDataPoint = XYChart.Data[Number, Number](dataPoint._1, dataPoint._2)
              series.getData.add(newDataPoint)
              addDeleteContextMenu(newDataPoint.getNode, series, newDataPoint)
            case None => println("Series not found")
          }
        case _ => println("Unsupported chart type")
      }
    }
  end addDataToPlotChart


  def addNewSeriesPlotChart(updatedCard: Card, seriesName: String, color: String): Unit =
    Platform.runLater {() =>
      getCurrentChart match
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
  end addNewSeriesPlotChart






end PlotChartComponent

