package card

/// Do these really need to separate? I could just have a single Plot class which has the option for having multiple plots
case class Plot(fields: Vector[(String, String)], names: Vector[String]) extends Chart
