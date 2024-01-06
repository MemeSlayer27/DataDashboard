import scalafx.Includes.*
import scalafx.scene.control.*
import scalafx.event.EventIncludes.*
import scalafx.scene.layout.VBox
import canvas.CanvasHandler

import java.io.File
import dataconfigurator.DataConfigurator
import scalafx.geometry.Side
import scalafx.scene.Scene
import scalafx.scene.control.Alert.AlertType
import scalafx.stage.{Modality, Stage}

object ButtonModule:

  def createNewCardButton(canvas: CanvasHandler): Button = {
    val button = new Button("Add New Card") {
      style =
        """
        -fx-background-color: #4a297d;
        -fx-text-fill: #ffffff;
        -fx-border-color: #8a6dcf;
        -fx-border-width: 2px;
        -fx-border-radius: 5px;
        -fx-background-radius: 5px;
        -fx-font-size: 14px;
        -fx-padding: 5 10 5 10;
      """
    }
    button.onAction = (_) => {
      val configWindow = new Stage {
        title = "Configuration"
        scene = new Scene {
          minWidth = 400
          minHeight = 600
          content = DataConfigurator(close, canvas).layout // Giving close() function as a construction parameter so that this window can be closed
        }.delegate
        initModality(Modality.ApplicationModal)
      }
      configWindow.showAndWait()
    }
    button
  }

  def createDashboardSelectionButton(canvas: CanvasHandler): (Button, ContextMenu) = {
    val button = new Button("Select Dashboard") {
      style =
        """
        -fx-background-color: #4a297d;
        -fx-text-fill: #ffffff;
        -fx-border-color: #8a6dcf;
        -fx-border-width: 2px;
        -fx-border-radius: 5px;
        -fx-background-radius: 5px;
        -fx-font-size: 14px;
        -fx-padding: 5 10 5 10;
      """
    }

    val contextMenu = ButtonModule.createDashboardContextMenu(canvas)

    button.onAction = (_) => {
      contextMenu.show(button, Side.Bottom, 0, 0)
    }

    (button, contextMenu)
  }

  def createDashboardContextMenu(canvas: CanvasHandler): ContextMenu = {
    val contextMenu = new ContextMenu()

    def addDashboardMenuItem(dashboardName: String): Unit = {
      val item = new MenuItem(dashboardName)
      item.onAction = (_) => {
        canvas.changeDashboard(dashboardName)
      }
      val separatorIndex = contextMenu.items.indexWhere {
        case _: javafx.scene.control.SeparatorMenuItem => true
        case _ => false
      }
      if (separatorIndex >= 0) then
        contextMenu.items.insert(separatorIndex, item)
      else
        contextMenu.items.add(item)
    }

    val directories = canvas.getExistingDashboards

    directories.foreach(addDashboardMenuItem)

    // Create the "New Dashboard" menu item
    val newDashboardMenuItem = new MenuItem("New Dashboard")

    newDashboardMenuItem.onAction = (_) => {
      val dialog = new TextInputDialog() {
        title = "New Dashboard"
        headerText = "Enter the name of the new dashboard:"
      }

      val result = dialog.showAndWait()

      result match
        case Some(name) =>
          val newDirectory = new File("dashboards" + File.separator + name)
          if (newDirectory.exists()) then
            // Show an alert if the directory already exists
            new Alert(AlertType.Error) {
              title = "Error"
              headerText = "Dashboard already exists."
              contentText = s"A dashboard with the name '$name' already exists."
            }.showAndWait()
          else
            newDirectory.mkdir()
            addDashboardMenuItem(newDirectory.getName)
        case None => () // User canceled the dialog

    }

    val deleteDashboardMenuItem = new MenuItem("Delete Dashboard")

    deleteDashboardMenuItem.onAction = (_) => {
      val confirmationDialog = new Alert(AlertType.Confirmation) {
        title = "Delete Dashboard"
        headerText = "Are you sure you want to delete the current dashboard?"
        contentText = "This action cannot be undone."
      }

      val result = confirmationDialog.showAndWait()

      result match
        case Some(ButtonType.OK) =>
          val currentDashboard = canvas.getCurrentDashboardName
          contextMenu.items.removeIf(_.text.value == currentDashboard)

          canvas.removeDashboard()
        case _ => () // User canceled the dialog or clicked 'No'

    }


    contextMenu.items ++= Seq(new SeparatorMenuItem(), newDashboardMenuItem, deleteDashboardMenuItem)

    contextMenu
  }


end ButtonModule
