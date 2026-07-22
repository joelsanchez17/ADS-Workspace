// ADS I Class Project
// Pipelined RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/15/2023 by Tobias Jauch (@tojauch)

/*
The goal of this task is to implement a 5-stage pipeline that features a subset of RV32I (all R-type and I-type instructions). 

    Instruction Memory:
        The CPU has an instruction memory (IMem) with 4096 words, each of 32 bits.
        The content of IMem is loaded from a binary file specified during the instantiation of the MultiCycleRV32Icore module.

    CPU Registers:
        The CPU has a program counter (PC) and a register file (regFile) with 32 registers, each holding a 32-bit value.
        Register x0 is hard-wired to zero.

    Microarchitectural Registers / Wires:
        Various signals are defined as either registers or wires depending on whether they need to be used in the same cycle or in a later cycle.

    Processor Stages:
        The FSM of the processor has five stages: fetch, decode, execute, memory, and writeback.
        All stages are active at the same time and process different instructions simultaneously.

        Fetch Stage:
            The instruction is fetched from the instruction memory based on the current value of the program counter (PC).

        Decode Stage:
            Instruction fields such as opcode, rd, funct3, and rs1 are extracted.
            For R-type instructions, additional fields like funct7 and rs2 are extracted.
            Control signals (isADD, isSUB, etc.) are set based on the opcode and funct3 values.
            Operands (operandA and operandB) are determined based on the instruction type.

        Execute Stage:
            Arithmetic and logic operations are performed based on the control signals and operands.
            The result is stored in the aluResult register.

        Memory Stage:
            No memory operations are implemented in this basic CPU.

        Writeback Stage:
            The result of the operation (writeBackData) is written back to the destination register (rd) in the register file.

    Check Result:
        The final result (writeBackData) is output to the io.check_res signal.
        The exception signal is also passed to the wrapper module. It indicates whether an invalid instruction has been encountered.
        In the fetch stage, a default value of 0 is assigned to io.check_res.
*/

package core_tile

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import Assignment02.{ALU, ALUOp}
import uopc._


