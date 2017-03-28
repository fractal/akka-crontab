package me.lightspeed7.crontab

import java.time.LocalDateTime

import scala.util.Try
import scala.util.parsing.combinator.RegexParsers

object Crontab extends RegexParsers {

  //
  // Parser 
  // //////////////////////////
  def apply(input: String): Try[Cron] = parseAll(cron, Option(input).map(_.toUpperCase()).getOrElse("")) match {
    case Success(cron, _) ⇒ scala.util.Success(cron)
    case e: NoSuccess     ⇒ scala.util.Failure(new IllegalArgumentException(e.msg))
  }

  //
  // The Guts
  // //////////////////////////
  private def cron: Parser[Cron] = minute ~ hour ~ day ~ month ~ dow ^? {
    case (min ~ hr ~ d ~ mth ~ dow) ⇒ Cron(min, hr, d, mth, dow)
  }

  private def dowAlpha: Parser[Timing] = "[SMTWF][UOEHRA][NEDUIT]".r ^? {
    case "SUN" ⇒ Fixed(0)
    case "MON" ⇒ Fixed(1)
    case "TUE" ⇒ Fixed(2)
    case "WED" ⇒ Fixed(3)
    case "THU" ⇒ Fixed(4)
    case "FRI" ⇒ Fixed(5)
    case "SAT" ⇒ Fixed(6)
  }

  private def monthAlpha: Parser[Timing] = "[JFMASOND][AEPUCO][NBRYLGPTVC]".r ^? {
    case "JAN" ⇒ Fixed(1)
    case "FEB" ⇒ Fixed(2)
    case "MAR" ⇒ Fixed(3)
    case "APR" ⇒ Fixed(4)
    case "MAY" ⇒ Fixed(5)
    case "JUN" ⇒ Fixed(6)
    case "JUL" ⇒ Fixed(7)
    case "AUG" ⇒ Fixed(8)
    case "SEP" ⇒ Fixed(9)
    case "OCT" ⇒ Fixed(10)
    case "NOV" ⇒ Fixed(11)
    case "DEC" ⇒ Fixed(12)
  }

  private def number: Parser[String] = "[0-9]+".r

  // Handle bounded range - n-m 
  private def bounds: Parser[Timing] = number ~ "-" ~ number ^^ { case f ~ x ~ t ⇒ Range( f.toInt, t.toInt) }

  // Handle wildcard and intevals
  private def steps: Parser[Timing] = "*" ~ opt("/" ~ number) ^^ {
    case x ~ Some(y ~ z) ⇒ Steps(Seq.fill(60 / z.toInt)(z.toInt).zipWithIndex.map { case (n, i) ⇒ n * i })
    case x ~ None        ⇒ Every
  }

  private def fixed: Parser[Timing] = number ~ opt("," ~ repsep(number, ",")) ^? {
    case x ~ Some("," ~ ys) ⇒ Steps((x :: ys).map(_.toInt))
    case x ~ None           ⇒ Fixed(x.toInt)
  }

  private def nthDayOfWeeek: Parser[Timing] = number ~ "#" ~ number ^^ {
    case d ~ p ~ n ⇒ NthDow(d.toInt, n.toInt)
  }

  private def lastOf(extract: LocalDateTime ⇒ Int): Parser[Timing] = number ~ "L" ^^ {
    case d ~ l ⇒ LastDow(d.toInt)
  }

  // position parsers 
  private def minute: Parser[Timing] = steps | bounds | fixed
  private def hour: Parser[Timing] = steps | bounds | fixed
  private def day: Parser[Timing] = steps | bounds | fixed
  private def month: Parser[Timing] = steps | bounds | fixed | monthAlpha
  private def dow: Parser[Timing] = steps | nthDayOfWeeek | bounds | fixed | dowAlpha

}

//
// Model
// //////////////////////////
final case class Cron(min: Timing, hour: Timing, day: Timing, month: Timing, dayOfWeek: Timing)
final case class TimeZoneDelta(month: Int, day: Int, hour: Int, min: Int, dow: Int)

sealed trait Timing
final case object Every extends Timing
final case class Steps(list: Seq[Int]) extends Timing
final case class Range(from: Int, to: Int) extends Timing
final case class Fixed(num: Int) extends Timing
final case class NthDow(dow: Int, nth: Int) extends Timing
final case class LastDow(dow: Int) extends Timing 
//