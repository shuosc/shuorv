import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}

class TopTest(top: Top) extends PeekPokeTester(top) {
//  todo
}

class TopSpec extends FlatSpec with Matchers {
  behavior of "TopSpec"

  it should "compute successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new Top) { top =>
      new TopTest(top)
    } should be(true)
  }
}
