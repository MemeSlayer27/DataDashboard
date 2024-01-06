package canvas

import card.Card.*
import card.{Card, CardFileHelper, Plot, TimeSeriesPlot}
import javafx.scene.layout.BorderPane
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.layout.{Pane, Region, StackPane}
import scalafx.Includes.observableList2ObservableBuffer

import java.io.File
import java.util.concurrent.*
import scala.collection.mutable.Buffer
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.io.Source
import scala.util.Try
import scalafx.Includes.jfxBorderPane2sfx
import carduihandler.{BigNumberComponent, CardUIBuilder, CardUIHandler, CategoricBarChartComponent, PieChartComponent, PlotChartComponent, TimeSeriesPlotComponent}

/**
 * This class is responsible for handling the canvas and all the cards on it.
 * It is responsible for loading cards from the file system, and saving cards to the file system.
 * It is also responsible for adding cards to the canvas, and removing cards from the canvas.
 * It is also responsible for moving cards around on the canvas.
 * It is also responsible for changing the dashboard.
 * It is also responsible for deleting cards.
 * It is also responsible for copying cards.
 * It is also responsible for moving cards to another dashboard.
 * It is also responsible for loading cards from another dashboard.
 * It is also responsible for saving cards to another dashboard.
 * It is also responsible for creating a new dashboard.
 * It is also responsible for deleting a dashboard.
 *
 * */
