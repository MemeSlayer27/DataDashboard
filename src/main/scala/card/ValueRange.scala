package card

case class ValueRange(start: Option[Double], end: Option[Double]):
  def inRange(x: Double): Boolean = (start, end) match
    case (Some(s), Some(e)) => s <= x && x <= e
    case (Some(s), None) => s <= x
    case (None, Some(e)) => x <= e
    case (None, None) => true

end ValueRange
