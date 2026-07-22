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


class PipelinedRV32Icore (BinaryFile: String) extends Module {
  val io = IO(new Bundle {
    val check_res = Output(UInt(32.W))
    val exception = Output(Bool())
    val coreDone  = Output(UInt(1.W))
    val gpRegVal  = Output(UInt(32.W))

    // Web visualizer interface.
    // Do not remove or rename these signals: LivePipelineTest and the browser
    // visualizer read them directly. The zero assignments below are only safe
    // placeholders. After implementing the pipeline, connect these debug outputs
    // to the corresponding internal pipeline signals. The reference solution
    // shows one possible correct wiring.
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

  // Visualizer-facing defaults.
  // These zero assignments are safe placeholders for the web visualizer
  // interface. Do not remove or rename io.dbg.*. After implementing the
  // pipeline, replace the relevant defaults with connections to your internal
  // pipeline state. The reference solution shows one possible correct wiring.
  io.check_res := 0.U
  io.exception := false.B
  io.coreDone  := 0.U
  io.gpRegVal  := 0.U

  io.dbg.if_pc   := 0.U; io.dbg.if_inst  := 0.U
  io.dbg.id_pc   := 0.U; io.dbg.id_inst  := 0.U
  io.dbg.ex_pc   := 0.U; io.dbg.ex_inst  := 0.U
  io.dbg.mem_pc  := 0.U; io.dbg.mem_inst := 0.U
  io.dbg.wb_pc   := 0.U; io.dbg.wb_inst  := 0.U

  io.dbg.id_rs1 := 0.U
  io.dbg.id_rs2 := 0.U
  io.dbg.id_rd  := 0.U
  io.dbg.id_we  := 0.U

  io.dbg.pc_write := 1.U
  io.dbg.if_stall := 0.U
  io.dbg.id_stall := 0.U
  io.dbg.flush    := 0.U

  io.dbg.fwd_a_sel := 0.U
  io.dbg.fwd_b_sel := 0.U

  io.dbg.ex_alu_result := 0.U
  io.dbg.ex_alu_op_a   := 0.U
  io.dbg.ex_alu_op_b   := 0.U
  io.dbg.ex_pc_src     := 0.U
  io.dbg.ex_pc_jb      := 0.U
  io.dbg.ex_rd         := 0.U
  io.dbg.ex_we         := 0.U
  io.dbg.ex_mem_rd_op  := 0.U
  io.dbg.ex_mem_wr_op  := 0.U
  io.dbg.ex_mem_to_reg := 0.U

  io.dbg.mem_addr   := 0.U
  io.dbg.mem_rd_op  := 0.U
  io.dbg.mem_wr_op  := 0.U
  io.dbg.mem_wdata  := 0.U
  io.dbg.mem_rdata  := 0.U
  io.dbg.mem_rd     := 0.U
  io.dbg.mem_we     := 0.U
  io.dbg.mem_to_reg := 0.U

  io.dbg.wb_rd        := 0.U
  io.dbg.wb_we        := 0.U
  io.dbg.wb_wdata     := 0.U
  io.dbg.wb_check_res := 0.U
  io.dbg.regs         := VecInit(Seq.fill(32)(0.U(32.W)))

}
