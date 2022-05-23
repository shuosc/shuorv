import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}

class TopTest(top: Top) extends PeekPokeTester(top) {
  var expected = List(
    BigInt("1")
  )
  var last = BigInt(0)
  var countDown = 65536
  while (!expected.isEmpty) {
    val next = peek(top.io.gpio)
    if (next != last) {
      expect(top.io.gpio, expected.head)
      expected = expected.tail
      countDown = 65536
    } else {
      countDown -= 1
      if (countDown <= 0) {
        fail
        expected = List()
      }
    }
    last = next
    step(1)
  }
}

class TopSpec extends FlatSpec with Matchers {
  behavior of "TopSpec"

  it should "compute successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new Top) { top =>
      new TopTest(top)
    } should be(true)
  }
}
