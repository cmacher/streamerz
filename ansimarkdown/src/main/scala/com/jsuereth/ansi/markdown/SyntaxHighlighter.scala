package com.jsuereth.ansi.markdown

import java.awt.Color

import com.jsuereth.ansi.Ansi

import scala.util.matching.Regex


/**
 * Highlight syntax into ASCII Strings.
 */
object SyntaxHighlighter {

  // NOTE - Does not handle ->
  //        - String interpolation
  //        - """ strings
  //        - XML
  //        - Anything non-trival or useful.
  def ansiHighlight(code: String): String = {
    // TODO - We should do something much nicer, possibly using a real parser
    val keywords = keywordRegex
    .replaceAllIn(code, replacer = { m =>
      s"${Ansi.BOLD}${Ansi.BLUE}${m.matched}${Ansi.RESET_COLOR}"
    })
    val specials = specialRegex.replaceAllIn(keywords, replacer = { m =>
      s"${Ansi.BOLD}${Ansi.BLUE}${m.matched}${Ansi.RESET_COLOR}"
    })
    val strings = simpleStringRegex.replaceAllIn(specials, replacer = { m =>
      s"${Ansi.GREEN}${m.matched}${Ansi.RESET_COLOR}"
    })
    val stringQuotes = typeRegex.replaceAllIn(strings, replacer = { m =>
      val matched = m.matched
      s"${matched.substring(0,1)}${Ansi.CYAN}${matched.substring(1)}${Ansi.RESET_COLOR}"
    })
    xmlRegex.replaceAllIn(stringQuotes, replacer = { m =>
      val matched = m.matched
      s"${Ansi.MAGENTA}${matched}${Ansi.RESET_COLOR}"
    })
  }


  // NOTE - Borrowed from REPLesent (ASL)
  val keywordRegex: Regex =("""\b(?:abstract|case|catch|class|def|do|else|extends|final|finally|for|""" +
    """forSome|if|implicit|import|lazy|match|new|object|override|package|private|""" +
    """protected|return|sealed|super|throw|trait|try|type|val|var|while|with|yield)\b""").r
  val specialRegex: Regex = """\b(?:true|false|null|this)\b""".r
  // Our own hackery for string parsing.
  val simpleStringRegex: Regex =
    ("""("[^"]+")""").r
  // Hackery to try to grab types...
  val typeRegex: Regex =
     """[^\:]\:[\s]+([^\s\)]+)""".r
  val xmlRegex: Regex = "(<[^>]+>)".r

  // TODO - string highlighting...

  def test =
    ansiHighlight(
      s"""object Foo {
         |  val x = "Hi"
         |  val y = true
         |  def foo(x: Int): Unit = println(x)
         |  val x = <xml>test</xml>
         |
         |  for {
         |    x <- hi
         |    b <- yo
         |  } yield x + b
         |}""".stripMargin)

  def main(args: Array[String]): Unit = println(test)
}
