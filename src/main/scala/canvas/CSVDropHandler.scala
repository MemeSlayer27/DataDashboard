package canvas

import javafx.scene.input.{DragEvent, TransferMode}
import scalafx.scene.layout.Pane
import java.nio.file.{Files, Paths, StandardCopyOption}
import scalafx.Includes.jfxDragEvent2sfx

class CSVDropHandler(targetPane: Pane, dataFolder: String):
  targetPane.onDragOver = (event: DragEvent) => {
    if (event.dragboard.hasFiles) {
      event.acceptTransferModes(TransferMode.COPY)
    }
    event.consume()
  }

  targetPane.onDragDropped = (event: DragEvent) => {
    val db = event.dragboard
    var success = false

    if (db.hasFiles) {
      db.files
        .filter(file => file.getName.toLowerCase.endsWith(".csv"))
        .foreach { file =>
          val sourcePath = file.toPath
          val targetPath = Paths.get(dataFolder, file.getName)
          Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }
      success = true
    }

    event.dropCompleted = success
    event.consume()
  }

end CSVDropHandler