class PipelinedRV32Icore (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val check_res = Output(UInt(32.W))
    val exception = Output(Bool())
    val coreDone  = Output(UInt(1.W))
    val gpRegVal  = Output(UInt(32.W))
    val dbg = new Bundle {
      val if_pc    = Output(UInt(32.W)); val if_inst  = Output(UInt(32.W))
      val id_pc    = Output(UInt(32.W)); val id_inst  = Output(UInt(32.W))
      val ex_pc    = Output(UInt(32.W)); val ex_inst  = Output(UInt(32.W))
      val mem_pc   = Output(UInt(32.W)); val mem_inst = Output(UInt(32.W))
      val wb_pc    = Output(UInt(32.W)); val wb_inst  = Output(UInt(32.W))

      val id_rs1   = Output(UInt(5.W))
      val id_rs2   = Output(UInt(5.W))
      val id_rd    = Output(UInt(5.W))
      val id_we    = Output(UInt(1.W))

      val pc_write = Output(UInt(1.W))
      val if_stall = Output(UInt(1.W))
      val id_stall = Output(UInt(1.W))
      val flush    = Output(UInt(1.W))

      val fwd_a_sel = Output(UInt(8.W))
      val fwd_b_sel = Output(UInt(8.W))

      val ex_alu_result = Output(UInt(32.W))
      val ex_alu_op_a   = Output(UInt(32.W))
      val ex_alu_op_b   = Output(UInt(32.W))
      val ex_pc_src     = Output(UInt(1.W))
      val ex_pc_jb      = Output(UInt(32.W))
      val ex_rd         = Output(UInt(5.W))
      val ex_we         = Output(UInt(1.W))
      val ex_mem_rd_op  = Output(UInt(8.W))
      val ex_mem_wr_op  = Output(UInt(8.W))
      val ex_mem_to_reg = Output(UInt(1.W))

      val mem_addr    = Output(UInt(32.W))
      val mem_rd_op   = Output(UInt(8.W))
      val mem_wr_op   = Output(UInt(8.W))
      val mem_wdata   = Output(UInt(32.W))
      val mem_rdata   = Output(UInt(32.W))
      val mem_rd      = Output(UInt(5.W))
      val mem_we      = Output(UInt(1.W))
      val mem_to_reg  = Output(UInt(1.W))

      val wb_rd        = Output(UInt(5.W))
      val wb_we        = Output(UInt(1.W))
      val wb_wdata     = Output(UInt(32.W))
      val wb_check_res = Output(UInt(32.W))
      val regs         = Output(Vec(32, UInt(32.W)))
    }
  })

  // Pipeline Registers
  val IFBarrier  = Module(new IFBarrier)
  val IDBarrier  = Module(new IDBarrier)
  val EXBarrier  = Module(new EXBarrier)
  val MEMBarrier = Module(new MEMBarrier)
  val WBBarrier  = Module(new WBBarrier)

  // Pipeline Stages
  val IF  = Module(new IF(BinaryFile))
  val ID  = Module(new ID)
  val EX  = Module(new EX)
  val MEM = Module(new MEM)
  val WB  = Module(new WB)

  //Register File
  val regFile = Module(new regFile)

  //Forwarding Unit
  val ForwardingUnit = Module(new ForwardingUnit)

  // Connections for IOs
  IFBarrier.io.inInstr      := IF.io.instr
  IFBarrier.io.inPC         := IF.io.pc
  
  ID.io.instr               := IFBarrier.io.outInstr
  ID.io.regFileReq_A        <> regFile.io.req_1
  ID.io.regFileReq_B        <> regFile.io.req_2
  ID.io.regFileResp_A       <> regFile.io.resp_1
  ID.io.regFileResp_B       <> regFile.io.resp_2

  IDBarrier.io.inUOP         := ID.io.uop
  IDBarrier.io.inInstr       := IFBarrier.io.outInstr
  IDBarrier.io.inPC          := IFBarrier.io.outPC
  IDBarrier.io.inRS1         := ID.io.rs1
  IDBarrier.io.inRS2         := ID.io.rs2
  IDBarrier.io.inRD          := ID.io.rd
  IDBarrier.io.inOperandA    := ID.io.operandA
  IDBarrier.io.inOperandB    := ID.io.operandB
  IDBarrier.io.inWrEn        := ID.io.wrEN
  IDBarrier.io.inXcptInvalid := ID.io.XcptInvalid

  EX.io.uop                  := IDBarrier.io.outUOP
  EX.io.operandA             := IDBarrier.io.outOperandA
  EX.io.operandB             := IDBarrier.io.outOperandB
  EX.io.forwardA             := ForwardingUnit.io.forwardA
  EX.io.forwardB             := ForwardingUnit.io.forwardB
  EX.io.memResult            := EXBarrier.io.outAluResult
  EX.io.wbResult             := MEMBarrier.io.outAluResult
  EX.io.inXcptInvalid        := IDBarrier.io.outXcptInvalid

  EXBarrier.io.inRD           := IDBarrier.io.outRD
  EXBarrier.io.inInstr        := IDBarrier.io.outInstr
  EXBarrier.io.inPC           := IDBarrier.io.outPC
  EXBarrier.io.inWrEn         := IDBarrier.io.outWrEn
  EXBarrier.io.inAluResult    := EX.io.aluResult
  EXBarrier.io.inXcptInvalid  := EX.io.outXcptInvalid

  MEMBarrier.io.inRD          := EXBarrier.io.outRD
  MEMBarrier.io.inInstr       := EXBarrier.io.outInstr
  MEMBarrier.io.inPC          := EXBarrier.io.outPC
  MEMBarrier.io.inAluResult   := EXBarrier.io.outAluResult
  MEMBarrier.io.inWrEn        := EXBarrier.io.outWrEn
  MEMBarrier.io.inXcptInvalid := EXBarrier.io.outXcptInvalid

  WB.io.rd                  := MEMBarrier.io.outRD
  WB.io.wrEN                := MEMBarrier.io.outWrEn
  WB.io.aluResult           := MEMBarrier.io.outAluResult
  WB.io.regFileReq          <> regFile.io.req_3

  WBBarrier.io.inCheckRes    := WB.io.check_res
  WBBarrier.io.inInstr       := MEMBarrier.io.outInstr
  WBBarrier.io.inPC          := MEMBarrier.io.outPC
  WBBarrier.io.inXcptInvalid := MEMBarrier.io.outXcptInvalid

  ForwardingUnit.io.rs1_EX   := IDBarrier.io.outRS1
  ForwardingUnit.io.rs2_EX   := IDBarrier.io.outRS2
  ForwardingUnit.io.rd_MEM   := EXBarrier.io.outRD
  ForwardingUnit.io.rd_WB    := MEMBarrier.io.outRD
  ForwardingUnit.io.wrEn_MEM := EXBarrier.io.outWrEn
  ForwardingUnit.io.wrEn_WB  := MEMBarrier.io.outWrEn

  def fwdDebugSel(native: UInt): UInt = {
    Mux(native === "b10".U, 1.U, Mux(native === "b01".U, 2.U, 0.U))
  }

  val dbgExOperandA = MuxLookup(ForwardingUnit.io.forwardA, IDBarrier.io.outOperandA, Array(
    "b01".U -> MEMBarrier.io.outAluResult,
    "b10".U -> EXBarrier.io.outAluResult
  ))
  val dbgExOperandB = MuxLookup(ForwardingUnit.io.forwardB, IDBarrier.io.outOperandB, Array(
    "b01".U -> MEMBarrier.io.outAluResult,
    "b10".U -> EXBarrier.io.outAluResult
  ))

  io.check_res               := WBBarrier.io.outCheckRes
  io.exception               := WBBarrier.io.outXcptInvalid
  io.coreDone                := IF.io.instr(6, 0) === "b1110011".U
  io.gpRegVal                := regFile.io.gpRegVal

  io.dbg.if_pc   := IF.io.pc
  io.dbg.if_inst := IF.io.instr
  io.dbg.id_pc   := IFBarrier.io.outPC
  io.dbg.id_inst := IFBarrier.io.outInstr
  io.dbg.ex_pc   := IDBarrier.io.outPC
  io.dbg.ex_inst := IDBarrier.io.outInstr
  io.dbg.mem_pc  := EXBarrier.io.outPC
  io.dbg.mem_inst:= EXBarrier.io.outInstr
  io.dbg.wb_pc   := MEMBarrier.io.outPC
  io.dbg.wb_inst := MEMBarrier.io.outInstr

  io.dbg.id_rs1 := ID.io.rs1
  io.dbg.id_rs2 := ID.io.rs2
  io.dbg.id_rd  := ID.io.rd
  io.dbg.id_we  := (ID.io.wrEN && ID.io.rd =/= 0.U).asUInt

  io.dbg.pc_write := 1.U
  io.dbg.if_stall := 0.U
  io.dbg.id_stall := 0.U
  io.dbg.flush    := 0.U

  // ForwardingUnit uses native encoding 2=MEM and 1=WB. The web visualizer
  // expects 1=MEM and 2=WB, so only the debug output is remapped here.
  io.dbg.fwd_a_sel := fwdDebugSel(ForwardingUnit.io.forwardA)
  io.dbg.fwd_b_sel := fwdDebugSel(ForwardingUnit.io.forwardB)

  io.dbg.ex_alu_result := EX.io.aluResult
  io.dbg.ex_alu_op_a   := dbgExOperandA
  io.dbg.ex_alu_op_b   := dbgExOperandB
  io.dbg.ex_pc_src     := 0.U
  io.dbg.ex_pc_jb      := 0.U
  io.dbg.ex_rd         := IDBarrier.io.outRD
  io.dbg.ex_we         := (IDBarrier.io.outWrEn && IDBarrier.io.outRD =/= 0.U).asUInt
  io.dbg.ex_mem_rd_op  := 0.U
  io.dbg.ex_mem_wr_op  := 0.U
  io.dbg.ex_mem_to_reg := 0.U

  io.dbg.mem_addr   := 0.U
  io.dbg.mem_rd_op  := 0.U
  io.dbg.mem_wr_op  := 0.U
  io.dbg.mem_wdata  := 0.U
  io.dbg.mem_rdata  := 0.U
  io.dbg.mem_rd     := EXBarrier.io.outRD
  io.dbg.mem_we     := 0.U
  io.dbg.mem_to_reg := 0.U

  io.dbg.wb_rd        := WB.io.rd
  io.dbg.wb_we        := (MEMBarrier.io.outWrEn && MEMBarrier.io.outRD =/= 0.U).asUInt
  io.dbg.wb_wdata     := WB.io.regFileReq.data
  io.dbg.wb_check_res := WB.io.check_res
  io.dbg.regs         := regFile.io.debug_regs

}
