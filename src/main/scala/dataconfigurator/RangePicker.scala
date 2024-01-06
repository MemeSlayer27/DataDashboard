package dataconfigurator

import scalafx.scene.control.{CheckBox, Label, Spinner}
import scalafx.scene.layout.VBox
import card.ValueRange

/**
 * A range picker that allows the user to select a minimum and maximum value.
 * The user can also choose to not have a minimum or maximum value.
 */

class RangePicker(title: String) extends VBox {
  val minValueSpinner = new Spinner[Int](Int.MinValue, Int.MaxValue, 0) {
    editable = true
  }
  val maxValueSpinner = new Spinner[Int](Int.MinValue, Int.MaxValue, 0) {
    editable = true
  }
  val noMinCheckBox = new CheckBox("No minimum value")
  val noMaxCheckBox = new CheckBox("No maximum value")

  noMinCheckBox.selected = true
  noMaxCheckBox.selected = true

  minValueSpinner.disable <== noMinCheckBox.selected
  maxValueSpinner.disable <== noMaxCheckBox.selected

  children = Seq(
    new Label(title),
    new Label("Minimum value:"), minValueSpinner, noMinCheckBox,
    new Label("Maximum value:"), maxValueSpinner, noMaxCheckBox
  )

 def getRange: Option[ValueRange] = {
    val min = if (noMinCheckBox.selected()) None else Some(minValueSpinner.value())
    val max = if (noMaxCheckBox.selected()) None else Some(maxValueSpinner.value())
    Some(ValueRange(min.map(_.toDouble), max.map(_.toDouble)))
  }
}
