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
  IF.io.redirect             := EX.io.redirect
  IF.io.redirectPC           := EX.io.redirectPC

  IFBarrier.io.inInstr      := IF.io.instr
  IFBarrier.io.inPC         := IF.io.pc
  IFBarrier.io.flush        := EX.io.redirect
  
  ID.io.instr               := IFBarrier.io.outInstr
  ID.io.regFileReq_A        <> regFile.io.req_1
  ID.io.regFileReq_B        <> regFile.io.req_2
  ID.io.regFileResp_A       <> regFile.io.resp_1
  ID.io.regFileResp_B       <> regFile.io.resp_2

  IDBarrier.io.inUOP         := ID.io.uop
  IDBarrier.io.inRS1         := ID.io.rs1
  IDBarrier.io.inRS2         := ID.io.rs2
  IDBarrier.io.inRD          := ID.io.rd
  IDBarrier.io.inOperandA    := ID.io.operandA
  IDBarrier.io.inOperandB    := ID.io.operandB
  IDBarrier.io.inImm         := ID.io.imm
  IDBarrier.io.inPC          := IFBarrier.io.outPC
  IDBarrier.io.inWrEn        := ID.io.wrEN
  IDBarrier.io.inXcptInvalid := ID.io.XcptInvalid
  IDBarrier.io.flush         := EX.io.redirect

  EX.io.uop                  := IDBarrier.io.outUOP
  EX.io.operandA             := IDBarrier.io.outOperandA
  EX.io.operandB             := IDBarrier.io.outOperandB
  EX.io.imm                  := IDBarrier.io.outImm
  EX.io.pc                   := IDBarrier.io.outPC
  EX.io.forwardA             := ForwardingUnit.io.forwardA
  EX.io.forwardB             := ForwardingUnit.io.forwardB
  EX.io.memResult            := EXBarrier.io.outAluResult
  EX.io.wbResult             := MEMBarrier.io.outAluResult
  EX.io.inXcptInvalid        := IDBarrier.io.outXcptInvalid

  EXBarrier.io.inRD           := IDBarrier.io.outRD
  EXBarrier.io.inWrEn         := IDBarrier.io.outWrEn
  EXBarrier.io.inAluResult    := EX.io.aluResult
  EXBarrier.io.inXcptInvalid  := EX.io.outXcptInvalid

  MEMBarrier.io.inRD          := EXBarrier.io.outRD
  MEMBarrier.io.inAluResult   := EXBarrier.io.outAluResult
  MEMBarrier.io.inWrEn        := EXBarrier.io.outWrEn
  MEMBarrier.io.inXcptInvalid := EXBarrier.io.outXcptInvalid

  WB.io.rd                  := MEMBarrier.io.outRD
  WB.io.wrEN                := MEMBarrier.io.outWrEn
  WB.io.aluResult           := MEMBarrier.io.outAluResult
  WB.io.regFileReq          <> regFile.io.req_3

  WBBarrier.io.inCheckRes    := WB.io.check_res
  WBBarrier.io.inXcptInvalid := MEMBarrier.io.outXcptInvalid

  ForwardingUnit.io.rs1_EX   := IDBarrier.io.outRS1
  ForwardingUnit.io.rs2_EX   := IDBarrier.io.outRS2
  ForwardingUnit.io.rd_MEM   := EXBarrier.io.outRD
  ForwardingUnit.io.rd_WB    := MEMBarrier.io.outRD
  ForwardingUnit.io.wrEn_MEM := EXBarrier.io.outWrEn
  ForwardingUnit.io.wrEn_WB  := MEMBarrier.io.outWrEn

  io.check_res               := WBBarrier.io.outCheckRes
  io.exception               := WBBarrier.io.outXcptInvalid

}
