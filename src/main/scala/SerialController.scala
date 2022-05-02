import chisel3._
import chisel3.util._

object TransitionState extends Enumeration {
  val IDLE = "b00".U(2.W)
  val START = "b01".U(2.W)
  val BITS = "b10".U(2.W)
  val PARITY = "b11".U(2.W)
}

object Registers extends Enumeration {
  val DATA = "b00".U(2.W)
  val STATUS = "b01".U(2.W)
}

object CountDownCalculator {
    def countDownBits(maxValue: Int): Int = {
        var result = 1;
        var currentMax = 1;
        while (currentMax < maxValue) {
            result += 1;
            currentMax <<= 1;
            currentMax |= 1;
        }
        return result;
    }
}

class SerialTransmitController(freqIn: Int, freqOut: Int, bufferWidthBits: Int) extends Module {
    val io = IO(new DataBusBundle {
        val txWire = Output(Bool())
    })
    val transmitBuffer = Reg(Vec(1 << bufferWidthBits, UInt(8.W)))
    val transmitBufferIndexToClient = RegInit(0.U(bufferWidthBits.W))
    val transmitBufferIndexFromHost = RegInit(0.U(bufferWidthBits.W))
    val statusOutput = Cat(transmitBufferIndexFromHost, transmitBufferIndexToClient)
    val byteTransmitting = transmitBuffer(transmitBufferIndexToClient)
    val state = RegInit(TransitionState.IDLE)
    val transitionCountDown = RegInit(0.U(CountDownCalculator.countDownBits(freqIn / freqOut - 1).W))
    val bitTransmittingIndex = Reg(UInt(3.W))

    io.txWire := false.B
    io.dataOut := 0xdead.U
    switch(state) {
        is(TransitionState.IDLE) {
            io.txWire := true.B
            when(transitionCountDown =/= 0.U) {
                transitionCountDown := transitionCountDown - 1.U
            }.elsewhen(transmitBufferIndexFromHost =/= transmitBufferIndexToClient) {
                state := TransitionState.START
                transitionCountDown := (freqIn / freqOut - 1).U
            }
        }
        is(TransitionState.START) {
            when(transitionCountDown === 0.U) {
                io.txWire := false.B
                state := TransitionState.BITS
                bitTransmittingIndex := 0.U
                transitionCountDown := (freqIn / freqOut - 1).U
            }.otherwise {
                io.txWire := false.B
                transitionCountDown := transitionCountDown - 1.U
            }
        }
        is(TransitionState.BITS) {
            when(transitionCountDown === 0.U) {
                io.txWire := byteTransmitting(bitTransmittingIndex)
                when(bitTransmittingIndex === 7.U) {
                    state := TransitionState.IDLE
                    transmitBufferIndexToClient := transmitBufferIndexToClient + 1.U
                    transitionCountDown := (freqIn / freqOut - 1).U
                }.otherwise {
                    bitTransmittingIndex := bitTransmittingIndex + 1.U
                    transitionCountDown := (freqIn / freqOut - 1).U
                }
            }.otherwise {
                io.txWire := byteTransmitting(bitTransmittingIndex)
                transitionCountDown := transitionCountDown - 1.U
            }
        }
        is(TransitionState.PARITY) {
            // unimplemented
        }
    }
    when(~io.readMode & io.maskLevel =/= Mask.NONE) {
        when(io.address(1, 0) === Registers.DATA) {
            // todo: raise exception when transmit buffer is full
            transmitBuffer(transmitBufferIndexFromHost) := io.dataIn
            transmitBufferIndexFromHost := transmitBufferIndexFromHost + 1.U
        }
    }.otherwise {
        when(io.address(1, 0) === Registers.STATUS) {
            io.dataOut := statusOutput
        }
    }
}

class SerialReceiveController(freqIn: Int, freqOut: Int, bufferWidthBits: Int) extends Module {
    val io = IO(new DataBusBundle {
        val rxWire = Input(Bool())
    })
    val receiveBuffer = Reg(Vec(1 << bufferWidthBits, UInt(8.W)))
    val receiveBufferIndexFromClient = RegInit(0.U(bufferWidthBits.W))
    val receiveBufferIndexToHost = RegInit(0.U(bufferWidthBits.W))

