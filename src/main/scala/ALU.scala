import chisel3._
import chisel3.util._

class ALU extends Module {

//  import ALUOperation._

  val io = IO(new Bundle {
    val A = Input(Bits(32.W))
    val B = Input(Bits(32.W))
    val op = Input(uOP())
    val result = Output(Bits(32.W))
  })
  io.result := 0xdead.U
  switch(io.op) {
    is(uOP.ADD) {
      io.result := io.A.asUInt + io.B.asUInt
    }
    is(uOP.SUB) {
      io.result := io.A.asUInt - io.B.asUInt
    }
    is(uOP.SLL) {
      io.result := io.A << io.B(4, 0).asUInt
    }
    is(uOP.SLT) {
      io.result := io.A.asSInt < io.B.asSInt
    }
    is(uOP.SLTU) {
      io.result := io.A.asUInt < io.B.asUInt
    }
    is(uOP.XOR) {
      io.result := io.A ^ io.B
    }
    is(uOP.SRL) {
      io.result := io.A >> io.B(4, 0).asUInt
    }
    is(uOP.SRA) {
      io.result := (io.A.asSInt >> io.B(4, 0)).asUInt()
    }
    is(uOP.OR) {
      io.result := io.A | io.B
    }
    is(uOP.AND) {
      io.result := io.A & io.B
    }
  }
}

//object ALUOperation extends Enumeration {
//  val ADD = "b0000".U
//  val SUB = "b1000".U
//  val SLL = "b0001".U
//  val SLT = "b0010".U
//  val SLTU = "b0011".U
//  val XOR = "b0100".U
//  val SRL = "b0101".U
//  val SRA = "b1101".U
//  val OR = "b0110".U
//  val AND = "b0111".U
//}
