import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}

class TopTest(top: Top) extends PeekPokeTester(top) {
  step(1)
  poke(top.io.reset, 0)
  step(1)
  poke(top.io.reset, 1)
  step(1)
  for (i <- 0 to 100) {
    step(10)
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
