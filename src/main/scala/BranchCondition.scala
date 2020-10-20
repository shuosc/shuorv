import chisel3._
import chisel3.util._

class BranchCondition extends Module {

  import BranchType._

  val io = IO(new Bundle {
    val A = Input(Bits(32.W))
    val B = Input(Bits(32.W))
    val op = Input(Bits(3.W))
    val take = Output(Bool())
  })
  io.take := false.B
  switch(io.op) {
    is(EQ) {
      io.take := io.A.asSInt() === io.B.asSInt()
    }
    is(NE) {
      io.take := io.A.asSInt() =/= io.B.asSInt()
    }
    is(LT) {
      io.take := io.A.asSInt() < io.B.asSInt()
    }
    is(GE) {
      io.take := io.A.asSInt() >= io.B.asSInt()
    }
    is(LTU) {
      io.take := io.A.asUInt() < io.B.asUInt()
    }
    is(GEU) {
      io.take := io.A.asUInt() >= io.B.asUInt()
    }
  }
}

object BranchType extends Enumeration {
  val EQ = "b000".U
  val NE = "b001".U
  val LT = "b100".U
  val GE = "b101".U
  val LTU = "b110".U
  val GEU = "b111".U
}
