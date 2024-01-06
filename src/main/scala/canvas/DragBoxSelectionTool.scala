import canvas.CanvasHandler
import scalafx.scene.input.MouseButton
import scalafx.scene.layout.Pane
import scalafx.scene.paint.Color
import scalafx.scene.shape.{Rectangle, StrokeLineCap}
import scalafx.scene.input.MouseEvent
import scalafx.Includes.{eventClosureWrapperWithZeroParam, jfxMouseEvent2sfx}
import scalafx.Includes.eventClosureWrapperWithParam
import scalafx.geometry.Bounds
import scalafx.scene.{Group, Node}
import scalafx.Includes.jfxNode2sfx
import scalafx.Includes.jfxBounds2sfx
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control.{Alert, ButtonType, ChoiceDialog, ContextMenu, MenuItem}

class DragBoxSelectionTool(backgroundPane: Pane, childComponentsPane: Pane, canvas: CanvasHandler):
  private val selectionRectangle = new Rectangle {
    fill =  Color.rgb(100, 100, 255, 0.3)
    stroke = Color.LightGray
    strokeWidth = 1
    strokeLineCap = StrokeLineCap.Round
  }

  private var initialMouseX: Double = 0
  private var initialMouseY: Double = 0

  backgroundPane.onMousePressed = (event: MouseEvent) => {

    if (event.button == MouseButton.Primary && !event.isSynthesized) {
      initialMouseX = event.x
      initialMouseY = event.y

      selectionRectangle.x = initialMouseX
      selectionRectangle.y = initialMouseY
      selectionRectangle.width = 0
      selectionRectangle.height = 0

      childComponentsPane.children.add(selectionRectangle)

      canvas.deselectCards()
    }
  }

  backgroundPane.onMouseDragged = (event: MouseEvent) => {
    if (event.button == MouseButton.Primary && !event.isSynthesized ) {
      selectionRectangle.width = math.abs(event.x - initialMouseX)
      selectionRectangle.height = math.abs(event.y - initialMouseY)
      selectionRectangle.x = math.min(event.x, initialMouseX)
      selectionRectangle.y = math.min(event.y, initialMouseY)
    }
  }

  private def isNodeInsideSelection(node: Node, selectionBounds: Bounds): Boolean = {
      val nodeBounds = node.localToScene(node.boundsInLocal.value)
      val corners = Seq(
        (nodeBounds.getMinX, nodeBounds.getMinY),
        (nodeBounds.getMaxX, nodeBounds.getMinY),
        (nodeBounds.getMinX, nodeBounds.getMaxY),
        (nodeBounds.getMaxX, nodeBounds.getMaxY)
      )

      corners.forall( (x, y) =>
        selectionBounds.contains(x, y)
      )
    }

  backgroundPane.onMouseReleased = (event: MouseEvent) => {
    if (event.button == MouseButton.Primary && !event.isSynthesized) {
      childComponentsPane.children.remove(selectionRectangle)

      val selectionBounds = selectionRectangle.localToScene(selectionRectangle.getBoundsInLocal)
      val selectedComponents: Vector[scalafx.scene.Node] =
        childComponentsPane.children
          .filter(node => isNodeInsideSelection(node, selectionBounds))
          .map(jfxNode2sfx)
          .toVector

      canvas.selectCards(selectedComponents)

      if (selectedComponents.nonEmpty) {
        showContextMenu(selectedComponents, event)
      }
    }
  }



  private def showContextMenu(selectedComponents: Vector[Node], event: MouseEvent): Unit = {
    val moveToDashboardMenuItem = new MenuItem("Move to another dashboard")
    val copyMenuItem = new MenuItem("Copy")
    val deleteMenuItem = new MenuItem("Delete")

    moveToDashboardMenuItem.onAction = (_) => {
      val existingDashboards = canvas.getExistingDashboards
      val choices = ObservableBuffer(existingDashboards: _*)

      val dialog = new ChoiceDialog[String](choices.head, choices) {
        title = "Move Cards to Another Dashboard"
        headerText = "Choose a dashboard to move the selected cards to:"
      }

      val result = dialog.showAndWait()
      result match
        case Some(selectedDashboard) =>
          canvas.moveCardsToAnotherDashboard(selectedComponents, selectedDashboard)
        case None => () // Do nothing if the user cancels the dialog

    }

    copyMenuItem.onAction = (_) => {
      canvas.copyCards(selectedComponents)
    }

    deleteMenuItem.onAction = (_) => {
      val alert = new Alert(AlertType.Confirmation) {
        title = "Delete Confirmation"
        headerText = "Are you sure you want to delete the selected cards?"
      }
      val result = alert.showAndWait()
      if (result.contains(ButtonType.OK)) {
        canvas.deleteMultipleCards(selectedComponents)
      }
    }



    val contextMenu = new ContextMenu {
      items ++= List(moveToDashboardMenuItem, copyMenuItem, deleteMenuItem)
    }
    contextMenu.show(childComponentsPane, event.screenX, event.screenY)
  }

end DragBoxSelectionTool
