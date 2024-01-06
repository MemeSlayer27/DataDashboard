package carduihandler

import canvas.CanvasHandler
import dataconfigurator.DataConfigurator
import javafx.scene.input.ContextMenuEvent
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.{Node, Scene}
import scalafx.scene.chart.LineChart
import scalafx.scene.control.{Button, ContextMenu, Label, MenuItem}
import scalafx.scene.input.MouseEvent
import scalafx.scene.layout.{HBox, StackPane, VBox}
import scalafx.stage.{Modality, Stage}
import scalafx.Includes.jfxScene2sfx
import scalafx.Includes.observableList2ObservableBuffer
import scalafx.Includes.eventClosureWrapperWithZeroParam
import scalafx.Includes.jfxXYChartSeries2sfx
import scalafx.scene
import scalafx.scene.Cursor


// Handles the interaction with the card
class CardInteractionHandler(cardUIHandler: CardUIHandler, canvasHandler: CanvasHandler, val path: String):
  private var newOffsetX = 0.0
  private var newOffsetY = 0.0
  private var cornerPress = false
  private var initialX = 0.0
  private var initialY = 0.0

  def handleMouseMoved(stackPane: StackPane)(event: MouseEvent): Unit =
    val xCornerBounds =
      stackPane.translateX() + stackPane.width() - 20 < event.getSceneX &&
      event.getSceneX < stackPane.translateX() + stackPane.width() + 20
    val yCornerBounds =
      stackPane.translateY() + stackPane.height() - 20 < event.getSceneY &&
      event.getSceneY < stackPane.translateY() + stackPane.height() + 20
  
    if (xCornerBounds && yCornerBounds) then
      stackPane.cursor = Cursor.SEResize
    else 
      stackPane.cursor = Cursor.Default
  end handleMouseMoved

  def handleMousePressed(stackPane: StackPane)(event: MouseEvent): Unit =
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

      initialX = stackPane.translateX()
      initialY = stackPane.translateY()

      canvasHandler.bringToFront(stackPane)
  end handleMousePressed

  def handleMouseDragged(stackPane: StackPane)(event: MouseEvent): Unit =
    if cornerPress then
      stackPane.prefWidth() = event.sceneX - stackPane.translateX()
      stackPane.prefHeight() = event.sceneY - stackPane.translateY()
    else
      stackPane.translateX = event.sceneX - newOffsetX
      stackPane.translateY = event.sceneY - newOffsetY
  end handleMouseDragged


  def handleMouseReleased(stackPane: StackPane)(event: MouseEvent): Unit =
    val scene = stackPane.scene()

    // If the card is outside the canvas, then we should reset the position and size
    if (stackPane.translateX() < 0 || stackPane.translateY() < 0 || stackPane.translateX() + stackPane.width() > scene.width.value || stackPane.translateY() + stackPane.height() > scene.height.value) {
      stackPane.translateX = initialX
      stackPane.translateY = initialY
      return
    }

    // Saving the new card assigning it to currentcard
    val newCard = canvasHandler.updateCardSizeAndPos(cardUIHandler.getCurrentCard, stackPane.translateX(), stackPane.translateY(), stackPane.width(), stackPane.height())
    cardUIHandler.changeCurrentCard(newCard)

    cornerPress = false

  end handleMouseReleased

  // Handles right click on the card
  def handleContextMenuRequested(stackPane: StackPane)(event: ContextMenuEvent): Unit =
      if (event.getEventType == ContextMenuEvent.CONTEXT_MENU_REQUESTED) {
        val contextMenu = new ContextMenu {
          cardUIHandler.getCurrentChart match
            case _: LineChart[Number, Number] => // If the chart is a linechart, then we should add the option to add a new series
             items += new MenuItem("Add new series") {
              onAction = _ => {

                val configWindow: Stage = new Stage {
                  title = "new series"
                  scene = new Scene {
                    content =  new VBox {
                    alignment = Pos.Center
                    padding = Insets(10, 10, 10, 10)
                    prefWidth = 400.0
                    prefHeight = 300.0
                    children = SeriesAdder(cardUIHandler, close()).layout
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
              val statisticalNumbers: Vector[(String, Vector[(String, Double)])] = cardUIHandler.getCurrentChart match {
                case l: LineChart[Number, Number] => l.data.value.map((series: javafx.scene.chart.XYChart.Series[Number, Number]) => (series.getName, cardUIHandler.calculateStats(Some(series)))).toVector
                case _ => Vector(("Data Series", cardUIHandler.calculateStats()))
              }

              // Creates a column for each series
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
                title = "Edit Card"
                scene = new Scene {
                  content = new VBox {
                    minWidth = 400
                    minHeight = 600
                    alignment = Pos.Center
                    children = DataConfigurator(close, canvasHandler, Some(cardUIHandler.getCurrentCard)).layout
                  }
                 }

                initModality(Modality.ApplicationModal)
              }
              configWindow.showAndWait()
            }
          }
          // This option deletes the card
          items += new MenuItem("Delete card") {
            onAction = () => {
              // Deletes that card from canvas
              canvasHandler.removeCard(cardUIHandler.getCurrentCard)
            }
          }
        }
        // Show the context menu at the location of the mouse click
        contextMenu.show(stackPane.scene().getWindow, event.getSceneX,event.getSceneY)
      }
  end handleContextMenuRequested

end CardInteractionHandler

