import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}

class RegFileTest(regFile: RegFile) extends PeekPokeTester(regFile) {
  val cases = Array(
    // addressA, addressB, addressC, input, writeEnable, expectedValueA, expectedValueB
    // x1 = 1
    (0, 0, 1, 1, true, 0, 0),
    // x1 = 2
    (0, 0, 1, 2, true, 0, 0),
    // assert(x1 == 2)
    (1, 0, 0, 0, false, 2, 0),
    // x2 = 1
    (0, 0, 2, 1, true, 0, 0),
    // assert(x1 == 2, x2 == 1)
    (1, 2, 0, 0, false, 2, 1))
  for ((addressA, addressB, addressC, input, writeEnable, expectedValueA, expectedValueB) <- cases) {
    poke(regFile.io.addressA, addressA)
    poke(regFile.io.addressB, addressB)
    poke(regFile.io.addressInput, addressC)
    poke(regFile.io.input, input)
    poke(regFile.io.writeEnable, writeEnable)
    step(1)
    if (expectedValueA != 0) {
      expect(regFile.io.outputA, expectedValueA)
    }
    if (expectedValueB != 0) {
      expect(regFile.io.outputB, expectedValueB)
    }
  }
}

class RegFileSpec extends FlatSpec with Matchers {
  behavior of "RegFileSpec"

  it should "read and write successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new RegFile) { regFile =>
      new RegFileTest(regFile)
    } should be(true)
  }
}
