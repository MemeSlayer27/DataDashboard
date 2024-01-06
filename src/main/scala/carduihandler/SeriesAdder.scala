package carduihandler

import card.{DataHandler, Plot, TimeSeriesPlot}
import scalafx.Includes.eventClosureWrapperWithZeroParam
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.chart.XYChart
import scalafx.scene.control.*
import scalafx.scene.layout.{HBox, VBox}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle

import java.io.File

class SeriesAdder(val cardUIHandler: CardUIHandler, onClose: => Unit):
  private val isLive = cardUIHandler.getCurrentCard.live

  private var dataSource: Option[String] = None
  private var plotFields: (Option[String], Option[String]) = (None, None)

  private var timeSeriesField: Option[String] = None

  private var seriesName: Option[String] = None

  private var color = ""

  val layout = new VBox {
    spacing = 10
  }

  // This needs to be befor initial update for this to work correctly
  private var dataSourceFieldPicker: Node = new Label("")

  // Initial update
  updateLayout()

  private def updateLayout(): Unit =
    layout.children = Seq(
      new Label("Choose datasource"),
      if isLive then dataSourceEntryLive() else dataSourceEntryLocal(),
      getDataStructurePicker,
      getSeriesNamePicker,
      getBottomButtons
    )
  end updateLayout


    // Component for entering the name of the local data source.
  private def dataSourceEntryLocal(): Node =
    // Get all the names of potential sourcefiles
    val sourceOptions =
      val directory = File("data")
      val fileNames = directory.listFiles().map( _.getName )
      ObservableBuffer.from(fileNames)

    val comboBox = new ComboBox[String] {
      items = sourceOptions
      value = dataSource.getOrElse("")
    }

    comboBox.onAction = () => {
      dataSource = Some("data/" + comboBox.value.value)
      this.updateLayout()
    }
    comboBox
  end dataSourceEntryLocal

  // Component for entering the url of the live data source.
  // Once confirmed, it will show the fields of the data source and let's the user pick one (or two if plot) to plot
  private def dataSourceEntryLive() =
    val textField = new TextField()

    // Set the default value of textField
    dataSource.foreach(textField.text = _)

    val confirmSource = new Button("Confirm Source")

    val buttonBox = new VBox(children = confirmSource)

    val sourcePicker = new HBox {
      spacing = 10
      children = Seq(textField, buttonBox)
    }

    // Create a layout for the text field and button
    val textFieldLayout = new VBox {
      spacing = 10
      children = Seq(sourcePicker, dataSourceFieldPicker)
    }

    // When Confirm source is pressed
    confirmSource.onAction = (_) => {
      val text = textField.text.value.trim()
      val dataHandler = DataHandler(text, this.isLive)

      dataHandler.getLiveDataStructure(
        onCompleteCallback =
          (structure: Vector[(String, String)]) =>

            // Apparently this is the only way to update the UI from a different thread
            Platform.runLater {
              this.dataSource = Some(text)
              val options = ObservableBuffer.from(structure.map(t => t._1 + ": " + t._2))

              // UIcomponent for the slidedown
              val comboBox = new ComboBox[String] {
                items = options
              }

              val secondaryComboBox = new ComboBox[String] {
                items = options
              }

              // This takes place when some option is selected in the slidedown
              comboBox.onAction = () => {
                this.timeSeriesField = Some(comboBox.value.value.split(":")(0))
                this.updateLayout()
              }

              val tempPicker =
                this.cardUIHandler.getCurrentCard.chart match
                  case _: Plot =>
                    val prunedOptions = options.filter( (field) => field.split(":")(1).trim() == "numeric" )
                    // UIcomponent for the slidedown
                    val comboBox = new ComboBox[String] {
                      items = prunedOptions
                    }
                    val secondaryComboBox = new ComboBox[String] {
                      items = prunedOptions
                    }

                   // This takes place when some option is selected in the slidedown
                    comboBox.onAction = () => {
                      this.plotFields = (Some(comboBox.value.value.split(":")(0)), plotFields._2)
                      this.updateLayout()
                    }
                   // This takes place when some option is selected in the slidedown
                    secondaryComboBox.onAction = () => {
                      this.plotFields = (plotFields._1, Some(secondaryComboBox.value.value.split(":")(0)))
                      this.updateLayout()
                    }

                    new HBox {
                      spacing = 10
                      children = Seq(comboBox, secondaryComboBox)
                    }
                  case _: TimeSeriesPlot =>
                    // UIcomponent for the slidedown
                    val prunedOptions = options.filter( (field) => field.split(":")(1).trim() == "numeric" )

                    val comboBox = new ComboBox[String] {
                      items = prunedOptions
                    }

                    // This takes place when some option is selected in the slidedown
                    comboBox.onAction = () => {
                      this.timeSeriesField = Some(comboBox.value.value.split(":")(0))
                      this.updateLayout()
                    }

                    comboBox
              dataSourceFieldPicker = tempPicker

              this.updateLayout()
            },


        onFailureCallback = () =>
          val label = new Label("Invalid URL (or something went wrong)")

          // Apparently this is the only way to update the UI from a different thread
          Platform.runLater {
            dataSourceFieldPicker = label
            this.updateLayout()
          }
      )

    }

    textFieldLayout

  end dataSourceEntryLive

  // Seriesname
  private def getSeriesNamePicker: Node =
    val label = new Label("Choose Series Name")

    val textField = new TextField {
      text = seriesName.getOrElse("")
    }

    // Add similar checkmark as title field


    val valueProvidedCheck = new Label {
      text = if seriesName.isDefined then "✓" else ""
      style = "-fx-text-fill: green"
    }

    textField.onAction = () =>
      seriesName = Some(textField.text.value)
      this.updateLayout()

    val picker = new VBox {
      children = Seq(label, textField,valueProvidedCheck)
    }

    picker
  end getSeriesNamePicker

  private def getDataStructurePicker: Node =
    this.dataSource match
      case None         => new VBox()
      case Some(source) =>
        val handler = DataHandler(source, this.isLive)
        handler.getLocalDataStructure match
          case None            => new Label("sus")
          case Some(structure) =>
            this.cardUIHandler.getCurrentCard.chart match
              case _: Plot =>
                val prunedOptions = ObservableBuffer.from(structure.filter((_, t) => t == "numeric").map( (f,t) => f + ": " + t ))

                val comboBoxX = new ComboBox[String] {
                  value = plotFields._1.getOrElse("")
                  items = prunedOptions
                }

                comboBoxX.onAction = () => {
                  this.plotFields = (Some(comboBoxX.value.value.split(":")(0)), this.plotFields._2)
                  this.updateLayout()
                }

                val comboBoxY = new ComboBox[String] {
                  value = plotFields._2.getOrElse("")
                  items = prunedOptions
                }

                comboBoxY.onAction = () => {
                  this.plotFields = (this.plotFields._1, Some(comboBoxY.value.value.split(":")(0)))
                  this.updateLayout()
                }
                val pickerLayout = new HBox {
                  children = Seq(comboBoxX, comboBoxY)
                }

                pickerLayout
              case _: TimeSeriesPlot =>
                val prunedOptions = ObservableBuffer.from(structure.filter((_, t) => t == "numeric").map( (f,t) => f + ": " + t ))

                val comboBox = new ComboBox[String] {
                  value = timeSeriesField.getOrElse("")
                  items = prunedOptions
                }
                comboBox.onAction = () => {
                  this.timeSeriesField = Some(comboBox.value.value.split(":")(0))
                  this.updateLayout()
                }

                // This gets returned
                new VBox {
                  children = Seq(
                    new Label("Choose data"),
                    comboBox
                  )
                }

  end getDataStructurePicker

  private def getBottomButtons: Node =
    // Create the Cancel button
    val cancelButton = new Button("Cancel")
    cancelButton.onAction = (_) => {
      // Close the window when the Cancel button is clicked
      onClose
    }

    // Create the Confirm button
    val confirmButton = new Button("Confirm")
    confirmButton.onAction = (_) => {
      // Do some extra actions here

      println(this.plotFields)
      this.addSeries() match
        case false => ()
        case true  =>
          // Close the window when the Confirm button is clicked
          onClose

    }

    // Create an HBox layout with the two buttons
    val buttonsLayout = new HBox {
      spacing = 10
      children = Seq(cancelButton, confirmButton)
    }

    buttonsLayout
  end getBottomButtons

  // returns true if success, false if not
  private def addSeries() =
    val seriesName = this.seriesName.getOrElse("")
    val dataSource = this.dataSource.getOrElse("")
    val plotFields = this.plotFields
    val timeSeriesField = this.timeSeriesField


    // If all fields are filled in, add the series
    if (seriesName.nonEmpty && dataSource.nonEmpty && ((plotFields._1.nonEmpty && plotFields._2.nonEmpty) || timeSeriesField.isDefined)) then
      val card = this.cardUIHandler.getCurrentCard
      val updatedCard = card.chart match
        case _: Plot =>
          // adds a new series to the plot, saves it, and returns the updated card
          card.addNewSeriesToPlot(cardUIHandler.path, seriesName, (plotFields._1.get, plotFields._2.get), dataSource)
        case _: TimeSeriesPlot =>
          card.addNewSeriesToPlot(cardUIHandler.path, seriesName, (timeSeriesField.get, ""), dataSource)  // Probably the simplest way to do this.
        case _ => throw new Exception("Invalid chart type in seriesadder confirmation")

      this.cardUIHandler match // add the ui component to the chart
        case pcc: PlotChartComponent => pcc.addNewSeriesPlotChart(updatedCard, seriesName, color) // add series to chart
        case tsc: TimeSeriesPlotComponent => tsc.addNewSeriesPlotChart(updatedCard, seriesName, color) // add series to chart
        case _ => throw new Exception("Invalid chart type in seriesadder confirmation")

      true  // return true if success, false if not
    else
      false


  end addSeries

end SeriesAdder
