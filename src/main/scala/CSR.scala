import chisel3._
import chisel3.util._

class CSR extends Module {
  val io = IO(new CSRBundle)

  val mie = RegInit(0xfff.U(12.W))
  val mscratch = RegInit(1.U(32.W) << MIRField.MIE)
  val mtvec = RegInit("h80010000".U(32.W))
  val mcause = RegInit(0.U(32.W))
  val mip = Cat(0.U(4.W), io.timerInterruptPending, 0.U(7.W))
  val mepc = RegInit(0.U(32.W))
  // todo: replace these with a table driven way
  io.output_value := 0xdead.U
  when(io.write_en) {
    switch(io.address) {
      is(CSRAddress.mtvec) {
        mtvec := io.input_value
      }
      is(CSRAddress.mie) {
        mie := io.input_value
      }
      is(CSRAddress.mepc) {
        mepc := io.input_value
      }
    }
  }.otherwise {
    switch(io.address) {
      is(CSRAddress.mtvec) {
        io.output_value := mtvec
      }
      is(CSRAddress.mie) {
        io.output_value := mie
      }
      is(CSRAddress.mip) {
        io.output_value := mip
      }
      is(CSRAddress.mepc) {
        io.output_value := mepc
      }
    }
  }

  io.interruptPending := mscratch(MIRField.MIE) & (mie & mip).orR()
  io.pcOnInterrupt := mtvec

  when(io.flipStatusMIE) {
    when(mscratch(MIRField.MIE) === 1.B) {
      mscratch := mscratch & (~(1.U(32.W) << MIRField.MIE).asUInt()).asUInt()
    }.otherwise {
      mscratch := mscratch | (1.U(32.W) << MIRField.MIE).asUInt()
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
  val mie = 0x304.U
  val mtvec = 0x305.U
  val mscratch = 0x340.U
  val mepc = 0x341.U
  val mcause = 0x342.U
  val mip = 0x344.U
}

object MIRField extends Enumeration {
  val MIE = 3
}

class CSRIOBundle extends Bundle {
  val write_en = Input(Bool())
  val address = Input(UInt(12.W))
  val input_value = Input(UInt(32.W))
  val output_value = Output(UInt(32.W))
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