class CanvasHandler(val cardUIComponentList: ObservableBuffer[javafx.scene.Node]):

  private var dashboardName =
    val directory = File("dashboards")
    if directory.exists && directory.isDirectory then
      directory.listFiles.filter( _.isDirectory ).map( _.getName ).toVector.head
    else
      println("directory missing or not a directory")
      "default"

  private val directoryPath = "dashboards" + File.separator + dashboardName
  private var cardUIBuilder = CardUIBuilder(this, directoryPath + File.separator)

  private var cardUIHandlerList = Buffer[CardUIHandler]()

  def getCurrentDashboardName = dashboardName


  def changeDashboard(newCanvas: String) =

    dashboardName = newCanvas
    cardUIComponentList.clear()

    loadCards(newCanvas)

  def getExistingDashboards: Vector[String] = {
    val directory = File("dashboards")
    if directory.exists && directory.isDirectory then
      directory.listFiles.filter( _.isDirectory ).map( _.getName ).toVector
    else
      println("directory missing or not a directory")
      Vector()
  }

  def removeDashboard() =
    // Helper function to delete a directory and its contents
    def deleteDirectoryRecursively(file: File): Boolean =
      if (file.isDirectory) then
        file.listFiles.foreach(deleteDirectoryRecursively)
      file.delete()
    end deleteDirectoryRecursively

    // delete the directory
    val directoryPath = "dashboards" + File.separator + dashboardName
    val directory = File(directoryPath)
    deleteDirectoryRecursively(directory)

    // get the name of some dashboard to switch over to
    val newDashboardName =
      val directory = File("dashboards")
      if directory.exists && directory.isDirectory then
        directory.listFiles.filter( _.isDirectory ).map( _.getName ).toVector.head
      else
        println("directory missing or not a directory")
        "default"

    changeDashboard(newDashboardName)


  // WORKIN so far
  def loadCards(dashboardName: String) =
      val directoryPath = "dashboards" + File.separator + dashboardName
      val directory = File(directoryPath)

      val cardFileHelper = CardFileHelper


      val filenames =
        if directory.exists && directory.isDirectory then
          directory.listFiles.filter( _.isFile ).map( _.getName ).toVector
        else
          println("directory missing or not a directory")
          Vector()


      val cardUIHandlerVector =
        for name <- filenames yield
          val path = directoryPath + File.separator + name
          val card = cardFileHelper.buildCardFromFile(path)

          cardUIBuilder.buildComponent(card)

      this.cardUIHandlerList = cardUIHandlerVector.toBuffer
      val cardComponents = this.cardUIHandlerList.toBuffer.map( _.getCardComponent )

      cardComponents.foreach( cardUIComponentList.addOne(_) )

  // Moves the given cards to another dashboard
  def moveCardsToAnotherDashboard(cardComponents: Vector[Node], dashBoard: String) = {
    for (component <- cardComponents) {
      val cardUIObject = cardUIHandlerList(cardUIComponentList.indexOf(component))
      val card = cardUIObject.getCurrentCard
      val oldPath = "dashboards" + File.separator + dashboardName + File.separator + card.id + ".json"
      val newPath = "dashboards" + File.separator + dashBoard + File.separator + card.id + ".json"

      val oldFile = new File(oldPath)
      val newFile = new File(newPath)
      oldFile.renameTo(newFile)

      removeCard(card)
    }
  }

  def deleteMultipleCards(cardComponents: Vector[Node]) = {
    for (component <- cardComponents) {
      val cardUIObject = cardUIHandlerList(cardUIComponentList.indexOf(component))
      val card = cardUIObject.getCurrentCard
      removeCard(card)
    }
  }

  // Should create a set of cards identical to the given cards, with only difference being a slight offset in position
  // and a different id. Add these cards to the current dashboard
  def copyCards(cardComponents: Vector[Node]) = {
    val xOffset = 10
    val yOffset = 10

    for (component <- cardComponents) {
      val cardUIObject = cardUIHandlerList(cardUIComponentList.indexOf(component))
      val originalCard = cardUIObject.getCurrentCard
      val newCardId = generateID.getOrElse(originalCard.id + 1)
      val newCard = originalCard.copy(
        id = newCardId,
        x = originalCard.x + xOffset,
        y = originalCard.y + yOffset
      )
      addCard(newCard)
    }
  }

  def removeCard(card: Card) =
    val cardFileHelper = CardFileHelper
    val path = "dashboards" + File.separator + dashboardName + File.separator + card.id + ".json"

    // delete the card file
    cardFileHelper.deleteCard(card, path)

    // remove the card from the ui component list
    val componentIndex = cardUIHandlerList.indexOf(cardUIHandlerList.find(_.getCurrentCard.id == card.id).get)
    cardUIComponentList.remove(componentIndex)

    // remove the card from the carduiobject list
    cardUIHandlerList.remove(cardUIHandlerList.indexOf(cardUIHandlerList.find(_.getCurrentCard.id == card.id).get))


  // Update pos and size, save changes, and return the new Card object
  def updateCardSizeAndPos(
    oldCard: Card,
    newX: Double,
    newY: Double,
    newWidth: Double,
    newHeight: Double
  ): Card =

    val newCard = oldCard.copy(
      x = newX,
      y = newY,
      width = newWidth,
      height = newHeight
    )

    val cardFileHelper = CardFileHelper
    val path = "dashboards" + File.separator + dashboardName + File.separator + oldCard.id + ".json"
    cardFileHelper.saveCard(newCard, path)
    newCard

  // TODO: this will also need to be passed into carduiobject as a callback. Just opens up dataconfigurator with the current card
  // TODO: Implement this if I ever get around to it. Not required
  //def updateCardConfig = ???


  // Updates the data for all cards that are live
  private def liveDataUpdate() =
   cardUIHandlerList.filter( handler => handler.getCurrentCard.live )
      .foreach(
        handler =>
          val card = handler.getCurrentCard
          handler match
            case pcc: PlotChartComponent           => card.plotLiveUpdate(pcc.addDataToPlotChart)
            case tspc: TimeSeriesPlotComponent     => card.timeSeriesPlotLiveUpdate(tspc.addDataToTimeSeriesChart)
            case pc: PieChartComponent             => card.nonPlotLiveUpdate(pc.addDataPoint)
            case cbcc: CategoricBarChartComponent  => card.nonPlotLiveUpdate(cbcc.addDataPoint)
            case bn: BigNumberComponent            => card.nonPlotLiveUpdate(bn.updateData)
            case _                                 => println("No live update for this card type")
      )

    // Create a ScheduledExecutorService with a single thread
  private val scheduledExecutor = Executors.newScheduledThreadPool(1)
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(scheduledExecutor)

  private val liveUpdateTask = new Runnable {
    def run() =
      println("running live update task")

      liveDataUpdate()
  }

    // Schedule the task to run every 5 seconds, with an initial delay of 4 seconds
    scheduledExecutor.scheduleAtFixedRate(liveUpdateTask, 2, 5, TimeUnit.SECONDS)

  def addCard(card: Card) =
    val cardFileHelper = CardFileHelper

    val path = "dashboards" + File.separator + dashboardName + File.separator + card.id + ".json"
    cardFileHelper.saveCard(card, path)


    val cardUIHandler = cardUIBuilder.buildComponent(card)

    cardUIHandlerList.addOne(cardUIHandler)

    val component = cardUIHandler.getCardComponent
    cardUIComponentList.addOne(component)

  // Generates a new ID for a card. New ID is the smallest number that is not already taken.
  def generateID =
    val folderPath = "dashboards"
    val folder = new File(folderPath)

    if (folder.isDirectory) {
      val files = folder.listFiles()
      val subfolders = files.filter(_.isDirectory)

      // Find all numbers that are already taken
      val numbers: Set[Int] = subfolders.flatMap { subfolder =>
        subfolder.listFiles().flatMap { file =>
          val filename = file.getName
          if (filename.endsWith(".json")) {
            Try(filename.stripSuffix(".json").toInt).toOption
          } else {
            None
          }
        }
      }.toSet

      val maxNumber = numbers.maxOption.getOrElse(0)

      // Find the smallest number that is not already taken and return it
      (1 to maxNumber).find(!numbers.contains(_)).orElse(Some(maxNumber + 1))


    } else {
      None
    }

  def bringToFront(cardComponent: Node): Unit = {
    // Find the index of the cardComponent
    val index = cardUIComponentList.indexOf(cardComponent)

    // Remove the component from the list and add it back to move it to the front
    cardUIComponentList.remove(cardComponent)
    cardUIComponentList.add(cardComponent)

    // Move the corresponding CardUIHandler to maintain the same order as cardUIComponentList
    val handler = cardUIHandlerList.remove(index)
    cardUIHandlerList.addOne(handler)
  }


  def selectCards(selectedComponents: Vector[Node]) = {
    val selectedCards = selectedComponents.map(component => cardUIHandlerList(cardUIComponentList.indexOf(component)))
    selectedCards.foreach(card => card.select)


    def isStackPane: PartialFunction[Any, Unit] = {
      case stackPane: javafx.scene.layout.StackPane =>
        stackPane.getChildren.headOption match {
          case Some(borderPane: BorderPane) =>
            borderPane.style = "-fx-border-color: white; -fx-border-width: 2px;"
          case _ => // Do nothing if the first child is not a BorderPane
        }
      case _ => // Do nothing if the component is not a StackPane or a subclass of StackPane
    }

    selectedComponents.foreach { component =>
      isStackPane(component.delegate)
    }
  }

  def deselectCards() = {
    cardUIHandlerList.foreach(card => card.deselect)

    def isStackPane: PartialFunction[Any, Unit] = {
      case stackPane: javafx.scene.layout.StackPane =>
        stackPane.getChildren.headOption match {
          case Some(borderPane: BorderPane) =>
            borderPane.style = "" // Reset the border style to the default state
          case _ => // Do nothing if the first child is not a BorderPane
        }
      case _ => // Do nothing if the component is not a StackPane or a subclass of StackPane
    }

    cardUIComponentList.foreach { component =>
      isStackPane(component)
    }
  }

  
end CanvasHandler

