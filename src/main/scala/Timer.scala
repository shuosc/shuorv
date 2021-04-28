import chisel3._
import chisel3.util._

class Timer extends Module {
  val io = IO(new DataBusBundle {
    val interruptPending = Output(Bool())
  })
  val mtime = RegInit(0.U(64.W))
  val mtimecmp = RegInit("hffffffffffffffff".U(64.W))

  // Timer Usage:
  // 
  // [0]: timer enable
  // [1]: timer Int enable
  // [2]: timer Int pending, write 1 to clear it
  // [3]: 64 bit mode
  val mtimectrl = RegInit(0.U(32.W))

  io.interruptPending := mtimectrl(2) & mtimectrl(1)

  // todo: add 64 bit mode support 
  val timeUp = mtime(31,0) >= mtimecmp(31, 0)

  when(mtimectrl(0)) {
    mtime := mtime + 1.U
  }

  when(timeUp) {
    mtime := 0.U(64.W)
    mtimectrl := Cat(mtimectrl(31, 3), "b1".U, mtimectrl(1), "b0".U)
  }

  io.dataOut := "hdead".U(32.W)
  // todo: support read/write in less than a word, ie. support maskLevel
  when(io.readMode) {
    switch(io.address(15, 0)) {
      is("hbff8".U) {
        io.dataOut := mtime(31, 0)
      }
      is("hbffc".U) {
        io.dataOut := mtime(63, 32)
      }
      is("h4000".U) {
        io.dataOut := mtimecmp(31, 0)
      }
      is("h4004".U) {
        io.dataOut := mtimecmp(63, 32)
      }
      is("h0000".U) {
        io.dataOut := mtimectrl
      }
    }
  }.otherwise {
    switch(io.address(15, 0)) {
      is("hbff8".U) {
        mtime := Cat(mtime(63, 32), io.dataIn)
      }
      is("hbffc".U) {
        mtime := Cat(io.dataIn, mtime(31, 0))
      }
      is("h4000".U) {
        mtimecmp := Cat(mtimecmp(63, 32), io.dataIn)
      }
      is("h4004".U) {
        mtimecmp := Cat(io.dataIn, mtimecmp(31, 0))
      }
      is("h0000".U) {
        mtimectrl := Cat(io.dataIn(31, 3), mtimectrl(2) & ~io.dataIn(2), io.dataIn(1, 0))
      }
    }
  }
}
