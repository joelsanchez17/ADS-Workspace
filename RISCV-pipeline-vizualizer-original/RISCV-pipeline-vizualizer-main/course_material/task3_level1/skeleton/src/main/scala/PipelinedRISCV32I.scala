// ADS I Class Project
// Pipelined RISC-V Core
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 05/10/2023 by Tobias Jauch (@tojauch)

/*
This file contains the top-level module for the Pipelined RISC-V 32I core and acts as the interface between the core and external testbenches.
*/

package PipelinedRV32I

import chisel3._
import chisel3.util._

import core_tile._

class PipelinedRV32I (BinaryFile: String) extends Module {

val io = IO(new Bundle {
  val result    = Output(UInt(32.W)) 
  val coreDone  = Output(UInt(1.W))
  val gpRegVal  = Output(UInt(32.W))
  val exception = Output(Bool())

  // Web visualizer interface.
  // Do not remove or rename these signals: LivePipelineTest and the browser
  // visualizer read them directly. The core skeleton provides zero placeholders
  // for these outputs. After implementing the pipeline, connect the debug
  // outputs to the corresponding internal pipeline signals. The reference
  // solution shows one possible correct wiring.
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
  
  val core = Module(new PipelinedRV32Icore(BinaryFile))

  io.result    := core.io.check_res
  io.coreDone  := core.io.coreDone
  io.gpRegVal  := core.io.gpRegVal
  io.exception := core.io.exception
  io.dbg.if_pc   := core.io.dbg.if_pc
  io.dbg.if_inst := core.io.dbg.if_inst
  io.dbg.id_pc   := core.io.dbg.id_pc
  io.dbg.id_inst := core.io.dbg.id_inst
  io.dbg.ex_pc   := core.io.dbg.ex_pc
  io.dbg.ex_inst := core.io.dbg.ex_inst
  io.dbg.mem_pc  := core.io.dbg.mem_pc
  io.dbg.mem_inst:= core.io.dbg.mem_inst
  io.dbg.wb_pc   := core.io.dbg.wb_pc
  io.dbg.wb_inst := core.io.dbg.wb_inst

  io.dbg.id_rs1 := core.io.dbg.id_rs1
  io.dbg.id_rs2 := core.io.dbg.id_rs2
  io.dbg.id_rd  := core.io.dbg.id_rd
  io.dbg.id_we  := core.io.dbg.id_we

  io.dbg.pc_write := core.io.dbg.pc_write
  io.dbg.if_stall := core.io.dbg.if_stall
  io.dbg.id_stall := core.io.dbg.id_stall
  io.dbg.flush    := core.io.dbg.flush

  io.dbg.fwd_a_sel := core.io.dbg.fwd_a_sel
  io.dbg.fwd_b_sel := core.io.dbg.fwd_b_sel

  io.dbg.ex_alu_result := core.io.dbg.ex_alu_result
  io.dbg.ex_alu_op_a   := core.io.dbg.ex_alu_op_a
  io.dbg.ex_alu_op_b   := core.io.dbg.ex_alu_op_b
  io.dbg.ex_pc_src     := core.io.dbg.ex_pc_src
  io.dbg.ex_pc_jb      := core.io.dbg.ex_pc_jb
  io.dbg.ex_rd         := core.io.dbg.ex_rd
  io.dbg.ex_we         := core.io.dbg.ex_we
  io.dbg.ex_mem_rd_op  := core.io.dbg.ex_mem_rd_op
  io.dbg.ex_mem_wr_op  := core.io.dbg.ex_mem_wr_op
  io.dbg.ex_mem_to_reg := core.io.dbg.ex_mem_to_reg

  io.dbg.mem_addr   := core.io.dbg.mem_addr
  io.dbg.mem_rd_op  := core.io.dbg.mem_rd_op
  io.dbg.mem_wr_op  := core.io.dbg.mem_wr_op
  io.dbg.mem_wdata  := core.io.dbg.mem_wdata
  io.dbg.mem_rdata  := core.io.dbg.mem_rdata
  io.dbg.mem_rd     := core.io.dbg.mem_rd
  io.dbg.mem_we     := core.io.dbg.mem_we
  io.dbg.mem_to_reg := core.io.dbg.mem_to_reg

  io.dbg.wb_rd        := core.io.dbg.wb_rd
  io.dbg.wb_we        := core.io.dbg.wb_we
  io.dbg.wb_wdata     := core.io.dbg.wb_wdata
  io.dbg.wb_check_res := core.io.dbg.wb_check_res
  io.dbg.regs         := core.io.dbg.regs

}
