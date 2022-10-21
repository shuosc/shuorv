import chisel3._
import chisel3.util._

class BranchCondition extends Module {

  val io = IO(new Bundle {
    val A = Input(Bits(32.W))
    val B = Input(Bits(32.W))
    val op = Input(UOp())
    val take = Output(Bool())
  })
  io.take := false.B
  switch(io.op) {
    is(UOp.BEQ) {
      io.take := io.A.asSInt() === io.B.asSInt()
    }
    is(UOp.BNE) {
      io.take := io.A.asSInt() =/= io.B.asSInt()
    }
    is(UOp.BLT) {
      io.take := io.A.asSInt() < io.B.asSInt()
    }
    is(UOp.BGE) {
      io.take := io.A.asSInt() >= io.B.asSInt()
    }
    is(UOp.BLTU) {
      io.take := io.A.asUInt() < io.B.asUInt()
    }
    is(UOp.BGEU) {
      io.take := io.A.asUInt() >= io.B.asUInt()
    }
  }
}

