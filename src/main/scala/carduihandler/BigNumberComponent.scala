package carduihandler

import canvas.CanvasHandler
import card.Card
import javafx.application.Platform
import scalafx.geometry.Pos
import scalafx.scene.Node
import scalafx.scene.control.Label
import scalafx.scene.layout.VBox

class BigNumberComponent(initialCard: Card, canvas: CanvasHandler, path: String) extends CardUIHandler(initialCard, canvas, path):


  
  override def getChartComponent: Node =
    val card = getCurrentCard
    
    val bgColor = card.bgColor
    
    // This gets returned
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
            
  end getChartComponent
  
  def updateData(dataPoint: (String, Int)) =
    Platform.runLater(() =>
      getCurrentChart match
        case vbox: VBox =>
          vbox.children.get(1) match
            case label: javafx.scene.control.Label =>

              label.setText(dataPoint._1)

            case _ => throw new Exception("bignum has no label for some reason")
        case _ => throw new Exception("bignum has no vbox for some reason")
    )
  end updateData
  
    

end BigNumberComponent

