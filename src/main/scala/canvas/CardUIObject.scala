
/*
package canvas

import card.Card.*
import card.*
import javafx.application.Platform
import javafx.scene.control.ColorPicker
import javafx.scene.input.{ContextMenuEvent, MouseButton}
import javafx.scene.paint.Color
import javafx.scene.shape.{Path, Shape}
import scalafx.Includes.{eventClosureWrapperWithParam, eventClosureWrapperWithZeroParam, jfxColor2sfx, jfxColorPicker2sfx, jfxMouseEvent2sfx, jfxNode2sfx, jfxShape2sfx, jfxXYChartData2sfx, jfxXYChartSeries2sfx, observableList2ObservableBuffer}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.SceneIncludes.jfxScene2sfx
import scalafx.scene.chart.XYChart.Data.sfxXYChartData2jfx
import scalafx.scene.chart.{Chart, *}
import scalafx.scene.control.{Button, ContextMenu, Label, MenuItem}
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.Priority.Always
import scalafx.scene.layout.{BorderPane, HBox, Priority, StackPane, VBox}
import scalafx.scene.{Node, Scene}
import scalafx.stage.{Modality, Stage}
import scalafx.util.StringConverter

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneId}
import scala.math.Fractional.Implicits.infixFractionalOps
import scala.math.Integral.Implicits.infixIntegralOps
import scala.math.Numeric.Implicits.infixNumericOps



// this is deprecated. It's functionality has been refactored into the carduihandler pacckage class
// Use this as an example of how to write bad code


/**
 * A utility class to encapsulate the creation of card UI components. getUIcomponent takes in a card.Card,
 * the builds the UI component based on it.
 *
 */
