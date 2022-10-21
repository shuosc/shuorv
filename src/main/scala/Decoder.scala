import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

class Decoder extends Bundle {
  import DecodeTable._
  val uOp      = UOp()
  val instType = InstType()
  val needImm = Bool()
  val rs1Addr = UInt(5.W)
  val rs2Addr = UInt(5.W)
  val rdAddr  = UInt(5.W)
  val immData = UInt(32.W)
  val csrIdx  = UInt(12.W)
  val maskLevel = UInt(2.W)

  def decode(inst: UInt): Decoder = {
    val immSel = Wire(ImmSel())

    val decoder = ListLookup(inst, decodeDefault, decodeTable)
    val signals = Seq(uOp, instType, immSel)
    signals zip decoder foreach { case (s, d) => s := d }

    val rd = inst(11, 7)
    val rs1 = inst(19, 15)
    val rs2 = inst(24, 20)
    val immI = Cat(Fill(20, inst(31)), inst(31, 20))
    val immS = Cat(Fill(20, inst(31)), inst(31, 25), inst(11, 7))
    val immB = Cat(Fill(19, inst(31)), inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
    val immU = Cat(inst(31, 12), 0.U(12.W))
    val immJ = Cat(Fill(11, inst(31)), inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))
    val shamt = Cat(Fill(26, false.B), inst(25, 20))
    val zimm = Cat(Fill(27, false.B), inst(19, 15))


    needImm := immSel =/= ImmSel.NOIMM
    rs1Addr := rs1
    rs2Addr := rs2
    rdAddr := rd
    immData := MuxLookup(immSel.asUInt(), 0.U(32.W), Array(
      ImmSel.I.asUInt() -> immI,
      ImmSel.S.asUInt() -> immS,
      ImmSel.B.asUInt() -> immB,
      ImmSel.U.asUInt() -> immU,
      ImmSel.J.asUInt() -> immJ,
      ImmSel.SHAMT.asUInt() -> shamt,
      ImmSel.ZIMM.asUInt() -> zimm
    ).toSeq)
    maskLevel := inst(13, 12)
    csrIdx := inst(31, 20)
    this
  }
}

object Instructions extends Enumeration {
  val BEQ                = BitPat("b?????????????????000?????1100011")
  val BNE                = BitPat("b?????????????????001?????1100011")
  val BLT                = BitPat("b?????????????????100?????1100011")
  val BGE                = BitPat("b?????????????????101?????1100011")
  val BLTU               = BitPat("b?????????????????110?????1100011")
  val BGEU               = BitPat("b?????????????????111?????1100011")
  val JALR               = BitPat("b?????????????????000?????1100111")
  val JAL                = BitPat("b?????????????????????????1101111")
  val LUI                = BitPat("b?????????????????????????0110111")
  val AUIPC              = BitPat("b?????????????????????????0010111")
  val ADDI               = BitPat("b?????????????????000?????0010011")
  val SLLI               = BitPat("b000000???????????001?????0010011")
  val SLTI               = BitPat("b?????????????????010?????0010011")
  val SLTIU              = BitPat("b?????????????????011?????0010011")
  val XORI               = BitPat("b?????????????????100?????0010011")
  val SRLI               = BitPat("b000000???????????101?????0010011")
  val SRAI               = BitPat("b010000???????????101?????0010011")
  val ORI                = BitPat("b?????????????????110?????0010011")
  val ANDI               = BitPat("b?????????????????111?????0010011")
  val ADD                = BitPat("b0000000??????????000?????0110011")
  val SUB                = BitPat("b0100000??????????000?????0110011")
  val SLL                = BitPat("b0000000??????????001?????0110011")
  val SLT                = BitPat("b0000000??????????010?????0110011")
  val SLTU               = BitPat("b0000000??????????011?????0110011")
  val XOR                = BitPat("b0000000??????????100?????0110011")
  val SRL                = BitPat("b0000000??????????101?????0110011")
  val SRA                = BitPat("b0100000??????????101?????0110011")
  val OR                 = BitPat("b0000000??????????110?????0110011")
  val AND                = BitPat("b0000000??????????111?????0110011")
  val LB                 = BitPat("b?????????????????000?????0000011")
  val LH                 = BitPat("b?????????????????001?????0000011")
  val LW                 = BitPat("b?????????????????010?????0000011")
  val LBU                = BitPat("b?????????????????100?????0000011")
  val LHU                = BitPat("b?????????????????101?????0000011")
  val LWU                = BitPat("b?????????????????110?????0000011")
  val SB                 = BitPat("b?????????????????000?????0100011")
  val SH                 = BitPat("b?????????????????001?????0100011")
  val SW                 = BitPat("b?????????????????010?????0100011")
  val FENCE              = BitPat("b?????????????????000?????0001111")
  val FENCE_I            = BitPat("b?????????????????001?????0001111")
  val ECALL              = BitPat("b00000000000000000000000001110011")
  val EBREAK             = BitPat("b00000000000100000000000001110011")
  val MRET               = BitPat("b00110000001000000000000001110011")
  val SRET               = BitPat("b00010000001000000000000001110011")
  val SFENCE_VMA         = BitPat("b0001001??????????000000001110011")
  val WFI                = BitPat("b00010000010100000000000001110011")
  val CSRRW              = BitPat("b?????????????????001?????1110011")
  val CSRRS              = BitPat("b?????????????????010?????1110011")
  val CSRRC              = BitPat("b?????????????????011?????1110011")
  val CSRRWI             = BitPat("b?????????????????101?????1110011")
  val CSRRSI             = BitPat("b?????????????????110?????1110011")
  val CSRRCI             = BitPat("b?????????????????111?????1110011")
}

