import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}

class TopTest(top: Top) extends PeekPokeTester(top) {
  for (i <- 0 to 8) {
    step(1)
  }
  expect(top.io.gpio, 5.U(32.W))
  for (i <- 0 to 8) {
    step(1)
  }
  expect(top.io.gpio, 0xf.U(32.W))
  for (i <- 0 to 12) {
    step(1)
  }
  expect(top.io.gpio, 0xe.U(32.W))
}

class TopSpec extends FlatSpec with Matchers {
  behavior of "TopSpec"

  it should "compute successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new Top) { top =>
      new TopTest(top)
    } should be(true)
  }
}
