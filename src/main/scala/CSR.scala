import chisel3._
import chisel3.util._

class CSR extends Module {
  val io = IO(new CSRBundle)

  val mvendorid = RegInit(0.U(32.W))
  val marchid = RegInit(0.U(32.W))
  val mimpid = RegInit(0.U(32.W))
  val mhardid = RegInit(0.U(32.W))

  val mstatus = RegInit(1.U(32.W) << MIRField.MIE)
  val misa = "h40000100".U //
  val mie = RegInit(0xfff.U(12.W))
  val mtvec = RegInit("h80010000".U(32.W))
  // val mcountinhibit =

  val mscratch = RegInit(0.U(32.W))
  val mepc = RegInit(0.U(32.W))
  val mcause = RegInit(0.U(32.W))
  val mip = Cat(0.U(4.W), io.timerInterruptPending, 0.U(7.W))
  val mtval = RegInit(0.U(32.W))

  val mcycle = RegInit(0.U(32.W))
  val minstret = RegInit(0.U(32.W))
  // todo: replace these with a table driven way
  io.outputValue := 0xdead.U
  when(io.writeEn) {
    switch(io.address) {
      is(CSRAddress.mtvec) {
        mtvec := io.inputValue
      }
      is(CSRAddress.mie) {
        mie := io.inputValue
      }
      is(CSRAddress.mepc) {
        mepc := io.inputValue
      }
      is(CSRAddress.mscratch) {
        mscratch := io.inputValue
      }
      is(CSRAddress.mcause) {
        mcause := io.inputValue
      }
      is(CSRAddress.mtval) {
        mtval := io.inputValue
      }
      // todo:mcountinhibit mcycle minstret
    }
  }.otherwise {
    switch(io.address) {
      is(CSRAddress.mtvec) {
        io.outputValue := mtvec
      }
      is(CSRAddress.mie) {
        io.outputValue := mie
      }
      is(CSRAddress.mip) {
        io.outputValue := mip
      }
      is(CSRAddress.mepc) {
        io.outputValue := mepc
      }
      is(CSRAddress.mscratch) {
        io.outputValue := mscratch
      }
      is(CSRAddress.mcause) {
        io.outputValue := mcause
      }
      is(CSRAddress.mtval) {
        io.outputValue := mtval
      }
      is(CSRAddress.mvendorid) {
        io.outputValue := mvendorid
      }
      is(CSRAddress.marchid) {
        io.outputValue := marchid
      }
      is(CSRAddress.mimpid) {
        io.outputValue := mimpid
      }
      is(CSRAddress.mhartid) {
        io.outputValue := mhardid
      }
      is(CSRAddress.misa) {
        io.outputValue := misa
      }
      is(CSRAddress.mcycle) {
        io.outputValue := mcycle
      }
      is(CSRAddress.minstret) {
        io.outputValue := minstret
      }
    }
  }

  io.interruptPending := mstatus(MIRField.MIE) & (mie & mip).orR()
  io.pcOnInterrupt := mtvec

  when(io.flipStatusMIE) {
    when(mstatus(MIRField.MIE) === 1.B) {
      mstatus := mstatus & (~(1.U(32.W) << MIRField.MIE).asUInt()).asUInt()
    }.otherwise {
      mstatus := mstatus | (1.U(32.W) << MIRField.MIE).asUInt()
    }
  }

  switch(mtvec(1, 0)) {
    is("b00".U) {
      io.pcOnInterrupt := mtvec
    }
    is("b01".U) {
      io.pcOnInterrupt := mtvec + (mcause(30, 0) << 2).asUInt()
    }
  }
}

object CSRAddress extends Enumeration {
  val mvendorid = 0xF11.U
  val marchid = 0xF12.U
  val mimpid = 0xF13.U
  val mhartid = 0XF14.U
  val mstatus = 0x300.U
  val misa = 0x301.U
  val mie = 0x304.U
  val mtvec = 0x305.U
  val mcountinhibit = 0x320.U
  val mscratch = 0x340.U
  val mepc = 0x341.U
  val mcause = 0x342.U
  val mip = 0x344.U
  val mtval = 0x343.U
  val mcycle = 0xB00.U
  val minstret = 0xB02.U
}

object MIRField extends Enumeration {
  val MIE = 3
}

class CSRIOBundle extends Bundle {
  val writeEn = Input(Bool())
  val address = Input(UInt(12.W))
  val inputValue = Input(UInt(32.W))
  val outputValue = Output(UInt(32.W))
}

class CSRBundle extends CSRIOBundle {
  val interruptPending = Output(Bool())
  val flipStatusMIE = Input(Bool())
  val pcOnInterrupt = Output(UInt(32.W))
  val timerInterruptPending = Input(Bool())
}

object InterruptType extends Enumeration {
  val Timer = 11
}
