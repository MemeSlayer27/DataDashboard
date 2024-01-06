import javafx.scene.input.{ContextMenuEvent, MouseButton}
import scalafx.application.JFXApp3
import scalafx.scene.{Node, Scene}
import scalafx.scene.layout.{HBox, Pane, StackPane, VBox}
import scalafx.scene.paint.Color
import scalafx.scene.shape.{Rectangle, StrokeLineCap}
import scalafx.Includes.{eventClosureWrapperWithZeroParam, jfxMouseEvent2sfx, jfxScene2sfx}
import scalafx.scene.control.{Alert, Button, ChoiceBox, ComboBox, ContextMenu, Label, ListView, MenuItem, RadioButton, SeparatorMenuItem, TextField, TextInputDialog, ToggleGroup}
import scalafx.scene.input.MouseEvent
import scalafx.event.EventIncludes.eventClosureWrapperWithParam
import scalafx.geometry.{Insets, Pos, Side}
import scalafx.stage.{Modality, Stage}
import scalafx.collections.ObservableBuffer
import scalafx.scene.chart.*
import scalafx.scene.chart.{LineChart, NumberAxis, XYChart}
import dataconfigurator.DataConfigurator
import canvas.{CSVDropHandler, CanvasHandler}
import scalafx.scene.control.Alert.AlertType

import java.io.File

object ScalaFXDashboard extends JFXApp3 {
  override def start(): Unit = {
    // Create a stack pane with a black background
    val mainStackPane = new StackPane {
      style = "-fx-background-color: #1a0533"
    }

    // Create a background pane
    val backgroundPane = new Pane()

    // Create a pane for child components
    val childComponentsPane = new Pane {
      pickOnBounds = false
    }

    // Create a pane for the buttons
    val buttonPane = new Pane {
      pickOnBounds = false
    }


    // Add the background pane and child components pane to the main stack pane
    mainStackPane.children.addAll(backgroundPane, childComponentsPane, buttonPane)

    val canvas = CanvasHandler(childComponentsPane.children)
    canvas.loadCards(canvas.getCurrentDashboardName)

    // Create an instance of DragBoxSelectionTool
    val dragBoxSelectionTool = new DragBoxSelectionTool(backgroundPane, childComponentsPane, canvas)

    // Initialize the CSVDropHandler
    val csvDropHandler = new CSVDropHandler(mainStackPane, "data")

    // Create the new card button
    val newCardButton = ButtonModule.createNewCardButton(canvas)

    // Create the dashboard selection button and its context menu
    val (dashboardSelectionButton, dashboardContextMenu) =
      ButtonModule.createDashboardSelectionButton(canvas)

    // Add the buttons to the child components pane
    val buttonColumn = new VBox()
    buttonColumn.children.add(newCardButton)
    buttonColumn.children.add(dashboardSelectionButton)

    buttonPane.children.add(buttonColumn)


    // Set the scene to display the main stack pane
    stage = new JFXApp3.PrimaryStage {
      title.value = "Dashboard"
      maximized = true
      scene = new Scene {
        root = mainStackPane
      }
    }
  }
}

