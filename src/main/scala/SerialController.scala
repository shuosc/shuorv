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

class SerialTransmitController extends Module {
    val io = IO(new DataBusBundle {
        val txWire = Output(Bool())
    })
    
}

class SerialRecieveController(freqIn: Int, freqOut: Int, bufferWidthBits: Int) extends Module {
    val io = IO(new DataBusBundle {
        val rxWire = Input(Bool())
    })
    val receiveBuffer = Reg(Vec(1 << bufferWidthBits, UInt(8.W)))
    val receiveBufferIndexFromClient = Reg(UInt(bufferWidthBits.W))
    val receiveBufferIndexToHost = Reg(UInt(bufferWidthBits.W))

    val state = RegInit(TransitionState.IDLE)
    val sampleCountDown = Reg(UInt(32.W))
    val byteRecieving = Reg(Vec(8, Bool()))
    val bitRecieving = Reg(UInt(3.W))

    val increaseReceiveBufferIndexToHost = Reg(Bool())
    val holdStatus = Reg(Bool())
    val updateBuffer = Reg(Bool())
    val fifoOutput = receiveBuffer(receiveBufferIndexToHost)
    val statusOutput = Cat(receiveBufferIndexToHost, receiveBufferIndexFromClient)
    val ioBuf = Reg(UInt(32.W))
    io.dataOut := ioBuf
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
                bitRecieving := 0.U
                state := TransitionState.BITS
            }.otherwise {
                sampleCountDown := sampleCountDown - 1.U
            }
        }
        is(TransitionState.BITS) {
            when(updateBuffer) {
                state := TransitionState.IDLE
                receiveBuffer(receiveBufferIndexFromClient) := byteRecieving.asUInt
                receiveBufferIndexFromClient := receiveBufferIndexFromClient + 1.U
                updateBuffer := false.B
            }.elsewhen(sampleCountDown === 0.U) {
                byteRecieving(bitRecieving) := io.rxWire
                sampleCountDown := (freqIn / freqOut - 1).U
                when(bitRecieving === 7.U) {
                    updateBuffer := true.B
                }.otherwise {
                    bitRecieving := bitRecieving + 1.U
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
            ioBuf := statusOutput
            holdStatus := false.B
    }.elsewhen(increaseReceiveBufferIndexToHost) {
            ioBuf := fifoOutput
            receiveBufferIndexToHost := receiveBufferIndexToHost + 1.U
            increaseReceiveBufferIndexToHost := false.B
    }.elsewhen(io.readMode & io.maskLevel =/= Mask.NONE) {
        when(io.address(1, 0) === Registers.DATA) {
            ioBuf := fifoOutput
            // todo: raise expection when receive buffer is empty
            increaseReceiveBufferIndexToHost := true.B
        }.elsewhen(io.address(1, 0) === Registers.STATUS) {
            ioBuf := statusOutput
            holdStatus := true.B
        }.otherwise {
            ioBuf := 0xef.U(32.W)
        }
    }.otherwise {
        ioBuf := 0xad.U(32.W)
    }
}