class CardUIObject(initialCard: Card, removeCardCallback: (Node, Card) => Unit, changePosOrSizeCallback: (Card, Double,Double,Double,Double) => Card, val path: String):
  
  private var currentCard: Card = initialCard
  private var currentChart = getChartComponent(currentCard)

  private var isSelected = false
  
  def getCurrentCard = currentCard


  // A method that makes the card selected, and updates the UI accordingly (colored boundaries)
  def select = isSelected = true
  def deselect = isSelected = false
  def getIsSelected = isSelected


  /**
   * A method to build the UI component of a card. This is called by the canvas.CanvasHandler class whenever it needs to
   * build a card
   *
   *
   */
  def getCardUIComponent() =
    val stackPane = new StackPane()
    stackPane.translateX = currentCard.x
    stackPane.translateY = currentCard.y
    stackPane.prefWidth  = currentCard.width
    stackPane.prefHeight = currentCard.height

    currentChart = getChartComponent(currentCard)

    val borderPane = new BorderPane()
    borderPane.center = currentChart
    stackPane.children.addAll(borderPane)


    var newOffsetX = 0.0
    var newOffsetY = 0.0

    var cornerPress = false

    stackPane.onMousePressed = (event) => {

      val xCornerBounds =
        stackPane.translateX() + stackPane.width() - 20 < event.getSceneX &&
        event.getSceneX < stackPane.translateX() + stackPane.width() + 20
      val yCornerBounds =
       stackPane.translateY() + stackPane.height() - 20 < event.getSceneY &&
       event.getSceneY < stackPane.translateY() + stackPane.height() + 20

      // If the press is on the corner, then we should resize the card. If not, then the card should be moved-
      cornerPress = xCornerBounds && yCornerBounds

      newOffsetX = event.sceneX - stackPane.translateX()
      newOffsetY = event.sceneY - stackPane.translateY()
    }

    //
    stackPane.onMouseDragged = (event) => {

      if cornerPress then
        stackPane.prefWidth() = event.sceneX - stackPane.translateX()
        stackPane.prefHeight() = event.sceneY - stackPane.translateY()
      else
        stackPane.translateX = event.sceneX - newOffsetX
        stackPane.translateY = event.sceneY - newOffsetY
    }
    
    stackPane.onMouseReleased = (event) => {

      // Saving the new card assigning it to currentcard
      currentCard = changePosOrSizeCallback(currentCard, stackPane.translateX(), stackPane.translateY(), stackPane.width(), stackPane.height())

      cornerPress = false
    }

    // Makes a contextmenu pop-up when a card is right-clicked
    stackPane.onContextMenuRequested = (event: ContextMenuEvent) => {
      if (event.getEventType == ContextMenuEvent.CONTEXT_MENU_REQUESTED) {
        println("Right-click detected!")
        val contextMenu = new ContextMenu {
          currentChart match
            case _: LineChart[Number, Number] =>
             items += new MenuItem("Add new series") {
              onAction = _ => {

                val configWindow: Stage = new Stage {
                  title = "new series"
                  scene = new Scene {
                    content =  new VBox {
                    alignment = Pos.Center
                    children = SeriesAdder(CardUIObject.this, close()).layout
                  }
                  }.delegate

                  initModality(Modality.ApplicationModal)
                }


                configWindow.showAndWait()
              }
            }
            case _ => ()


          // Clicking on statistics opens a new window with the statistics numbers calculated from the cards data
          items += new MenuItem("Statistics") {
            // Creates a window that shows the statistics of the data in the card
            onAction = _ => {
              val statisticalNumbers: Vector[(String, Vector[(String, Double)])] = currentChart match {
                case l: LineChart[Number, Number] => l.data.value.map((series: javafx.scene.chart.XYChart.Series[Number, Number]) => (series.getName, calculateStats(Some(series)))).toVector
                case _ => Vector(("Data Series", calculateStats()))
              }

              val representationColumns: Seq[Node] = statisticalNumbers.map { case (seriesName, numbers) =>
                new VBox {
                  spacing = 10
                  children = Seq(
                    new Label(seriesName) {
                      style = "-fx-font-weight: bold; -fx-font-size: 16;"
                    }
                  ) ++ numbers.map(number => new HBox {
                    spacing = 5
                    children = Seq(
                      new Label(number._1 + ": "),
                      new Label(BigDecimal(number._2).setScale(2, BigDecimal.RoundingMode.HALF_UP).toString)
                    )
                  })
                }
              }

              val exitButton = new Button("Exit")
              val contentHBox = new HBox {
                spacing = 15
                padding = Insets(10, 10, 10, 10)
                alignment = Pos.Center
                children = representationColumns
              }

              val contentVBox = new VBox {
                spacing = 10
                padding = Insets(10, 10, 10, 10)
                alignment = Pos.Center
                children = Seq(contentHBox, exitButton)
              }

              val configWindow: Stage = new Stage {
                title = "Statistics"
                scene = new Scene {
                  content = contentVBox
                }.delegate

                initModality(Modality.ApplicationModal)
              }

              exitButton.onAction = () => configWindow.close()

              configWindow.showAndWait()
            }
          }



          // Add menu items to the context menu
          items += new MenuItem("Configure card") {
            // Creates a configuration window for the card when this option is pressed

            onAction = _ => {
              val configWindow = new Stage {
                title = "Configuration"
                scene = new Scene {
                  content = new VBox {
                    alignment = Pos.Center
                    children = Seq(
                      new Label("Configuration Window"),
                      new Button("Save"),
                      new Button("Cancel")
                    )
                  }
                 }.delegate

                initModality(Modality.ApplicationModal)
              }
              configWindow.showAndWait()
            }
          }
          // This option deletes the card
          items += new MenuItem("Delete card") {
            onAction = () => {
              // Deletes that card from canvas
              removeCardCallback(stackPane, currentCard)
            }
          }
        }
        // Show the context menu at the location of the mouse click
        contextMenu.show(stackPane.scene().getWindow, event.getSceneX,event.getSceneY)
      }
     }

    stackPane


  def calculateStats(seriesOption: Option[XYChart.Series[Number, Number]] = None) =
    val dataValues = this.currentChart match
      case b: BarChart[String, Number] => b.getData().get(0).getData().map(x => x.getYValue().doubleValue())
      case p: PieChart => p.getData().map(x => x.getPieValue())
      case l: LineChart[String, Number] =>
        seriesOption match
          case Some(s) => s.getData().map(x => x.getYValue().doubleValue())
          case None => throw new Exception("No series selected")
      case _ => throw new Exception("This card is not pie or bar chart")

    val dataSum = dataValues.sum
    val dataMean = dataSum / dataValues.length

    val sortedData = dataValues.sorted
    val dataMedian = if (dataValues.length % 2 == 0) then
      // If the list has an even number of elements, take the average of the two middle elements
      val middle1 = sortedData((dataValues.length / 2) + 1)
      val middle2 = sortedData(dataValues.length / 2)
      (middle1 + middle2) / 2.0
    else
      // If the list has an odd number of elements, take the middle element
      sortedData(dataValues.length / 2)

    val dataMode = dataValues.groupBy(identity).maxBy(_._2.size)._1
    val dataMax = dataValues.max
    val dataMin = dataValues.min
    val dataVariance = dataValues.map(x => math.pow(x - dataMean, 2)).sum / dataValues.length
    val dataStdDev = math.sqrt(dataVariance)

    val stats = Vector(
      "Sum" -> dataSum,
      "Mean" -> dataMean,
      "Median" -> dataMedian,
      "Mode" -> dataMode,
      "Max" -> dataMax,
      "Min" -> dataMin,
      "Variance" -> dataVariance,
      "Standard Deviation" -> dataStdDev
    )

    stats

  //TODO: Move the color picker to the right side of the menu if possible

  // Adds a context menu to the node
  private def addContextMenu(node: Node, chart: scalafx.scene.chart.Chart, dataName: String, dataValue: Double, updateColor: String => Unit, deleteData: () => Unit): Unit =
    node.setOnMouseClicked((e: MouseEvent) => {
      if (e.getButton() != MouseButton.SECONDARY) {
        val colorString = if (chart.isInstanceOf[PieChart]) {
          val style = node.getStyle
          if (style.contains("-fx-pie-color")) {
            style.split("-fx-pie-color:")(1).split(";")(0).trim()
          } else {
            "white"
          }
        } else {
          "white" // Default color for BarChart
        }

        val colorPicker = new ColorPicker(Color.web(colorString))
        colorPicker.setOnAction(() => {
          val hexColor = colorToHex(colorPicker.value())
          updateColor(hexColor)
        })

        val contextMenu = new ContextMenu {
          items ++= Seq(
            new MenuItem(s"Name: ${dataName}") {
              disable = true
            },
            new MenuItem(s"Value: ${dataValue}") {
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



  /**
   * A private method to build the chart part of the card. This is called by getCardUIComponent
   *
   * @param card
   * @return
   */
  private def getChartComponent(card: Card): Node =
    val chart = card.chart

    val bgColor = card.bgColor

    chart match
      case Plot(fields, names) =>
        val xAxis = new NumberAxis{
          label = fields(0)._1
        }
        val yAxis = new NumberAxis{
          label = fields(0)._2
        }


        val allSeries =

          if this.currentCard.live then
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
          series.getNode().setOnMouseClicked((e: MouseEvent) => {
            if (e.getButton() != MouseButton.SECONDARY) {
              val colorPicker = new ColorPicker()
                colorPicker.setOnAction(() => {
                  val hexColor = colorToHex(colorPicker.value())
                  series.getNode().setStyle(s"-fx-stroke: $hexColor")
                  series.getData().forEach(data => data.getNode().setStyle(s"-fx-background-color: $hexColor, white;"))
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


        }
        chart

      case TimeSeriesPlot(fields, names) =>
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
          label = fields(0)
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
          val data = series.getData()
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

        chart

      case PieChartCategoric(field) =>

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
            dp.getName,
            dp.getPieValue,
            hexColor => {
              dp.getNode.setStyle(s"-fx-pie-color: ${hexColor}")
            },
            () => {
              pieChart.data.get().remove(dp)
            }
          )
        }

        pieChart

      case BarChartNumeric(intervals, field) =>
        val xAxis = new CategoryAxis()
        val yAxis = new NumberAxis()

        // if live, then we don't want to load any data
        val barChartData =
          if card.live then Seq[javafx.scene.chart.XYChart.Data[String, Number]]()
          else card.loadNonPlotData.map(x => XYChart.Data[String, Number](x._1, x._2))
        val bcdObservableBuffer = ObservableBuffer.from(barChartData)


        val series = new XYChart.Series[String, Number] {
          name = "Data"
          data = bcdObservableBuffer
        }

        val barChart = new BarChart[String, Number](xAxis, yAxis) {
          title = card.title
          data = series
          barGap = -5
          style = s"-fx-background-radius: 20px; -fx-background-color: $bgColor;"
        }


        barChart



      case BarChartCategoric(field) =>
        val xAxis = new CategoryAxis()
        val yAxis = new NumberAxis()

        // if live, then we don't want to load any data
        val barChartData =
          if card.live then Seq[javafx.scene.chart.XYChart.Data[String, Number]]()
          else card.loadNonPlotData.map(x => XYChart.Data[String, Number](x._1, x._2))

        val bcdObservableBuffer = ObservableBuffer.from(barChartData)


        val series = new XYChart.Series[String, Number] {
          name = "Data"
          data = bcdObservableBuffer
        }

        val barChart = new BarChart[String, Number](xAxis, yAxis) {
          title = card.title
          data = series
          style = s"-fx-background-radius: 20px; -fx-background-color: $bgColor;"
        }

        for (series <- barChart.data.get()) {
          for (data <- series.getData()) {
            data.getNode().setOnMouseClicked((e: MouseEvent) => {
              if (e.getButton() != MouseButton.SECONDARY) {
                val colorPicker = new ColorPicker()
                colorPicker.setOnAction(
                  () => {
                    val hexColor = colorToHex(colorPicker.value())
                    data.getNode().setStyle(s"-fx-background-color: $hexColor;")
                  })

                val contextMenu = new ContextMenu {
                  items ++= Seq(
                    new MenuItem(s"Series: ${series.getName}, Name: ${data.getXValue}") {
                      disable = true
                    },
                    new MenuItem("Change color") {
                      graphic = colorPicker
                    },
                    new MenuItem("Remove") {
                      onAction = () => {
                        series.getData().remove(data)
                      }
                    }
                  )
                }
                contextMenu.show(data.getNode.scene().getWindow, e.getScreenX, e.getScreenY)
              }
            })
          }
        }

        barChart
      case BigNumber(field) =>

        new VBox {
          alignment = Pos.Center
          style = s"-fx-background-radius: 20px; -fx-background-color: $bgColor;"
          minWidth = 200
          minHeight = 100
          children = Seq(
            new Label(card.title) {
              style = "-fx-font-size: 18pt; -fx-font-weight: bold;"
            },
            new Label("") {
              style = "-fx-font-size: 32pt;"
            }
          )
        }

  // Takes in a chartcomponent, adds a datapoint to it, and returns the updated chart.
  // Used for liveupdates

  // Adds a new data point to the chart or updates an existing one
  def addDataPointToNonPlotChart(dataPoint: (String, Int)): Unit =
    // Platform.runLater is used to make sure that the UI is updated on the UI thread.
    Platform.runLater(() => {
      this.currentChart match
        // If the current chart is a PieChart
        case pieChart: PieChart =>
          // Try to find an existing data point with the same name
          pieChart.data.get().find(dp => dp.getName == dataPoint._1) match {
            // If the data point already exists, update its value
            case Some(data) => data.setPieValue(data.getPieValue + dataPoint._2)
            // If the data point doesn't exist, create and add it to the chart
            case None =>
              val newDataPoint = PieChart.Data(dataPoint._1, dataPoint._2)
              pieChart.data.get().add(newDataPoint)
              // Add the context menu to the new data point
              addContextMenu(newDataPoint.getNode, pieChart, newDataPoint.getName, newDataPoint.getPieValue, hexColor => {
                newDataPoint.getNode.setStyle(s"-fx-pie-color: ${hexColor}")
              }, () => {
                pieChart.data.get().remove(newDataPoint)
              })
          }

        // If the current chart is a BarChart
        case barChart: BarChart[String, Number] =>
          // Try to find an existing data point with the same name
          barChart.data.get().head.getData.find(dp => dp.getXValue == dataPoint._1) match {
            // If the data point already exists, update its value
            case Some(data) => data.setYValue(data.getYValue.intValue() + dataPoint._2)
            // If the data point doesn't exist, create and add it to the chart
            case None =>
              val newDataPoint = new javafx.scene.chart.XYChart.Data[String, Number](dataPoint._1, Integer.valueOf(dataPoint._2)) // Create the javafx object directly

              barChart.data.get().head.getData.add(newDataPoint)
              // Add the context menu to the new data point
              addContextMenu(
                newDataPoint.getNode,
                barChart,
                newDataPoint.getXValue,
                newDataPoint.getYValue.doubleValue(),
                hexColor => {
                  newDataPoint.getNode.setStyle(s"-fx-background-color: ${hexColor};") // Set the color for the bar
                },
                () => {
                  barChart.data.get().head.getData.remove(newDataPoint)
                }
              )
          }


        // this takes place if chartType is card.BigNumber
        case vbox: VBox =>
          vbox.children.get(1) match
            case label: javafx.scene.control.Label => label.setText(dataPoint._1)
            case _ => throw new Exception("bignum has no label for some reason")
        case _ => println("Unsupported chart type")

    })
  end addDataPointToNonPlotChart

  // Function for adding a deletioncontextmenu to newly created plot datapoints
  private def addDeleteContextMenu(node: Node, series: XYChart.Series[Number, Number], data: XYChart.Data[Number, Number]): Unit = {
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


  def addDataToTimeSeriesChart(dataPoint: Double, seriesName: String): Unit =
    Platform.runLater(() => {
      this.currentChart match {
        case lineChart: LineChart[Number, Number] =>
          lineChart.data.get().find(_.getName == seriesName) match {
            case Some(series) =>
              val newDataPoint = XYChart.Data[Number, Number](Instant.now().toEpochMilli, dataPoint)
              series.getData.add(newDataPoint)
              addDeleteContextMenu(newDataPoint.getNode, series, newDataPoint)

              if series.getData.size == 1 then
                series.getNode().setOnMouseClicked((e: MouseEvent) => {
                if (e.getButton() != MouseButton.SECONDARY) {
                  val colorPicker = new ColorPicker()
                    colorPicker.setOnAction(() => {
                      val hexColor = colorToHex(colorPicker.value())
                      series.getNode().setStyle(s"-fx-stroke: $hexColor")
                      series.getData().forEach(data => data.getNode().setStyle(s"-fx-background-color: $hexColor, white;"))
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

  def addDataToPlotChart(dataPoint: (Double, Double), seriesName: String): Unit =
    Platform.runLater {() =>
      this.currentChart match {
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


  def addNewSeriesPlotChart(updatedCard: Card, seriesName: String, color: String): Unit =
    Platform.runLater {() =>
      this.currentChart match {
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

          this.currentCard = updatedCard

        case _ => println("Unsupported chart type")
      }
    }


end CardUIObject
*/