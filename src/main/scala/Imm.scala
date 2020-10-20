import chisel3._
import chisel3.util._

class Imm extends Module {
  val io = IO(new Bundle {
    val instruction = Input(Bits(32.W))
    val result = Output(SInt(32.W))
  })
  val I = io.instruction(31, 20).asSInt
  val S = Cat(io.instruction(31, 25), io.instruction(11, 7)).asSInt
  val B = Cat(io.instruction(31), io.instruction(7), io.instruction(30, 25), io.instruction(11, 8), 0.U(1.W)).asSInt
  val U = Cat(io.instruction(31, 12), 0.U(12.W)).asSInt
  val J = Cat(io.instruction(31), io.instruction(19, 12), io.instruction(20), io.instruction(30, 21), 0.U(1.W)).asSInt
  io.result := 0xdead.S(32.W)
  switch(io.instruction(6, 2)) {
    is("b00100".U) {
      io.result := I
    }
    is("b00000".U) {
      io.result := I
    }
    is("b01000".U) {
      io.result := S
    }
    is("b11000".U) {
      io.result := B
    }
    is("b01101".U) {
      io.result := U
    }
    is("b00101".U) {
      io.result := U
    }
    is("b11011".U) {
      io.result := J
    }
  }
}
