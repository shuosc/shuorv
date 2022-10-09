import Instructions._
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

class Decoder extends Bundle {
  val uop      = uOP()
  val inst_type = InstType()
  val need_imm = Bool()
  val rs1_addr = UInt(5.W)
  val rs2_addr = UInt(5.W)
  val rd_addr  = UInt(5.W)
  val imm_data = UInt(32.W)
  val csr_idx  = UInt(12.W)
  val maskLevel = UInt(2.W)

  def decode(inst: UInt) = {
    val imm_sel = Wire(ImmSel())

    val decoder = ListLookup(inst, decode_default, decode_table)
    val signals = Seq(uop, inst_type, imm_sel)
    signals zip decoder foreach { case (s, d) => s := d }

    val rd    = inst(11, 7)
    val rs1   = inst(19, 15)
    val rs2   = inst(24, 20)
    val immI  = Cat(Fill(20,inst(31)),inst(31, 20))
    val immS  = Cat(Fill(20,inst(31)),inst(31, 25), inst(11, 7))
    val immB  = Cat(Fill(19,inst(31)),inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
    val immU  = Cat(inst(31, 12), 0.U(12.W))
    val immJ  = Cat(Fill(11,inst(31)),inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))
    val shamt = Cat(Fill(26,false.B),inst(25, 20))
    val zimm  = Cat(Fill(27,false.B),inst(19, 15))



    need_imm := imm_sel =/= ImmSel.no_imm
    rs1_addr := rs1
    rs2_addr := rs2
    rd_addr := rd
    imm_data := MuxLookup(imm_sel.asUInt(), 0.U(32.W), Array(
      ImmSel.is_I.asUInt() -> immI,
      ImmSel.is_S.asUInt() -> immS,
      ImmSel.is_B.asUInt() -> immB,
      ImmSel.is_U.asUInt() -> immU,
      ImmSel.is_J.asUInt() -> immJ,
      ImmSel.is_shamt.asUInt() -> shamt,
      ImmSel.is_zimm.asUInt() -> zimm,
    ).toSeq)
    maskLevel := inst(13, 12)
    csr_idx := inst(31, 20)
    this
  }
}

object Instructions {
  def BEQ                = BitPat("b?????????????????000?????1100011")
  def BNE                = BitPat("b?????????????????001?????1100011")
  def BLT                = BitPat("b?????????????????100?????1100011")
  def BGE                = BitPat("b?????????????????101?????1100011")
  def BLTU               = BitPat("b?????????????????110?????1100011")
  def BGEU               = BitPat("b?????????????????111?????1100011")
  def JALR               = BitPat("b?????????????????000?????1100111")
  def JAL                = BitPat("b?????????????????????????1101111")
  def LUI                = BitPat("b?????????????????????????0110111")
  def AUIPC              = BitPat("b?????????????????????????0010111")
  def ADDI               = BitPat("b?????????????????000?????0010011")
  def SLLI               = BitPat("b000000???????????001?????0010011")
  def SLTI               = BitPat("b?????????????????010?????0010011")
  def SLTIU              = BitPat("b?????????????????011?????0010011")
  def XORI               = BitPat("b?????????????????100?????0010011")
  def SRLI               = BitPat("b000000???????????101?????0010011")
  def SRAI               = BitPat("b010000???????????101?????0010011")
  def ORI                = BitPat("b?????????????????110?????0010011")
  def ANDI               = BitPat("b?????????????????111?????0010011")
  def ADD                = BitPat("b0000000??????????000?????0110011")
  def SUB                = BitPat("b0100000??????????000?????0110011")
  def SLL                = BitPat("b0000000??????????001?????0110011")
  def SLT                = BitPat("b0000000??????????010?????0110011")
  def SLTU               = BitPat("b0000000??????????011?????0110011")
  def XOR                = BitPat("b0000000??????????100?????0110011")
  def SRL                = BitPat("b0000000??????????101?????0110011")
  def SRA                = BitPat("b0100000??????????101?????0110011")
  def OR                 = BitPat("b0000000??????????110?????0110011")
  def AND                = BitPat("b0000000??????????111?????0110011")
  def LB                 = BitPat("b?????????????????000?????0000011")
  def LH                 = BitPat("b?????????????????001?????0000011")
  def LW                 = BitPat("b?????????????????010?????0000011")
  def LBU                = BitPat("b?????????????????100?????0000011")
  def LHU                = BitPat("b?????????????????101?????0000011")
  def LWU                = BitPat("b?????????????????110?????0000011")
  def SB                 = BitPat("b?????????????????000?????0100011")
  def SH                 = BitPat("b?????????????????001?????0100011")
  def SW                 = BitPat("b?????????????????010?????0100011")
  def FENCE              = BitPat("b?????????????????000?????0001111")
  def FENCE_I            = BitPat("b?????????????????001?????0001111")
  def ECALL              = BitPat("b00000000000000000000000001110011")
  def EBREAK             = BitPat("b00000000000100000000000001110011")
  def MRET               = BitPat("b00110000001000000000000001110011")
  def SRET               = BitPat("b00010000001000000000000001110011")
  def SFENCE_VMA         = BitPat("b0001001??????????000000001110011")
  def WFI                = BitPat("b00010000010100000000000001110011")
  def CSRRW              = BitPat("b?????????????????001?????1110011")
  def CSRRS              = BitPat("b?????????????????010?????1110011")
  def CSRRC              = BitPat("b?????????????????011?????1110011")
  def CSRRWI             = BitPat("b?????????????????101?????1110011")
  def CSRRSI             = BitPat("b?????????????????110?????1110011")
  def CSRRCI             = BitPat("b?????????????????111?????1110011")

