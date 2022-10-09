import chisel3._
import chisel3.util._

class CPUBundle extends Bundle {
  val programROMBundle = Flipped(new ProgramROMBundle)
  val dataBusBundle = Flipped(new DataBusBundle)
  val timerInterruptPending = Input(Bool())
}

class CPU extends Module {

  //import CommandType._

  val io = IO(new CPUBundle)

  // start up address here are from BSD's CPU
  val pc = RegInit("h80000000".U(32.W))
  pc := pc + 4.U
  io.programROMBundle.address := pc

  val instruction = Wire(UInt(32.W))
  instruction := io.programROMBundle.value

  val decoder = Wire(new Decoder())
  decoder.decode(instruction)

  io.dataBusBundle.readMode := true.B
  io.dataBusBundle.maskLevel := Mask.WORD
  io.dataBusBundle.address := 0.U
  io.dataBusBundle.dataIn := 0.U

  val csr = Module(new CSR)
  csr.io.writeEn := false.B
  csr.io.flipStatusMIE := false.B
  csr.io.address := 0.U
  csr.io.inputValue := 0.U
  csr.io.timerInterruptPending := io.timerInterruptPending

  val regFile = Module(new RegFile)
  regFile.io.addressInput := decoder.rd_addr
  regFile.io.addressA := decoder.rs1_addr
  regFile.io.addressB := decoder.rs2_addr
  regFile.io.input := 0xdead.U
  regFile.io.writeEnable := false.B

  val branchCondition = Module(new BranchCondition)
  branchCondition.io.A := regFile.io.outputA
  branchCondition.io.B := regFile.io.outputB
  branchCondition.io.op := decoder.uop

  val alu = Module(new ALU)
  alu.io.A := 0xdead.U
  alu.io.B := 0xdead.U
  alu.io.op := uOP.NOP

  val stall = RegInit(false.B)

  when(stall) {
    stall := false.B
    regFile.io.writeEnable := true.B
    io.dataBusBundle.address := (regFile.io.outputA.asSInt() + decoder.imm_data.asSInt()).asUInt()
    io.dataBusBundle.maskLevel := decoder.maskLevel
    // second part of load
    switch(decoder.uop) {
      // LB
      is(uOP.LB) {
        regFile.io.input := io.dataBusBundle.dataOut(7, 0)
      }
      // LH
      is(uOP.LH) {
        regFile.io.input := io.dataBusBundle.dataOut(15, 0)
      }
      // LW
      is(uOP.LW) {
        regFile.io.input := io.dataBusBundle.dataOut
      }
      // LBU
      is(uOP.LBU) {
        regFile.io.input := io.dataBusBundle.dataOut(7, 0).asUInt()
      }
      // LHU
      is(uOP.LHU) {
        regFile.io.input := io.dataBusBundle.dataOut(15, 0).asUInt()
      }
    }
  }.elsewhen(csr.io.interruptPending) {
    csr.io.writeEn := true.B
    csr.io.flipStatusMIE := true.B
    csr.io.address := CSRAddress.mepc
    csr.io.inputValue := pc + 4.U
    pc := csr.io.pcOnInterrupt
  }.otherwise {
    switch(decoder.inst_type) {
      is(InstType.lui) {
        regFile.io.input := decoder.imm_data
        regFile.io.writeEnable := true.B
      }
      is(InstType.auipc) {
        alu.io.A := decoder.imm_data
        alu.io.B := pc
        alu.io.op := uOP.ADD

        regFile.io.input := alu.io.result
        regFile.io.writeEnable := true.B
      }
      is(InstType.jal) {
        alu.io.A := 4.U
        alu.io.B := pc
        alu.io.op := uOP.ADD

        regFile.io.input := alu.io.result
        regFile.io.writeEnable := true.B
        pc := (pc.asSInt() + decoder.imm_data.asSInt()).asUInt()
      }
      is(InstType.jalr) {
        alu.io.A := regFile.io.outputA
        alu.io.B := decoder.imm_data
        alu.io.op := uOP.ADD

        regFile.io.input := pc + 4.U
        regFile.io.writeEnable := true.B
        pc := alu.io.result & "hfffffffe".U
      }
      is(InstType.branch) {
        when(branchCondition.io.take) {
          pc := (pc.asSInt() + decoder.imm_data.asSInt()).asUInt()
        }
      }
      is(InstType.load) {
        // first part of load: send a load command to addressSpace
        io.dataBusBundle.address := (regFile.io.outputA.asSInt() + decoder.imm_data.asSInt()).asUInt()
        io.dataBusBundle.maskLevel := decoder.maskLevel
        // stall once for waiting for the load result come out
        pc := pc
        stall := true.B
      }
      is(InstType.store) {
        io.dataBusBundle.address := (regFile.io.outputA.asSInt() + decoder.imm_data.asSInt()).asUInt()
        io.dataBusBundle.readMode := false.B
        io.dataBusBundle.maskLevel := decoder.maskLevel
        io.dataBusBundle.dataIn := regFile.io.outputB
      }
      is(InstType.calculate) {
        regFile.io.writeEnable := true.B
        alu.io.A := regFile.io.outputA
        alu.io.B := Mux(decoder.need_imm,decoder.imm_data,regFile.io.outputB)
        alu.io.op := decoder.uop
        regFile.io.input := alu.io.result
      }
      is(InstType.fence) {
        // we don't have multicore, pipeline, etc. now, so we don't need this command
      }
      is(InstType.system) {
        switch(decoder.uop) {
          is(uOP.MRET) {
            csr.io.flipStatusMIE := true.B
            csr.io.address := CSRAddress.mepc
            pc := csr.io.outputValue
          }
        }
      }
    }
  }
}

//object CommandType extends Enumeration {
//  val LUI = "b01101".U
//  val AUIPC = "b00101".U
//  val JAL = "b11011".U
//  val JALR = "b11001".U
//  val BRANCH = "b11000".U
//  val LOAD = "b00000".U
//  val STORE = "b01000".U
//  val CALCULATE_IMM = "b00100".U
//  val CALCULATE_REG = "b01100".U
//  val FENCE = "b00011".U
//  val SYSTEM = "b11100".U
//}

//object SystemCommand extends Enumeration {
//  val MRET = "h302".U
//  val ECALL = "b000".U
//  val EBREAK = "b001".U
//}
