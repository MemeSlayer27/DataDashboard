package carduihandler

import canvas.CanvasHandler
import card.Card
import javafx.application.Platform
import javafx.scene.control.ColorPicker
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.chart.PieChart
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.Includes.jfxNode2sfx
import scalafx.scene.control.{ContextMenu, MenuItem}
import scalafx.scene.input.MouseEvent
import scalafx.Includes.jfxColorPicker2sfx
import scalafx.Includes.eventClosureWrapperWithZeroParam
import scalafx.Includes.jfxMouseEvent2sfx
import scalafx.Includes.eventClosureWrapperWithParam

class PieChartComponent(initialCard: Card, canvas: CanvasHandler, path: String) extends CardUIHandler(initialCard, canvas, path):

  override def getChartComponent: Node =
    val card = getCurrentCard
    
    val bgColor = card.bgColor
    // if live, then we don't want to load any data
    val pieChartData =
      if card.live then Seq[javafx.scene.chart.PieChart.Data]()
      else card.loadNonPlotData.map(x => PieChart.Data(x._1, x._2))
    val pcdObservableBuffer = ObservableBuffer.from(pieChartData)

    val pieChart = new PieChart {
      data = pieChartData
      title = card.title
      style = s"-fx-background-radius: 20px; -fx-background-color: $bgColor;"
    }

    for (dp <- pieChart.data.get()) {
      addContextMenu(
        dp.getNode,
        pieChart,
        dp,
        hexColor => {
          dp.getNode.setStyle(s"-fx-pie-color: ${hexColor}")
        },
        () => {
          pieChart.data.get().remove(dp)
        },
        hexColor => {
          findLegendItem(pieChart, dp.getName).foreach { legendItem =>
            legendItem.lookup(".chart-legend-item-symbol").setStyle(s"-fx-background-color: ${hexColor};")
          }
        }
      )
    }

    pieChart

  end getChartComponent
  
  def findLegendItem(chart: PieChart, dataName: String): Option[javafx.scene.Node] = {
    chart.lookupAll(".chart-legend-item").find { node =>
      node.lookup(".label").asInstanceOf[javafx.scene.control.Label].getText == dataName
    }
  }

      // Adds a context menu to the node
  def addContextMenu(node: Node, chart: scalafx.scene.chart.Chart, dataPoint: javafx.scene.chart.PieChart.Data, updateColor: String => Unit, deleteData: () => Unit, updateLegendColor: String => Unit): Unit =
    node.setOnMouseClicked((e: MouseEvent) => {
      if (e.getButton() != MouseButton.SECONDARY) {
        val colorString =
          val style = node.getStyle
          if (style.contains("-fx-pie-color")) {
            style.split("-fx-pie-color:")(1).split(";")(0).trim()
          } else {
            "white"
          }


        val colorPicker = new ColorPicker(Color.web(colorString))
        colorPicker.setOnAction(() => {
          val hexColor = colorToHex(colorPicker.value())
          updateColor(hexColor)
          updateLegendColor(hexColor)
        })

        val contextMenu = new ContextMenu {
          items ++= Seq(
            new MenuItem(s"Name: ${dataPoint.getName}") {
              disable = true
            },
            new MenuItem(s"Value: ${dataPoint.getPieValue}") {
              disable = true
            },
            new MenuItem("Change color") {
              graphic = colorPicker
            },
            new MenuItem("Delete") {
              onAction = () => {
                deleteData()
              }
            }
          )
        }
        contextMenu.show(node.scene().getWindow, e.getScreenX, e.getScreenY)
      }
    })
  end addContextMenu
  
  def addDataPoint(dataPoint: (String, Int)) =
    Platform.runLater(() => {
      val pieChart = getCurrentChart.asInstanceOf[scalafx.scene.chart.PieChart]
      // Try to find an existing data point with the same name
      pieChart.data.get().find(dp => dp.getName == dataPoint._1) match {
        // If the data point already exists, update its value
        case Some(data) => data.setPieValue(data.getPieValue + dataPoint._2)
        // If the data point doesn't exist, create and add it to the chart
        case None =>
          val newDataPoint = PieChart.Data(dataPoint._1, dataPoint._2)
          pieChart.data.get().add(newDataPoint)

          // Add the context menu to the new data point
          addContextMenu(newDataPoint.getNode, pieChart, newDataPoint, hexColor => {
            newDataPoint.getNode.setStyle(s"-fx-pie-color: ${hexColor}")
          }, () => {
            pieChart.data.get().remove(newDataPoint)
          }, hexColor => {
          findLegendItem(pieChart, newDataPoint.getName).foreach { legendItem =>
            legendItem.lookup(".chart-legend-item-symbol").setStyle(s"-fx-background-color: ${hexColor};")
          }
        })
      }
    })

  end addDataPoint
  
end PieChartComponent



