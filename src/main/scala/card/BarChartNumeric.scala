package card

// Numeric should have bars with no space between, Categoric should have separate bars
case class BarChartNumeric(intervals: Int, field: String) extends Chart
