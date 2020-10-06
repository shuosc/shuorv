import chisel3._
import chisel3.util._

// An sram
// @see https://www.chisel-lang.org/chisel3/memories.html
class SRAM extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val addr = Input(UInt(10.W))
    val mask = Input(Vec(4, Bool()))
    val dataIn = Input(Vec(4, UInt(8.W)))
    val dataOut = Output(Vec(4, UInt(8.W)))
  })

  // Create a 32-bit wide memory that is byte-masked
  val mem = SyncReadMem(1024, Vec(4, UInt(8.W)))
  // Write with mask
  mem.write(io.addr, io.dataIn, io.mask)
  io.dataOut := mem.read(io.addr, io.enable)
}

// Byte addressable SRAM
class ByteAddressedSRAM extends Module {

  import Mask._

  val io = IO(new Bundle {
    val read_mode = Input(Bool())
    val addr = Input(UInt(12.W))
    val maskLevel = Input(UInt(2.W))
    val dataIn = Input(UInt(32.W))
    val dataOut = Output(UInt(32.W))
  })

  val inner = Module(new SRAM)
  inner.io.enable := io.read_mode

  inner.io.dataIn(0) := io.dataIn(7, 0)
  inner.io.dataIn(1) := io.dataIn(15, 8)
  inner.io.dataIn(2) := io.dataIn(23, 16)
  inner.io.dataIn(3) := io.dataIn(31, 24)
  // last 2 bits are not always used
  inner.io.addr := io.addr(11, 2)
  // prevent all writes by default
  inner.io.mask := VecInit(false.B, false.B, false.B, false.B)
  io.dataOut := DontCare
  switch(io.maskLevel) {
    is(BYTE) {
      // use the last two bits to decide the mask
      switch(io.addr(1, 0)) {
        is("b00".U) {
          when(io.read_mode) {
            io.dataOut := inner.io.dataOut(0)
          }.otherwise {
            inner.io.mask := VecInit(true.B, false.B, false.B, false.B)
          }
        }
        is("b01".U) {
          when(io.read_mode) {
            io.dataOut := Cat(0.U(24.W), inner.io.dataOut(1))
          }.otherwise {
            inner.io.mask := VecInit(false.B, true.B, false.B, false.B)
          }
        }
        is("b10".U) {
          when(io.read_mode) {
            io.dataOut := Cat(0.U(24.W), inner.io.dataOut(2))
          }.otherwise {
            inner.io.mask := VecInit(false.B, false.B, true.B, false.B)
          }
        }
        is("b11".U) {
          when(io.read_mode) {
            io.dataOut := Cat(0.U(24.W), inner.io.dataOut(3))
          }.otherwise {
            inner.io.mask := VecInit(false.B, false.B, false.B, true.B)
          }
        }
      }
    }
    is(HALF_WORD) {
      // the last 1 bit must be zero, currently we just ignore it
      // since the spec says the behaviour in this situation is implementation defined
      // todo: either support unaligned write or raise an exception
      when(io.read_mode) {
        io.dataOut := Mux(io.addr(1),
          Cat(0.U(16.W), inner.io.dataOut(3), inner.io.dataOut(2)),
          Cat(0.U(16.W), inner.io.dataOut(1), inner.io.dataOut(0))
        )
      }.otherwise {
        inner.io.mask := Mux(io.addr(1),
          VecInit(false.B, false.B, true.B, true.B),
          VecInit(true.B, true.B, false.B, false.B)
        )
      }
    }
    is(WORD) {
      // the last 2 bits must be zero, currently we just ignore it
      // since the spec says the behaviour in this situation is implementation defined
      // todo: either support unaligned write or raise an exception
      when(io.read_mode) {
        io.dataOut := Cat(inner.io.dataOut(3), inner.io.dataOut(2), inner.io.dataOut(1), inner.io.dataOut(0))
      }.otherwise {
        inner.io.mask := VecInit(true.B, true.B, true.B, true.B)
      }
    }
  }
}

object Mask extends Enumeration {
  val BYTE = "b00".U
  val HALF_WORD = "b01".U
  val WORD = "b10".U
  val NONE = "b11".U
}
