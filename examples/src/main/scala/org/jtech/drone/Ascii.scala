package org.jtech.drone

import java.awt.image.{DataBufferInt, BufferedImage}
import java.util.Base64
import java.util.zip.Deflater

import upickle.default.write

/**
 * - Operations get repeated a lot, need to pre-cook things:
 *     + Array is directly Strings instead of casting from char at every loop iteration
 *     + BufferedImage.getRGB() is slow: get whole pixel data in a buffer     first
 *       benchmark: http://stackoverflow.com/a/9470843
 * - chooseAsciiChar was too complex and wrong
 * - HTML is way too big:
 *     + Switching to JSON, with an array of colors and a long String of chars
 *     + Client knows resolution, will split string in lines and color the chars
 *     + JSON -> base64 -> zip
 * - Made things more functional
 */
object Ascii extends App {

  type AsciiPicture = (Vector[String], String)

  val palette = Vector(" ", ".", ",", ":", "*", "=", "+", "$", "%", "@", "A", "A", "#", "#")

  def chooseAsciiChar(color: Int, palette: Vector[String] = palette): String = {
    // Average value of RGB components
    val value = ((color & 0xff) + ((color & 0xff00) >> 8) + ((color & 0xff000) >> 16)) / 3.0
    val index = ((value / 255.0) * palette.length).toInt
    palette(index)
  }

  def shorthandColor(color: Int): String = {
    // Manual string format is way too slow: +40ms
    Integer.toHexString(color).substring(2, 8) // Trick only works with TYPE_INT_ARGB
  }

  def asciify(image: BufferedImage): (Vector[String], String) = {

    val pixels = image.getRaster.getDataBuffer.asInstanceOf[DataBufferInt].getData

    val picture = pixels.toVector.map { color =>
      (shorthandColor(color), chooseAsciiChar(color))
    }.unzip

    (picture._1, picture._2.mkString)
  }

  // uPickler is too slow ! 73ms on average
  def toJSON(asciiPicture: AsciiPicture): Array[Byte] = {
    write(asciiPicture).getBytes
  }

  def toJSON2(asciiPicture: AsciiPicture): Array[Byte] = {
    val buffer = new StringBuilder()
    buffer.append("{\"colors\":[")
    buffer.append(asciiPicture._1.map(s => "\""+s(0)+s(2)+s(4)+"\"").mkString(","))
    buffer.append("],\"chars\":\"")
    buffer.append(asciiPicture._2)
    buffer.append("\"}")
    buffer.toString.getBytes
  }

  def toBase64(input: Array[Byte]): String = {
    new String(Base64.getEncoder.encode(input))
  }

  def compress(input: Array[Byte]): Array[Byte] = {
    val deflater = new Deflater(Deflater.BEST_SPEED)
    deflater.setInput(input)
    deflater.finish

    val compressed = new Array[Byte](input.length * 2)
    val size = deflater.deflate(compressed)

    compressed.take(size)
  }

  // Nice function from http://stackoverflow.com/a/9160068
  def time[R](message: String = "")(block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    val ms = (t1 - t0) / 1000000
    val ps = if (ms > 0) "%5d/s".format(1000 / ms) else "    INF"
    println("%-12s%3dms  %s".format(message, ms, ps))
    result
  }

  def test = {
    val testImage = new BufferedImage(80, 60, BufferedImage.TYPE_INT_ARGB)
    // Fill with gradient
    for (x <- 0 until testImage.getWidth; y <- 0 until testImage.getHeight) {
      val value = ((x * 1.0 / testImage.getWidth) * 255).toInt
      testImage.setRGB(x, y, 0xff << 24 | value << 16 | value << 8 | value)
    }

    println("=" * 26)
    println(" " * 10 + "TIMES")
    println("=" * 26)

    val oldMethod = time("HTML") { com.jsuereth.image.Ascii.toCharacterColoredHtml(testImage) }
    val oldZipped = time("ZIP") { compress(oldMethod.getBytes) }
    val oldBase64 = time("Base64") { toBase64(oldZipped) }

    println("-" * 26)

    val ascii = time("ASCIIfy") { asciify(testImage) }
    val json = time("JSON") { toJSON2(ascii) }
    val zipped = time("ZIP") { compress(json) }
    val base64 = time("Base64") { toBase64(zipped) }

    println("=" * 26)
    println(" " * 10 + "SIZES")
    println("=" * 26)

    def printResult(title: String, length: Int) {
      println("%-10s%6dB %6.1fKB".format(title, length, length / 1000.0))
    }

    printResult("HTML", oldMethod.length)
    printResult("ZIP", oldZipped.length)
    printResult("Base64", oldBase64.length)

    println("-" * 26)

    printResult("JSON", json.length)
    printResult("ZIP", zipped.length)
    printResult("Base64", base64.length)
  }

  test
}
