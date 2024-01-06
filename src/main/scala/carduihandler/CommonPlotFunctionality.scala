package carduihandler

import card.Card
import javafx.application.Platform
import javafx.scene.input.MouseButton
import scalafx.scene.Node
import scalafx.scene.chart.{LineChart, XYChart}
import scalafx.scene.control.{ContextMenu, MenuItem}
import scalafx.scene.input.MouseEvent
import scalafx.Includes.{eventClosureWrapperWithParam, eventClosureWrapperWithZeroParam, jfxMouseEvent2sfx}
import scalafx.collections.ObservableBuffer

trait CommonPlotFunctionality:
  // Function for adding a deletioncontextmenu to newly created plot datapoints
  def addDeleteContextMenu(node: Node, series: XYChart.Series[Number, Number], data: XYChart.Data[Number, Number]): Unit = {
    node.setOnMouseClicked((e: MouseEvent) => {
      if (e.getButton != MouseButton.SECONDARY) {
        val contextMenu = new ContextMenu {
          items += new MenuItem("X: " + data.XValue.value.toString + ", Y: " + data.YValue.value.toString) {
            disable = true
          }
          items += new MenuItem("Delete") {
            onAction = () => {
              series.getData.remove(data)
            }
          }
        }
        contextMenu.show(node.getScene.getWindow, e.getScreenX, e.getScreenY)
      }
    })
  }

  def findLegendItem(chart: LineChart[Number, Number], dataName: String): Option[javafx.scene.Node] = {
    chart.lookupAll(".chart-legend-item").find { node =>
      node.lookup(".label").asInstanceOf[javafx.scene.control.Label].getText == dataName
    }
  }


  def updateLegendColor(chart: LineChart[Number, Number], seriesName: String, color: String): Unit = {
    findLegendItem(chart, seriesName).foreach { legendItem =>
      legendItem.lookup(".chart-legend-item-symbol").setStyle(s"-fx-background-color: $color, white;")
    }
  }


  def addSeriesColorChangeListener(chart: LineChart[Number, Number], series: XYChart.Series[Number, Number]): Unit = {
    series.nodeProperty().addListener { (_, _, newNode) =>
      if (newNode != null) {
        newNode.lookup(".chart-series-line").styleProperty().addListener { (_, _, newStyle) =>
          if (newStyle.contains("-fx-stroke:")) {
            val hexColor = newStyle.split("-fx-stroke:")(1).trim()
            if (hexColor.nonEmpty) updateLegendColor(chart, series.getName, hexColor)
          }
        }
      }
    }
  }

end CommonPlotFunctionality

