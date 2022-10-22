import chisel3._
import chisel3.util._

class ALU extends Module {

  val io = IO(new Bundle {
    val A = Input(Bits(32.W))
    val B = Input(Bits(32.W))
    val op = Input(UOp())
    val result = Output(Bits(32.W))
  })
  io.result := 0xdead.U
  switch(io.op) {
    is(UOp.ADD) {
      io.result := io.A.asUInt + io.B.asUInt
    }
    is(UOp.SUB) {
      io.result := io.A.asUInt - io.B.asUInt
    }
    is(UOp.SLL) {
      io.result := io.A << io.B(4, 0).asUInt
    }
    is(UOp.SLT) {
      io.result := io.A.asSInt < io.B.asSInt
    }
    is(UOp.SLTU) {
      io.result := io.A.asUInt < io.B.asUInt
    }
    is(UOp.XOR) {
      io.result := io.A ^ io.B
    }
    is(UOp.SRL) {
      io.result := io.A >> io.B(4, 0).asUInt
    }
    is(UOp.SRA) {
      io.result := (io.A.asSInt >> io.B(4, 0)).asUInt()
    }
    is(UOp.OR) {
      io.result := io.A | io.B
    }
    is(UOp.AND) {
      io.result := io.A & io.B
    }
  }
}

