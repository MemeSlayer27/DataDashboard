package carduihandler

import canvas.CanvasHandler
import card.Card
import javafx.scene.control.ColorPicker
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.chart.{BarChart, CategoryAxis, NumberAxis, XYChart}
import scalafx.scene.control.{ContextMenu, MenuItem}
import scalafx.scene.input.MouseEvent
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.Includes.jfxColorPicker2sfx
import scalafx.Includes.eventClosureWrapperWithZeroParam
import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.Includes.jfxMouseEvent2sfx
import scalafx.Includes.jfxNode2sfx

// TODO: Drop this class. too much of a hassle to implement live updates for histograms
/*
class HistogramComponent(initialCard: Card, canvas: CanvasHandler, path: String) extends CardUIHandler(initialCard, canvas, path):

  override def getChartComponent: Node = 

    val card = getCurrentCard

    val bgColor = card.bgColor

    val xAxis = new CategoryAxis()
    val yAxis = new NumberAxis()

    // if live, then we don't want to load any data
    val barChartData =
      if card.live then
        Seq[javafx.scene.chart.XYChart.Data[String, Number]]()
      else
        card.loadNonPlotData.map(x => XYChart.Data[String, Number](x._1, x._2)) // load the data from the card

    val bcdObservableBuffer = ObservableBuffer.from(barChartData)


    val series = new XYChart.Series[String, Number] {
      name = "Data"
      data = bcdObservableBuffer
    }

    val barChart = new BarChart[String, Number](xAxis, yAxis) {
      title = card.title
      data = series
      barGap = -5
      legendVisible = false
      style = s"-fx-background-radius: 20px; -fx-background-color: $bgColor;"
    }

    // Add the context menu to the bars
    barChart.getData.headOption.foreach { dataSeries =>
      dataSeries.getData.foreach { barData =>
        val node = barData.getNode
        val dataName = barData.getXValue
        val dataValue = barData.getYValue.doubleValue

        addContextMenu(node, barChart, barData, updateColor = newColor => {
          dataSeries.getData.foreach { innerBarData =>
            innerBarData.getNode.setStyle(s"-fx-bar-fill: $newColor;")
          }
        }
       )
      }
    }


    barChart

  end getChartComponent

  // TODO: this feature is removed. No live updates for histograms. Too much of a hassle
  def addDataPoint(dataPoint: (String, Int)) =
    // Try to find an existing data point with the same name
    val barChart = getCurrentChart match
      case bc: BarChart[String, Number] => bc
      case _ => throw new Exception("Chart is not a BarChart")

    barChart.data.get().head.getData.find(dp => dp.getXValue == dataPoint._1) match {
      // If the data point already exists, update its value
      case Some(data) => data.setYValue(data.getYValue.intValue() + dataPoint._2)
      // If the data point doesn't exist, create and add it to the chart
      case None =>
        val newDataPoint = new javafx.scene.chart.XYChart.Data[String, Number](dataPoint._1, Integer.valueOf(dataPoint._2)) // Create the javafx object directly

        barChart.data.get().head.getData.add(newDataPoint)
        // Add the context menu to the new data point
        addContextMenu( //TODO: needs to add a different context menu. Colorchange needs to change every in the series
          newDataPoint.getNode,
          barChart,
          newDataPoint,
          hexColor => {
            newDataPoint.getNode.setStyle(s"-fx-background-color: ${hexColor};") // Set the color for the bar
          }
        )
    }

  end addDataPoint


  // Adds a context menu to the node
  def addContextMenu(node: Node, chart: scalafx.scene.chart.Chart, dataPoint: javafx.scene.chart.XYChart.Data[String, Number], updateColor: String => Unit): Unit =
    node.setOnMouseClicked((e: MouseEvent) => {
      if (e.getButton() != MouseButton.SECONDARY) then
        val colorString =
          val style = node.getStyle
          if (style.contains("-fx-pie-color")) then
            style.split("-fx-pie-color:")(1).split(";")(0).trim()
          else
            "white"


        val colorPicker = new ColorPicker(Color.web(colorString))
        colorPicker.setOnAction(() => {
          val hexColor = colorToHex(colorPicker.value())
          updateColor(hexColor)
        })

        val contextMenu = new ContextMenu {
          items ++= Seq(
            new MenuItem(s"Name: ${dataPoint.getXValue}") {
              disable = true
            },
            new MenuItem(s"Value: ${dataPoint.getYValue}") {
              disable = true
            },
            new MenuItem("Change color") {
              graphic = colorPicker
            },
          )
        }
        contextMenu.show(node.scene().getWindow, e.getScreenX, e.getScreenY)
    })
  end addContextMenu


end HistogramComponent
*/