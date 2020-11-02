import chisel3._
import chisel3.util._

class Timer extends Module {
  val io = IO(new DataBusBundle {
    val interruptPending = Output(Bool())
  })
  val mtime = RegInit(0.U(64.W))
  val mtimecmp = RegInit("hffffffffffffffff".U(64.W))
  io.interruptPending := mtime >= mtimecmp
  mtime := mtime + 1.U

  io.dataOut := "hdead".U(32.W)
  // todo: support read/write in less than a word, ie. support maskLevel
  when(io.read_mode) {
    switch(io.addr) {
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
    }
  }.otherwise {
    switch(io.addr) {
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
    }
  }
}
