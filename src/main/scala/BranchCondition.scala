import chisel3._
import chisel3.util._

class BranchCondition extends Module {

//  import BranchType._

  val io = IO(new Bundle {
    val A = Input(Bits(32.W))
    val B = Input(Bits(32.W))
    val op = Input(uOP())
    val take = Output(Bool())
  })
  io.take := false.B
  switch(io.op) {
    is(uOP.BEQ) {
      io.take := io.A.asSInt() === io.B.asSInt()
    }
    is(uOP.BNE) {
      io.take := io.A.asSInt() =/= io.B.asSInt()
    }
    is(uOP.BLT) {
      io.take := io.A.asSInt() < io.B.asSInt()
    }
    is(uOP.BGE) {
      io.take := io.A.asSInt() >= io.B.asSInt()
    }
    is(uOP.BLTU) {
      io.take := io.A.asUInt() < io.B.asUInt()
    }
    is(uOP.BGEU) {
      io.take := io.A.asUInt() >= io.B.asUInt()
    }
  }
}

//object BranchType extends Enumeration {
//  val EQ = "b000".U
//  val NE = "b001".U
//  val LT = "b100".U
//  val GE = "b101".U
//  val LTU = "b110".U
//  val GEU = "b111".U
//}
