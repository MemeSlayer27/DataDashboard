package carduihandler

import canvas.CanvasHandler
import card.Card
import carduihandler.CardInteractionHandler
import javafx.scene.input.ContextMenuEvent
import javafx.scene.paint.Color
import scalafx.scene.Node
import scalafx.scene.chart.{BarChart, LineChart, PieChart, XYChart}
import scalafx.scene.layout.{BorderPane, StackPane}
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.Includes.jfxMouseEvent2sfx

trait CardUIHandler(initialCard: Card, canvas: CanvasHandler, val path: String):

  private var currentCard: Card = initialCard
  private var currentChart = getChartComponent
  private var isSelected = false
  
  private var interactionHandler = CardInteractionHandler(this, canvas, path)

  // Getters and setters for the current card and chart
  def getCurrentCard = currentCard
  def getCurrentChart = currentChart
  
  def changeCurrentCard(newCard: Card) = 
    currentCard = newCard
    deselect
  end changeCurrentCard


  // A method that makes the card selected, and updates the UI accordingly (colored boundaries)
  def select = isSelected = true
  def deselect = isSelected = false
  def getIsSelected = isSelected


  // This method returns the component that is displayed on the canvas
  def getCardComponent = 
    val stackPane = new StackPane()
    stackPane.translateX = currentCard.x
    stackPane.translateY = currentCard.y
    stackPane.prefWidth  = currentCard.width
    stackPane.prefHeight = currentCard.height

    // Uses the getChartComponent method to get the chart component of the card. This is defined in the subclasses
    currentChart = getChartComponent

    // Add the chart to a border pane. Border panes are empty. They become existant when the user selects a card
    val borderPane = new BorderPane()
    borderPane.center = currentChart
    stackPane.children.addAll(borderPane)

    // Set the event handlers for the card. These are defined in the CardInteractionHandler class
    stackPane.onMouseMoved = (event) => interactionHandler.handleMouseMoved(stackPane)(event)
    stackPane.onMousePressed = (event) => interactionHandler.handleMousePressed(stackPane)(event)
    stackPane.onMouseDragged = (event) => interactionHandler.handleMouseDragged(stackPane)(event)
    stackPane.onMouseReleased = (event) => interactionHandler.handleMouseReleased(stackPane)(event)
    stackPane.onContextMenuRequested = (event: ContextMenuEvent) => interactionHandler.handleContextMenuRequested(stackPane)(event)

    stackPane
    
  end getCardComponent

  // This method calculates the statistics of the data in the chart
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
      val middle1 = sortedData(dataValues.length / 2)
      val middle2 = sortedData((dataValues.length / 2) - 1)
      (middle1 + middle2) / 2.0
    else
      // If the list has an odd number of elements, take the middle element
      sortedData(dataValues.length / 2)

    val dataMode = dataValues.groupBy(identity).maxBy(_._2.size)._1
    val dataMax = dataValues.max
    val dataMin = dataValues.min
    val dataVariance = dataValues.map(x => math.pow(x - dataMean, 2)).sum / dataValues.length
    val dataStdDev = math.sqrt(dataVariance)

    // Create a vector of the stats, so that they can be easily iterated over
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

  end calculateStats

  // Helper method to convert a color to a hex string
  def colorToHex(color: Color): String = {
    val r = (color.getRed * 255).toInt
    val g = (color.getGreen * 255).toInt
    val b = (color.getBlue * 255).toInt
    f"#$r%02x$g%02x$b%02x"
  }



  // This method is implemented by the subclasses, and returns the chart component of the card
  def getChartComponent: Node


end CardUIHandler