    val state = RegInit(TransitionState.IDLE)
    val sampleCountDown = Reg(UInt(CountDownCalculator.countDownBits(freqIn / freqOut - 1).W))
    val byteReceiving = Reg(Vec(8, Bool()))
    val bitReceivingIndex = Reg(UInt(3.W))

    val increaseReceiveBufferIndexToHost = Reg(Bool())
    val holdStatus = Reg(Bool())
    val updateBuffer = Reg(Bool())
    val fifoOutput = receiveBuffer(receiveBufferIndexToHost)
    val statusOutput = Cat(receiveBufferIndexToHost, receiveBufferIndexFromClient)
    val dataOutBuffer = Reg(UInt(32.W))
    io.dataOut := dataOutBuffer
    switch(state) {
        is(TransitionState.IDLE) {
            when(io.rxWire === 0.U) {
                state := TransitionState.START
                sampleCountDown := (freqIn / freqOut / 2 - 1).U
            }
        }
        is(TransitionState.START) {
            when(sampleCountDown === 0.U) {
                sampleCountDown := (freqIn / freqOut - 1).U
                bitReceivingIndex := 0.U
                state := TransitionState.BITS
            }.otherwise {
                sampleCountDown := sampleCountDown - 1.U
            }
        }
        is(TransitionState.BITS) {
            when(updateBuffer) {
                state := TransitionState.IDLE
                receiveBuffer(receiveBufferIndexFromClient) := byteReceiving.asUInt
                receiveBufferIndexFromClient := receiveBufferIndexFromClient + 1.U
                updateBuffer := false.B
            }.elsewhen(sampleCountDown === 0.U) {
                byteReceiving(bitReceivingIndex) := io.rxWire
                sampleCountDown := (freqIn / freqOut - 1).U
                when(bitReceivingIndex === 7.U) {
                    updateBuffer := true.B
                }.otherwise {
                    bitReceivingIndex := bitReceivingIndex + 1.U
                }
            }.otherwise {
                sampleCountDown := sampleCountDown - 1.U
            }
        }
        is(TransitionState.PARITY) {
            // unimplemented
        }
    }
    when(holdStatus) {
        dataOutBuffer := statusOutput
        holdStatus := false.B
    }.elsewhen(increaseReceiveBufferIndexToHost) {
        dataOutBuffer := fifoOutput
        receiveBufferIndexToHost := receiveBufferIndexToHost + 1.U
        increaseReceiveBufferIndexToHost := false.B
    }.elsewhen(io.readMode & io.maskLevel =/= Mask.NONE) {
        when(io.address(1, 0) === Registers.DATA) {
            dataOutBuffer := fifoOutput
            // todo: raise exception when receive buffer is empty
            increaseReceiveBufferIndexToHost := true.B
        }.elsewhen(io.address(1, 0) === Registers.STATUS) {
            dataOutBuffer := statusOutput
            holdStatus := true.B
        }.otherwise {
            dataOutBuffer := 0xef.U(32.W)
        }
    }.otherwise {
        dataOutBuffer := 0xad.U(32.W)
    }
}

class SerialController(freqIn: Int, freqOut: Int, bufferWidthBits: Int) extends Module {
    val io = IO(new DataBusBundle {
        val rxWire = Input(Bool())
        val txWire = Output(Bool())
    })
    val receiveController = Module(new SerialReceiveController(freqIn, freqOut, bufferWidthBits))
    val transmitController = Module(new SerialTransmitController(freqIn, freqOut, bufferWidthBits))
    receiveController.io.rxWire := io.rxWire
    receiveController.io.readMode := io.readMode
    receiveController.io.address := io.address(1, 0)

    io.txWire := transmitController.io.txWire
    transmitController.io.readMode := io.readMode
    transmitController.io.address := io.address(1, 0)
    when(io.address(2) === 0.U) {
        receiveController.io.dataIn := io.dataIn
        transmitController.io.dataIn := 0xdead.U
        io.dataOut := receiveController.io.dataOut
        receiveController.io.maskLevel := io.maskLevel
        transmitController.io.maskLevel := Mask.NONE
    }.otherwise {
        receiveController.io.dataIn := 0xdead.U
        transmitController.io.dataIn := io.dataIn
        io.dataOut := transmitController.io.dataOut
        transmitController.io.maskLevel := io.maskLevel
        receiveController.io.maskLevel := Mask.NONE
    }
}