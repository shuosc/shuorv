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
    is(ADD) {
      io.result := io.A.asUInt + io.B.asUInt
    }
    is(SUB) {
      io.result := io.A.asUInt - io.B.asUInt
    }
    is(SLL) {
      io.result := io.A << io.B(4, 0).asUInt
    }
    is(SLT) {
      io.result := io.A.asSInt < io.B.asSInt
    }
    is(SLTU) {
      io.result := io.A.asUInt < io.B.asUInt
    }
    is(XOR) {
      io.result := io.A ^ io.B
    }
    is(SRL) {
      io.result := io.A >> io.B(4, 0).asUInt
    }
    is(SRA) {
      io.result := (io.A.asSInt >> io.B(4, 0)).asUInt()
    }
    is(OR) {
      io.result := io.A | io.B
    }
    is(AND) {
      io.result := io.A & io.B
    }
  }
}

object ALUOperation extends Enumeration {
  val ADD = "b0000".U
  val SUB = "b1000".U
  val SLL = "b0001".U
  val SLT = "b0010".U
  val SLTU = "b0011".U
  val XOR = "b0100".U
  val SRL = "b0101".U
  val SRA = "b1101".U
  val OR = "b0110".U
  val AND = "b0111".U
}
