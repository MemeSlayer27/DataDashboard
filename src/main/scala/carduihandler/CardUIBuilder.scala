package carduihandler

import canvas.CanvasHandler
import carduihandler.*
import card.*

// Factory for CardUIHandler
class CardUIBuilder(canvas: CanvasHandler, currentDashboard: String):

  // Build a CardUIHandler based on the type of the chart
  def buildComponent(card: Card): CardUIHandler =
    card.chart match
      case _: TimeSeriesPlot    => TimeSeriesPlotComponent(card, canvas, currentDashboard + card.id + ".json")
      case _: Plot              => PlotChartComponent(card, canvas, currentDashboard + card.id + ".json")
      case _: PieChartCategoric => PieChartComponent(card, canvas, currentDashboard + card.id + ".json")
      case _: BarChartCategoric => CategoricBarChartComponent(card, canvas, currentDashboard + card.id + ".json")
      case _: BigNumber         => BigNumberComponent(card, canvas, currentDashboard + card.id + ".json")
      case _                    => throw new Exception("Chart type not supported")


end CardUIBuilder