object DecodeTable {
  import Instructions._
  val decodeDefault = List(UOp.NOP, InstType.NONE, ImmSel.NOIMM)
  val decodeTable = Array(
    BEQ        -> List(UOp.BEQ       , InstType.BRANCH   , ImmSel.B),
    BNE        -> List(UOp.BNE       , InstType.BRANCH   , ImmSel.B),
    BLT        -> List(UOp.BLT       , InstType.BRANCH   , ImmSel.B),
    BGE        -> List(UOp.BGE       , InstType.BRANCH   , ImmSel.B),
    BLTU       -> List(UOp.BLTU      , InstType.BRANCH   , ImmSel.B),
    BGEU       -> List(UOp.BGEU      , InstType.BRANCH   , ImmSel.B),
    JALR       -> List(UOp.JALR      , InstType.JALR     , ImmSel.I),
    JAL        -> List(UOp.JAL       , InstType.JAL      , ImmSel.J),
    LUI        -> List(UOp.LUI       , InstType.LUI      , ImmSel.U),
    AUIPC      -> List(UOp.AUIPC     , InstType.AUIPC    , ImmSel.U),
    ADDI       -> List(UOp.ADD       , InstType.CALCULATE, ImmSel.I),
    SLLI       -> List(UOp.SLL       , InstType.CALCULATE, ImmSel.SHAMT),
    SLTI       -> List(UOp.SLT       , InstType.CALCULATE, ImmSel.I),
    SLTIU      -> List(UOp.SLTU      , InstType.CALCULATE, ImmSel.I),
    XORI       -> List(UOp.XOR       , InstType.CALCULATE, ImmSel.I),
    SRLI       -> List(UOp.SRL       , InstType.CALCULATE, ImmSel.SHAMT),
    SRAI       -> List(UOp.SRA       , InstType.CALCULATE, ImmSel.SHAMT),
    ORI        -> List(UOp.OR        , InstType.CALCULATE, ImmSel.I),
    ANDI       -> List(UOp.AND       , InstType.CALCULATE, ImmSel.I),
    ADD        -> List(UOp.ADD       , InstType.CALCULATE, ImmSel.NOIMM),
    SUB        -> List(UOp.SUB       , InstType.CALCULATE, ImmSel.NOIMM),
    SLL        -> List(UOp.SLL       , InstType.CALCULATE, ImmSel.NOIMM),
    SLT        -> List(UOp.SLT       , InstType.CALCULATE, ImmSel.NOIMM),
    SLTU       -> List(UOp.SLTU      , InstType.CALCULATE, ImmSel.NOIMM),
    XOR        -> List(UOp.XOR       , InstType.CALCULATE, ImmSel.NOIMM),
    SRL        -> List(UOp.SRL       , InstType.CALCULATE, ImmSel.NOIMM),
    SRA        -> List(UOp.SRA       , InstType.CALCULATE, ImmSel.NOIMM),
    OR         -> List(UOp.OR        , InstType.CALCULATE, ImmSel.NOIMM),
    AND        -> List(UOp.AND       , InstType.CALCULATE, ImmSel.NOIMM),
    LB         -> List(UOp.LB        , InstType.LOAD     , ImmSel.I),
    LH         -> List(UOp.LH        , InstType.LOAD     , ImmSel.I),
    LW         -> List(UOp.LW        , InstType.LOAD     , ImmSel.I),
    LBU        -> List(UOp.LBU       , InstType.LOAD     , ImmSel.I),
    LHU        -> List(UOp.LHU       , InstType.LOAD     , ImmSel.I),
    LWU        -> List(UOp.LWU       , InstType.LOAD     , ImmSel.I),
    SB         -> List(UOp.SB        , InstType.STORE    , ImmSel.S),
    SH         -> List(UOp.SH        , InstType.STORE    , ImmSel.S),
    SW         -> List(UOp.SW        , InstType.STORE    , ImmSel.S),
    CSRRW      -> List(UOp.CSRRW     , InstType.CALCULATE, ImmSel.NOIMM),
    CSRRS      -> List(UOp.CSRRS     , InstType.CALCULATE, ImmSel.NOIMM),
    CSRRC      -> List(UOp.CSRRC     , InstType.CALCULATE, ImmSel.NOIMM),
    CSRRWI     -> List(UOp.CSRRW     , InstType.CALCULATE, ImmSel.ZIMM),
    CSRRSI     -> List(UOp.CSRRS     , InstType.CALCULATE, ImmSel.ZIMM),
    CSRRCI     -> List(UOp.CSRRC     , InstType.CALCULATE, ImmSel.ZIMM),
    FENCE      -> List(UOp.FENCE     , InstType.FENCE    , ImmSel.NOIMM),
    FENCE_I    -> List(UOp.FENCE_I   , InstType.FENCE    , ImmSel.NOIMM),
    ECALL      -> List(UOp.ECALL     , InstType.SYSTEM   , ImmSel.NOIMM),
    EBREAK     -> List(UOp.EBREAK    , InstType.SYSTEM   , ImmSel.NOIMM),
    MRET       -> List(UOp.MRET      , InstType.SYSTEM   , ImmSel.NOIMM),
    SRET       -> List(UOp.SRET      , InstType.SYSTEM   , ImmSel.NOIMM),
    SFENCE_VMA -> List(UOp.SFENCE_VMA, InstType.FENCE    , ImmSel.NOIMM),
    WFI        -> List(UOp.WFI       , InstType.SYSTEM   , ImmSel.NOIMM)

  )
}


//micro operations
object UOp extends ChiselEnum {
  val
  NOP,
  //branch and jump
  BEQ,
  BNE,
  BLT,
  BGE,
  BLTU,
  BGEU,
  JALR,
  JAL,
  //arithmetic and logic
  LUI,
  AUIPC,
  ADD,
  SUB,
  SLL,
  SLT,
  SLTU,
  XOR,
  SRL,
  SRA,
  OR,
  AND,
  //load and store
  LB,
  LH,
  LW,
  LBU,
  LHU,
  LWU,
  SB,
  SH,
  SW,
  //privilege
  ECALL,
  EBREAK,
  MRET,
  SRET,
  FENCE,
  FENCE_I,
  SFENCE_VMA,
  WFI,
  //csr
  CSRRW,
  CSRRS,
  CSRRC = Value
}

object InstType extends ChiselEnum {
  val
  NONE,
  LUI,
  AUIPC,
  JAL,
  JALR,
  BRANCH,
  CALCULATE,
  LOAD,
  STORE,
  FENCE,
  SYSTEM = Value
}


object ImmSel extends ChiselEnum {
  val
  I,
  U,
  J,
  S,
  B,
  ZIMM,
  SHAMT,
  NOIMM = Value
}