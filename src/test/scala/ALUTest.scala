import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}

class ALUTest(alu: ALU) extends PeekPokeTester(alu) {

  import ALUOperation._

  val cases = Array(
    (1, 2, ADD, 3.U),
    (3, 2, SUB, 1.U),
    (3, 2, SHL, 12.U),
    (2, 3, SLT, 1.U),
    (2, -3, SLT, 0.U),
    (3, 2, SLT, 0.U),
    (3, -2, SLT, 0.U),
    (3, 2, SLTU, 0.U),
    (2, 3, SLTU, 1.U),
    (9, 3, XOR, 0xA.U),
    (0x8000000F, 1, SRL, "h40000007".U),
    (0x8000000F, 1, SRA, "hc0000007".U),
    (9, 3, OR, 0xB.U),
    (9, 3, AND, 0x1.U))
  for ((a, b, op, result) <- cases) {
    poke(alu.io.A, a)
    poke(alu.io.B, b)
    poke(alu.io.op, op)
    expect(alu.io.result, result)
    // just for render vcd
    step(1)
  }
}

class ALUSpec extends FlatSpec with Matchers {
  behavior of "ALUSpec"

  it should "compute successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new ALU) { alu =>
      new ALUTest(alu)
    } should be(true)
  }
}
