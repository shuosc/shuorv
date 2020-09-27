import chisel3._
import chisel3.util._

class ALU extends Module {

  import ALUOperation._

  val io = IO(new Bundle {
    val A = Input(Bits(32.W))
    val B = Input(Bits(32.W))
    val op = Input(Bits(4.W))
    val result = Output(Bits(32.W))
  })
  io.result := 0xdead.U
  switch(io.op) {
    is(ADD.U) {
      io.result := io.A.asUInt + io.B.asUInt
    }
    is(SUB.U) {
      io.result := io.A.asUInt - io.B.asUInt
    }
    is(SHL.U) {
      io.result := io.A << io.B(4, 0).asUInt
    }
    is(SLT.U) {
      io.result := io.A.asSInt < io.B.asSInt
    }
    is(SLTU.U) {
      io.result := io.A.asUInt < io.B.asUInt
    }
    is(XOR.U) {
      io.result := io.A ^ io.B
    }
    is(SRL.U) {
      io.result := io.A >> io.B(4, 0).asUInt
    }
    is(SRA.U) {
      io.result := (io.A.asSInt >> io.B(4, 0)).asUInt()
    }
    is(OR.U) {
      io.result := io.A | io.B
    }
    is(AND.U) {
      io.result := io.A & io.B
    }
  }
}

object ALUOperation extends Enumeration {
  val ADD = "b0000"
  val SUB = "b1000"
  val SHL = "b0001"
  val SLT = "b0010"
  val SLTU = "b0011"
  val XOR = "b0100"
  val SRL = "b0101"
  val SRA = "b1101"
  val OR = "b0110"
  val AND = "b0111"
}
