import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}

class ImmTest(imm: Imm) extends PeekPokeTester(imm) {
  val cases = Array(
    ("hfe010113", -32), // I
    ("h00112e23", 28), // S
    ("hf4e7d4e3", -184), // B
    ("h000007b7", 0), // U
    // J
    ("h130000ef", 304))
  for ((instruction, result) <- cases) {
    poke(imm.io.instruction, instruction.U)
    expect(imm.io.result, result)
    // just for render vcd
    step(1)
  }
}

class ImmSpec extends FlatSpec with Matchers {
  behavior of "ImmSpec"

  it should "generate imm successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new Imm) { imm =>
      new ImmTest(imm)
    } should be(true)
  }
}