  val decode_default = List(  uOP.NOP, InstType.none, ImmSel.no_imm)
  val decode_table = Array(
    BEQ        -> List(uOP.BEQ       , InstType.branch   , ImmSel.is_B    ),
    BNE        -> List(uOP.BNE       , InstType.branch   , ImmSel.is_B    ),
    BLT        -> List(uOP.BLT       , InstType.branch   , ImmSel.is_B    ),
    BGE        -> List(uOP.BGE       , InstType.branch   , ImmSel.is_B    ),
    BLTU       -> List(uOP.BLTU      , InstType.branch   , ImmSel.is_B    ),
    BGEU       -> List(uOP.BGEU      , InstType.branch   , ImmSel.is_B    ),
    JALR       -> List(uOP.JALR      , InstType.jalr     , ImmSel.is_I    ),
    JAL        -> List(uOP.JAL       , InstType.jal      , ImmSel.is_J    ),

    LUI        -> List(uOP.LUI       , InstType.lui      , ImmSel.is_U    ),
    AUIPC      -> List(uOP.AUIPC     , InstType.auipc    , ImmSel.is_U    ),
    ADDI       -> List(uOP.ADD       , InstType.calculate, ImmSel.is_I    ),
    SLLI       -> List(uOP.SLL       , InstType.calculate, ImmSel.is_shamt),
    SLTI       -> List(uOP.SLT       , InstType.calculate, ImmSel.is_I    ),
    SLTIU      -> List(uOP.SLTU      , InstType.calculate, ImmSel.is_I    ),
    XORI       -> List(uOP.XOR       , InstType.calculate, ImmSel.is_I    ),
    SRLI       -> List(uOP.SRL       , InstType.calculate, ImmSel.is_shamt),
    SRAI       -> List(uOP.SRA       , InstType.calculate, ImmSel.is_shamt),
    ORI        -> List(uOP.OR        , InstType.calculate, ImmSel.is_I    ),
    ANDI       -> List(uOP.AND       , InstType.calculate, ImmSel.is_I    ),
    ADD        -> List(uOP.ADD       , InstType.calculate, ImmSel.no_imm  ),
    SUB        -> List(uOP.SUB       , InstType.calculate, ImmSel.no_imm  ),
    SLL        -> List(uOP.SLL       , InstType.calculate, ImmSel.no_imm  ),
    SLT        -> List(uOP.SLT       , InstType.calculate, ImmSel.no_imm  ),
    SLTU       -> List(uOP.SLTU      , InstType.calculate, ImmSel.no_imm  ),
    XOR        -> List(uOP.XOR       , InstType.calculate, ImmSel.no_imm  ),
    SRL        -> List(uOP.SRL       , InstType.calculate, ImmSel.no_imm  ),
    SRA        -> List(uOP.SRA       , InstType.calculate, ImmSel.no_imm  ),
    OR         -> List(uOP.OR        , InstType.calculate, ImmSel.no_imm  ),
    AND        -> List(uOP.AND       , InstType.calculate, ImmSel.no_imm  ),

    LB         -> List(uOP.LB        , InstType.load     , ImmSel.is_I    ),
    LH         -> List(uOP.LH        , InstType.load     , ImmSel.is_I    ),
    LW         -> List(uOP.LW        , InstType.load     , ImmSel.is_I    ),
    LBU        -> List(uOP.LBU       , InstType.load     , ImmSel.is_I    ),
    LHU        -> List(uOP.LHU       , InstType.load     , ImmSel.is_I    ),
    LWU        -> List(uOP.LWU       , InstType.load     , ImmSel.is_I    ),
    SB         -> List(uOP.SB        , InstType.store    , ImmSel.is_S    ),
    SH         -> List(uOP.SH        , InstType.store    , ImmSel.is_S    ),
    SW         -> List(uOP.SW        , InstType.store    , ImmSel.is_S    ),

    CSRRW      -> List(uOP.CSRRW     , InstType.calculate, ImmSel.no_imm  ),
    CSRRS      -> List(uOP.CSRRS     , InstType.calculate, ImmSel.no_imm  ),
    CSRRC      -> List(uOP.CSRRC     , InstType.calculate, ImmSel.no_imm  ),
    CSRRWI     -> List(uOP.CSRRW     , InstType.calculate, ImmSel.is_zimm ),
    CSRRSI     -> List(uOP.CSRRS     , InstType.calculate, ImmSel.is_zimm ),
    CSRRCI     -> List(uOP.CSRRC     , InstType.calculate, ImmSel.is_zimm ),
    FENCE      -> List(uOP.FENCE     , InstType.fence    , ImmSel.no_imm  ),
    FENCE_I    -> List(uOP.FENCE_I   , InstType.fence    , ImmSel.no_imm  ),
    ECALL      -> List(uOP.ECALL     , InstType.system   , ImmSel.no_imm  ),
    EBREAK     -> List(uOP.EBREAK    , InstType.system   , ImmSel.no_imm  ),
    MRET       -> List(uOP.MRET      , InstType.system   , ImmSel.no_imm  ),
    SRET       -> List(uOP.SRET      , InstType.system   , ImmSel.no_imm  ),
    SFENCE_VMA -> List(uOP.SFENCE_VMA, InstType.fence    , ImmSel.no_imm  ),
    WFI        -> List(uOP.WFI       , InstType.system   , ImmSel.no_imm  ),
    )

}

object uOP extends ChiselEnum {
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
  //load,store
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
  none,
  lui,
  auipc,
  jal,
  jalr,
  branch,
  calculate,
  load,
  store,
  fence,
  system = Value
}


object ImmSel extends ChiselEnum {
  val
  is_I,
  is_U,
  is_J,
  is_S,
  is_B,
  is_zimm,
  is_shamt,
  no_imm = Value
}