package dataconfigurator

import card.*
import dataconfigurator.RangePicker
import scalafx.Includes.*
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.{Node, Scene}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle

import java.io.File
import scala.collection.mutable.Buffer
import scala.util.Try
import canvas.CanvasHandler
import javafx.beans.value.ObservableValue

/**
 * This class contains the UI and business logic used for configuring new and existing cards.
 * Having the UI code and business logic combined like this is in this case pretty well justified
 * due to them being so heavily intertwined.
 *
 * In essence, this class collects the configuration parameters, then uses them to construct a new card.Card.
 * Then it calls CanvasHandlers addCard function with said card.
 *
 */


// TODO: make Option Card a construction parameter for the case where the user wants to edit an existing card
// TODO: Not required so most likely won't but would be useful
// Every component should show a green checkmark if it is valid and the user has provided a value for it
class DataConfigurator(onClose: () => Unit, val canvasHandler: CanvasHandler, initialCard: Option[Card] = None):
  private var title: Option[String] = initialCard.map(_.title)
  private var isLive = initialCard.map(_.live).getOrElse(false)
  private var chartType: Option[String] =
    initialCard.flatMap(card => card.chart match
      case _: Plot => Some("Plot")
      case _: PieChartCategoric => Some("Piechart")
      case _: BarChartCategoric => Some("Barchart, categoric")
      case _: TimeSeriesPlot => Some("Time-Series Plot")
      case _: BigNumber => Some("Big Number")
      case _ => None)

  println(chartType)

  private var dataSource: Option[String] = initialCard.flatMap(card => card.dataSource.headOption)

  private var backgroundColor: String = initialCard.map(_.bgColor).getOrElse("#87CEEB")

  private var fieldNonPlot: Option[String] =
    initialCard.flatMap(card => card.chart match
      case PieChartCategoric(field)     => Some(field)
      case BarChartCategoric(field)     => Some(field)
      case BigNumber(field)              => Some(field)
      case TimeSeriesPlot(fields, names) => Some(fields.head)
      case _ => None)

  private var plotFields: (Option[String], Option[String]) =
    initialCard.flatMap(card => card.chart match
      case Plot(fields, names) => Some((Some(fields.head._1), Some(fields.head._2)))
      case _ => None).getOrElse((None, None))

  private var seriesName: Option[String] =
    initialCard.flatMap(card => card.chart match
      case Plot(fields, names) => Some(names.head)
      case TimeSeriesPlot(fields, names) => Some(names.head)
      case _ => None)



  val layout = new VBox {
      spacing = 10
    }

  // Initializes the range pickers
  private val xRangePicker = new RangePicker("X-Axis Range")
  private val yRangePicker = new RangePicker("Y-Axis Range")

  // This gets updated when the user confirms the data source
  private var liveDataSourceFieldPicker: Node = new VBox{
    children = Seq()
  }


  // Initial update
  updateLayout()

  private def updateLayout(): Unit =
    layout.children = Seq(
      getTitleBox,
      getDataSourceRadioButtons,
      chartTypePicker,
      if isLive then dataSourceEntryLive() else dataSourceEntryLocal(),
      getDataStructurePicker,
      getRestOfConfigs,
      getBottomButtons
    )

  private def getTitleBox =
    val label = new Label("Title (press enter to confirm)")

    val textField = new TextField {
      text = title.getOrElse("")

    }

    val valueProvidedCheck = new Label {
      text = if title.isDefined then "✓" else ""
      style = "-fx-text-fill: green"
    }

    textField.onAction = () =>
      this.title = Some(textField.text.value)
      this.updateLayout()

    new VBox {
      children = Seq(label,textField, valueProvidedCheck)
    }

  private def getDataSourceRadioButtons: Node =
    // Create a toggle group for the radio buttons
    val toggleGroupForBoth = new ToggleGroup()

    // Create the first radio button
    val radioButton1 = new RadioButton("Local Data") {
      toggleGroup = toggleGroupForBoth
      selected = !isLive // Set this option as the default
    }

    // Create the second radio button
    val radioButton2 = new RadioButton("Live Data") {
      toggleGroup = toggleGroupForBoth
      selected = isLive
    }


    // Create the layout for the radio buttons
    val radioButtonsLayout = new HBox {
      spacing = 10
      children = Seq(
        radioButton1,
        radioButton2
      )
    }

    // Update the main layout based on which radio button is selected
    toggleGroupForBoth.selectedToggle.onChange {
      isLive = !isLive
      this.updateLayout()
    }

    radioButtonsLayout


  private def chartTypePicker: Node =
    val title = new Label("Chart Type")

    // Options for different chart types to choose from
    val commonOptions = ObservableBuffer[String]("Plot", "Piechart", "Barchart, categoric")
    
    // Add the time series and Big Number options if the data is live
    val options =
      if this.isLive then commonOptions ++  ObservableBuffer[String]("Time-Series Plot", "Big Number") // Add the time series and Big Number options if the data is live
      else commonOptions

    // UIcomponent for the slidedown
    val comboBox = new ComboBox[String] {
      items = options
      value = chartType.getOrElse("")

    }

    // This takes place when some option is selected in the slidedown menu
    comboBox.onAction = () => {
      this.chartType = Some(comboBox.value.value)
      this.updateLayout()
    }

    new VBox {
      children = Seq(
        title,
        comboBox
      )
    }
  
  // Component for entering the name of the local data source.
  private def dataSourceEntryLocal(): Node =

    val title = new Label("Local Data Source")

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

    new VBox {
      children = Seq(
        title,
        comboBox
      )
    }


  // Component for entering the url of the live data source.
  // Once confirmed, it will show the fields of the data source and let's the user pick one (or two if plot) to plot
  private def dataSourceEntryLive() =
    val title = new Label("Live Data Source")

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
      children = Seq(title, sourcePicker, liveDataSourceFieldPicker) //TODO: This right here is the problem
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
              val options = ObservableBuffer.from(structure.map( (field,  typeName) => field + ": " + typeName ))

              // UIcomponent for the slidedown
              val comboBox = new ComboBox[String] {
                items = options
              }

              val secondaryComboBox = new ComboBox[String] {
                items = options
              }

              // This takes place when some option is selected in the slidedown
              comboBox.onAction = () => {
                this.fieldNonPlot = Some(comboBox.value.value.split(":")(0))
                this.updateLayout()
              }

              liveDataSourceFieldPicker =
                this.chartType match


                  case Some("Plot") =>
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
                  case _ =>
                    // UIcomponent for the slidedown
                    val prunedOptions =
                      this.chartType match
                        case Some("Time-Series Plot") => options.filter( (field) => field.split(":")(1).trim() == "numeric" )
                        case _ => options

                    val comboBox = new ComboBox[String] {
                      items = prunedOptions
                    }

                    // This takes place when some option is selected in the slidedown
                    comboBox.onAction = () => {
                      this.fieldNonPlot = Some(comboBox.value.value.split(":")(0))
                      this.updateLayout()
                    }

                    comboBox

              this.updateLayout()
            },


        onFailureCallback = () =>
          val label = new Label("Invalid URL (or something went wrong)")

          // Apparently this is the only way to update the UI from a different thread
          Platform.runLater {
            liveDataSourceFieldPicker = label
            this.updateLayout()
          }
      )

    }

    textFieldLayout

  // Component for picking the columns which should be plotted
  private def getDataStructurePicker: Node =

    val title = new Label("Pick fields to plot")

    this.dataSource match
      case None         => new VBox()
      case Some(source) =>
        val handler = DataHandler(source, this.isLive) // datahandler used to get the data structure

        handler.getLocalDataStructure match
          case None            => new Label("sus")
          case Some(structure) =>
            this.chartType match
              case Some("Plot") =>
                val prunedOptions = ObservableBuffer.from(structure.filter((_, t) => t == "numeric").map((f, t) => f + ": " + t))

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
                new VBox {
                  children = Seq(
                    title,
                    pickerLayout
                  )
                }

              case None => new VBox()
              case _ =>
                val options = ObservableBuffer.from(structure.map( (f,t) => f + ": " + t ))
                val comboBox = new ComboBox[String] {
                  value = fieldNonPlot.getOrElse("")
                  items = options
                }
                comboBox.onAction = () => {
                  this.fieldNonPlot = Some(comboBox.value.value.split(":")(0))
                  this.updateLayout()
                }

                new VBox {
                  children = Seq(
                    title,
                    comboBox
                  )
                }

  // Component for picking the background color of the card
  private def bgColorPicker: Node =
    val title = new Label("Background Color")

    // TODO: refactor these into some constants file or something
    val colorOptions = ObservableBuffer(
      "Electric Lavender" -> "#F4BBFF",
      "Ocean Blue" -> "#0077BE",
      "Fire Engine Red" -> "#CE2029",
      "Lemon Zest Zap" -> "#FFD700",
      "Mint Green" -> "#98FF98",
      "Sunset Orange" -> "#FD5E53",
      "Purple Mountains Majesty" -> "#9678B6",
      "Screamin' Green" -> "#76FF7A",
      "Sky Blue" -> "#87CEEB",
      "Frostbite" -> "#E936A7"
    )
    val backgroundColorPicker = new ComboBox[String] {
      items = colorOptions.map( _._1 )
      value = colorOptions.find(_._2 == backgroundColor).get._1
    }

    // This makes the color picker show the color of the selected option
    backgroundColorPicker.setCellFactory(
      (_) => {
        new ListCell[String]() {
          item.onChange { (_, _, colorName) =>
            if (colorName != null) {
              text = colorName
              graphic = new Rectangle {
                width = 16
                height = 16
                fill = Color.web(colorOptions.find(_._1 == colorName).get._2)
              }
            }
          }
        }
      }
    )

    backgroundColorPicker.onAction = () =>
      val backgroundColorName = backgroundColorPicker.value.value
      backgroundColor = colorOptions.find(_._1 == backgroundColorName).get._2

    new VBox {
      children = Seq(
        title,
        backgroundColorPicker
      )
    }


  private def getSeriesNamePicker: Node =
    val label = new Label("Series Name")

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



  // Gets UI components for everything besides live/local, type, source and fields.
  // these depend on the type of chart
  private def getRestOfConfigs: Node =
    val restLayoutItems =
      chartType match
        case None        => Seq[Node]()
        case Some("Barchart, numeric") =>
          Seq(
            bgColorPicker,
            xRangePicker,
          )
        case Some("Barchart, categoric") =>
          Seq(
            bgColorPicker
          )
        case Some("Plot") =>
          Seq(
            getSeriesNamePicker,
            bgColorPicker,
            new HBox() {
              children = Seq(xRangePicker, yRangePicker)
            }
          )
        case Some("Piechart") =>
          Seq(
            bgColorPicker
          )
        case Some("Time-Series Plot") =>
          Seq(
            getSeriesNamePicker,
            bgColorPicker,
            yRangePicker
          )
        case Some("Big Number") =>
          Seq(
            bgColorPicker
          )
        case _ => Seq[Node]()

    val restConfigLayout = new VBox {
      children = restLayoutItems
    }

    restConfigLayout



    // colors of individual components are to be determined by rightclicking on said component


  // Gets the bottom buttons for the window
  private def getBottomButtons: Node =
    // Create the Cancel button
    val cancelButton = new Button("Cancel")
    cancelButton.onAction = (_) => {
      // Close the window when the Cancel button is clicked
      onClose()
    }

    // Create the Confirm button
    val confirmButton = new Button("Confirm")
    confirmButton.onAction = (_) => {
      // Do some extra actions here

      println(this.plotFields)

      this.buildCard match
        case None       => ()
        case Some(card) =>
          // Close the window when the Confirm button is clicked
          this.canvasHandler.addCard(card)
          onClose()
    }

    // Create an HBox layout with the two buttons
    val buttonsLayout = new HBox {
      spacing = 10
      children = Seq(cancelButton, confirmButton)
    }

    buttonsLayout


  // If all necessary parameters are set, return a Card built from those parameters wrapped in an Option
  // If not, return None
  private def buildCard: Option[Card] =
    val commonOk =
      !Vector[Option[String]](
        title,
        chartType,
        dataSource,
      ).contains(None)

    val fieldOk =
      this.chartType match
        case Some("Plot") => plotFields._1.isDefined && plotFields._2.isDefined
        case Some(_)      => fieldNonPlot.isDefined
        case None         => false

    val specificsOk =
      this.chartType match
        case Some("Plot") => seriesName.isDefined
        case Some("Time-Series Plot") => seriesName.isDefined
        case _ => true



    if commonOk && fieldOk && specificsOk then
      val id: Int =
        initialCard match
          case Some(card) => card.id // If we are editing an existing card, use the same id
          case None       =>
            canvasHandler.generateID match
              case Some(idexists) => idexists  // If we are creating a new card, generate a new id
              case None           => throw Exception("Something went wrong with generating an id for the card")

      val pos =
        initialCard match
          case Some(card) => (card.x, card.y) // If we are editing an existing card, use the same position
          case None       => (300.0,400.0)

      val height =
        initialCard match
          case Some(card) => card.height // If we are editing an existing card, use the same height
          case None       => 300.0

      val width =
        initialCard match
          case Some(card) => card.width // If we are editing an existing card, use the same width
          case None       => 400.0

      val chartObject =
        this.chartType match // choose the right chart object
          case Some("Plot") => Plot(fields = Vector((plotFields._1.get, plotFields._2.get)), names = Vector(seriesName.get))
          case Some("Barchart, categoric") => BarChartCategoric(field = fieldNonPlot.get)
          case Some("Piechart") => PieChartCategoric(field = fieldNonPlot.get)
          case Some("Time-Series Plot") => TimeSeriesPlot(fields = Vector(fieldNonPlot.get), names = Vector(seriesName.get))
          case Some("Big Number") => BigNumber(field = fieldNonPlot.get)
          case Some(_) => throw Error("susss")
          case None => throw Error("sus sus")

      // construct the new card
      val card = Card(id = id, title = title.get, x = pos._1, y = pos._2, width = width, height = height, bgColor = this.backgroundColor, dataSource = Vector(dataSource.get), xRange = xRangePicker.getRange, yRange = yRangePicker.getRange, live = isLive, chart = chartObject)

      initialCard match // If we are editing an existing card, remove the old one before adding the new one
        case Some(icard) => canvasHandler.removeCard(icard)
        case None => ()

      Some(card)

    else
      None


end DataConfigurator

